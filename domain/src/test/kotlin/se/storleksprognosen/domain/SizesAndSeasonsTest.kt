package se.storleksprognosen.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.season.SeasonPeriod
import se.storleksprognosen.domain.season.SeasonPhase
import se.storleksprognosen.domain.sizing.ClothingSizes
import se.storleksprognosen.domain.sizing.ShoeSizes
import java.time.LocalDate

class SizesAndSeasonsTest {

    @Test
    fun `shoe size is foot plus allowance in paris points rounded up`() {
        // 140 + 12 = 152 mm innerlängd → 22,8 → storlek 23.
        assertEquals(23, ShoeSizes.sizeFor(140.0, 12))
        // 148 + 12 = 160 mm = exakt storlek 24.
        assertEquals(24, ShoeSizes.sizeFor(148.0, 12))
        assertEquals(25, ShoeSizes.sizeFor(148.1, 12))
        // Större växtmån ger större sko.
        assertEquals(25, ShoeSizes.sizeFor(148.0, 17))
        assertEquals(160.0, ShoeSizes.innerLengthMm(24), 1e-9)
    }

    @Test
    fun `clothing size is smallest centilong at least as long as the child`() {
        assertEquals(44, ClothingSizes.sizeFor(30.0))
        assertEquals(50, ClothingSizes.sizeFor(44.5))
        assertEquals(92, ClothingSizes.sizeFor(92.0))
        assertEquals(98, ClothingSizes.sizeFor(92.1))
        assertEquals(104, ClothingSizes.sizeFor(100.0))
        assertEquals(listOf(44, 50, 56, 62), ClothingSizes.SELECTABLE.take(4))
    }

    @Test
    fun `winter spans the new year`() {
        val winter = SeasonPeriod(SeasonPhase.WINTER, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 31))
        assertEquals(winter, SeasonCalendar.periodContaining(LocalDate.of(2026, 12, 15)))
        assertEquals(winter, SeasonCalendar.periodContaining(LocalDate.of(2027, 2, 1)))
        assertEquals(Season.WINTER, SeasonCalendar.seasonOf(LocalDate.of(2027, 3, 31)))
        assertEquals(Season.SPRING_FALL, SeasonCalendar.seasonOf(LocalDate.of(2027, 4, 1)))
    }

    @Test
    fun `periods follow each other without gaps`() {
        val periods = SeasonCalendar.periodsFrom(LocalDate.of(2026, 9, 26)).take(9).toList()
        assertEquals(
            listOf(
                SeasonPhase.AUTUMN, SeasonPhase.WINTER, SeasonPhase.SPRING, SeasonPhase.SUMMER,
                SeasonPhase.AUTUMN, SeasonPhase.WINTER, SeasonPhase.SPRING, SeasonPhase.SUMMER,
                SeasonPhase.AUTUMN,
            ),
            periods.map { it.phase },
        )
        periods.zipWithNext().forEach { (a, b) -> assertEquals(a.endInclusive.plusDays(1), b.start) }
    }

    @Test
    fun `upcoming includes the current season and those starting within the window`() {
        val upcoming = SeasonCalendar.upcoming(LocalDate.of(2026, 9, 26), 12)
        assertEquals(5, upcoming.size)
        assertEquals(LocalDate.of(2026, 9, 1), upcoming.first().start)
        assertEquals(LocalDate.of(2027, 9, 1), upcoming.last().start)
    }
}
