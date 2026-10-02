package com.addedomarus.daybudget.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.addedomarus.daybudget.core.PaletteColor

@Composable
fun DayBudgetTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        content = content,
    )
}

private val base = mapOf(
    PaletteColor.Terracotta to 0xFFC8714A,
    PaletteColor.Mustard to 0xFFE0B54E,
    PaletteColor.Raspberry to 0xFFA3485C,
    PaletteColor.Sage to 0xFF7E9670,
    PaletteColor.Slate to 0xFF5F6F82,
    PaletteColor.Teal to 0xFF2E8C88,
    PaletteColor.Plum to 0xFF7A4B8C,
    PaletteColor.Sky to 0xFF6FA8DC,
)

/** The palette colour nudged up to 15° toward the scheme's primary hue, so it sits with the wallpaper. */
@Composable @ReadOnlyComposable
fun PaletteColor.color(): Color {
    val own = FloatArray(3).also { android.graphics.Color.colorToHSV(base.getValue(this).toInt(), it) }
    val primary = FloatArray(3).also { android.graphics.Color.colorToHSV(MaterialTheme.colorScheme.primary.toArgb(), it) }
    val delta = ((primary[0] - own[0] + 540) % 360) - 180
    own[0] = (own[0] + delta.coerceIn(-15f, 15f) * 0.5f + 360) % 360
    return Color(android.graphics.Color.HSVToColor(own))
}

/** Spent parts are the priority colour sunk into the ring surface. */
@Composable @ReadOnlyComposable
fun PaletteColor.spent(surface: Color): Color = lerp(color(), surface, if (surface.luminance() > 0.5f) 0.62f else 0.58f)

fun Color.onColor(): Color = if (luminance() > 0.45f) Color(0xFF1B1B1B) else Color.White
