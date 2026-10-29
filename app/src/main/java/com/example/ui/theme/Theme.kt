package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OceanPrimary = Color(0xFF0284C7)
val OceanSecondary = Color(0xFF0EA5E9)
val OceanBackground = Color(0xFFF8FAFC)
val OceanSurface = Color(0xFFFFFFFF)
val OceanOnPrimary = Color(0xFFFFFFFF)

val DarkOceanPrimary = Color(0xFF38BDF8)
val DarkOceanSecondary = Color(0xFF0284C7)
val DarkOceanBackground = Color(0xFF0F172A)
val DarkOceanSurface = Color(0xFF1E293B)
val DarkOceanOnPrimary = Color(0xFF0F172A)


enum class AppColorPalette(val id: String, val displayName: String) {
    OCEAN("ocean", "Ocean Blue (शाही नीला)"),
    EMERALD("emerald", "Emerald Green (पन्ना हरा)"),
    SUNSET("sunset", "Sunset Amber (सूर्यास्त नारंगी)");

    companion object {
        val DEFAULT = OCEAN
        fun fromId(id: String): AppColorPalette {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

private val LightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    background = OceanBackground,
    surface = OceanSurface,
    onPrimary = OceanOnPrimary,
    onSurface = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkOceanPrimary,
    secondary = DarkOceanSecondary,
    background = DarkOceanBackground,
    surface = DarkOceanSurface,
    onPrimary = DarkOceanOnPrimary,
    onSurface = Color(0xFFF1F5F9),
    onBackground = Color(0xFFF1F5F9)
)

@Composable
fun RaiChilledWaterTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
