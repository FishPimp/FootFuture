package se.storleksprognosen.domain.growth

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * LMS-parametrar (Cole & Green) för en viss ålder: Box-Cox-potens [l], median [m]
 * och variationskoefficient [s]. Används av både WHO och BVC för Z-score.
 */
data class Lms(val l: Double, val m: Double, val s: Double) {

    /** Z-score för ett uppmätt värde. */
    fun zScore(value: Double): Double =
        if (abs(l) < EPSILON) ln(value / m) / s
        else ((value / m).pow(l) - 1.0) / (l * s)

    /** Värdet som motsvarar en viss Z-score. */
    fun valueAt(z: Double): Double =
        if (abs(l) < EPSILON) m * exp(s * z)
        else m * (1.0 + l * s * z).pow(1.0 / l)

    private companion object {
        const val EPSILON = 1e-9
    }
}

/** Standardnormalfördelningen, för omvandling mellan Z-score och percentil. */
object NormalDistribution {

    /** P(Z ≤ z). Abramowitz & Stegun 7.1.26 (fel < 1,5e-7). */
    fun cdf(z: Double): Double {
        val x = abs(z) / kotlin.math.sqrt(2.0)
        val t = 1.0 / (1.0 + 0.3275911 * x)
        val poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))))
        val erf = 1.0 - poly * exp(-x * x)
        return if (z >= 0) 0.5 * (1.0 + erf) else 0.5 * (1.0 - erf)
    }

    /** Percentil 0–100 för en Z-score. */
    fun percentile(z: Double): Double = cdf(z) * 100.0
}

/** Percentilkurvorna som visas i diagrammet (samma som i WHO:s och BVC:s kurvor). */
enum class Percentile(val label: String, val z: Double) {
    P3("P3", -1.8808),
    P15("P15", -1.0364),
    P50("P50", 0.0),
    P85("P85", 1.0364),
    P97("P97", 1.8808),
}
