package se.storleksprognosen.app.data.demo

import se.storleksprognosen.domain.forecast.GrowthForecaster
import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.FootLengthReference
import se.storleksprognosen.domain.growth.WhoHeightReference
import se.storleksprognosen.domain.matcher.HandMeDownMatcher
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.repository.InventoryRepository
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.sizing.SizeTimeline
import se.storleksprognosen.domain.sizing.SizingSettings
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

/** Exempeldata så att man kan utforska appen utan egna mätningar. */
class DemoData(
    private val childRepository: ChildRepository,
    private val inventoryRepository: InventoryRepository,
) {

    suspend fun load(
        names: DemoNames,
        today: LocalDate = LocalDate.now(),
        sizing: SizingSettings = SizingSettings(),
    ): String {
        val brother = Child(newId(), names.brother, Gender.BOY, today.minusYears(4).minusMonths(3))
        val sister = Child(newId(), names.sister, Gender.GIRL, today.minusYears(1).minusMonths(8))
        listOf(brother, sister).forEach { childRepository.saveChild(it) }

        val measurements = measurements(brother, heightZ = 0.6, footZ = 0.3, ages = listOf(6, 12, 24, 36, 48), footFrom = 36) +
            measurements(sister, heightZ = -0.2, footZ = 0.1, ages = listOf(2, 6, 12, 18), footFrom = 18)
        measurements.forEach { childRepository.saveMeasurement(it) }

        demoItems(brother, sister, measurements, names, today, sizing).forEach { inventoryRepository.saveItem(it) }
        return sister.id
    }

    private fun measurements(
        child: Child,
        heightZ: Double,
        footZ: Double,
        ages: List<Int>,
        footFrom: Int,
    ): List<Measurement> = ages.map { months ->
        val date = child.birthdate.plusMonths(months.toLong())
        val age = AgeMath.ageInMonths(child.birthdate, date)
        Measurement(
            id = newId(),
            childId = child.id,
            date = date,
            heightCm = WhoHeightReference.valueAt(child.gender, age, heightZ).roundTo(1),
            footMm = if (months >= footFrom) FootLengthReference.valueAt(child.gender, age, footZ).roundTo(0) else null,
        )
    }

    /** Plagg efter storebror, valda så att matrisen visar både träffar, missar och luckor. */
    private fun demoItems(
        brother: Child,
        sister: Child,
        measurements: List<Measurement>,
        names: DemoNames,
        today: LocalDate,
        sizing: SizingSettings,
    ): List<InventoryItem> {
        val matcher = HandMeDownMatcher(sizing)
        val forecast = GrowthForecaster.forecast(sister, measurements)
        val periods = SeasonCalendar.periodsFrom(today).take(8).toList()
        val nextWinter = periods.first { it.season == Season.WINTER }
        val nextSummer = periods.first { it.season == Season.SUMMER }
        val winterShoeSizes = periods.filter { it.season == Season.WINTER }
            .map { matcher.neededSize(forecast, Category.SHOES, it) }
            .toSet()
        val offSeasonShoeSize = SizeTimeline.build(forecast, Category.SHOES, sizing)
            .firstOrNull { it.start.isAfter(today) && it.size !in winterShoeSizes }
            ?.size

        fun item(title: String, category: Category, size: Int, season: Season) =
            InventoryItem(newId(), brother.id, title, category, size, season)

        return listOfNotNull(
            item(names.winterBoots, Category.SHOES, matcher.neededSize(forecast, Category.SHOES, nextWinter), Season.WINTER),
            item(names.snowsuit, Category.CLOTHES, matcher.neededSize(forecast, Category.CLOTHES, nextWinter), Season.WINTER),
            item(names.sandals, Category.SHOES, matcher.neededSize(forecast, Category.SHOES, nextSummer), Season.SUMMER),
            offSeasonShoeSize?.let { item(names.linedBoots, Category.SHOES, it, Season.WINTER) },
            item(names.bodysuits, Category.CLOTHES, 62, Season.ALL_YEAR),
        )
    }

    private fun Double.roundTo(decimals: Int): Float {
        val factor = if (decimals == 0) 1.0 else 10.0
        return ((this * factor).roundToInt() / factor).toFloat()
    }

    private fun newId() = UUID.randomUUID().toString()
}

/** Lokaliserade namn för exempeldatan. */
data class DemoNames(
    val brother: String,
    val sister: String,
    val winterBoots: String,
    val snowsuit: String,
    val sandals: String,
    val linedBoots: String,
    val bodysuits: String,
)
