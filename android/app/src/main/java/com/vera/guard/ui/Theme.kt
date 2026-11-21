package com.vera.guard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val VeraRed = Color(0xFFFF4D6A)
val VeraRedDeep = Color(0xFFD90F3A)
val VeraGreen = Color(0xFF34D399)
val VeraAmber = Color(0xFFFBBF24)
val VeraIndigo = Color(0xFF7C8CFF)
val VeraViolet = Color(0xFF9B6BFF)
val VeraTeal = Color(0xFF2DD4BF)
val VeraInk = Color(0xFF0A0F1F)

val BrandBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)))
val SosBrush: Brush get() = Brush.radialGradient(listOf(Color(0xFFFF7A8E), VeraRed, VeraRedDeep))

@Composable
fun isVeraDark(theme: String): Boolean = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }

private val veraTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodySmall = t.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    )
}

private val veraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VeraTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = isVeraDark(theme)
    val colors = if (dark) darkColorScheme(
        primary = VeraIndigo, onPrimary = VeraInk, primaryContainer = Color(0xFF29337E), onPrimaryContainer = Color(0xFFDDE3FF),
        secondary = VeraRed, onSecondary = Color.White, secondaryContainer = Color(0xFF5A1626), onSecondaryContainer = Color(0xFFFFD9DF),
        tertiary = VeraTeal, background = VeraInk, onBackground = Color(0xFFEEF2FF),
        surface = Color(0xFF131B33), onSurface = Color(0xFFEEF2FF), surfaceVariant = Color(0xFF1C2646), onSurfaceVariant = Color(0xFFA9B4D6),
        surfaceContainer = Color(0xFF131B33), surfaceContainerHigh = Color(0xFF1C2646), outline = Color(0xFF2C3862), outlineVariant = Color(0xFF232E54),
        error = VeraRed,
    ) else lightColorScheme(
        primary = Color(0xFF4F46E5), onPrimary = Color.White, primaryContainer = Color(0xFFE0E4FF), onPrimaryContainer = Color(0xFF14196B),
        secondary = Color(0xFFE11D48), secondaryContainer = Color(0xFFFFE0E6), tertiary = Color(0xFF0D9488),
        background = Color(0xFFF3F5FC), surface = Color.White, onSurface = Color(0xFF111827), onBackground = Color(0xFF111827),
        surfaceVariant = Color(0xFFE9ECF8), onSurfaceVariant = Color(0xFF55607E), outline = Color(0xFFD5DAEC), outlineVariant = Color(0xFFE3E7F4),
        surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFE9ECF8),
    )
    MaterialTheme(colorScheme = colors, typography = veraTypography, shapes = veraShapes, content = content)
}
