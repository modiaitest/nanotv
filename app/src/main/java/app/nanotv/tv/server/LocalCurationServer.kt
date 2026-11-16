package app.nanotv.tv.server

import android.content.Context
import app.nanotv.tv.data.repository.AddSourceResult
import app.nanotv.tv.data.repository.CatalogRepository
import app.nanotv.tv.data.repository.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

@Serializable
private data class PortalSourceDto(
    val sourceId: String,
    val sourceType: String,
    val targetId: String,
    val shelfTitle: String
)

@Serializable
private data class PortalVideoDto(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val durationFormatted: String,
    val thumbnailUrl: String
)

@Serializable
private data class PortalStateResponse(
    val sources: List<PortalSourceDto>,
    val totalApprovedVideos: Int,
    val recentVideos: List<PortalVideoDto>
)

@Serializable
private data class PinVerifyRequest(val pin: String = "")

@Serializable
private data class AddRequestDto(val input: String = "", val shelfTitle: String = "")

@Serializable
private data class SourceIdRequestDto(val sourceId: String = "")

@Serializable
private data class VideoIdRequestDto(val videoId: String = "")

@Serializable
private data class ApiActionResponse(val ok: Boolean, val message: String)

class LocalCurationServer(
    private val context: Context,
    private val repository: CatalogRepository,
    val port: Int = 8485
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val portalHtml: String by lazy {
        context.assets.open("portal.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun start() {
        if (acceptJob?.isActive == true) return
        acceptJob = scope.launch {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress("0.0.0.0", port))
                serverSocket = socket

                while (isActive && !socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    launch { handleClient(client) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        client.use { sock ->
            sock.soTimeout = 20_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val path = parts[1].substringBefore("?")

            var contentLength = 0
            var headerPin = ""
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                } else if (header.startsWith("X-Parent-Pin:", ignoreCase = true)) {
                    headerPin = header.substringAfter(":").trim()
                }
            }

            val body = if (contentLength > 0 && contentLength < 65_536) {
                val buf = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val n = reader.read(buf, totalRead, contentLength - totalRead)
                    if (n <= 0) break
                    totalRead += n
                }
                String(buf, 0, totalRead)
            } else {
                ""
            }

            if (method == "GET" && (path == "/" || path == "/index.html")) {
                writeResponse(sock, 200, "text/html; charset=utf-8", portalHtml)
                return
            }

            if (method == "POST" && path == "/api/verify-pin") {
                val req = runCatching { json.decodeFromString<PinVerifyRequest>(body) }
                    .getOrDefault(PinVerifyRequest())
                val valid = repository.verifyPin(req.pin)
                writeResponse(
                    sock,
                    200,
                    "application/json; charset=utf-8",
                    json.encodeToString(ApiActionResponse(valid, if (valid) "Unlocked" else "Invalid PIN"))
                )
                return
            }

            // Require valid Parent PIN on all curation read/write endpoints
            if (!repository.verifyPin(headerPin)) {
                writeResponse(
                    sock,
                    401,
                    "application/json; charset=utf-8",
                    json.encodeToString(ApiActionResponse(false, "Parent PIN required"))
                )
                return
            }

            when {
                method == "GET" && path == "/api/state" -> {
                    val sources = repository.getCuratedSourcesSnapshot().map {
                        PortalSourceDto(it.sourceId, it.sourceType, it.targetId, it.shelfTitle)
                    }
                    val allVideos = repository.getApprovedVideosSnapshot()
                    val payload = PortalStateResponse(
                        sources = sources,
                        totalApprovedVideos = allVideos.size,
                        recentVideos = allVideos.take(80).map {
                            PortalVideoDto(it.videoId, it.title, it.channelTitle, it.durationFormatted, it.thumbnailUrl)
                        }
                    )
                    writeResponse(sock, 200, "application/json; charset=utf-8", json.encodeToString(payload))
                }

                method == "POST" && path == "/api/add" -> {
                    val req = runCatching { json.decodeFromString<AddRequestDto>(body) }
                        .getOrDefault(AddRequestDto())
                    val resp = when (val res = repository.addCuratedInput(req.input, req.shelfTitle)) {
                        is AddSourceResult.Added -> ApiActionResponse(true, res.summary)
                        is AddSourceResult.Rejected -> ApiActionResponse(false, res.reason)
                    }
                    writeResponse(sock, 200, "application/json; charset=utf-8", json.encodeToString(resp))
                }

                method == "POST" && path == "/api/remove" -> {
                    val req = runCatching { json.decodeFromString<SourceIdRequestDto>(body) }
                        .getOrDefault(SourceIdRequestDto())
                    if (req.sourceId.isNotBlank()) repository.removeCuratedSource(req.sourceId)
                    writeResponse(sock, 200, "application/json; charset=utf-8", json.encodeToString(ApiActionResponse(true, "Removed")))
                }

                method == "POST" && path == "/api/hide-video" -> {
                    val req = runCatching { json.decodeFromString<VideoIdRequestDto>(body) }
                        .getOrDefault(VideoIdRequestDto())
                    if (req.videoId.isNotBlank()) repository.markVideoBlockedAtRuntime(req.videoId)
                    writeResponse(sock, 200, "application/json; charset=utf-8", json.encodeToString(ApiActionResponse(true, "Hidden")))
                }

                method == "POST" && path == "/api/sync" -> {
                    val resp = when (val res = repository.syncCatalog()) {
                        is SyncResult.Success -> ApiActionResponse(true, "Synced ${res.playlistCount} shelves & ${res.videoCount} videos")
                        is SyncResult.Error -> ApiActionResponse(false, res.message)
                    }
                    writeResponse(sock, 200, "application/json; charset=utf-8", json.encodeToString(resp))
                }

                else -> writeResponse(sock, 404, "text/plain; charset=utf-8", "Not found")
            }
        }
    }

    private fun writeResponse(socket: Socket, status: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = when (status) {
            200 -> "OK"
            401 -> "Unauthorized"
            else -> "Not Found"
        }
        val headers = buildString {
            append("HTTP/1.1 $status $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

        val out = socket.getOutputStream()
        out.write(headers)
        out.write(bytes)
        out.flush()
    }

    companion object {
        fun findLocalIpv4(): String? {
            return try {
                val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                val candidates = mutableListOf<Pair<Int, String>>()
                for (nif in interfaces) {
                    if (!nif.isUp || nif.isLoopback) continue
                    val name = nif.name.lowercase()
                    for (addr in nif.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                            val ip = addr.hostAddress ?: continue
                            val priority = when {
                                name.startsWith("wlan") || name.startsWith("en") -> 0
                                name.startsWith("eth") -> 1
                                else -> 2
                            }
                            candidates += priority to ip
                        }
                    }
                }
                candidates.minByOrNull { it.first }?.second
            } catch (_: Exception) {
                null
            }
        }
    }
}
