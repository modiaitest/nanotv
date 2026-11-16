package app.nanotv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.local.AppSettingsState
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.components.PoweredByYouTubeBadge
import app.nanotv.tv.ui.theme.NanoPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    isSyncing: Boolean,
    playlistCount: Int,
    videoCount: Int,
    syncMessage: String?,
    onOpenQrCurate: () -> Unit,
    onRefreshNow: () -> Unit,
    onToggleAutoplay: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onRestoreHidden: () -> Unit,
    onChangePin: (String) -> Unit,
    onLockParentSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinModal by remember { mutableStateOf(false) }

    if (showChangePinModal) {
        ParentPinUnlockModal(
            title = "Set New 4-Digit Parent PIN",
            subtitle = "Enter a new 4-digit PIN to protect Parent Curate & Settings",
            errorMessage = null,
            onSubmitPin = { newPin ->
                onChangePin(newPin)
                showChangePinModal = false
            },
            onCancel = { showChangePinModal = false }
        )
        return
    }

    val lastSyncFormatted = if (settings.lastSyncEpochMs > 0L) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(settings.lastSyncEpochMs))
    } else {
        "Never"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Parent Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = NanoPalette.TextPrimary
            )
            NanoActionButton(
                label = "Lock Parent Mode",
                icon = Icons.Rounded.Lock,
                accentColor = NanoPalette.WarmAmber,
                onClick = onLockParentSession
            )
        }

        // Sync & Curation Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NanoPalette.CardSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Curated Family Library",
                        style = MaterialTheme.typography.titleLarge,
                        color = NanoPalette.NeonMint
                    )
                    Text(
                        text = "$playlistCount shelves • $videoCount videos • Last synced: $lastSyncFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanoPalette.TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NanoActionButton(
                        label = "Curate via QR",
                        icon = Icons.Rounded.QrCode2,
                        accentColor = NanoPalette.WarmAmber,
                        onClick = onOpenQrCurate
                    )
                    NanoActionButton(
                        label = if (isSyncing) "Syncing..." else "Refresh",
                        icon = Icons.Rounded.Refresh,
                        accentColor = NanoPalette.ElectricCyan,
                        enabled = !isSyncing,
                        onClick = onRefreshNow
                    )
                }
            }

            val banner = syncMessage ?: settings.lastSyncStatus
            if (banner.isNotBlank()) {
                Text(
                    text = banner,
                    color = NanoPalette.ElectricCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Security, Autoplay, and History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NanoPalette.CardSurface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Parent PIN & Playback",
                    style = MaterialTheme.typography.titleLarge,
                    color = NanoPalette.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NanoActionButton(
                        label = "Change PIN",
                        icon = Icons.Rounded.Password,
                        accentColor = NanoPalette.RoyalViolet,
                        onClick = { showChangePinModal = true }
                    )
                    NanoActionButton(
                        label = if (settings.autoplayEnabled) "Autoplay: ON" else "Autoplay: OFF",
                        icon = Icons.Rounded.AutoMode,
                        accentColor = if (settings.autoplayEnabled) NanoPalette.NeonMint else NanoPalette.CardFocusedSurface,
                        onClick = { onToggleAutoplay(!settings.autoplayEnabled) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NanoPalette.CardSurface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Library Maintenance",
                    style = MaterialTheme.typography.titleLarge,
                    color = NanoPalette.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NanoActionButton(
                        label = "Clear History",
                        icon = Icons.Rounded.DeleteSweep,
                        accentColor = NanoPalette.CardFocusedSurface,
                        onClick = onClearHistory
                    )
                    NanoActionButton(
                        label = "Unhide Videos",
                        icon = Icons.Rounded.Visibility,
                        accentColor = NanoPalette.CardFocusedSurface,
                        onClick = onRestoreHidden
                    )
                }
            }
        }

        // Mandatory YouTube API Services ToS & Google Privacy Legal Disclosure
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NanoPalette.CardSurface.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Terms & Attribution",
                        color = NanoPalette.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "NanoTV uses YouTube API Services and is not affiliated with YouTube or Google LLC. Use is subject to the YouTube Terms of Service (youtube.com/t/terms) and Google Privacy Policy (policies.google.com/privacy).",
                        color = NanoPalette.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                PoweredByYouTubeBadge()
            }
        }
    }
}
