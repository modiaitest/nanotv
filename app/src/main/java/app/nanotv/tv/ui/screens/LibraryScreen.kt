package app.nanotv.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nanotv.tv.data.local.PlaylistEntity
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.ui.LibraryTab
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.NanoFilterPill
import app.nanotv.tv.ui.components.NanoVideoCard
import app.nanotv.tv.ui.components.PlaylistCard
import app.nanotv.tv.ui.theme.NanoPalette

@Composable
fun LibraryScreen(
    activeTab: LibraryTab,
    playlists: List<PlaylistEntity>,
    selectedPlaylistId: String?,
    selectedPlaylistVideos: List<VideoCardModel>,
    historyVideos: List<VideoCardModel>,
    likedVideos: List<VideoCardModel>,
    watchLaterVideos: List<VideoCardModel>,
    onSelectTab: (LibraryTab) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onClosePlaylist: () -> Unit,
    onPlayVideo: (VideoCardModel, List<VideoCardModel>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlaylist = playlists.find { it.playlistId == selectedPlaylistId }

    if (activePlaylist != null) {
        BackHandler(onBack = onClosePlaylist)
        val accent = NanoPalette.accentFor(activePlaylist.colorIndex)

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NanoActionButton(
                        label = "Back",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        accentColor = NanoPalette.CardFocusedSurface,
                        onClick = onClosePlaylist
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = activePlaylist.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = NanoPalette.TextPrimary
                    )
                }
                if (selectedPlaylistVideos.isNotEmpty()) {
                    NanoActionButton(
                        label = "Play All",
                        icon = Icons.Rounded.PlayArrow,
                        accentColor = accent,
                        onClick = {
                            onPlayVideo(selectedPlaylistVideos.first(), selectedPlaylistVideos, activePlaylist.title)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(selectedPlaylistVideos, key = { "pl_vid_${it.videoId}" }) { video ->
                    NanoVideoCard(
                        video = video,
                        accentColor = accent,
                        onClick = { onPlayVideo(video, selectedPlaylistVideos, activePlaylist.title) }
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            color = NanoPalette.TextPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NanoFilterPill(
                label = "Shelves (${playlists.size})",
                selected = activeTab == LibraryTab.PLAYLISTS,
                accentColor = NanoPalette.NeonMint,
                leadingIcon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                onClick = { onSelectTab(LibraryTab.PLAYLISTS) }
            )
            NanoFilterPill(
                label = "History (${historyVideos.size})",
                selected = activeTab == LibraryTab.HISTORY,
                accentColor = NanoPalette.ElectricCyan,
                leadingIcon = Icons.Rounded.History,
                onClick = { onSelectTab(LibraryTab.HISTORY) }
            )
            NanoFilterPill(
                label = "Favorites (${likedVideos.size})",
                selected = activeTab == LibraryTab.LIKED,
                accentColor = NanoPalette.CoralAccent,
                leadingIcon = Icons.Rounded.Favorite,
                onClick = { onSelectTab(LibraryTab.LIKED) }
            )
            NanoFilterPill(
                label = "Watch Later (${watchLaterVideos.size})",
                selected = activeTab == LibraryTab.WATCH_LATER,
                accentColor = NanoPalette.WarmAmber,
                leadingIcon = Icons.Rounded.WatchLater,
                onClick = { onSelectTab(LibraryTab.WATCH_LATER) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (activeTab) {
            LibraryTab.PLAYLISTS -> {
                if (playlists.isEmpty()) {
                    EmptyMsg("No shelves yet.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlists, key = { it.playlistId }) { pl ->
                            PlaylistCard(playlist = pl, onClick = { onOpenPlaylist(pl.playlistId) })
                        }
                    }
                }
            }
            LibraryTab.HISTORY -> VideoGridOrEmpty(historyVideos, "Watch History", NanoPalette.ElectricCyan, "Recently watched videos appear here.", onPlayVideo)
            LibraryTab.LIKED -> VideoGridOrEmpty(likedVideos, "Favorites", NanoPalette.CoralAccent, "Favorite videos from the pause panel to save them here.", onPlayVideo)
            LibraryTab.WATCH_LATER -> VideoGridOrEmpty(watchLaterVideos, "Watch Later", NanoPalette.WarmAmber, "Save videos to Watch Later from the pause panel.", onPlayVideo)
        }
    }
}

@Composable
private fun VideoGridOrEmpty(
    videos: List<VideoCardModel>,
    contextTitle: String,
    accent: androidx.compose.ui.graphics.Color,
    emptyText: String,
    onPlayVideo: (VideoCardModel, List<VideoCardModel>, String) -> Unit
) {
    if (videos.isEmpty()) {
        EmptyMsg(emptyText)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos, key = { "${contextTitle}_${it.videoId}" }) { video ->
                NanoVideoCard(
                    video = video,
                    accentColor = accent,
                    onClick = { onPlayVideo(video, videos, contextTitle) }
                )
            }
        }
    }
}

@Composable
private fun EmptyMsg(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = NanoPalette.TextSecondary)
    }
}
