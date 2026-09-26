package se.storleksprognosen.domain.sizing

import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Metric
import java.time.LocalDate
import kotlin.math.ceil

data class SizingSettings(
    /** Växtmån: hur mycket längre skons innerlängd ska vara än foten. */
    val shoeAllowanceMm: Int = ShoeSizes.DEFAULT_ALLOWANCE_MM,
)

/** EU-skostorlekar (parisstick). */
object ShoeSizes {
    /** En EU-storlek motsvarar 2/3 cm innerlängd. */
    const val PARIS_POINT_MM = 20.0 / 3.0

    /** Vanlig rekommendation är 12–17 mm växtmån för barnskor. */
    const val DEFAULT_ALLOWANCE_MM = 12
    val ALLOWANCE_RANGE_MM = 8..20

    /** Storlekar som går att välja för plagg i förrådet. */
    val SELECTABLE = 16..38

    /** Minsta storlek vars innerlängd rymmer foten plus växtmån. */
    fun sizeFor(footMm: Double, allowanceMm: Int = DEFAULT_ALLOWANCE_MM): Int =
        ceil((footMm + allowanceMm) / PARIS_POINT_MM - ROUNDING_TOLERANCE).toInt()

    fun innerLengthMm(size: Int): Double = size * PARIS_POINT_MM

    private const val ROUNDING_TOLERANCE = 1e-6
}

/** Nordiska klädstorlekar (centilong): storleken anger barnets längd i cm, i steg om 6 cm. */
object ClothingSizes {
    const val SMALLEST = 44
    const val STEP_CM = 6

    /** Storlekar som går att välja för plagg i förrådet. */
    val SELECTABLE: List<Int> = (SMALLEST..152 step STEP_CM).toList()

    /** Minsta storlek som är minst lika lång som barnet. */
    fun sizeFor(heightCm: Double): Int {
        if (heightCm <= SMALLEST) return SMALLEST
        val steps = ceil((heightCm - SMALLEST) / STEP_CM - ROUNDING_TOLERANCE).toInt()
        return SMALLEST + steps * STEP_CM
    }

    private const val ROUNDING_TOLERANCE = 1e-6
}

object Sizes {
    fun sizeFor(category: Category, value: Double, settings: SizingSettings): Int = when (category) {
        Category.SHOES -> ShoeSizes.sizeFor(value, settings.shoeAllowanceMm)
        Category.CLOTHES -> ClothingSizes.sizeFor(value)
    }

    fun metricFor(category: Category): Metric = when (category) {
        Category.SHOES -> Metric.FOOT_MM
        Category.CLOTHES -> Metric.HEIGHT_CM
    }
}

/** Rekommenderad storlek enligt prognosen vid ett visst datum. */
fun ChildForecast.sizeAt(category: Category, date: LocalDate, settings: SizingSettings): Int =
    Sizes.sizeFor(category, valueAt(Sizes.metricFor(category), date), settings)
