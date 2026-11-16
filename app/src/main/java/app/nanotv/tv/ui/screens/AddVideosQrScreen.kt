package app.nanotv.tv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.data.local.CuratedSourceEntity
import app.nanotv.tv.server.LocalCurationServer
import app.nanotv.tv.ui.components.NanoActionButton
import app.nanotv.tv.ui.qr.QrCodeCard
import app.nanotv.tv.ui.theme.NanoPalette

@Composable
fun AddVideosQrScreen(
    curatedSources: List<CuratedSourceEntity>,
    totalVideos: Int,
    isSyncing: Boolean,
    onRefreshNow: () -> Unit,
    onLockParentSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portalUrl = remember {
        LocalCurationServer.findLocalIpv4()?.let { "http://$it:8485" }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(NanoPalette.CardSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (portalUrl != null) {
                QrCodeCard(content = portalUrl, size = 212.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scan with Phone Camera",
                    style = MaterialTheme.typography.titleLarge,
                    color = NanoPalette.WarmAmber
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NanoPalette.MidnightBg)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = portalUrl,
                        color = NanoPalette.ElectricCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your 4-digit Parent PIN on your phone to unlock remote curation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanoPalette.TextSecondary
                )
            } else {
                Text(
                    text = "Connect TV to Wi-Fi to display QR code",
                    style = MaterialTheme.typography.titleLarge,
                    color = NanoPalette.WarmAmber
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Parent Curation Portal",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NanoPalette.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add YouTube videos, playlists, or @channels from your phone • Shorts blocked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanoPalette.TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NanoActionButton(
                        label = if (isSyncing) "Syncing..." else "Refresh",
                        icon = Icons.Rounded.Refresh,
                        accentColor = NanoPalette.ElectricCyan,
                        enabled = !isSyncing,
                        onClick = onRefreshNow
                    )
                    NanoActionButton(
                        label = "Lock & Exit",
                        icon = Icons.Rounded.Lock,
                        accentColor = NanoPalette.WarmAmber,
                        onClick = onLockParentSession
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Active Shelves (${curatedSources.size} sources • $totalVideos videos)",
                style = MaterialTheme.typography.titleMedium,
                color = NanoPalette.NeonMint
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(curatedSources, key = { it.sourceId }) { src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NanoPalette.CardSurface)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NanoPalette.ElectricCyan.copy(alpha = 0.18f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = src.sourceType,
                                    color = NanoPalette.ElectricCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = src.shelfTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = NanoPalette.TextPrimary
                            )
                        }
                        Text(
                            text = "PIN Protected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NanoPalette.TextMuted
                        )
                    }
                }
            }
        }
    }
}
