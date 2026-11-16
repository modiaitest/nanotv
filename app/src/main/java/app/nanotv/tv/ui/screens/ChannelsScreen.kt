package app.nanotv.tv.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.nanotv.tv.data.local.ChannelSummaryModel
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.ui.components.ChannelCard
import app.nanotv.tv.ui.components.CreatorAvatarBubble
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.NanoVideoCard
import app.nanotv.tv.ui.theme.NanoPalette

@Composable
fun ChannelsScreen(
    channels: List<ChannelSummaryModel>,
    selectedChannelId: String?,
    selectedChannelVideos: List<VideoCardModel>,
    onSelectChannel: (String) -> Unit,
    onCloseChannelDetail: () -> Unit,
    onPlayVideo: (VideoCardModel, List<VideoCardModel>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = channels.find { it.channelId == selectedChannelId }

    if (activeChannel != null) {
        BackHandler(onBack = onCloseChannelDetail)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(NanoPalette.CardSurface)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NanoActionButton(
                        label = "Channels",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        accentColor = NanoPalette.CardFocusedSurface,
                        onClick = onCloseChannelDetail
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    CreatorAvatarBubble(
                        avatarUrl = activeChannel.avatarUrl,
                        title = activeChannel.title,
                        size = 64.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeChannel.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = NanoPalette.TextPrimary
                        )
                        Text(
                            text = "${selectedChannelVideos.size} approved videos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NanoPalette.ElectricCyan
                        )
                    }
                    if (selectedChannelVideos.isNotEmpty()) {
                        NanoActionButton(
                            label = "Play All",
                            icon = Icons.Rounded.PlayArrow,
                            accentColor = NanoPalette.WarmAmber,
                            onClick = {
                                onPlayVideo(selectedChannelVideos.first(), selectedChannelVideos, activeChannel.title)
                            }
                        )
                    }
                }
            }

            items(selectedChannelVideos, key = { "ch_vid_${it.videoId}" }) { video ->
                NanoVideoCard(
                    video = video,
                    accentColor = NanoPalette.WarmAmber,
                    onClick = { onPlayVideo(video, selectedChannelVideos, activeChannel.title) }
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Approved Creators",
                style = MaterialTheme.typography.headlineLarge,
                color = NanoPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Channels derived strictly from your parent-curated shelves",
                style = MaterialTheme.typography.bodyMedium,
                color = NanoPalette.TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (channels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No channels added yet. Open Parent Curate (QR) to add channels!",
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(channels, key = { _, ch -> ch.channelId }) { index, channel ->
                        ChannelCard(
                            channel = channel,
                            index = index,
                            onClick = { onSelectChannel(channel.channelId) }
                        )
                    }
                }
            }
        }
    }
}
