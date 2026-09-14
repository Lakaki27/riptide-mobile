package com.evertide.riptide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
        lightColorScheme(
                primary = LightAccent,
                onPrimary = Color.White,
                background = LightBg,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightVioletPale,
                onSurfaceVariant = LightTextMuted,
                outline = LightBorder,
                error = DangerRed,
        )

private val DarkColors =
        darkColorScheme(
                primary = DarkAccent,
                onPrimary = DarkBg,
                background = DarkBg,
                onBackground = DarkTextPrimary,
                surface = DarkSurface,
                onSurface = DarkTextPrimary,
                surfaceVariant = DarkVioletPale,
                onSurfaceVariant = DarkTextMuted,
                outline = DarkBorder,
                error = DangerRed,
        )

/**
 * [darkTheme] follows your app's own theme preference (system/light/dark), resolved by the caller —
 * not hardcoded to [isSystemInDarkTheme] so Settings (Phase 9) can override it explicitly, same as
 * the web app's themeStore.
 */
@Composable
fun RiptideTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
            colorScheme = colorScheme,
            typography = RiptideTypography,
            content = content,
    )
}
