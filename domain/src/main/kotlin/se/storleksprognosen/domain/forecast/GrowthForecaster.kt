package se.storleksprognosen.domain.forecast

import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.GrowthReferences
import se.storleksprognosen.domain.growth.NormalDistribution
import se.storleksprognosen.domain.growth.WhoHeightReference
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.model.Metric
import java.time.LocalDate

/** Vad prognosens percentilkanal bygger på. */
enum class ForecastBasis {
    /** Barnets senaste mätning av samma mått. */
    MEASURED,

    /** Ingen fotmätning finns – fotens kanal antas följa längdens. */
    FROM_HEIGHT,

    /** Inga mätningar – medianen (P50) används. */
    MEDIAN,
}

data class MetricForecast(
    val metric: Metric,
    val zScore: Double,
    val basis: ForecastBasis,
    val basedOn: Measurement?,
) {
    val percentile: Double get() = NormalDistribution.percentile(zScore)
}

/** Ett barns prognos: barnet antas följa sin percentilkanal (konstant Z-score). */
class ChildForecast(
    val child: Child,
    val height: MetricForecast,
    val foot: MetricForecast,
) {
    /** Sista datum som referenskurvorna täcker (6 år). */
    val horizon: LocalDate = AgeMath.dateAtAge(child.birthdate, WhoHeightReference.maxAgeMonths)

    fun forMetric(metric: Metric): MetricForecast = when (metric) {
        Metric.HEIGHT_CM -> height
        Metric.FOOT_MM -> foot
    }

    fun ageInMonths(date: LocalDate): Double = AgeMath.ageInMonths(child.birthdate, date)

    fun valueAtAge(metric: Metric, ageMonths: Double): Double =
        GrowthReferences.forMetric(metric).valueAt(child.gender, ageMonths, forMetric(metric).zScore)

    fun valueAt(metric: Metric, date: LocalDate): Double = valueAtAge(metric, ageInMonths(date))
}

object GrowthForecaster {
    /** Extrema mätfel ska inte ge orimliga prognoser. */
    const val MAX_ABS_Z = 4.0

    fun forecast(child: Child, measurements: List<Measurement>): ChildForecast {
        val own = measurements.filter { it.childId == child.id }
        val height = latestZScore(child, own, Metric.HEIGHT_CM)
        val foot = latestZScore(child, own, Metric.FOOT_MM)

        val heightForecast = height
            ?.let { (measurement, z) -> MetricForecast(Metric.HEIGHT_CM, z, ForecastBasis.MEASURED, measurement) }
            ?: MetricForecast(Metric.HEIGHT_CM, 0.0, ForecastBasis.MEDIAN, null)

        val footForecast = when {
            foot != null -> MetricForecast(Metric.FOOT_MM, foot.second, ForecastBasis.MEASURED, foot.first)
            height != null -> MetricForecast(Metric.FOOT_MM, height.second, ForecastBasis.FROM_HEIGHT, height.first)
            else -> MetricForecast(Metric.FOOT_MM, 0.0, ForecastBasis.MEDIAN, null)
        }
        return ChildForecast(child, heightForecast, footForecast)
    }

    /** Z-score för ett uppmätt värde vid ett visst datum. */
    fun zScore(child: Child, metric: Metric, date: LocalDate, value: Double): Double {
        val age = AgeMath.ageInMonths(child.birthdate, date)
        return GrowthReferences.forMetric(metric)
            .zScore(child.gender, age, value)
            .coerceIn(-MAX_ABS_Z, MAX_ABS_Z)
    }

    private fun latestZScore(
        child: Child,
        measurements: List<Measurement>,
        metric: Metric,
    ): Pair<Measurement, Double>? {
        val maxAge = GrowthReferences.forMetric(metric).maxAgeMonths
        val latest = measurements
            .filter { (it.valueOf(metric) ?: 0f) > 0f }
            .filter { AgeMath.ageInMonths(child.birthdate, it.date) in 0.0..maxAge }
            .maxByOrNull { it.date }
            ?: return null
        return latest to zScore(child, metric, latest.date, latest.valueOf(metric)!!.toDouble())
    }
}
