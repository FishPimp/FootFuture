package se.storleksprognosen.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import se.storleksprognosen.app.R

/** Rundad och lekfull rubrikstil. */
val Fredoka = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

/** Mjuk och välkomnande brödtext. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

val PlayfulTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        displaySmall = base.displaySmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
        bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
        labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    )
}
