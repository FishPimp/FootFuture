package se.storleksprognosen.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Varm och lekfull färgpalett för Material 3.
val SoftSagePrimary = Color(0xFF2E6A4F)
val SoftSageContainer = Color(0xFFE2F0E7)
val SunnyYellow = Color(0xFFFFE082) // Sommar / varningar
val IceBlue = Color(0xFFE1F5FE) // Vinter
val LavenderAccent = Color(0xFF7E57C2)
val WarmBackground = Color(0xFFFAF8F5) // Varm papperskänsla för bakgrunden
val SeaBlue = Color(0xFF2B6C8F)

/**
 * Färger utanför Material-schemat: säsonger och matchningsbrickor.
 * De följer inte dynamisk färg eftersom de bär betydelse (blått = vinter osv.).
 */
@Immutable
data class ExtendedColors(
    val winter: Color,
    val onWinter: Color,
    val summer: Color,
    val onSummer: Color,
    val springFall: Color,
    val onSpringFall: Color,
    val match: Color,
    val onMatch: Color,
    val mismatch: Color,
    val onMismatch: Color,
    val gap: Color,
    val onGap: Color,
    val forecastLine: Color,
    val chartWinter: Color,
    val chartSummer: Color,
)

val LightExtendedColors = ExtendedColors(
    winter = IceBlue,
    onWinter = Color(0xFF0B4A6F),
    summer = SunnyYellow,
    onSummer = Color(0xFF5C4300),
    springFall = Color(0xFFF3E3D3),
    onSpringFall = Color(0xFF5B3A1E),
    match = Color(0xFFD5F2DC),
    onMatch = Color(0xFF14532D),
    mismatch = Color(0xFFFFDAD4),
    onMismatch = Color(0xFF8C1D12),
    gap = Color(0xFFEFECE8),
    onGap = Color(0xFF55504A),
    forecastLine = LavenderAccent,
    chartWinter = Color(0xFFCDEBFA),
    chartSummer = Color(0xFFFFF0B8),
)

val DarkExtendedColors = ExtendedColors(
    winter = Color(0xFF16394B),
    onWinter = Color(0xFFCDEBFA),
    summer = Color(0xFF4D3E0B),
    onSummer = Color(0xFFFFE9A6),
    springFall = Color(0xFF45301D),
    onSpringFall = Color(0xFFF6DCC2),
    match = Color(0xFF1C4A2C),
    onMatch = Color(0xFFBDF0CB),
    mismatch = Color(0xFF5E1C14),
    onMismatch = Color(0xFFFFDAD4),
    gap = Color(0xFF34312D),
    onGap = Color(0xFFD6D0C9),
    forecastLine = Color(0xFFCDB8FF),
    chartWinter = Color(0xFF15303F),
    chartSummer = Color(0xFF3B3113),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
