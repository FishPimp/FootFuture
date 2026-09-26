package se.storleksprognosen.domain.growth

import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Metric
import kotlin.math.floor
import kotlin.math.sqrt

/** En tillväxtreferens som ger LMS-parametrar per kön och ålder (i månader). */
interface GrowthReference {
    val metric: Metric
    val maxAgeMonths: Double

    fun lms(gender: Gender, ageMonths: Double): Lms

    fun zScore(gender: Gender, ageMonths: Double, value: Double): Double =
        lms(gender, ageMonths).zScore(value)

    fun valueAt(gender: Gender, ageMonths: Double, z: Double): Double =
        lms(gender, ageMonths).valueAt(z)

    /** Punkter (ålder i månader, värde) för en percentilkurva. */
    fun curve(gender: Gender, z: Double, stepMonths: Double = 0.5): List<Pair<Double, Double>> {
        val steps = (maxAgeMonths / stepMonths).toInt()
        return (0..steps).map { i ->
            val age = (i * stepMonths).coerceAtMost(maxAgeMonths)
            age to valueAt(gender, age, z)
        }
    }
}

/** WHO:s kroppslängd (cm) 0–72 månader. Linjär interpolation mellan hela månader. */
object WhoHeightReference : GrowthReference {
    override val metric = Metric.HEIGHT_CM
    override val maxAgeMonths = WhoHeightForAge.MAX_MONTH.toDouble()

    override fun lms(gender: Gender, ageMonths: Double): Lms {
        val median = if (gender == Gender.BOY) WhoHeightForAge.boysMedian else WhoHeightForAge.girlsMedian
        val cv = if (gender == Gender.BOY) WhoHeightForAge.boysCv else WhoHeightForAge.girlsCv
        val age = ageMonths.coerceIn(0.0, maxAgeMonths)
        val index = floor(age).toInt().coerceAtMost(WhoHeightForAge.MAX_MONTH - 1)
        val t = age - index
        return Lms(
            l = 1.0,
            m = lerp(median[index], median[index + 1], t),
            s = lerp(cv[index], cv[index + 1], t),
        )
    }
}

/**
 * Fotlängd (mm) härledd ur WHO:s kroppslängd.
 *
 * Det finns ingen officiell WHO- eller BVC-kurva för fotlängd. Hos barn 0–6 år är
 * fotlängden ungefär 15,6–15,8 % av kroppslängden (antropometriska studier), så
 * medianen blir `kvot(ålder) × medianlängd`. Spridningen kombinerar längdens
 * variationskoefficient med kvotens egen variation ([RATIO_CV]), vilket ger en
 * variationskoefficient på cirka 5,5–6 % – i nivå med uppmätt spridning hos barn.
 *
 * Kurvorna är därför en uppskattning och ska presenteras som sådan.
 */
object FootLengthReference : GrowthReference {
    override val metric = Metric.FOOT_MM
    override val maxAgeMonths = WhoHeightReference.maxAgeMonths

    /** Kvotens variationskoefficient (fotlängd / kroppslängd). */
    const val RATIO_CV = 0.042

    /** Stödpunkter (ålder i månader, fotlängd / kroppslängd). */
    private val ratioKnots = listOf(
        0.0 to 0.156,
        12.0 to 0.157,
        48.0 to 0.157,
        72.0 to 0.158,
    )

    fun footToHeightRatio(ageMonths: Double): Double {
        val age = ageMonths.coerceIn(ratioKnots.first().first, ratioKnots.last().first)
        val upper = ratioKnots.indexOfFirst { it.first >= age }.coerceAtLeast(1)
        val (a0, r0) = ratioKnots[upper - 1]
        val (a1, r1) = ratioKnots[upper]
        return lerp(r0, r1, (age - a0) / (a1 - a0))
    }

    override fun lms(gender: Gender, ageMonths: Double): Lms {
        val height = WhoHeightReference.lms(gender, ageMonths)
        return Lms(
            l = 1.0,
            m = footToHeightRatio(ageMonths) * height.m * MM_PER_CM,
            s = sqrt(height.s * height.s + RATIO_CV * RATIO_CV),
        )
    }

    private const val MM_PER_CM = 10.0
}

object GrowthReferences {
    fun forMetric(metric: Metric): GrowthReference = when (metric) {
        Metric.HEIGHT_CM -> WhoHeightReference
        Metric.FOOT_MM -> FootLengthReference
    }
}

private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
