package com.example.diabite.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Modified to use the new Blue/Teal colors for trust and health
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue, // Main buttons, focus state
    secondary = SupportingTeal, // Secondary buttons, icons, highlights
    tertiary = PrimaryBlue, // Fallback, using primary blue
    background = BackgroundLightGray, // Soft background
    surface = NeutralWhite, // Card background
    onPrimary = NeutralWhite, // Text on primary blue buttons
    onSecondary = NeutralWhite, // Text on supporting teal
    onBackground = TextDarkGray, // Body text color
    onSurface = TextDarkGray, // Text on white cards
)

@Composable
fun DiaBiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
