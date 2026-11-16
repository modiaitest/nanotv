package app.nanotv.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nanotv.tv.ui.NavSection
import app.nanotv.tv.ui.theme.NanoPalette

@Composable
fun SidebarNavRail(
    familySpaceName: String,
    isParentUnlocked: Boolean,
    activeSection: NavSection,
    onSelectSection: (NavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    var railExpanded by remember { mutableStateOf(false) }
    val railWidth by animateDpAsState(
        targetValue = if (railExpanded) 224.dp else 82.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "railWidth"
    )

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(NanoPalette.SidebarBg)
            .onFocusChanged { railExpanded = it.hasFocus }
            .padding(vertical = 20.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand & Family Space Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NanoPalette.ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = NanoPalette.MidnightBg,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            AnimatedVisibility(visible = railExpanded, enter = fadeIn(), exit = fadeOut()) {
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "NanoTV",
                        color = NanoPalette.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        text = familySpaceName,
                        color = NanoPalette.ElectricCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Family Viewing Sections
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SidebarNavItem(
                section = NavSection.SEARCH,
                icon = Icons.Rounded.Search,
                accent = NanoPalette.SkyBlue,
                selected = activeSection == NavSection.SEARCH,
                expanded = railExpanded,
                locked = false,
                onClick = { onSelectSection(NavSection.SEARCH) }
            )
            SidebarNavItem(
                section = NavSection.HOME,
                icon = Icons.Rounded.Home,
                accent = NanoPalette.ElectricCyan,
                selected = activeSection == NavSection.HOME,
                expanded = railExpanded,
                locked = false,
                onClick = { onSelectSection(NavSection.HOME) }
            )
            SidebarNavItem(
                section = NavSection.CHANNELS,
                icon = Icons.Rounded.Subscriptions,
                accent = NanoPalette.WarmAmber,
                selected = activeSection == NavSection.CHANNELS,
                expanded = railExpanded,
                locked = false,
                onClick = { onSelectSection(NavSection.CHANNELS) }
            )
            SidebarNavItem(
                section = NavSection.LIBRARY,
                icon = Icons.Rounded.VideoLibrary,
                accent = NanoPalette.NeonMint,
                selected = activeSection == NavSection.LIBRARY,
                expanded = railExpanded,
                locked = false,
                onClick = { onSelectSection(NavSection.LIBRARY) }
            )
        }

        // PIN-Protected Parent Management Sections
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SidebarNavItem(
                section = NavSection.PARENT_CURATE,
                icon = Icons.Rounded.QrCode2,
                accent = NanoPalette.WarmAmber,
                selected = activeSection == NavSection.PARENT_CURATE,
                expanded = railExpanded,
                locked = !isParentUnlocked,
                onClick = { onSelectSection(NavSection.PARENT_CURATE) }
            )
            SidebarNavItem(
                section = NavSection.PARENT_SETTINGS,
                icon = Icons.Rounded.Settings,
                accent = NanoPalette.RoyalViolet,
                selected = activeSection == NavSection.PARENT_SETTINGS,
                expanded = railExpanded,
                locked = !isParentUnlocked,
                onClick = { onSelectSection(NavSection.PARENT_SETTINGS) }
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    section: NavSection,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    expanded: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> accent
            selected -> accent.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        label = "navItemBg"
    )
    val contentColor = when {
        isFocused && accent != NanoPalette.RoyalViolet -> NanoPalette.MidnightBg
        isFocused -> Color.White
        selected -> accent
        else -> NanoPalette.TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(bgColor)
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, Color.White), shape)
                else if (selected) Modifier.border(BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)), shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = section.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(13.dp))
                Text(
                    text = section.label,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (expanded && locked) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Parent PIN Required",
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
