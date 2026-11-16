package app.nanotv.tv.data.remote

import android.util.Xml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder

@Serializable
data class OEmbedResponse(
    val title: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_url") val authorUrl: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = ""
)

data class ParsedPublicFeed(
    val channelId: String,
    val channelTitle: String,
    val entries: List<ParsedFeedEntry>
)

data class ParsedFeedEntry(
    val videoId: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val authorName: String,
    val authorChannelId: String
)

object KeylessYouTubeClient {

    private val json = Json { ignoreUnknownKeys = true }

    // Known popular @handles mapped for instant keyless fallback when backend URL isn't deployed yet
    private val wellKnownHandles = mapOf(
        "@scishowkids" to ("UCRFIPG2u1DxKLNuE3y2SjHA" to "SciShow Kids"),
        "@peekaboo_kidz" to ("UCxlJ45KjG4XVcQ_hd8j227A" to "Peekaboo Kidz"),
        "@natgeokids" to ("UCXVCgDuD_QCkI7gTKU7-tpg" to "Nat Geo Kids"),
        "@markrober" to ("UCY1kMZp36IQSyNx_9h4mpCg" to "Mark Rober"),
        "@kurzgesagt" to ("UCsXVk37bltHxD1rDPwtNM8Q" to "Kurzgesagt – In a Nutshell"),
        "@veritasium" to ("UCHnyfMqiRRG1u-2MsSQLbXA" to "Veritasium"),
        "@peppapigofficial" to ("UCAOtE1V7Ots4DjM8JLlrYgg" to "Peppa Pig - Official Channel"),
        "@artforkidshub" to ("UC5XMF3Inoi8R9nSI8ChOsdQ" to "Art for Kids Hub"),
        "@numberblocks" to ("UCPlwvN0w4qFSP1FllALB92w" to "Numberblocks"),
        "@teded" to ("UCsooa4yRKGN_zEE8iknghZA" to "TED-Ed"),
        "@smartereveryday" to ("UC6107grRI4m0o2-emgoDnAA" to "SmarterEveryDay"),
        "@nasa" to ("UCLA_DiR1FfKNvjuUpBHmylQ" to "NASA")
    )

    fun resolveHandleKeyless(client: OkHttpClient, handle: String): Pair<String, String>? {
        val norm = (if (handle.startsWith("@")) handle else "@$handle").lowercase()
        wellKnownHandles[norm]?.let { return it }

        // Resolve arbitrary @handle via public channel page canonical/externalId meta tag
        return try {
            val url = "https://www.youtube.com/$norm"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string() ?: return null
                val idMatch = Regex(""""externalId":"(UC[A-Za-z0-9_-]{20,})"""").find(html)
                    ?: Regex("""channel/(UC[A-Za-z0-9_-]{20,})""").find(html)
                val cid = idMatch?.groupValues?.getOrNull(1) ?: return null
                val titleMatch = Regex("""<meta property="og:title" content="([^"]+)"""").find(html)
                val title = titleMatch?.groupValues?.getOrNull(1) ?: handle
                cid to title
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchOEmbedVideo(client: OkHttpClient, videoId: String): OEmbedResponse? {
        val watchUrl = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
        val url = "https://www.youtube.com/oembed?url=$watchUrl&format=json"
        val req = Request.Builder().url(url).build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                json.decodeFromString<OEmbedResponse>(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchChannelFeed(client: OkHttpClient, channelId: String): ParsedPublicFeed? {
        val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "NanoTV/1.0 (Android TV)")
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parseAtomXml(body, fallbackChannelId = channelId)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchPlaylistFeed(client: OkHttpClient, playlistId: String): ParsedPublicFeed? {
        val url = "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "NanoTV/1.0 (Android TV)")
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parseAtomXml(body, fallbackChannelId = playlistId)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAtomXml(xmlContent: String, fallbackChannelId: String): ParsedPublicFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xmlContent))

        var feedTitle = ""
        var feedChannelId = fallbackChannelId

        var insideEntry = false
        var insideAuthor = false
        var entryVideoId = ""
        var entryTitle = ""
        var entryPublished = ""
        var entryDescription = ""
        var entryThumb = ""
        var entryAuthorName = ""
        var entryAuthorUri = ""

        val entries = mutableListOf<ParsedFeedEntry>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.orEmpty()
                    when {
                        name == "entry" -> {
                            insideEntry = true
                            entryVideoId = ""
                            entryTitle = ""
                            entryPublished = ""
                            entryDescription = ""
                            entryThumb = ""
                            entryAuthorName = ""
                            entryAuthorUri = ""
                        }
                        name == "author" -> insideAuthor = true
                        !insideEntry && !insideAuthor && name == "title" -> {
                            feedTitle = parser.nextText().trim()
                        }
                        !insideEntry && name == "channelId" -> {
                            val raw = parser.nextText().trim()
                            if (raw.isNotEmpty()) {
                                feedChannelId = if (raw.startsWith("UC")) raw else "UC$raw"
                            }
                        }
                        insideEntry && name == "videoId" -> entryVideoId = parser.nextText().trim()
                        insideEntry && !insideAuthor && name == "title" && entryTitle.isEmpty() -> {
                            entryTitle = parser.nextText().trim()
                        }
                        insideEntry && insideAuthor && name == "name" -> {
                            entryAuthorName = parser.nextText().trim()
                        }
                        insideEntry && insideAuthor && name == "uri" -> {
                            entryAuthorUri = parser.nextText().trim()
                        }
                        insideEntry && name == "published" -> entryPublished = parser.nextText().trim()
                        insideEntry && name == "description" -> entryDescription = parser.nextText().trim()
                        insideEntry && name == "thumbnail" -> {
                            val attrUrl = parser.getAttributeValue(null, "url")
                            if (!attrUrl.isNullOrBlank()) entryThumb = attrUrl
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.orEmpty()
                    if (name == "author") {
                        insideAuthor = false
                    } else if (name == "entry") {
                        if (entryVideoId.isNotBlank() && entryTitle.isNotBlank()) {
                            val authorCid = entryAuthorUri.substringAfter("/channel/", "").ifBlank { feedChannelId }
                            entries += ParsedFeedEntry(
                                videoId = entryVideoId,
                                title = entryTitle,
                                description = entryDescription,
                                publishedAt = entryPublished,
                                thumbnailUrl = entryThumb.ifBlank {
                                    "https://i.ytimg.com/vi/$entryVideoId/hqdefault.jpg"
                                },
                                authorName = entryAuthorName.ifBlank { feedTitle },
                                authorChannelId = authorCid
                            )
                        }
                        insideEntry = false
                    }
                }
            }
            eventType = parser.next()
        }

        return ParsedPublicFeed(
            channelId = feedChannelId,
            channelTitle = feedTitle,
            entries = entries
        )
    }
}
