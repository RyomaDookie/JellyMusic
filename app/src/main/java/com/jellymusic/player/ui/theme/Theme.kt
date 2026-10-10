package com.jellymusic.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellymusic.player.preferences.AccentPalette
import com.jellymusic.player.preferences.ThemeMode
import com.jellymusic.player.preferences.UserCustomizations

private val ArtworkTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp,
        color = ArtworkTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.015).sp,
        color = ArtworkTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = ArtworkTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = ArtworkTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = ArtworkTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.sp,
        color = ArtworkTextMuted
    )
)

@Composable
fun JellyMusicTheme(
    customizations: UserCustomizations,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (customizations.themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val isAmoled = customizations.themeMode == ThemeMode.AMOLED

    val colorScheme: ColorScheme = if (customizations.accentPalette == AccentPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) {
            val base = dynamicDarkColorScheme(context)
            if (isAmoled) base.copy(background = Color.Black, surface = ArtworkSurface, surfaceVariant = ArtworkSurfaceVariant)
            else base.copy(background = ArtworkBackground, surface = ArtworkSurface, surfaceVariant = ArtworkSurfaceVariant)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        val (primary, secondary, tertiary) = when (customizations.accentPalette) {
            AccentPalette.DYNAMIC, AccentPalette.JELLYFIN_PURPLE -> Triple(ArtworkTextPrimary, JellyfinSecondary, JellyfinTertiary)
            AccentPalette.CYBERPUNK_CYAN -> Triple(CyberpunkPrimary, CyberpunkSecondary, CyberpunkTertiary)
            AccentPalette.SUNSET_AMBER -> Triple(SunsetPrimary, SunsetSecondary, SunsetTertiary)
            AccentPalette.EMERALD_FOREST -> Triple(EmeraldPrimary, EmeraldSecondary, EmeraldTertiary)
            AccentPalette.SAKURA_PINK -> Triple(SakuraPrimary, SakuraSecondary, SakuraTertiary)
        }

        if (isDark) {
            darkColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = if (isAmoled) Color.Black else ArtworkBackground,
                surface = ArtworkSurface,
                surfaceVariant = ArtworkSurfaceVariant,
                outline = ArtworkBorder,
                onPrimary = if (customizations.accentPalette == AccentPalette.JELLYFIN_PURPLE) Color.Black else Color.White,
                onBackground = ArtworkTextPrimary,
                onSurface = ArtworkTextPrimary,
                onSurfaceVariant = ArtworkTextSecondary
            )
        } else {
            lightColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = Color(0xFFF6F5F2),
                surface = Color.White,
                surfaceVariant = Color(0xFFEAEAED),
                outline = Color(0x1A000000),
                onPrimary = Color.Black,
                onBackground = Color(0xFF141318),
                onSurface = Color(0xFF141318),
                onSurfaceVariant = Color(0xFF676570)
            )
        }
    }

    val shapes = Shapes(
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(customizations.cornerRadiusDp.dp),
        large = RoundedCornerShape(20.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArtworkTypography,
        shapes = shapes,
        content = content
    )
}
