package app.nanotv.tv.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SpaceBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.NanoVideoCard
import app.nanotv.tv.ui.components.PoweredByYouTubeBadge
import app.nanotv.tv.ui.theme.NanoPalette

private val ABC_KEYS = listOf(
    "A", "B", "C", "D", "E", "F",
    "G", "H", "I", "J", "K", "L",
    "M", "N", "O", "P", "Q", "R",
    "S", "T", "U", "V", "W", "X",
    "Y", "Z", "1", "2", "3", "4",
    "5", "6", "7", "8", "9", "0"
)

@Composable
fun SearchScreen(
    query: String,
    results: List<VideoCardModel>,
    onQueryChange: (String) -> Unit,
    onAppendChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPlayVideo: (VideoCardModel, List<VideoCardModel>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var voiceNotice by remember { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                voiceNotice = null
                onQueryChange(spoken)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .width(284.dp)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NanoPalette.CardSurface)
                    .border(BorderStroke(1.5.dp, NanoPalette.ElectricCyan.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = NanoPalette.ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (query.isEmpty()) "Search approved library..." else query,
                    color = if (query.isEmpty()) NanoPalette.TextMuted else NanoPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            NanoActionButton(
                label = "Voice Search",
                icon = Icons.Rounded.Mic,
                accentColor = NanoPalette.ElectricCyan,
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search your NanoTV library")
                        }
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        voiceNotice = "Voice search unavailable on this TV — use the letters below."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (voiceNotice != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = voiceNotice!!, color = NanoPalette.WarmAmber, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyChip("SPACE", Icons.Rounded.SpaceBar, { onAppendChar(" ") }, Modifier.weight(1.3f))
                KeyChip("DEL", Icons.AutoMirrored.Rounded.Backspace, onBackspace, Modifier.weight(1f))
                KeyChip("CLR", Icons.Rounded.Clear, onClear, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ABC_KEYS, key = { it }) { ch ->
                    var focused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (focused) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "searchKey"
                    )
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .scale(scale)
                            .onFocusChanged { focused = it.isFocused }
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (focused) NanoPalette.WarmAmber else NanoPalette.CardSurface)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAppendChar(ch) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch,
                            color = if (focused) NanoPalette.MidnightBg else NanoPalette.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isBlank()) "Approved Library (${results.size})" else "Matches for \"$query\" (${results.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NanoPalette.TextPrimary
                )
                PoweredByYouTubeBadge()
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No matching videos in your curated library.",
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { "search_${it.videoId}" }) { video ->
                        NanoVideoCard(
                            video = video,
                            cardWidth = 258.dp,
                            accentColor = NanoPalette.ElectricCyan,
                            onClick = { onPlayVideo(video, results, "Search Results") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(38.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) NanoPalette.ElectricCyan else NanoPalette.CardSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (focused) NanoPalette.MidnightBg else Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (focused) NanoPalette.MidnightBg else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
