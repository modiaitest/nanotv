package app.nanotv.tv.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class BackendSourceItem(
    val sourceId: String,
    val sourceType: String,
    val targetId: String,
    val shelfTitle: String
)

@Serializable
data class BackendSyncRequest(
    val sources: List<BackendSourceItem>
)

@Serializable
data class BackendPlaylistDto(
    val playlistId: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoIds: List<String> = emptyList()
)

@Serializable
data class BackendVideoDto(
    val videoId: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val channelAvatarUrl: String = "",
    val durationSeconds: Long = 0L,
    val durationFormatted: String = "",
    val publishedAt: String = ""
)

@Serializable
data class BackendChannelDto(
    val channelId: String,
    val title: String,
    val description: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = ""
)

@Serializable
data class BackendSyncResponse(
    val ok: Boolean = false,
    val playlists: List<BackendPlaylistDto> = emptyList(),
    val videos: List<BackendVideoDto> = emptyList(),
    val channels: List<BackendChannelDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class BackendResolveHandleResponse(
    val ok: Boolean = false,
    val channelId: String = "",
    val title: String = "",
    val error: String? = null
)

@Serializable
data class ParentSearchVideoItem(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val durationFormatted: String = "",
    val thumbnailUrl: String
)
