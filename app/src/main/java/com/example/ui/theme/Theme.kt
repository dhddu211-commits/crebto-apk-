package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CryptoLuxuryDarkColorScheme =
  darkColorScheme(
    primary = MetallicGold,
    onPrimary = ObsidianVoid,
    primaryContainer = GoldContainer,
    onPrimaryContainer = ChampagneGold,
    secondary = EmeraldLive,
    onSecondary = ObsidianVoid,
    secondaryContainer = Color(0xFF0B2E22),
    onSecondaryContainer = EmeraldLive,
    tertiary = ChampagneGold,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = WarmIvory,
    surface = MidnightOnyx,
    onSurface = WarmIvory,
    surfaceVariant = CharcoalGlass,
    onSurfaceVariant = MutedSand,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = MidnightOnyx,
    surfaceContainer = CharcoalGlass,
    surfaceContainerHigh = ElevatedCarbon,
    outline = HairlineGoldBorder,
    outlineVariant = Color(0xFF282B38),
    error = CrimsonBan,
    onError = WarmIvory,
  )

private val CryptoShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun CryptoTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = CryptoLuxuryDarkColorScheme,
    typography = CryptoTypography,
    shapes = CryptoShapes,
    content = content,
  )
}
