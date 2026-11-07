package com.evertide.riptide.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.evertide.riptide.data.local.ThemePreference

enum class ThemeMode {
    System,
    Light,
    Dark
}

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

@Composable
fun RiptideTheme(
        themePreference: ThemePreference = ThemePreference.SYSTEM,
        content: @Composable () -> Unit,
) {
    val darkTheme =
            when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

    val useDynamic =
            themePreference == ThemePreference.SYSTEM &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme =
            when {
                useDynamic -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColors
                else -> LightColors
            }

    MaterialTheme(
            colorScheme = colorScheme,
            typography = RiptideTypography,
            content = content,
    )
}
