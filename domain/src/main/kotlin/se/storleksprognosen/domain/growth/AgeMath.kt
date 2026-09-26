package se.storleksprognosen.domain.growth

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** Omvandling mellan datum och ålder i (decimala) månader, som i WHO:s tabeller. */
object AgeMath {
    /** Genomsnittlig månadslängd i dagar (365,25 / 12). */
    const val DAYS_PER_MONTH = 30.4375

    fun ageInMonths(birthdate: LocalDate, date: LocalDate): Double =
        ChronoUnit.DAYS.between(birthdate, date) / DAYS_PER_MONTH

    fun dateAtAge(birthdate: LocalDate, ageMonths: Double): LocalDate =
        birthdate.plusDays((ageMonths * DAYS_PER_MONTH).roundToLong())

    /** Hela år och resterande hela månader, t.ex. (2, 3) för 2 år och 3 månader. */
    fun yearsAndMonths(birthdate: LocalDate, date: LocalDate): Pair<Int, Int> {
        val totalMonths = ChronoUnit.MONTHS.between(birthdate, date).toInt().coerceAtLeast(0)
        return totalMonths / 12 to totalMonths % 12
    }
}
