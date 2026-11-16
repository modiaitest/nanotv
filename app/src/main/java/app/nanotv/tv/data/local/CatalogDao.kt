package app.nanotv.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query("SELECT * FROM curated_sources ORDER BY sortOrder ASC, addedEpochMs ASC")
    fun observeCuratedSources(): Flow<List<CuratedSourceEntity>>

    @Query("SELECT * FROM curated_sources ORDER BY sortOrder ASC, addedEpochMs ASC")
    suspend fun getCuratedSources(): List<CuratedSourceEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM curated_sources")
    suspend fun maxSourceSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuratedSource(source: CuratedSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuratedSources(sources: List<CuratedSourceEntity>)

    @Query("DELETE FROM curated_sources WHERE sourceId = :sourceId")
    suspend fun deleteCuratedSource(sourceId: String)

    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        LEFT JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
        ORDER BY v.rowid ASC
        """
    )
    fun observeAllApprovedVideos(): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        LEFT JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
        ORDER BY v.publishedAt DESC, v.title ASC
        """
    )
    suspend fun getAllApprovedVideosSnapshot(): List<VideoCardModel>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM playlist_video_cross_ref ref
        INNER JOIN videos v ON ref.videoId = v.videoId
        LEFT JOIN watch_states w ON v.videoId = w.videoId
        WHERE ref.playlistId = :playlistId
          AND v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
        ORDER BY ref.position ASC
        """
    )
    fun observeVideosForPlaylist(playlistId: String): Flow<List<VideoCardModel>>

    @Query("SELECT * FROM playlist_video_cross_ref ORDER BY playlistId ASC, position ASC")
    fun observeAllCrossRefs(): Flow<List<PlaylistVideoCrossRef>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        INNER JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
          AND w.lastWatchedEpochMs > 0
          AND w.progressSeconds >= 8
          AND (w.durationSeconds <= 0 OR w.progressSeconds < (w.durationSeconds - 12))
        ORDER BY w.lastWatchedEpochMs DESC
        LIMIT 15
        """
    )
    fun observeContinueWatching(): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        INNER JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
          AND w.lastWatchedEpochMs > 0
        ORDER BY w.lastWatchedEpochMs DESC
        LIMIT 40
        """
    )
    fun observeWatchHistory(): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        INNER JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
          AND w.isLiked = 1
        ORDER BY w.lastWatchedEpochMs DESC, v.title ASC
        """
    )
    fun observeLikedVideos(): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        INNER JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
          AND w.isWatchLater = 1
        ORDER BY w.lastWatchedEpochMs DESC, v.title ASC
        """
    )
    fun observeWatchLaterVideos(): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT c.channelId, c.title, c.description, c.avatarUrl, c.bannerUrl,
               COUNT(v.videoId) AS videoCount
        FROM channels c
        INNER JOIN videos v ON c.channelId = v.channelId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
        GROUP BY c.channelId
        ORDER BY videoCount DESC, c.title ASC
        """
    )
    fun observeApprovedChannels(): Flow<List<ChannelSummaryModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        LEFT JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.channelId = :channelId
          AND v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
        ORDER BY v.publishedAt DESC, v.title ASC
        """
    )
    fun observeVideosForChannel(channelId: String): Flow<List<VideoCardModel>>

    @Query(
        """
        SELECT v.videoId, v.title, v.description, v.thumbnailUrl, v.channelId, v.channelTitle,
               v.channelAvatarUrl, v.durationSeconds, v.durationFormatted, v.publishedAt,
               w.progressSeconds AS progressSeconds,
               w.durationSeconds AS stateDurationSeconds,
               w.lastWatchedEpochMs AS lastWatchedEpochMs,
               w.isLiked AS isLiked,
               w.isWatchLater AS isWatchLater
        FROM videos v
        LEFT JOIN watch_states w ON v.videoId = w.videoId
        WHERE v.isEmbeddable = 1
          AND v.isBlockedAtRuntime = 0
          AND (v.durationSeconds = 0 OR v.durationSeconds > 75)
          AND LOWER(v.title) NOT LIKE '%#short%'
          AND (
            v.title LIKE '%' || :query || '%'
            OR v.channelTitle LIKE '%' || :query || '%'
            OR v.description LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN v.title LIKE :query || '%' THEN 0 ELSE 1 END,
          v.title ASC
        """
    )
    fun searchApprovedVideos(query: String): Flow<List<VideoCardModel>>

    @Query("SELECT videoId FROM videos WHERE isBlockedAtRuntime = 1")
    suspend fun getRuntimeBlockedVideoIds(): List<String>

    @Query("UPDATE videos SET isBlockedAtRuntime = 1 WHERE videoId = :videoId")
    suspend fun markVideoBlockedAtRuntime(videoId: String)

    @Query("UPDATE videos SET isBlockedAtRuntime = 0 WHERE isBlockedAtRuntime = 1")
    suspend fun restoreHiddenVideos()

    @Query("SELECT * FROM watch_states WHERE videoId = :videoId LIMIT 1")
    suspend fun getWatchState(videoId: String): WatchStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchState(state: WatchStateEntity)

    @Query("UPDATE watch_states SET progressSeconds = 0, lastWatchedEpochMs = 0 WHERE lastWatchedEpochMs > 0")
    suspend fun clearWatchHistory()

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("DELETE FROM videos")
    suspend fun clearVideos()

    @Query("DELETE FROM playlist_video_cross_ref")
    suspend fun clearCrossRefs()

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(items: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(items: List<PlaylistVideoCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(items: List<ChannelEntity>)

    @Transaction
    suspend fun replaceSyncedCatalog(
        playlists: List<PlaylistEntity>,
        videos: List<VideoEntity>,
        crossRefs: List<PlaylistVideoCrossRef>,
        channels: List<ChannelEntity>
    ) {
        val previouslyBlocked = getRuntimeBlockedVideoIds().toSet()
        clearCrossRefs()
        clearPlaylists()
        clearVideos()
        clearChannels()

        val mergedVideos = videos.map { video ->
            if (video.videoId in previouslyBlocked) {
                video.copy(isBlockedAtRuntime = true)
            } else {
                video
            }
        }

        insertPlaylists(playlists)
        insertVideos(mergedVideos)
        insertCrossRefs(crossRefs)
        insertChannels(channels)
    }
}
