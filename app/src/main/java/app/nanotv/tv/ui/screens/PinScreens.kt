package app.nanotv.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.repository.CatalogRepository
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.PoweredByYouTubeBadge
import app.nanotv.tv.ui.theme.NanoPalette
import kotlinx.coroutines.delay

private val PIN_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("CLR", "0", "DEL")
)

private fun mapHardwareDigitKey(key: Key): String? = when (key) {
    Key.Zero, Key.NumPad0 -> "0"
    Key.One, Key.NumPad1 -> "1"
    Key.Two, Key.NumPad2 -> "2"
    Key.Three, Key.NumPad3 -> "3"
    Key.Four, Key.NumPad4 -> "4"
    Key.Five, Key.NumPad5 -> "5"
    Key.Six, Key.NumPad6 -> "6"
    Key.Seven, Key.NumPad7 -> "7"
    Key.Eight, Key.NumPad8 -> "8"
    Key.Nine, Key.NumPad9 -> "9"
    else -> null
}

@Composable
fun ParentOnboardingScreen(
    onComplete: (pin: String, familySpaceName: String, selectedChannels: Set<String>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var selectedIds by remember {
        mutableStateOf(CatalogRepository.OptionalStarterPacks.map { it.channelId }.toSet())
    }
    val continueFocusRequester = remember { FocusRequester() }

    LaunchedEffect(pin.length) {
        if (pin.length == 4) {
            delay(80L)
            runCatching { continueFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NanoPalette.MidnightBg)
            .padding(horizontal = 44.dp, vertical = 28.dp)
    ) {
        if (step == 1) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(NanoPalette.ElectricCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N",
                                color = NanoPalette.MidnightBg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Welcome to NanoTV",
                            style = MaterialTheme.typography.headlineLarge,
                            color = NanoPalette.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Parent-Managed Family TV Media Organizer",
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NanoTV is designed for parents and guardians (18+) to curate exact YouTube channels, playlists, and full-length videos for their household TV — while keeping curation and settings locked behind your 4-digit Parent PIN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanoPalette.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NanoPalette.CardSurface)
                            .padding(14.dp)
                    ) {
                        Column {
                            PoweredByYouTubeBadge()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NanoTV uses YouTube API Services. By continuing, you agree to be bound by the YouTube Terms of Service (youtube.com/t/terms) and acknowledge the Google Privacy Policy (policies.google.com/privacy).",
                                color = NanoPalette.TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .width(348.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(NanoPalette.CardSurface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Step 1: Create Parent PIN",
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.WarmAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Use D-pad arrows & OK to enter 4 digits",
                        color = NanoPalette.TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    PinDotsDisplay(pinLength = pin.length)
                    Spacer(modifier = Modifier.height(14.dp))

                    PinNumericPad(
                        autoFocusFirstKey = true,
                        onDigit = { d -> if (pin.length < 4) pin += d },
                        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                        onClear = { pin = "" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    NanoActionButton(
                        label = if (pin.length == 4) "Continue →" else "Enter 4 Digits",
                        accentColor = NanoPalette.ElectricCyan,
                        enabled = pin.length == 4,
                        onClick = { step = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(continueFocusRequester)
                    )
                }
            }
        } else {
            val finishFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(80L)
                runCatching { finishFocusRequester.requestFocus() }
            }
            BackHandler { step = 1 }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Step 2: Choose Starter Channels (Optional)",
                            style = MaterialTheme.typography.headlineLarge,
                            color = NanoPalette.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select educational channels to start with, or uncheck all to curate from scratch via QR code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NanoPalette.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    NanoActionButton(
                        label = "Finish Setup & Launch",
                        icon = Icons.Rounded.Check,
                        accentColor = NanoPalette.NeonMint,
                        onClick = {
                            onComplete(pin, "Family Space", selectedIds)
                        },
                        modifier = Modifier.focusRequester(finishFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(CatalogRepository.OptionalStarterPacks, key = { it.channelId }) { pack ->
                        val checked = pack.channelId in selectedIds
                        StarterPackToggleCard(
                            title = pack.title,
                            subtitle = pack.subtitle,
                            checked = checked,
                            onToggle = {
                                selectedIds = if (checked) selectedIds - pack.channelId else selectedIds + pack.channelId
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParentPinUnlockModal(
    title: String = "Enter 4-Digit Parent PIN",
    subtitle: String = "Use remote D-pad arrows & OK to enter your Parent PIN",
    errorMessage: String?,
    onSubmitPin: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    BackHandler(onBack = onCancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NanoPalette.MidnightBg)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val hwDigit = mapHardwareDigitKey(event.key)
                if (hwDigit != null) {
                    if (pin.length < 4) {
                        val next = pin + hwDigit
                        pin = next
                        if (next.length == 4) {
                            onSubmitPin(next)
                            pin = ""
                        }
                    }
                    true
                } else if (event.key == Key.Backspace || event.key == Key.Delete) {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NanoPalette.CardSurface)
                .border(BorderStroke(2.dp, NanoPalette.WarmAmber.copy(alpha = 0.7f)), RoundedCornerShape(24.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = NanoPalette.WarmAmber,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NanoPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = NanoPalette.TextSecondary,
                fontSize = 12.sp
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = NanoPalette.CoralAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            PinDotsDisplay(pinLength = pin.length)
            Spacer(modifier = Modifier.height(16.dp))

            PinNumericPad(
                autoFocusFirstKey = true,
                onDigit = { d ->
                    if (pin.length < 4) {
                        val next = pin + d
                        pin = next
                        if (next.length == 4) {
                            onSubmitPin(next)
                            pin = ""
                        }
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onClear = { pin = "" }
            )

            Spacer(modifier = Modifier.height(16.dp))
            NanoActionButton(
                label = "Cancel",
                accentColor = NanoPalette.CardFocusedSurface,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PinDotsDisplay(pinLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { idx ->
            val filled = idx < pinLength
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (filled) NanoPalette.ElectricCyan else Color.White.copy(alpha = 0.16f))
                    .border(BorderStroke(2.dp, NanoPalette.ElectricCyan), CircleShape)
            )
        }
    }
}

@Composable
private fun PinNumericPad(
    autoFocusFirstKey: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val firstKeyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocusFirstKey) {
        if (autoFocusFirstKey) {
            delay(80L)
            runCatching { firstKeyFocusRequester.requestFocus() }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup()
    ) {
        PIN_ROWS.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowKeys.forEach { key ->
                    PinPadKeyButton(
                        keyLabel = key,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (key == "1") Modifier.focusRequester(firstKeyFocusRequester)
                                else Modifier
                            ),
                        onClick = {
                            when (key) {
                                "DEL" -> onBackspace()
                                "CLR" -> onClear()
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinPadKeyButton(
    keyLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.10f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pinBtnScale"
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) NanoPalette.WarmAmber else Color(0xFF101C30))
            .then(
                if (focused) {
                    Modifier.border(BorderStroke(3.dp, Color.White), shape)
                } else {
                    Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), shape)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (keyLabel == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Delete",
                tint = if (focused) NanoPalette.MidnightBg else NanoPalette.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = keyLabel,
                color = if (focused) NanoPalette.MidnightBg else NanoPalette.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (keyLabel == "CLR") 14.sp else 19.sp
            )
        }
    }
}

@Composable
private fun StarterPackToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) NanoPalette.CardFocusedSurface else NanoPalette.CardSurface)
            .then(
                if (focused) Modifier.border(BorderStroke(2.5.dp, NanoPalette.ElectricCyan), shape)
                else if (checked) Modifier.border(BorderStroke(1.5.dp, NanoPalette.NeonMint.copy(alpha = 0.6f)), shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) NanoPalette.NeonMint else NanoPalette.TextMuted,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NanoPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NanoPalette.TextSecondary
            )
        }
    }
}
