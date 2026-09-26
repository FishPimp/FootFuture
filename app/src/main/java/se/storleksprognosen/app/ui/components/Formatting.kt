package se.storleksprognosen.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import se.storleksprognosen.app.R
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonPeriod
import se.storleksprognosen.domain.season.SeasonPhase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val Swedish: Locale = Locale.forLanguageTag("sv-SE")

/** Decimaltal med svenskt decimalkomma, t.ex. "92,4". */
fun formatDecimal(value: Double, decimals: Int = 1): String =
    String.format(Swedish, "%.${decimals}f", value)

fun formatDecimal(value: Float, decimals: Int = 1): String = formatDecimal(value.toDouble(), decimals)

/** Tolkar inmatning med komma eller punkt. */
fun parseDecimal(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()

@Composable
fun monthYear(date: LocalDate): String =
    stringResource(R.string.month_year, stringArrayResource(R.array.months)[date.monthValue - 1], date.year)

@Composable
fun shortMonthYear(date: LocalDate): String =
    stringResource(R.string.month_year, stringArrayResource(R.array.months_short)[date.monthValue - 1], date.year)

@Composable
fun fullDate(date: LocalDate): String =
    stringResource(R.string.day_month_year, date.dayOfMonth, stringArrayResource(R.array.months)[date.monthValue - 1], date.year)

/** "2 år 3 mån", "5 mån" eller "Nyfödd". */
@Composable
fun ageText(birthdate: LocalDate, date: LocalDate): String {
    val months = ChronoUnit.MONTHS.between(birthdate, date).toInt().coerceAtLeast(0)
    return durationText(months, zeroIsNewborn = true)
}

@Composable
fun durationText(totalMonths: Int, zeroIsNewborn: Boolean = false): String {
    val months = abs(totalMonths)
    val years = months / 12
    val rest = months % 12
    return when {
        months == 0 && zeroIsNewborn -> stringResource(R.string.age_newborn)
        years == 0 -> stringResource(R.string.age_months, rest)
        rest == 0 -> stringResource(R.string.age_years, years)
        else -> stringResource(R.string.age_years_months, years, rest)
    }
}

/** "Vinter 2026/27", "Sommar 2027" … */
@Composable
fun periodLabel(period: SeasonPeriod): String {
    val year = period.start.year
    return when (period.phase) {
        SeasonPhase.SPRING -> stringResource(R.string.period_spring, year)
        SeasonPhase.SUMMER -> stringResource(R.string.period_summer, year)
        SeasonPhase.AUTUMN -> stringResource(R.string.period_autumn, year)
        SeasonPhase.WINTER -> stringResource(R.string.period_winter, year, (year + 1) % 100)
    }
}

/** "vintern 2026/27", "sommaren 2027" … för användning mitt i en mening. */
@Composable
fun periodInSentence(period: SeasonPeriod): String {
    val year = period.start.year
    return when (period.phase) {
        SeasonPhase.SPRING -> stringResource(R.string.period_spring_in_sentence, year)
        SeasonPhase.SUMMER -> stringResource(R.string.period_summer_in_sentence, year)
        SeasonPhase.AUTUMN -> stringResource(R.string.period_autumn_in_sentence, year)
        SeasonPhase.WINTER -> stringResource(R.string.period_winter_in_sentence, year, (year + 1) % 100)
    }
}

fun phaseEmoji(phase: SeasonPhase): String = when (phase) {
    SeasonPhase.SPRING -> "🌷"
    SeasonPhase.SUMMER -> "☀️"
    SeasonPhase.AUTUMN -> "🍂"
    SeasonPhase.WINTER -> "❄️"
}

fun seasonEmoji(season: Season): String = when (season) {
    Season.WINTER -> "❄️"
    Season.SUMMER -> "☀️"
    Season.SPRING_FALL -> "🍂"
    Season.ALL_YEAR -> "🔁"
}

@Composable
fun seasonName(season: Season): String = stringResource(
    when (season) {
        Season.WINTER -> R.string.season_winter
        Season.SUMMER -> R.string.season_summer
        Season.SPRING_FALL -> R.string.season_spring_fall
        Season.ALL_YEAR -> R.string.season_all_year
    }
)

fun categoryEmoji(category: Category): String = when (category) {
    Category.SHOES -> "👟"
    Category.CLOTHES -> "👕"
}

@Composable
fun categoryName(category: Category): String = stringResource(
    when (category) {
        Category.SHOES -> R.string.category_shoes
        Category.CLOTHES -> R.string.category_clothes
    }
)

fun genderEmoji(gender: Gender): String = when (gender) {
    Gender.GIRL -> "👧"
    Gender.BOY -> "👦"
}
