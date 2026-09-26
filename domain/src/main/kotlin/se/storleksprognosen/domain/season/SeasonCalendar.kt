package se.storleksprognosen.domain.season

import se.storleksprognosen.domain.model.Season
import java.time.LocalDate
import java.time.Month

/** Årets delar. Vår och höst hör båda till [Season.SPRING_FALL]. */
enum class SeasonPhase(val season: Season) {
    SPRING(Season.SPRING_FALL),
    SUMMER(Season.SUMMER),
    AUTUMN(Season.SPRING_FALL),
    WINTER(Season.WINTER),
}

/** En konkret säsong, t.ex. vintern 2026/27 (1 nov 2026 – 31 mar 2027). */
data class SeasonPeriod(
    val phase: SeasonPhase,
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    val season: Season get() = phase.season

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)

    fun next(): SeasonPeriod = SeasonCalendar.periodContaining(endInclusive.plusDays(1))
}

/**
 * Svenska klädsäsonger:
 * vinter nov–mar, vår apr–maj, sommar jun–aug och höst sep–okt.
 */
object SeasonCalendar {

    fun phaseOf(month: Month): SeasonPhase = when (month) {
        Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.MARCH -> SeasonPhase.WINTER
        Month.APRIL, Month.MAY -> SeasonPhase.SPRING
        Month.JUNE, Month.JULY, Month.AUGUST -> SeasonPhase.SUMMER
        Month.SEPTEMBER, Month.OCTOBER -> SeasonPhase.AUTUMN
    }

    fun seasonOf(date: LocalDate): Season = phaseOf(date.month).season

    fun periodContaining(date: LocalDate): SeasonPeriod {
        val year = date.year
        return when (phaseOf(date.month)) {
            SeasonPhase.SPRING -> SeasonPeriod(SeasonPhase.SPRING, LocalDate.of(year, 4, 1), LocalDate.of(year, 5, 31))
            SeasonPhase.SUMMER -> SeasonPeriod(SeasonPhase.SUMMER, LocalDate.of(year, 6, 1), LocalDate.of(year, 8, 31))
            SeasonPhase.AUTUMN -> SeasonPeriod(SeasonPhase.AUTUMN, LocalDate.of(year, 9, 1), LocalDate.of(year, 10, 31))
            SeasonPhase.WINTER -> {
                val startYear = if (date.monthValue >= Month.NOVEMBER.value) year else year - 1
                SeasonPeriod(SeasonPhase.WINTER, LocalDate.of(startYear, 11, 1), LocalDate.of(startYear + 1, 3, 31))
            }
        }
    }

    /** Säsongerna i tur och ordning, med början i den som pågår vid [date]. */
    fun periodsFrom(date: LocalDate): Sequence<SeasonPeriod> =
        generateSequence(periodContaining(date)) { it.next() }

    /** Säsonger som börjar inom [monthsAhead] månader, inklusive den pågående. */
    fun upcoming(today: LocalDate, monthsAhead: Long): List<SeasonPeriod> {
        val until = today.plusMonths(monthsAhead)
        return periodsFrom(today).takeWhile { it.start.isBefore(until) }.toList()
    }
}
