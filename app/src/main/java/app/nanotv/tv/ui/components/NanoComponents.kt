package app.nanotv.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.local.ChannelSummaryModel
import app.nanotv.tv.data.local.PlaylistEntity
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.ui.theme.NanoPalette
import coil.compose.AsyncImage

@Composable
fun PoweredByYouTubeBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "Powered by YouTube",
            color = NanoPalette.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NanoVideoCard(
    video: VideoCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 264.dp,
    accentColor: Color = NanoPalette.ElectricCyan
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) NanoPalette.CardFocusedSurface else NanoPalette.CardSurface,
        label = "cardBg"
    )
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .width(cardWidth)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(bgColor)
            .then(
                if (isFocused) Modifier.border(BorderStroke(3.dp, accentColor), shape)
                else Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF0A111D))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))
                    )
            )

            if (video.liked) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(NanoPalette.CoralAccent)
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Saved",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NanoPalette.ElectricCyan.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = NanoPalette.MidnightBg,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (video.durationFormatted.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = if (video.progressFraction > 0f) 11.dp else 8.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xD90A111D))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = video.durationFormatted,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (video.progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(video.progressFraction)
                            .fillMaxHeight()
                            .background(NanoPalette.ElectricCyan)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            CreatorAvatarBubble(
                avatarUrl = video.channelAvatarUrl,
                title = video.channelTitle,
                size = 34.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NanoPalette.TextPrimary,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFocused) accentColor else NanoPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CreatorAvatarBubble(
    avatarUrl: String,
    title: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val fallbackColor = NanoPalette.accentFor(title.hashCode())
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackColor.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.trim().take(1).uppercase().ifBlank { "N" },
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.44f).sp
        )
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NanoFilterPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillScale"
    )
    val shape = RoundedCornerShape(50)
    val bg = when {
        selected -> accentColor
        isFocused -> NanoPalette.CardFocusedSurface
        else -> NanoPalette.CardSurface
    }
    val contentColor = if (selected && accentColor != NanoPalette.RoyalViolet) {
        NanoPalette.MidnightBg
    } else {
        NanoPalette.TextPrimary
    }

    Row(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(bg)
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.5.dp, NanoPalette.WarmAmber), shape)
                else Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun NanoActionButton(
    label: String,
    icon: ImageVector? = null,
    accentColor: Color = NanoPalette.ElectricCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )
    val shape = RoundedCornerShape(16.dp)
    val bgColor = when {
        !enabled -> NanoPalette.CardSurface.copy(alpha = 0.5f)
        isFocused -> accentColor
        else -> accentColor.copy(alpha = 0.85f)
    }
    val fgColor = if (
        accentColor == NanoPalette.ElectricCyan ||
        accentColor == NanoPalette.NeonMint ||
        accentColor == NanoPalette.WarmAmber ||
        accentColor == NanoPalette.SkyBlue
    ) {
        NanoPalette.MidnightBg
    } else {
        Color.White
    }

    Row(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(bgColor)
            .then(
                if (isFocused && enabled) Modifier.border(BorderStroke(2.5.dp, Color.White), shape)
                else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = fgColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun VideoHorizontalShelf(
    title: String,
    accentColor: Color,
    videos: List<VideoCardModel>,
    onVideoClick: (VideoCardModel) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    if (videos.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = NanoPalette.TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanoPalette.TextMuted
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(videos, key = { "${title}_${it.videoId}" }) { video ->
                NanoVideoCard(
                    video = video,
                    accentColor = accentColor,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = NanoPalette.accentFor(playlist.colorIndex)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "plScale"
    )
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .width(264.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(if (isFocused) NanoPalette.CardFocusedSurface else NanoPalette.CardSurface)
            .then(
                if (isFocused) Modifier.border(BorderStroke(3.dp, accent), shape)
                else Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(accent.copy(alpha = 0.2f))
        ) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDF0E1726))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${playlist.itemCount} videos",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleLarge,
                color = NanoPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelSummaryModel,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = NanoPalette.accentFor(index)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chScale"
    )
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .width(220.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(if (isFocused) NanoPalette.CardFocusedSurface else NanoPalette.CardSurface)
            .then(
                if (isFocused) Modifier.border(BorderStroke(3.dp, accent), shape)
                else Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CreatorAvatarBubble(
            avatarUrl = channel.avatarUrl,
            title = channel.title,
            size = 80.dp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = channel.title,
            style = MaterialTheme.typography.titleMedium,
            color = NanoPalette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${channel.videoCount} approved",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
