package com.example.myapplication.ui.theme // Dein Paketname

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Import für Color hinzufügen
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Deine LightColorScheme und DarkColorScheme Definitionen bleiben hier
// Stelle sicher, dass du sowohl ein ansprechendes LightColorScheme als auch ein DarkColorScheme hast.
// Zum Beispiel:
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Lila als Primärfarbe
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6), // Türkis als Sekundärfarbe
    background = Color(0xFFFFFFFF), // Weißer Hintergrund
    surface = Color(0xFFFFFFFF), // Weiße Oberfläche für Karten etc.
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    // ... weitere Farben definieren
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Helleres Lila für Dark Mode
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFF121212), // Sehr dunkler Hintergrund
    surface = Color(0xFF1E1E1E),   // Etwas hellere Oberfläche für Karten im Dark Mode
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    // ... weitere Farben definieren
)

@Composable
fun MyApplicationTheme(
    // darkTheme: Boolean = isSystemInDarkTheme(), // Alte Zeile
    useDarkTheme: Boolean, // Neuer Parameter
    // Optional: dynamicColor weiterhin unterstützen, wenn gewünscht
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Oder eine andere passende Farbe
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Deine Typografie-Definition
        content = content
    )
}
