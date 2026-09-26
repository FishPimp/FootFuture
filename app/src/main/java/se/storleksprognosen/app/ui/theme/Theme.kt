package se.storleksprognosen.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = SoftSagePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SoftSageContainer,
    onPrimaryContainer = Color(0xFF0B3A26),
    secondary = LavenderAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBE0FF),
    onSecondaryContainer = Color(0xFF2A1161),
    tertiary = SeaBlue,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3ECFA),
    onTertiaryContainer = Color(0xFF052D42),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = WarmBackground,
    onBackground = Color(0xFF1C1B19),
    surface = WarmBackground,
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFE8E4DC),
    onSurfaceVariant = Color(0xFF4A4740),
    outline = Color(0xFF7B776F),
    outlineVariant = Color(0xFFCCC7BE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F3EE),
    surfaceContainer = Color(0xFFF1EEE8),
    surfaceContainerHigh = Color(0xFFEBE8E2),
    surfaceContainerHighest = Color(0xFFE5E2DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93D5B2),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF0F5137),
    onPrimaryContainer = Color(0xFFAEF2CD),
    secondary = Color(0xFFCDB8FF),
    onSecondary = Color(0xFF391E72),
    secondaryContainer = Color(0xFF50378A),
    onSecondaryContainer = Color(0xFFEBE0FF),
    tertiary = Color(0xFF9CCEEE),
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = Color(0xFF0D4C6B),
    onTertiaryContainer = Color(0xFFD3ECFA),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121412),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF121412),
    onSurface = Color(0xFFE4E2DD),
    surfaceVariant = Color(0xFF45483F),
    onSurfaceVariant = Color(0xFFC7C7BD),
    outline = Color(0xFF919288),
    outlineVariant = Color(0xFF45483F),
    surfaceContainerLowest = Color(0xFF0D0F0D),
    surfaceContainerLow = Color(0xFF1A1C1A),
    surfaceContainer = Color(0xFF1E201E),
    surfaceContainerHigh = Color(0xFF282B28),
    surfaceContainerHighest = Color(0xFF333532),
)

/** Mjuka former: 24 dp för kort, 28 dp för dialoger och bladen. */
private val PlayfulShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Form för kort i hela appen. */
val CardShape = RoundedCornerShape(24.dp)

/** Pillerform för statusbrickor och knappar. */
val PillShape = RoundedCornerShape(50)

@Composable
fun StorleksprognosenTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlayfulTypography,
            shapes = PlayfulShapes,
            content = content,
        )
    }
}

/** Genväg: `MaterialTheme.extendedColors`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
