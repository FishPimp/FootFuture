package se.storleksprognosen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.storleksprognosen.domain.forecast.ForecastBasis
import se.storleksprognosen.domain.forecast.GrowthForecaster
import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.FootLengthReference
import se.storleksprognosen.domain.growth.WhoHeightReference
import se.storleksprognosen.domain.matcher.HandMeDownMatcher
import se.storleksprognosen.domain.matcher.MatchStatus
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.model.Metric
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.sizing.SizeTimeline
import se.storleksprognosen.domain.sizing.SizingSettings
import se.storleksprognosen.domain.sizing.sizeAt
import java.time.LocalDate

class ForecastAndMatcherTest {

    private val today = LocalDate.of(2026, 9, 26)
    private val settings = SizingSettings()
    private val matcher = HandMeDownMatcher(settings)

    private val bigBrother = Child("bror", "Storebror", Gender.BOY, LocalDate.of(2022, 5, 10))
    private val littleSister = Child("syster", "Lillasyster", Gender.GIRL, LocalDate.of(2024, 11, 2))

    private fun heightAt(child: Child, date: LocalDate, z: Double) =
        WhoHeightReference.valueAt(child.gender, AgeMath.ageInMonths(child.birthdate, date), z).toFloat()

    private fun footAt(child: Child, date: LocalDate, z: Double) =
        FootLengthReference.valueAt(child.gender, AgeMath.ageInMonths(child.birthdate, date), z).toFloat()

    @Test
    fun `without measurements the forecast follows the median`() {
        val forecast = GrowthForecaster.forecast(littleSister, emptyList())
        assertEquals(ForecastBasis.MEDIAN, forecast.height.basis)
        assertEquals(ForecastBasis.MEDIAN, forecast.foot.basis)
        assertEquals(50.0, forecast.height.percentile, 1e-6)
        assertEquals(WhoHeightReference.lms(Gender.GIRL, 30.0).m, forecast.valueAtAge(Metric.HEIGHT_CM, 30.0), 1e-9)
    }

    @Test
    fun `forecast follows the percentile channel of the latest measurement`() {
        val older = Measurement("m1", littleSister.id, LocalDate.of(2025, 5, 2), footMm = null, heightCm = heightAt(littleSister, LocalDate.of(2025, 5, 2), -1.0))
        val latest = Measurement("m2", littleSister.id, LocalDate.of(2026, 5, 2), footMm = null, heightCm = heightAt(littleSister, LocalDate.of(2026, 5, 2), 1.0))
        val forecast = GrowthForecaster.forecast(littleSister, listOf(latest, older))

        assertEquals(ForecastBasis.MEASURED, forecast.height.basis)
        assertEquals(latest, forecast.height.basedOn)
        assertEquals(1.0, forecast.height.zScore, 1e-4)
        // Vid 36 mån: 95,0515 × (1 + 0,04006) ≈ 98,86 cm.
        assertEquals(98.86, forecast.valueAtAge(Metric.HEIGHT_CM, 36.0), 0.01)
        // Ingen fotmätning: foten antas följa längdens kanal.
        assertEquals(ForecastBasis.FROM_HEIGHT, forecast.foot.basis)
        assertEquals(1.0, forecast.foot.zScore, 1e-4)
    }

    @Test
    fun `measured foot overrides the height channel and other children are ignored`() {
        val date = LocalDate.of(2026, 8, 1)
        val measurements = listOf(
            Measurement("m1", bigBrother.id, date, footMm = footAt(bigBrother, date, -0.5), heightCm = heightAt(bigBrother, date, 1.5)),
            Measurement("m2", littleSister.id, date, footMm = 999f, heightCm = 999f),
        )
        val forecast = GrowthForecaster.forecast(bigBrother, measurements)
        assertEquals(1.5, forecast.height.zScore, 1e-4)
        assertEquals(ForecastBasis.MEASURED, forecast.foot.basis)
        assertEquals(-0.5, forecast.foot.zScore, 1e-4)
    }

    @Test
    fun `extreme measurements are clamped`() {
        val date = LocalDate.of(2026, 8, 1)
        val forecast = GrowthForecaster.forecast(
            bigBrother,
            listOf(Measurement("m", bigBrother.id, date, footMm = null, heightCm = 300f)),
        )
        assertEquals(GrowthForecaster.MAX_ABS_Z, forecast.height.zScore, 1e-9)
    }

    @Test
    fun `size timeline is contiguous and increasing`() {
        val forecast = GrowthForecaster.forecast(bigBrother, emptyList())
        for (category in Category.entries) {
            val windows = SizeTimeline.build(forecast, category, settings)
            windows.zipWithNext().forEach { (a, b) ->
                assertEquals(a.endExclusive, b.start)
                assertTrue(b.size > a.size)
            }
            assertEquals(bigBrother.birthdate, windows.first().start)
            assertNull(windows.last().endExclusive)
        }
    }

    @Test
    fun `winter item in the size needed next winter matches`() {
        val forecast = GrowthForecaster.forecast(littleSister, emptyList())
        val nextWinter = SeasonCalendar.periodsFrom(today).first { it.season == Season.WINTER }
        val size = matcher.neededSize(forecast, Category.SHOES, nextWinter)
        val item = InventoryItem("i", bigBrother.id, "Vinterkängor", Category.SHOES, size, Season.WINTER)

        val status = matcher.evaluate(item, forecast, today)
        assertTrue(status is MatchStatus.Match)
        assertEquals(nextWinter, (status as MatchStatus.Match).period)
        // Behovet räknas på säsongens sista dag.
        assertEquals(forecast.sizeAt(Category.SHOES, nextWinter.endInclusive, settings), size)
    }

    @Test
    fun `size reached outside the item's season is flagged as wrong season`() {
        val forecast = GrowthForecaster.forecast(littleSister, emptyList())
        val winters = SeasonCalendar.periodsFrom(today)
            .takeWhile { !it.endInclusive.isAfter(forecast.horizon) }
            .filter { it.season == Season.WINTER }
            .map { matcher.neededSize(forecast, Category.SHOES, it) }
            .toSet()
        val window = SizeTimeline.build(forecast, Category.SHOES, settings)
            .first { it.start.isAfter(today) && it.size !in winters && it.endExclusive != null }

        val item = InventoryItem("i", bigBrother.id, "Vinterkängor", Category.SHOES, window.size, Season.WINTER)
        val status = matcher.evaluate(item, forecast, today)

        assertTrue(status is MatchStatus.WrongSeason)
        status as MatchStatus.WrongSeason
        assertEquals(window.start, status.reachesOn)
        assertEquals(window.endExclusive, status.outgrowsOn)
    }

    @Test
    fun `too small is outgrown and too big is beyond the forecast`() {
        val forecast = GrowthForecaster.forecast(bigBrother, emptyList())
        val current = forecast.sizeAt(Category.SHOES, today, settings)

        val small = InventoryItem("s", littleSister.id, "Sandaler", Category.SHOES, current - 2, Season.SUMMER)
        assertTrue(matcher.evaluate(small, forecast, today) is MatchStatus.Outgrown)

        val huge = InventoryItem("h", littleSister.id, "Stövlar", Category.SHOES, 38, Season.WINTER)
        assertTrue(matcher.evaluate(huge, forecast, today) is MatchStatus.BeyondForecast)
    }

    @Test
    fun `all year item in the current size matches from today`() {
        val forecast = GrowthForecaster.forecast(bigBrother, emptyList())
        val current = forecast.sizeAt(Category.CLOTHES, today, settings)
        val item = InventoryItem("a", littleSister.id, "Mjukisbyxor", Category.CLOTHES, current, Season.ALL_YEAR)

        val status = matcher.evaluate(item, forecast, today) as MatchStatus.Match
        assertNull(status.period)
        assertEquals(today, status.from)
        assertNotNull(status.until)
    }

    @Test
    fun `items are evaluated against siblings but not the owner`() {
        val forecasts = listOf(bigBrother, littleSister).map { GrowthForecaster.forecast(it, emptyList()) }
        val item = InventoryItem("i", bigBrother.id, "Overall", Category.CLOTHES, 98, Season.WINTER)

        val evaluation = matcher.evaluate(listOf(item), forecasts, today).single()
        assertEquals(listOf(littleSister), evaluation.statuses.map { it.child })

        // Med bara ett barn utvärderas ägaren själv.
        val alone = matcher.evaluate(listOf(item), forecasts.take(1), today).single()
        assertEquals(listOf(bigBrother), alone.statuses.map { it.child })
    }

    @Test
    fun `season needs report gaps and covering items`() {
        val forecast = GrowthForecaster.forecast(littleSister, emptyList())
        val nextWinter = SeasonCalendar.periodsFrom(today).first { it.season == Season.WINTER }
        val bootSize = matcher.neededSize(forecast, Category.SHOES, nextWinter)
        val boots = InventoryItem("b", bigBrother.id, "Kängor", Category.SHOES, bootSize, Season.WINTER)
        val summerOnly = InventoryItem("s", bigBrother.id, "Sandaler", Category.SHOES, bootSize, Season.SUMMER)

        val needs = matcher.seasonNeeds(listOf(forecast), listOf(boots, summerOnly), today)
        assertEquals(SeasonCalendar.upcoming(today, 12).size * Category.entries.size, needs.size)

        val winterShoes = needs.single { it.period == nextWinter && it.category == Category.SHOES }
        assertEquals(bootSize, winterShoes.size)
        assertEquals(listOf(boots), winterShoes.coveredBy)

        val winterClothes = needs.single { it.period == nextWinter && it.category == Category.CLOTHES }
        assertTrue(winterClothes.isGap)
    }
}
