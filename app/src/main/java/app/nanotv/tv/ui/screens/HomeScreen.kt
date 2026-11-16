package app.nanotv.tv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.ui.HomeUiState
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.NanoFilterPill
import app.nanotv.tv.ui.components.NanoVideoCard
import app.nanotv.tv.ui.components.PoweredByYouTubeBadge
import app.nanotv.tv.ui.components.VideoHorizontalShelf
import app.nanotv.tv.ui.theme.NanoPalette

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSelectCategory: (String?) -> Unit,
    onPlayVideo: (VideoCardModel, List<VideoCardModel>, String) -> Unit,
    onOpenParentCurate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeShelf = uiState.shelves.find { it.playlist.playlistId == uiState.selectedCategoryId }

    if (uiState.isSyncing && uiState.allVideos.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = NanoPalette.ElectricCyan,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Syncing family shelves...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NanoPalette.TextPrimary
                )
            }
        }
        return
    }

    if (activeShelf != null) {
        val accent = NanoPalette.accentFor(activeShelf.playlist.colorIndex)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                Column {
                    HomeTopHeader(uiState = uiState, onSelectCategory = onSelectCategory)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeShelf.playlist.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = NanoPalette.TextPrimary
                        )
                        if (activeShelf.videos.isNotEmpty()) {
                            NanoActionButton(
                                label = "Play All",
                                icon = Icons.Rounded.PlayArrow,
                                accentColor = accent,
                                onClick = {
                                    onPlayVideo(activeShelf.videos.first(), activeShelf.videos, activeShelf.playlist.title)
                                }
                            )
                        }
                    }
                }
            }
            items(activeShelf.videos, key = { "cat_${it.videoId}" }) { video ->
                NanoVideoCard(
                    video = video,
                    accentColor = accent,
                    onClick = { onPlayVideo(video, activeShelf.videos, activeShelf.playlist.title) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "home_header") {
                Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                    HomeTopHeader(uiState = uiState, onSelectCategory = onSelectCategory)
                }
            }

            if (uiState.continueWatching.isNotEmpty()) {
                item(key = "continue_watching") {
                    VideoHorizontalShelf(
                        title = "Continue Watching",
                        accentColor = NanoPalette.ElectricCyan,
                        videos = uiState.continueWatching,
                        onVideoClick = { video ->
                            onPlayVideo(video, uiState.continueWatching, "Continue Watching")
                        }
                    )
                }
            }

            items(uiState.shelves, key = { "shelf_${it.playlist.playlistId}" }) { shelf ->
                val accent = NanoPalette.accentFor(shelf.playlist.colorIndex)
                VideoHorizontalShelf(
                    title = shelf.playlist.title,
                    subtitle = "${shelf.videos.size} videos",
                    accentColor = accent,
                    videos = shelf.videos,
                    onVideoClick = { video ->
                        onPlayVideo(video, shelf.videos, shelf.playlist.title)
                    }
                )
            }

            if (uiState.shelves.isEmpty()) {
                item(key = "empty_home") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your Family TV Library Is Ready to Curate",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NanoPalette.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Open Parent Curate (PIN protected) to scan the QR code and add channels, playlists, or videos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NanoPalette.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            NanoActionButton(
                                label = "Open Parent Curate (QR)",
                                icon = Icons.Rounded.QrCode2,
                                accentColor = NanoPalette.WarmAmber,
                                onClick = onOpenParentCurate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    uiState: HomeUiState,
    onSelectCategory: (String?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NanoPalette.ElectricCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = NanoPalette.MidnightBg,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NanoTV",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NanoPalette.TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        color = NanoPalette.ElectricCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
                PoweredByYouTubeBadge()
            }
        }

        if (uiState.playlists.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item(key = "pill_all") {
                    NanoFilterPill(
                        label = "All Shelves",
                        selected = uiState.selectedCategoryId == null,
                        accentColor = NanoPalette.ElectricCyan,
                        leadingIcon = Icons.Rounded.AutoAwesome,
                        onClick = { onSelectCategory(null) }
                    )
                }
                items(uiState.playlists, key = { "pill_${it.playlistId}" }) { playlist ->
                    NanoFilterPill(
                        label = playlist.title,
                        selected = uiState.selectedCategoryId == playlist.playlistId,
                        accentColor = NanoPalette.accentFor(playlist.colorIndex),
                        onClick = { onSelectCategory(playlist.playlistId) }
                    )
                }
            }
        }
    }
}
