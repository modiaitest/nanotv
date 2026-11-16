package app.nanotv.tv.data.repository

import app.nanotv.tv.BuildConfig
import app.nanotv.tv.data.local.AppPreferences
import app.nanotv.tv.data.local.AppSettingsState
import app.nanotv.tv.data.local.CatalogDao
import app.nanotv.tv.data.local.ChannelEntity
import app.nanotv.tv.data.local.ChannelSummaryModel
import app.nanotv.tv.data.local.CuratedSourceEntity
import app.nanotv.tv.data.local.PlaylistEntity
import app.nanotv.tv.data.local.PlaylistVideoCrossRef
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.data.local.VideoEntity
import app.nanotv.tv.data.local.WatchStateEntity
import app.nanotv.tv.data.remote.BackendResolveHandleResponse
import app.nanotv.tv.data.remote.BackendSourceItem
import app.nanotv.tv.data.remote.BackendSyncRequest
import app.nanotv.tv.data.remote.BackendSyncResponse
import app.nanotv.tv.data.remote.DurationFormatter
import app.nanotv.tv.data.remote.KeylessYouTubeClient
import app.nanotv.tv.data.remote.ParentSearchVideoItem
import app.nanotv.tv.data.remote.ParsedYouTubeInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

data class StarterChannelPack(
    val channelId: String,
    val title: String,
    val subtitle: String
)

sealed interface SyncResult {
    data class Success(val playlistCount: Int, val videoCount: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

sealed interface AddSourceResult {
    data class Added(val summary: String) : AddSourceResult
    data class Rejected(val reason: String) : AddSourceResult
}

class CatalogRepository(
    private val dao: CatalogDao,
    private val httpClient: OkHttpClient,
    private val preferences: AppPreferences
) {
    private val syncMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val curatedSourcesFlow: Flow<List<CuratedSourceEntity>> = dao.observeCuratedSources()
    val playlistsFlow: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val allVideosFlow: Flow<List<VideoCardModel>> = dao.observeAllApprovedVideos()
    val crossRefsFlow: Flow<List<PlaylistVideoCrossRef>> = dao.observeAllCrossRefs()
    val continueWatchingFlow: Flow<List<VideoCardModel>> = dao.observeContinueWatching()
    val watchHistoryFlow: Flow<List<VideoCardModel>> = dao.observeWatchHistory()
    val likedVideosFlow: Flow<List<VideoCardModel>> = dao.observeLikedVideos()
    val watchLaterFlow: Flow<List<VideoCardModel>> = dao.observeWatchLaterVideos()
    val channelsFlow: Flow<List<ChannelSummaryModel>> = dao.observeApprovedChannels()
    val settingsFlow: Flow<AppSettingsState> = preferences.settingsFlow

    private val backendBaseUrl: String
        get() = BuildConfig.NANO_BACKEND_URL.trim().trimEnd('/')

    val isCloudProxyConfigured: Boolean
        get() = backendBaseUrl.isNotBlank()

    fun observeVideosForPlaylist(playlistId: String): Flow<List<VideoCardModel>> =
        dao.observeVideosForPlaylist(playlistId)

    fun observeVideosForChannel(channelId: String): Flow<List<VideoCardModel>> =
        dao.observeVideosForChannel(channelId)

    fun searchApprovedVideos(query: String): Flow<List<VideoCardModel>> =
        dao.searchApprovedVideos(query.trim())

    companion object {
        val OptionalStarterPacks = listOf(
            StarterChannelPack(
                channelId = "UCRFIPG2u1DxKLNuE3y2SjHA",
                title = "SciShow Kids",
                subtitle = "Hands-on science, animals & space"
            ),
            StarterChannelPack(
                channelId = "UCxlJ45KjG4XVcQ_hd8j227A",
                title = "Peekaboo Kidz (Dr. Binocs)",
                subtitle = "Animated how-things-work explainers"
            ),
            StarterChannelPack(
                channelId = "UCXVCgDuD_QCkI7gTKU7-tpg",
                title = "Nat Geo Kids",
                subtitle = "Wildlife, dinosaurs & nature"
            ),
            StarterChannelPack(
                channelId = "UCY1kMZp36IQSyNx_9h4mpCg",
                title = "Mark Rober",
                subtitle = "Creative engineering & STEM builds"
            ),
            StarterChannelPack(
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                title = "Kurzgesagt – In a Nutshell",
                subtitle = "Animated astronomy & science stories"
            ),
            StarterChannelPack(
                channelId = "UC5XMF3Inoi8R9nSI8ChOsdQ",
                title = "Art for Kids Hub",
                subtitle = "Step-by-step family drawing lessons"
            )
        )
    }

    suspend fun completeOnboarding(
        pin: String,
        familySpaceName: String,
        selectedStarterChannelIds: Set<String>
    ): SyncResult = withContext(Dispatchers.IO) {
        preferences.completeParentOnboarding(pin = pin, profileName = familySpaceName)
        val now = System.currentTimeMillis()
        val chosen = OptionalStarterPacks
            .filter { it.channelId in selectedStarterChannelIds }
            .mapIndexed { idx, pack ->
                CuratedSourceEntity(
                    sourceId = "ch:${pack.channelId}",
                    sourceType = "CHANNEL",
                    targetId = pack.channelId,
                    shelfTitle = pack.title,
                    sortOrder = idx + 1,
                    addedEpochMs = now
                )
            }
        if (chosen.isNotEmpty()) {
            dao.insertCuratedSources(chosen)
            syncCatalog()
        } else {
            preferences.updateSyncResult(now, "Ready — scan QR in Parent Curate to add channels or videos")
            SyncResult.Success(0, 0)
        }
    }

    suspend fun verifyPin(pin: String): Boolean = preferences.verifyParentPin(pin)

    suspend fun changeParentPin(newPin: String) = preferences.updateParentPin(newPin)

    suspend fun getCuratedSourcesSnapshot(): List<CuratedSourceEntity> = withContext(Dispatchers.IO) {
        dao.getCuratedSources()
    }

    suspend fun getApprovedVideosSnapshot(): List<VideoCardModel> = withContext(Dispatchers.IO) {
        dao.getAllApprovedVideosSnapshot()
    }

    suspend fun addCuratedInput(
        rawInput: String,
        customShelfTitle: String? = null
    ): AddSourceResult = withContext(Dispatchers.IO) {
        when (val parsed = DurationFormatter.parseYouTubeInput(rawInput)) {
            ParsedYouTubeInput.RejectedShort -> {
                AddSourceResult.Rejected("YouTube Shorts are disabled in NanoTV. Please add a full-length video, playlist, or channel.")
            }
            is ParsedYouTubeInput.Invalid -> {
                AddSourceResult.Rejected(parsed.reason)
            }
            is ParsedYouTubeInput.Video -> {
                val oembed = KeylessYouTubeClient.fetchOEmbedVideo(httpClient, parsed.videoId)
                    ?: return@withContext AddSourceResult.Rejected("That video is unavailable or has embedded playback disabled.")
                if (DurationFormatter.isShortVideo(0L, oembed.title)) {
                    return@withContext AddSourceResult.Rejected("Shorts clips are disabled in NanoTV.")
                }
                val shelf = customShelfTitle?.trim()?.takeIf { it.isNotEmpty() } ?: "Parent Approved Picks"
                dao.insertCuratedSource(
                    CuratedSourceEntity(
                        sourceId = "vid:${parsed.videoId}",
                        sourceType = "VIDEO",
                        targetId = parsed.videoId,
                        shelfTitle = shelf,
                        sortOrder = 0,
                        addedEpochMs = System.currentTimeMillis()
                    )
                )
                syncCatalog()
                AddSourceResult.Added("Added \"${oembed.title}\" to \"$shelf\"!")
            }
            is ParsedYouTubeInput.Playlist -> {
                val label = customShelfTitle?.trim()?.takeIf { it.isNotEmpty() }
                    ?: KeylessYouTubeClient.fetchPlaylistFeed(httpClient, parsed.playlistId)?.channelTitle
                    ?: "Approved Playlist"
                val nextOrder = dao.maxSourceSortOrder() + 1
                dao.insertCuratedSource(
                    CuratedSourceEntity(
                        sourceId = "pl:${parsed.playlistId}",
                        sourceType = "PLAYLIST",
                        targetId = parsed.playlistId,
                        shelfTitle = label,
                        sortOrder = nextOrder,
                        addedEpochMs = System.currentTimeMillis()
                    )
                )
                syncCatalog()
                AddSourceResult.Added("Added playlist \"$label\"!")
            }
            is ParsedYouTubeInput.ChannelId -> {
                val label = customShelfTitle?.trim()?.takeIf { it.isNotEmpty() }
                    ?: KeylessYouTubeClient.fetchChannelFeed(httpClient, parsed.channelId)?.channelTitle
                    ?: "Approved Channel"
                val nextOrder = dao.maxSourceSortOrder() + 1
                dao.insertCuratedSource(
                    CuratedSourceEntity(
                        sourceId = "ch:${parsed.channelId}",
                        sourceType = "CHANNEL",
                        targetId = parsed.channelId,
                        shelfTitle = label,
                        sortOrder = nextOrder,
                        addedEpochMs = System.currentTimeMillis()
                    )
                )
                syncCatalog()
                AddSourceResult.Added("Added channel \"$label\"!")
            }
            is ParsedYouTubeInput.ChannelHandle -> {
                val resolved = resolveHandle(parsed.handle)
                    ?: return@withContext AddSourceResult.Rejected("Could not locate YouTube channel ${parsed.handle}.")
                val label = customShelfTitle?.trim()?.takeIf { it.isNotEmpty() } ?: resolved.second
                val nextOrder = dao.maxSourceSortOrder() + 1
                dao.insertCuratedSource(
                    CuratedSourceEntity(
                        sourceId = "ch:${resolved.first}",
                        sourceType = "CHANNEL",
                        targetId = resolved.first,
                        shelfTitle = label,
                        sortOrder = nextOrder,
                        addedEpochMs = System.currentTimeMillis()
                    )
                )
                syncCatalog()
                AddSourceResult.Added("Added channel \"$label\"!")
            }
        }
    }

    private fun resolveHandle(handle: String): Pair<String, String>? {
        if (isCloudProxyConfigured) {
            runCatching {
                val encoded = URLEncoder.encode(handle, "UTF-8")
                val req = Request.Builder().url("$backendBaseUrl/v1/resolve-handle?handle=$encoded").build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val dto = json.decodeFromString<BackendResolveHandleResponse>(resp.body?.string().orEmpty())
                        if (dto.ok && dto.channelId.isNotBlank()) {
                            return dto.channelId to dto.title.ifBlank { handle }
                        }
                    }
                }
            }
        }
        return KeylessYouTubeClient.resolveHandleKeyless(httpClient, handle)
    }

    suspend fun searchViaCloudProxy(query: String): List<ParentSearchVideoItem> = withContext(Dispatchers.IO) {
        if (!isCloudProxyConfigured || query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val req = Request.Builder().url("$backendBaseUrl/v1/search?q=$encoded").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                json.decodeFromString<List<ParentSearchVideoItem>>(resp.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun removeCuratedSource(sourceId: String): SyncResult = withContext(Dispatchers.IO) {
        dao.deleteCuratedSource(sourceId)
        syncCatalog()
    }

    suspend fun syncCatalog(): SyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val sources = dao.getCuratedSources()
            if (sources.isEmpty()) {
                dao.replaceSyncedCatalog(emptyList(), emptyList(), emptyList(), emptyList())
                preferences.updateSyncResult(
                    System.currentTimeMillis(),
                    "No channels added yet — open Parent Curate (QR) to add channels or playlists"
                )
                return@withContext SyncResult.Success(0, 0)
            }

            if (isCloudProxyConfigured) {
                val proxyRes = syncViaCloudProxy(sources)
                if (proxyRes != null) return@withContext proxyRes
            }

            syncViaKeylessPublicFeeds(sources)
        }
    }

    private suspend fun syncViaCloudProxy(sources: List<CuratedSourceEntity>): SyncResult? {
        return try {
            val payload = BackendSyncRequest(
                sources = sources.map {
                    BackendSourceItem(
                        sourceId = it.sourceId,
                        sourceType = it.sourceType,
                        targetId = it.targetId,
                        shelfTitle = it.shelfTitle
                    )
                }
            )
            val body = json.encodeToString(payload).toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$backendBaseUrl/v1/sync")
                .post(body)
                .build()

            val dto = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                json.decodeFromString<BackendSyncResponse>(resp.body?.string().orEmpty())
            }
            if (!dto.ok) return null

            val playlists = dto.playlists.mapIndexed { idx, pl ->
                PlaylistEntity(
                    playlistId = pl.playlistId,
                    title = pl.title,
                    description = pl.description,
                    thumbnailUrl = pl.thumbnailUrl,
                    itemCount = pl.videoIds.size,
                    sortOrder = idx,
                    colorIndex = idx % 6
                )
            }
            val crossRefs = dto.playlists.flatMap { pl ->
                pl.videoIds.mapIndexed { pos, vid ->
                    PlaylistVideoCrossRef(playlistId = pl.playlistId, videoId = vid, position = pos)
                }
            }
            val videos = dto.videos.map { v ->
                VideoEntity(
                    videoId = v.videoId,
                    title = v.title,
                    description = v.description,
                    thumbnailUrl = v.thumbnailUrl,
                    channelId = v.channelId,
                    channelTitle = v.channelTitle,
                    channelAvatarUrl = v.channelAvatarUrl,
                    durationSeconds = v.durationSeconds,
                    durationFormatted = v.durationFormatted,
                    publishedAt = v.publishedAt,
                    isEmbeddable = true,
                    isBlockedAtRuntime = false
                )
            }
            val channels = dto.channels.map { c ->
                ChannelEntity(
                    channelId = c.channelId,
                    title = c.title,
                    description = c.description,
                    avatarUrl = c.avatarUrl,
                    bannerUrl = c.bannerUrl
                )
            }

            dao.replaceSyncedCatalog(playlists, videos, crossRefs, channels)
            val msg = "Synced ${playlists.size} shelves & ${videos.size} full-length videos"
            preferences.updateSyncResult(System.currentTimeMillis(), msg)
            SyncResult.Success(playlists.size, videos.size)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun syncViaKeylessPublicFeeds(sources: List<CuratedSourceEntity>): SyncResult {
        try {
            val playlists = mutableListOf<PlaylistEntity>()
            val videos = LinkedHashMap<String, VideoEntity>()
            val crossRefs = mutableListOf<PlaylistVideoCrossRef>()
            val channels = LinkedHashMap<String, ChannelEntity>()

            // 1. Hand-picked individual videos via keyless oEmbed
            val videoSources = sources.filter { it.sourceType == "VIDEO" }
            videoSources.groupBy { it.shelfTitle.ifBlank { "Parent Approved Picks" } }
                .entries
                .forEachIndexed { idx, (shelfName, list) ->
                    val accepted = mutableListOf<String>()
                    for (src in list.sortedByDescending { it.addedEpochMs }) {
                        val oem = KeylessYouTubeClient.fetchOEmbedVideo(httpClient, src.targetId) ?: continue
                        if (DurationFormatter.isShortVideo(0L, oem.title)) continue
                        val cid = oem.authorUrl.substringAfter("/channel/", "").ifBlank { "creator_${oem.authorName.hashCode()}" }
                        val thumb = oem.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/${src.targetId}/hqdefault.jpg" }
                        videos[src.targetId] = VideoEntity(
                            videoId = src.targetId,
                            title = oem.title,
                            description = "",
                            thumbnailUrl = thumb,
                            channelId = cid,
                            channelTitle = oem.authorName.ifBlank { "Creator" },
                            channelAvatarUrl = thumb,
                            durationSeconds = 0L,
                            durationFormatted = "",
                            publishedAt = ""
                        )
                        channels.putIfAbsent(
                            cid,
                            ChannelEntity(
                                channelId = cid,
                                title = oem.authorName.ifBlank { "Creator" },
                                description = "",
                                avatarUrl = thumb,
                                bannerUrl = thumb
                            )
                        )
                        accepted += src.targetId
                    }
                    if (accepted.isNotEmpty()) {
                        val plId = "vid_shelf_$idx"
                        accepted.forEachIndexed { pos, vid ->
                            crossRefs += PlaylistVideoCrossRef(plId, vid, pos)
                        }
                        playlists += PlaylistEntity(
                            playlistId = plId,
                            title = shelfName,
                            description = "Parent-selected videos",
                            thumbnailUrl = videos[accepted.first()]?.thumbnailUrl.orEmpty(),
                            itemCount = accepted.size,
                            sortOrder = playlists.size,
                            colorIndex = playlists.size % 6
                        )
                    }
                }

            // 2. Channels & Playlists via official YouTube Atom feeds
            for (src in sources.filter { it.sourceType != "VIDEO" }) {
                val feed = when (src.sourceType) {
                    "CHANNEL" -> KeylessYouTubeClient.fetchChannelFeed(httpClient, src.targetId)
                    "PLAYLIST" -> KeylessYouTubeClient.fetchPlaylistFeed(httpClient, src.targetId)
                    else -> null
                } ?: continue

                val filtered = feed.entries.filter { e ->
                    !DurationFormatter.isShortVideo(0L, e.title, e.description) &&
                        !e.title.contains("🔴") &&
                        !e.title.contains("LIVE:", ignoreCase = true)
                }
                if (filtered.isEmpty()) continue

                val plId = "${src.sourceType.lowercase()}_${src.targetId}"
                filtered.forEachIndexed { pos, entry ->
                    val cid = entry.authorChannelId.ifBlank { feed.channelId }
                    val cTitle = entry.authorName.ifBlank { feed.channelTitle }
                    videos[entry.videoId] = VideoEntity(
                        videoId = entry.videoId,
                        title = entry.title,
                        description = entry.description,
                        thumbnailUrl = entry.thumbnailUrl,
                        channelId = cid,
                        channelTitle = cTitle,
                        channelAvatarUrl = filtered.first().thumbnailUrl,
                        durationSeconds = 0L,
                        durationFormatted = "",
                        publishedAt = entry.publishedAt
                    )
                    channels.putIfAbsent(
                        cid,
                        ChannelEntity(
                            channelId = cid,
                            title = cTitle,
                            description = "",
                            avatarUrl = filtered.first().thumbnailUrl,
                            bannerUrl = filtered.first().thumbnailUrl
                        )
                    )
                    crossRefs += PlaylistVideoCrossRef(plId, entry.videoId, pos)
                }

                playlists += PlaylistEntity(
                    playlistId = plId,
                    title = src.shelfTitle.ifBlank { feed.channelTitle },
                    description = "Parent-curated shelf",
                    thumbnailUrl = filtered.first().thumbnailUrl,
                    itemCount = filtered.size,
                    sortOrder = playlists.size,
                    colorIndex = playlists.size % 6
                )
            }

            dao.replaceSyncedCatalog(
                playlists = playlists,
                videos = videos.values.toList(),
                crossRefs = crossRefs,
                channels = channels.values.toList()
            )
            val msg = "Synced ${playlists.size} shelves & ${videos.size} videos"
            preferences.updateSyncResult(System.currentTimeMillis(), msg)
            return SyncResult.Success(playlists.size, videos.size)
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Network error while syncing shelves"
            preferences.updateSyncResult(System.currentTimeMillis(), "Sync error: $err")
            return SyncResult.Error(err)
        }
    }

    suspend fun updateWatchProgress(videoId: String, progressSeconds: Float, durationSeconds: Float) =
        withContext(Dispatchers.IO) {
            val existing = dao.getWatchState(videoId)
            val now = System.currentTimeMillis()
            val updated = (existing ?: WatchStateEntity(videoId = videoId)).copy(
                progressSeconds = progressSeconds.coerceAtLeast(0f),
                durationSeconds = if (durationSeconds > 0f) durationSeconds else (existing?.durationSeconds ?: 0f),
                lastWatchedEpochMs = now,
                watchCount = if (existing == null || existing.lastWatchedEpochMs == 0L) 1 else existing.watchCount
            )
            dao.upsertWatchState(updated)
        }

    suspend fun markVideoCompleted(videoId: String, durationSeconds: Float) = withContext(Dispatchers.IO) {
        val existing = dao.getWatchState(videoId)
        val now = System.currentTimeMillis()
        val updated = (existing ?: WatchStateEntity(videoId = videoId)).copy(
            progressSeconds = durationSeconds.coerceAtLeast(0f),
            durationSeconds = durationSeconds.coerceAtLeast(0f),
            lastWatchedEpochMs = now,
            watchCount = (existing?.watchCount ?: 0) + 1
        )
        dao.upsertWatchState(updated)
    }

    suspend fun toggleLike(videoId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getWatchState(videoId) ?: WatchStateEntity(videoId = videoId)
        dao.upsertWatchState(existing.copy(isLiked = !existing.isLiked))
    }

    suspend fun toggleWatchLater(videoId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getWatchState(videoId) ?: WatchStateEntity(videoId = videoId)
        dao.upsertWatchState(existing.copy(isWatchLater = !existing.isWatchLater))
    }

    suspend fun markVideoBlockedAtRuntime(videoId: String) = withContext(Dispatchers.IO) {
        dao.markVideoBlockedAtRuntime(videoId)
    }

    suspend fun restoreHiddenVideos() = withContext(Dispatchers.IO) {
        dao.restoreHiddenVideos()
    }

    suspend fun clearWatchHistory() = withContext(Dispatchers.IO) {
        dao.clearWatchHistory()
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        preferences.setAutoplayEnabled(enabled)
    }
}
