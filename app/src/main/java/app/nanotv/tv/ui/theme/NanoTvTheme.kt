package app.nanotv.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NanoPalette {
    val MidnightBg = Color(0xFF0E1726)
    val SidebarBg = Color(0xFF131F33)
    val CardSurface = Color(0xFF182842)
    val CardFocusedSurface = Color(0xFF22385C)

    val ElectricCyan = Color(0xFF00E5FF)
    val NeonMint = Color(0xFF00E676)
    val WarmAmber = Color(0xFFFFB300)
    val RoyalViolet = Color(0xFF7C4DFF)
    val CoralAccent = Color(0xFFFF6E6E)
    val SkyBlue = Color(0xFF40C4FF)

    val TextPrimary = Color(0xFFF5F9FF)
    val TextSecondary = Color(0xFFB0C4DE)
    val TextMuted = Color(0xFF7890B5)

    val ShelfAccents = listOf(
        ElectricCyan,
        NeonMint,
        WarmAmber,
        RoyalViolet,
        SkyBlue,
        CoralAccent
    )

    fun accentFor(index: Int): Color =
        ShelfAccents[index.mod(ShelfAccents.size)]
}

private val NanoColorScheme = darkColorScheme(
    primary = NanoPalette.ElectricCyan,
    secondary = NanoPalette.NeonMint,
    tertiary = NanoPalette.WarmAmber,
    background = NanoPalette.MidnightBg,
    surface = NanoPalette.CardSurface,
    onPrimary = NanoPalette.MidnightBg,
    onSecondary = NanoPalette.MidnightBg,
    onTertiary = NanoPalette.MidnightBg,
    onBackground = NanoPalette.TextPrimary,
    onSurface = NanoPalette.TextPrimary
)

private val NanoTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 29.sp,
        lineHeight = 35.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )
)

@Composable
fun NanoTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NanoColorScheme,
        typography = NanoTypography,
        content = content
    )
}
