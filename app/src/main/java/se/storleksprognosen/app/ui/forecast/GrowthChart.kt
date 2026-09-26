package se.storleksprognosen.app.ui.forecast

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.theme.extendedColors
import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.GrowthReferences
import se.storleksprognosen.domain.growth.Percentile
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.model.Metric
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

const val MAX_AGE_MONTHS = 72.0

private val PadLeft = 40.dp
private val PadRight = 34.dp
private val PadTop = 28.dp
private val PadBottom = 22.dp

private typealias Point = Pair<Double, Double>

/** Ett vinter- eller sommarfält i grafen, i barnets ålder (månader). */
private data class SeasonBand(val startAge: Double, val endAge: Double, val season: Season)

private fun seasonBands(birthdate: LocalDate): List<SeasonBand> {
    val lastDate = AgeMath.dateAtAge(birthdate, MAX_AGE_MONTHS)
    return SeasonCalendar.periodsFrom(birthdate)
        .takeWhile { !it.start.isAfter(lastDate) }
        .filter { it.season == Season.WINTER || it.season == Season.SUMMER }
        .map { period ->
            SeasonBand(
                startAge = AgeMath.ageInMonths(birthdate, period.start).coerceIn(0.0, MAX_AGE_MONTHS),
                endAge = AgeMath.ageInMonths(birthdate, period.endInclusive.plusDays(1)).coerceIn(0.0, MAX_AGE_MONTHS),
                season = period.season,
            )
        }
        .filter { it.endAge > it.startAge }
        .toList()
}

private data class YAxis(val min: Double, val max: Double, val step: Double)

private fun yAxis(metric: Metric, series: List<List<Point>>): YAxis {
    val step = if (metric == Metric.HEIGHT_CM) 10.0 else 20.0
    val values = series.flatten().map { it.second }
    return YAxis(
        min = floor(values.min() / step) * step,
        max = ceil(values.max() / step) * step,
        step = step,
    )
}

/**
 * Tillväxtdiagram 0–72 månader ritat med Compose Canvas: percentilkurvor P3–P97,
 * säsongsfält (❄️ vinter, ☀️ sommar), mätpunkter, prognoslinje och en markör för
 * vald ålder. Tryck eller dra i grafen för att flytta markören.
 */
@Composable
fun GrowthChart(
    metric: Metric,
    gender: Gender,
    birthdate: LocalDate?,
    measurements: List<Measurement>,
    forecast: ChildForecast?,
    todayAgeMonths: Double?,
    selectedAgeMonths: Float?,
    onSelectAge: (Float) -> Unit,
    childName: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val labelStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    val yearLabels = (0..6).map { if (it == 0) "0" else stringResource(R.string.chart_years, it) }
    val todayLabel = stringResource(R.string.chart_today)
    val description = stringResource(R.string.chart_description, childName)

    val curves = remember(metric, gender) {
        val reference = GrowthReferences.forMetric(metric)
        Percentile.entries.associateWith { reference.curve(gender, it.z, stepMonths = 1.0) }
    }
    val points = remember(measurements, metric, birthdate) {
        if (birthdate == null) emptyList()
        else measurements.mapNotNull { measurement ->
            val value = measurement.valueOf(metric) ?: return@mapNotNull null
            val age = AgeMath.ageInMonths(birthdate, measurement.date)
            if (age < 0 || age > MAX_AGE_MONTHS) null else age to value.toDouble()
        }.sortedBy { it.first }
    }
    val forecastLine = remember(forecast, metric, todayAgeMonths) {
        if (forecast == null) emptyList()
        else {
            val start = (todayAgeMonths ?: 0.0).coerceIn(0.0, MAX_AGE_MONTHS)
            val steps = ((MAX_AGE_MONTHS - start) / 0.5).toInt()
            (0..steps).map { i ->
                val age = (start + i * 0.5).coerceAtMost(MAX_AGE_MONTHS)
                age to forecast.valueAtAge(metric, age)
            }
        }
    }
    val bands = remember(birthdate) { birthdate?.let(::seasonBands) ?: emptyList() }
    val axis = remember(metric, curves, points, forecastLine) {
        yAxis(metric, curves.values.toList() + listOf(points, forecastLine))
    }

    // Prognoslinjen ritas fram mjukt när barn, mått eller percentilkanal byts.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(forecast?.child?.id, metric, forecast?.forMetric(metric)?.zScore) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow))
    }
    val cursorAge by animateFloatAsState(
        targetValue = selectedAgeMonths ?: 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cursorAge",
    )
    val currentOnSelectAge by rememberUpdatedState(onSelectAge)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                fun ageAt(x: Float): Float {
                    val left = PadLeft.toPx()
                    val right = size.width - PadRight.toPx()
                    return ((x - left) / (right - left) * MAX_AGE_MONTHS.toFloat()).coerceIn(0f, MAX_AGE_MONTHS.toFloat())
                }
                detectTapGestures { offset -> currentOnSelectAge(ageAt(offset.x)) }
            }
            .pointerInput(Unit) {
                fun ageAt(x: Float): Float {
                    val left = PadLeft.toPx()
                    val right = size.width - PadRight.toPx()
                    return ((x - left) / (right - left) * MAX_AGE_MONTHS.toFloat()).coerceIn(0f, MAX_AGE_MONTHS.toFloat())
                }
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnSelectAge(ageAt(change.position.x))
                }
            },
    ) {
        val left = PadLeft.toPx()
        val right = size.width - PadRight.toPx()
        val top = PadTop.toPx()
        val bottom = size.height - PadBottom.toPx()
        fun x(age: Double): Float = left + (age / MAX_AGE_MONTHS * (right - left)).toFloat()
        fun y(value: Double): Float = bottom - ((value - axis.min) / (axis.max - axis.min) * (bottom - top)).toFloat()
        fun linePath(series: List<Point>): Path = Path().apply {
            series.forEachIndexed { i, (age, value) -> if (i == 0) moveTo(x(age), y(value)) else lineTo(x(age), y(value)) }
        }
        fun bandPath(lower: List<Point>, upper: List<Point>): Path = Path().apply {
            (lower + upper.asReversed()).forEachIndexed { i, (age, value) ->
                if (i == 0) moveTo(x(age), y(value)) else lineTo(x(age), y(value))
            }
            close()
        }

        val corner = CornerRadius(16.dp.toPx())
        val plotArea = Path().apply { addRoundRect(RoundRect(left, top, right, bottom, corner)) }
        drawRoundRect(colors.surfaceContainerLowest, Offset(left, top), Size(right - left, bottom - top), corner)

        clipPath(plotArea) {
            // Säsongsfält: blått för vinter, gult för sommar.
            bands.forEach { band ->
                val color = if (band.season == Season.WINTER) extended.chartWinter else extended.chartSummer
                drawRect(color, Offset(x(band.startAge), top), Size(x(band.endAge) - x(band.startAge), bottom - top))
            }
            // Percentilband.
            drawPath(bandPath(curves.getValue(Percentile.P3), curves.getValue(Percentile.P97)), colors.primary.copy(alpha = 0.07f))
            drawPath(bandPath(curves.getValue(Percentile.P15), curves.getValue(Percentile.P85)), colors.primary.copy(alpha = 0.09f))
        }

        // Säsongssymboler ovanför grafen.
        bands.forEach { band ->
            val width = x(band.endAge) - x(band.startAge)
            if (width > 16.dp.toPx()) {
                val icon = textMeasurer.measure(if (band.season == Season.WINTER) "❄️" else "☀️", labelStyle)
                drawText(icon, topLeft = Offset((x(band.startAge) + x(band.endAge) - icon.size.width) / 2f, top - icon.size.height - 4.dp.toPx()))
            }
        }

        // Rutnät och axlar.
        val gridColor = colors.outlineVariant.copy(alpha = 0.6f)
        val axisText = labelStyle.copy(color = colors.onSurfaceVariant)
        var value = axis.min
        while (value <= axis.max + 1e-6) {
            val yy = y(value)
            drawLine(gridColor, Offset(left, yy), Offset(right, yy), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(value.toInt().toString(), axisText)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), yy - label.size.height / 2f))
            value += axis.step
        }
        yearLabels.forEachIndexed { year, text ->
            val xx = x(year * 12.0)
            drawLine(gridColor, Offset(xx, top), Offset(xx, bottom), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(text, axisText)
            val labelX = (xx - label.size.width / 2f).coerceIn(left - 8.dp.toPx(), right - label.size.width.toFloat())
            drawText(label, topLeft = Offset(labelX, bottom + 4.dp.toPx()))
        }

        // Percentilkurvor.
        Percentile.entries.forEach { percentile ->
            val (color, width) = when (percentile) {
                Percentile.P50 -> colors.primary to 2.5.dp
                Percentile.P15, Percentile.P85 -> colors.primary.copy(alpha = 0.55f) to 1.2.dp
                Percentile.P3, Percentile.P97 -> colors.primary.copy(alpha = 0.35f) to 1.dp
            }
            val series = curves.getValue(percentile)
            drawPath(linePath(series), color, style = Stroke(width.toPx(), cap = StrokeCap.Round))
            if (percentile == Percentile.P3 || percentile == Percentile.P50 || percentile == Percentile.P97) {
                val label = textMeasurer.measure(percentile.label, labelStyle.copy(color = colors.primary))
                drawText(label, topLeft = Offset(right + 4.dp.toPx(), y(series.last().second) - label.size.height / 2f))
            }
        }

        // Idag-markering.
        todayAgeMonths?.takeIf { it in 0.0..MAX_AGE_MONTHS }?.let { age ->
            val xx = x(age)
            drawLine(
                colors.onSurfaceVariant.copy(alpha = 0.6f),
                Offset(xx, top), Offset(xx, bottom),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            val label = textMeasurer.measure(todayLabel, labelStyle.copy(color = colors.onSurfaceVariant))
            val labelX = (xx + 4.dp.toPx()).coerceAtMost(right - label.size.width - 4.dp.toPx())
            drawText(label, topLeft = Offset(labelX, bottom - label.size.height - 4.dp.toPx()))
        }

        // Uppmätta värden.
        if (points.size > 1) {
            drawPath(linePath(points), extended.forecastLine.copy(alpha = 0.5f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        points.forEach { (age, v) ->
            drawCircle(colors.surface, 6.dp.toPx(), Offset(x(age), y(v)))
            drawCircle(extended.forecastLine, 4.5.dp.toPx(), Offset(x(age), y(v)))
        }

        // Prognoslinjen, streckad.
        if (forecastLine.size > 1) {
            val count = (forecastLine.size * reveal.value).toInt().coerceIn(2, forecastLine.size)
            drawPath(
                linePath(forecastLine.take(count)),
                extended.forecastLine,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
                ),
            )
        }

        // Markör för vald ålder.
        if (forecast != null && selectedAgeMonths != null) {
            val age = cursorAge.toDouble().coerceIn(0.0, MAX_AGE_MONTHS)
            val center = Offset(x(age), y(forecast.valueAtAge(metric, age)))
            drawLine(colors.secondary.copy(alpha = 0.35f), Offset(center.x, top), Offset(center.x, bottom), strokeWidth = 2.dp.toPx())
            drawCircle(extended.forecastLine.copy(alpha = 0.22f), 14.dp.toPx(), center)
            drawCircle(colors.surface, 8.dp.toPx(), center)
            drawCircle(extended.forecastLine, 5.dp.toPx(), center)
        }
    }
}
