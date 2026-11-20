package com.vera.guard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VeraRed = Color(0xFFE5484D)
val VeraGreen = Color(0xFF2FB877)
val VeraAmber = Color(0xFFF5A524)

@Composable
fun VeraTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val colors = if (dark) darkColorScheme(primary = Color(0xFF8EC5FF), secondary = VeraRed, background = Color(0xFF0F1720), surface = Color(0xFF17212D), onBackground = Color(0xFFEEF3F8), onSurface = Color(0xFFEEF3F8))
    else lightColorScheme(primary = Color(0xFF1D4ED8), secondary = VeraRed)
    MaterialTheme(colorScheme = colors, content = content)
}
