package app.nanotv.tv.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "curated_sources")
data class CuratedSourceEntity(
    @PrimaryKey val sourceId: String,
    val sourceType: String, // "CHANNEL", "PLAYLIST", "VIDEO"
    val targetId: String,
    val shelfTitle: String,
    val sortOrder: Int,
    val addedEpochMs: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val itemCount: Int,
    val sortOrder: Int,
    val colorIndex: Int
)

@Entity(
    tableName = "videos",
    indices = [Index("channelId"), Index("isEmbeddable")]
)
data class VideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String,
    val durationSeconds: Long,
    val durationFormatted: String,
    val publishedAt: String,
    val isEmbeddable: Boolean = true,
    val isBlockedAtRuntime: Boolean = false
)

@Entity(
    tableName = "playlist_video_cross_ref",
    primaryKeys = ["playlistId", "videoId"],
    indices = [Index("videoId"), Index("playlistId")]
)
data class PlaylistVideoCrossRef(
    val playlistId: String,
    val videoId: String,
    val position: Int
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val channelId: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String
)

@Entity(tableName = "watch_states")
data class WatchStateEntity(
    @PrimaryKey val videoId: String,
    val progressSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val lastWatchedEpochMs: Long = 0L,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val watchCount: Int = 0
)

data class VideoCardModel(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String,
    val durationSeconds: Long,
    val durationFormatted: String,
    val publishedAt: String,
    @ColumnInfo(name = "progressSeconds") val progressSeconds: Float?,
    @ColumnInfo(name = "stateDurationSeconds") val stateDurationSeconds: Float?,
    @ColumnInfo(name = "lastWatchedEpochMs") val lastWatchedEpochMs: Long?,
    @ColumnInfo(name = "isLiked") val isLiked: Boolean?,
    @ColumnInfo(name = "isWatchLater") val isWatchLater: Boolean?
) {
    val progressFraction: Float
        get() {
            val prog = progressSeconds ?: 0f
            val total = when {
                (stateDurationSeconds ?: 0f) > 0f -> stateDurationSeconds ?: 0f
                durationSeconds > 0L -> durationSeconds.toFloat()
                else -> 0f
            }
            return if (total > 0f && prog > 2f) (prog / total).coerceIn(0f, 1f) else 0f
        }

    val liked: Boolean get() = isLiked == true
    val watchLater: Boolean get() = isWatchLater == true
}

data class ChannelSummaryModel(
    val channelId: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val videoCount: Int
)
