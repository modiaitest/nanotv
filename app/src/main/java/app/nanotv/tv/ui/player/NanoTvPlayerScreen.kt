package app.nanotv.tv.ui.player

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.data.remote.DurationFormatter
import app.nanotv.tv.ui.PlaybackSession
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.PoweredByYouTubeBadge
import app.nanotv.tv.ui.theme.NanoPalette
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

private fun findInternalWebView(view: View): WebView? {
    if (view is WebView) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val found = findInternalWebView(view.getChildAt(i))
            if (found != null) return found
        }
    }
    return null
}

private fun computeResumeSeconds(video: VideoCardModel?): Float {
    if (video == null) return 0f
    val prog = video.progressSeconds ?: 0f
    val total = if ((video.stateDurationSeconds ?: 0f) > 0f) {
        video.stateDurationSeconds ?: 0f
    } else {
        video.durationSeconds.toFloat()
    }
    return if (prog >= 8f && (total <= 0f || prog < total - 15f)) prog else 0f
}

/**
 * Play-Store & YouTube API Services ToS (§III.E.4) Compliant Player:
 * - Official YouTube web controls & branding remain enabled (`controls(1)`).
 * - ZERO UI overlays are rendered on top of the active playing video frame!
 * - Outbound navigation links attempting to leave the embedded player are intercepted and blocked.
 * - Pressing Pause/Up/Down pauses playback and opens a separate Pause & Queue Menu; resuming hides it completely.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NanoTvPlayerScreen(
    session: PlaybackSession,
    allVideos: List<VideoCardModel>,
    autoplayEnabled: Boolean,
    onSelectQueueVideo: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onSaveProgress: (String, Float, Float) -> Unit,
    onMarkFinished: (String, Float) -> Unit,
    onUnplayableVideo: (String) -> Unit,
    onExitPlayer: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val videosById = remember(allVideos) { allVideos.associateBy { it.videoId } }
    val currentVideo = videosById[session.currentVideoId]
    val queueVideos = remember(session.queueVideoIds, videosById) {
        session.queueVideoIds.mapNotNull { videosById[it] }
    }
    val nextVideo = remember(session.currentVideoId, queueVideos) {
        if (queueVideos.size <= 1) null
        else {
            val idx = queueVideos.indexOfFirst { it.videoId == session.currentVideoId }
            if (idx >= 0 && idx + 1 < queueVideos.size) queueVideos[idx + 1]
            else if (idx == queueVideos.lastIndex) queueVideos.first()
            else null
        }
    }

    var playerViewRef by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var ytPlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var loadedVideoId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentSeconds by remember { mutableFloatStateOf(0f) }
    var durationSeconds by remember { mutableFloatStateOf((currentVideo?.durationSeconds ?: 0L).toFloat()) }

    // Pause menu is ONLY shown while video is paused (never obscuring active video playback!)
    var pauseMenuOpen by remember { mutableStateOf(false) }
    var isTimelineFocused by remember { mutableStateOf(false) }
    var scrubPreviewSeconds by remember { mutableStateOf<Float?>(null) }
    var lastScrubKeyTick by remember { mutableLongStateOf(0L) }

    var showUpNextOverlay by remember { mutableStateOf(false) }
    var upNextCountdown by remember { mutableIntStateOf(5) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }

    val stageFocusRequester = remember { FocusRequester() }
    val resumeFocusRequester = remember { FocusRequester() }

    val latestVideoId by rememberUpdatedState(session.currentVideoId)
    val latestCurrentVideo by rememberUpdatedState(currentVideo)
    val latestNextVideo by rememberUpdatedState(nextVideo)
    val latestAutoplay by rememberUpdatedState(autoplayEnabled)

    fun openPauseMenu() {
        ytPlayer?.pause()
        pauseMenuOpen = true
    }

    fun resumePlayback() {
        scrubPreviewSeconds = null
        pauseMenuOpen = false
        ytPlayer?.play()
    }

    fun seekRelative(deltaSeconds: Float) {
        val player = ytPlayer ?: return
        val maxDur = if (durationSeconds > 0f) durationSeconds else 36_000f
        val target = (currentSeconds + deltaSeconds).coerceIn(0f, maxDur)
        currentSeconds = target
        player.seekTo(target)
    }

    fun commitTimelineScrub(target: Float) {
        val player = ytPlayer ?: return
        val maxDur = if (durationSeconds > 0f) durationSeconds else 36_000f
        val clamped = target.coerceIn(0f, maxDur)
        currentSeconds = clamped
        scrubPreviewSeconds = null
        player.seekTo(clamped)
    }

    fun stepTimelineScrub(direction: Int) {
        val maxDur = if (durationSeconds > 0f) durationSeconds else (currentVideo?.durationSeconds ?: 600L).toFloat()
        val step = when {
            maxDur >= 1200f -> 25f
            maxDur >= 300f -> 15f
            else -> 10f
        }
        val base = scrubPreviewSeconds ?: currentSeconds
        scrubPreviewSeconds = (base + direction * step).coerceIn(0f, maxDur.coerceAtLeast(10f))
        lastScrubKeyTick = System.currentTimeMillis()
    }

    LaunchedEffect(scrubPreviewSeconds, lastScrubKeyTick) {
        val target = scrubPreviewSeconds ?: return@LaunchedEffect
        delay(650L)
        commitTimelineScrub(target)
    }

    BackHandler {
        if (showUpNextOverlay) {
            showUpNextOverlay = false
            onExitPlayer()
        } else if (pauseMenuOpen) {
            resumePlayback()
        } else {
            if (currentSeconds > 3f) {
                onSaveProgress(session.currentVideoId, currentSeconds, durationSeconds)
            }
            onExitPlayer()
        }
    }

    LaunchedEffect(ytPlayer, session.currentVideoId) {
        val player = ytPlayer ?: return@LaunchedEffect
        if (loadedVideoId != session.currentVideoId) {
            val prevId = loadedVideoId
            if (prevId != null && currentSeconds > 3f) {
                onSaveProgress(prevId, currentSeconds, durationSeconds)
            }

            loadedVideoId = session.currentVideoId
            showUpNextOverlay = false
            playbackErrorMessage = null
            pauseMenuOpen = false
            scrubPreviewSeconds = null
            currentSeconds = 0f
            durationSeconds = (latestCurrentVideo?.durationSeconds ?: 0L).toFloat()

            val resumeAt = computeResumeSeconds(latestCurrentVideo)
            player.loadVideo(session.currentVideoId, resumeAt)
        }
    }

    LaunchedEffect(session.currentVideoId, isPlaying) {
        while (isPlaying) {
            delay(5_000L)
            if (currentSeconds > 3f) {
                onSaveProgress(session.currentVideoId, currentSeconds, durationSeconds)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            if (currentSeconds > 3f) {
                onSaveProgress(latestVideoId, currentSeconds, durationSeconds)
            }
            playerViewRef?.let { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            }
        }
    }

    LaunchedEffect(pauseMenuOpen, showUpNextOverlay) {
        delay(60L)
        try {
            if (!showUpNextOverlay) {
                if (pauseMenuOpen) resumeFocusRequester.requestFocus()
                else stageFocusRequester.requestFocus()
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(showUpNextOverlay, session.currentVideoId, autoplayEnabled) {
        if (showUpNextOverlay && autoplayEnabled && nextVideo != null) {
            upNextCountdown = 5
            while (upNextCountdown > 0 && showUpNextOverlay) {
                delay(1_000L)
                upNextCountdown -= 1
            }
            if (showUpNextOverlay && nextVideo != null) {
                showUpNextOverlay = false
                onSelectQueueVideo(nextVideo.videoId)
            }
        }
    }

    LaunchedEffect(playbackErrorMessage, session.currentVideoId) {
        if (playbackErrorMessage != null) {
            delay(3_200L)
            val fallback = latestNextVideo
            if (fallback != null && fallback.videoId != session.currentVideoId) {
                onSelectQueueVideo(fallback.videoId)
            } else {
                onExitPlayer()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Official YouTube IFrame Player with controls(1) & navigation containment
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(stageFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (pauseMenuOpen || showUpNextOverlay) return@onPreviewKeyEvent false

                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.DirectionUp, Key.DirectionDown -> {
                            openPauseMenu()
                            true
                        }
                        Key.MediaPause -> {
                            openPauseMenu()
                            true
                        }
                        Key.MediaPlay -> {
                            resumePlayback()
                            true
                        }
                        Key.DirectionLeft, Key.MediaRewind -> {
                            seekRelative(-10f)
                            true
                        }
                        Key.DirectionRight, Key.MediaFastForward -> {
                            seekRelative(10f)
                            true
                        }
                        else -> false
                    }
                },
            factory = { ctx ->
                val initialVideoId = session.currentVideoId
                val initialResume = computeResumeSeconds(currentVideo)

                YouTubePlayerView(ctx).apply {
                    playerViewRef = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    enableAutomaticInitialization = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    findInternalWebView(this)?.apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.domStorageEnabled = true
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        // Block outbound browser escapes while allowing YouTube embed frames
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val host = request?.url?.host?.lowercase().orEmpty()
                                val path = request?.url?.path?.lowercase().orEmpty()
                                val isEmbedFrame = (host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) &&
                                    path.startsWith("/embed")
                                return !isEmbedFrame
                            }
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(this)

                    // controls(1) keeps YouTube's official branding & player attribution unobscured!
                    val options = IFramePlayerOptions.Builder(ctx)
                        .controls(1)
                        .autoplay(1)
                        .rel(0)
                        .ivLoadPolicy(3)
                        .apply {
                            if (initialResume >= 8f) start(initialResume.toInt())
                        }
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                loadedVideoId = initialVideoId
                                ytPlayer = youTubePlayer
                                youTubePlayer.loadVideo(initialVideoId, initialResume)
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState
                            ) {
                                when (state) {
                                    PlayerConstants.PlayerState.PLAYING -> {
                                        isPlaying = true
                                        pauseMenuOpen = false
                                        playbackErrorMessage = null
                                    }
                                    PlayerConstants.PlayerState.PAUSED -> {
                                        isPlaying = false
                                    }
                                    PlayerConstants.PlayerState.VIDEO_CUED -> {
                                        youTubePlayer.play()
                                    }
                                    PlayerConstants.PlayerState.ENDED -> {
                                        isPlaying = false
                                        pauseMenuOpen = false
                                        onMarkFinished(latestVideoId, durationSeconds)
                                        if (latestNextVideo != null) {
                                            showUpNextOverlay = true
                                        } else {
                                            onExitPlayer()
                                        }
                                    }
                                    else -> Unit
                                }
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                if (scrubPreviewSeconds == null) {
                                    currentSeconds = second
                                }
                            }

                            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                if (duration > 0f) durationSeconds = duration
                            }

                            override fun onError(
                                youTubePlayer: YouTubePlayer,
                                error: PlayerConstants.PlayerError
                            ) {
                                isPlaying = false
                                pauseMenuOpen = false
                                onUnplayableVideo(latestVideoId)
                                playbackErrorMessage =
                                    "Video unavailable for embedded TV playback (${error.name}). Advancing to next approved video..."
                            }
                        },
                        false,
                        options,
                        initialVideoId
                    )
                }
            }
        )

        // 2. Pause & Queue Sheet — ONLY shown when video is PAUSED (zero overlay during active video playback!)
        AnimatedVisibility(
            visible = pauseMenuOpen && !showUpNextOverlay && playbackErrorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEB0A111D))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        NanoActionButton(
                            label = "Exit Video",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            accentColor = NanoPalette.CardSurface,
                            onClick = {
                                if (currentSeconds > 3f) {
                                    onSaveProgress(session.currentVideoId, currentSeconds, durationSeconds)
                                }
                                onExitPlayer()
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentVideo?.title ?: "Paused",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentVideo?.channelTitle.orEmpty()} • ${session.contextTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NanoPalette.ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val liked = currentVideo?.liked == true
                        NanoActionButton(
                            label = if (liked) "Favorited" else "Favorite",
                            icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            accentColor = if (liked) NanoPalette.CoralAccent else NanoPalette.CardSurface,
                            onClick = { onToggleLike(session.currentVideoId) }
                        )

                        val watchLater = currentVideo?.watchLater == true
                        NanoActionButton(
                            label = if (watchLater) "Saved" else "Watch Later",
                            icon = Icons.Rounded.WatchLater,
                            accentColor = if (watchLater) NanoPalette.WarmAmber else NanoPalette.CardSurface,
                            onClick = { onToggleWatchLater(session.currentVideoId) }
                        )

                        PoweredByYouTubeBadge()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    val displayedSeconds = scrubPreviewSeconds ?: currentSeconds
                    val elapsedLabel = DurationFormatter.formatSeconds(displayedSeconds.toLong().coerceAtLeast(0L)).ifBlank { "0:00" }
                    val totalLabel = if (durationSeconds > 0f) {
                        DurationFormatter.formatSeconds(durationSeconds.toLong())
                    } else {
                        currentVideo?.durationFormatted.orEmpty()
                    }

                    InteractiveTimelineScrubber(
                        currentSeconds = currentSeconds,
                        scrubPreviewSeconds = scrubPreviewSeconds,
                        durationSeconds = durationSeconds,
                        isFocused = isTimelineFocused,
                        timeText = if (totalLabel.isNotBlank()) "$elapsedLabel / $totalLabel" else elapsedLabel,
                        onFocusChange = { focused ->
                            isTimelineFocused = focused
                            if (!focused && scrubPreviewSeconds != null) {
                                commitTimelineScrub(scrubPreviewSeconds!!)
                            }
                        },
                        onStepLeft = { stepTimelineScrub(-1) },
                        onStepRight = { stepTimelineScrub(+1) },
                        onCommit = {
                            val target = scrubPreviewSeconds ?: currentSeconds
                            commitTimelineScrub(target)
                            resumePlayback()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NanoActionButton(
                                label = "Resume Video",
                                icon = Icons.Rounded.PlayArrow,
                                accentColor = NanoPalette.ElectricCyan,
                                onClick = { resumePlayback() },
                                modifier = Modifier.focusRequester(resumeFocusRequester)
                            )
                            NanoActionButton(
                                label = "-15s",
                                icon = Icons.Rounded.Replay10,
                                accentColor = NanoPalette.CardSurface,
                                onClick = {
                                    seekRelative(-15f)
                                    resumePlayback()
                                }
                            )
                            NanoActionButton(
                                label = "+15s",
                                icon = Icons.Rounded.Forward10,
                                accentColor = NanoPalette.CardSurface,
                                onClick = {
                                    seekRelative(15f)
                                    resumePlayback()
                                }
                            )
                            if (nextVideo != null) {
                                NanoActionButton(
                                    label = "Next Video",
                                    icon = Icons.Rounded.SkipNext,
                                    accentColor = NanoPalette.NeonMint,
                                    onClick = { onSelectQueueVideo(nextVideo.videoId) }
                                )
                            }
                        }

                        Text(
                            text = if (isTimelineFocused) "◄ ► Scrub timeline & press OK to jump" else "Press UP to scrub timeline",
                            color = if (isTimelineFocused) NanoPalette.WarmAmber else NanoPalette.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (queueVideos.size > 1) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Up Next in ${session.contextTitle}",
                            color = NanoPalette.WarmAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = queueVideos.filter { it.videoId != session.currentVideoId },
                                key = { "pause_q_${it.videoId}" }
                            ) { item ->
                                MiniQueueCard(video = item, onClick = { onSelectQueueVideo(item.videoId) })
                            }
                        }
                    }
                }
            }
        }

        // 3. Post-Video Up Next Screen (shown AFTER video has ENDED)
        if (showUpNextOverlay && nextVideo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00E1726))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(520.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(NanoPalette.CardSurface)
                        .border(BorderStroke(2.5.dp, NanoPalette.ElectricCyan), RoundedCornerShape(24.dp))
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (latestAutoplay) "Up Next in $upNextCountdown..." else "Up Next",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NanoPalette.ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    AsyncImage(
                        model = nextVideo.thumbnailUrl,
                        contentDescription = nextVideo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(300.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = nextVideo.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        NanoActionButton(
                            label = "Play Now",
                            icon = Icons.Rounded.PlayArrow,
                            accentColor = NanoPalette.ElectricCyan,
                            onClick = {
                                showUpNextOverlay = false
                                onSelectQueueVideo(nextVideo.videoId)
                            }
                        )
                        NanoActionButton(
                            label = "Back to Browse",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            accentColor = NanoPalette.CardFocusedSurface,
                            onClick = {
                                showUpNextOverlay = false
                                onExitPlayer()
                            }
                        )
                    }
                }
            }
        }

        if (playbackErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00E1726))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(NanoPalette.CardSurface)
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Skipping Unavailable Video",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NanoPalette.WarmAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackErrorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanoPalette.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveTimelineScrubber(
    currentSeconds: Float,
    scrubPreviewSeconds: Float?,
    durationSeconds: Float,
    isFocused: Boolean,
    timeText: String,
    onFocusChange: (Boolean) -> Unit,
    onStepLeft: () -> Unit,
    onStepRight: () -> Unit,
    onCommit: () -> Unit
) {
    val activePos = scrubPreviewSeconds ?: currentSeconds
    val fraction = if (durationSeconds > 0f) (activePos / durationSeconds).coerceIn(0f, 1f) else 0f
    val barHeight by animateDpAsState(targetValue = if (isFocused) 15.dp else 9.dp, label = "barH")
    val thumbSize by animateDpAsState(targetValue = if (isFocused) 24.dp else 13.dp, label = "thumbS")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !isFocused) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.MediaRewind -> {
                        onStepLeft()
                        true
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        onStepRight()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onCommit()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (scrubPreviewSeconds != null) {
                Text(
                    text = "Seeking to ${DurationFormatter.formatSeconds(scrubPreviewSeconds.toLong().coerceAtLeast(0L)).ifBlank { "0:00" }}",
                    color = NanoPalette.WarmAmber,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Text(
                text = timeText,
                color = if (isFocused) NanoPalette.WarmAmber else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .then(
                        if (isFocused) Modifier.border(BorderStroke(2.dp, NanoPalette.WarmAmber), RoundedCornerShape(50))
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (scrubPreviewSeconds != null) NanoPalette.WarmAmber else NanoPalette.ElectricCyan)
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = BiasAlignment(
                    horizontalBias = (fraction * 2f - 1f).coerceIn(-1f, 1f),
                    verticalBias = 0f
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(if (isFocused) NanoPalette.WarmAmber else NanoPalette.ElectricCyan)
                        .border(BorderStroke(2.dp, Color.White), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun MiniQueueCard(
    video: VideoCardModel,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .width(244.dp)
            .height(72.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) NanoPalette.CardFocusedSurface else NanoPalette.CardSurface)
            .then(
                if (focused) Modifier.border(BorderStroke(2.dp, NanoPalette.ElectricCyan), shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(54.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = video.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
