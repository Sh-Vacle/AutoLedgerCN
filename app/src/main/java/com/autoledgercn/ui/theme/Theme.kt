package com.autoledgercn.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.autoledgercn.domain.model.ThemeMode

data class MascotPalette(
    val mode: ThemeMode,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val text: Color
)

fun paletteFor(mode: ThemeMode): MascotPalette = when (mode) {
    ThemeMode.YIER -> MascotPalette(mode, Color(0xFFFFFCFE), Color(0xFFFFFFFF), Color(0xFFFFF1F7), Color(0xFFFF5FA2), Color(0xFFE8CBD6), Color(0xFFFF7AB8), Color(0xFF2F2630))
    ThemeMode.BUBU -> MascotPalette(mode, Color(0xFFFFF7EE), Color(0xFFFFFEFA), Color(0xFFFFEBD4), Color(0xFFA9652F), Color(0xFFD8B894), Color(0xFFC88445), Color(0xFF332820))
    // Old stored value compatibility; it is not exposed as a selectable theme.
    ThemeMode.COUPLE -> MascotPalette(ThemeMode.YIER, Color(0xFFFFFCFE), Color(0xFFFFFFFF), Color(0xFFFFF1F7), Color(0xFFFF5FA2), Color(0xFFE8CBD6), Color(0xFFFF7AB8), Color(0xFF2F2630))
}

@Composable
fun AutoLedgerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val p = paletteFor(mode)
    val scheme: ColorScheme = lightColorScheme(
        primary = p.primary,
        secondary = p.secondary,
        tertiary = p.accent,
        background = p.background,
        surface = p.surface,
        surfaceVariant = p.surfaceAlt,
        primaryContainer = p.surfaceAlt,
        secondaryContainer = p.surfaceAlt,
        tertiaryContainer = p.accent.copy(alpha = 0.18f),
        outline = p.secondary,
        outlineVariant = p.secondary.copy(alpha = 0.42f),
        onPrimary = Color.White,
        onSecondary = Color(0xFF2E211D),
        onPrimaryContainer = p.text,
        onSecondaryContainer = p.text,
        onTertiaryContainer = p.text,
        onBackground = p.text,
        onSurface = p.text,
        onSurfaceVariant = p.text
    )
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
