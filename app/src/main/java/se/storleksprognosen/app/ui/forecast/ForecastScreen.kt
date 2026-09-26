package se.storleksprognosen.app.ui.forecast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.storleksprognosen.app.R
import se.storleksprognosen.app.data.demo.DemoNames
import se.storleksprognosen.app.ui.components.ChildAvatar
import se.storleksprognosen.app.ui.components.EmptyState
import se.storleksprognosen.app.ui.components.SectionHeader
import se.storleksprognosen.app.ui.components.ageText
import se.storleksprognosen.app.ui.components.durationText
import se.storleksprognosen.app.ui.components.formatDecimal
import se.storleksprognosen.app.ui.components.fullDate
import se.storleksprognosen.app.ui.components.genderEmoji
import se.storleksprognosen.app.ui.components.monthYear
import se.storleksprognosen.app.ui.components.phaseEmoji
import se.storleksprognosen.app.ui.components.periodLabel
import se.storleksprognosen.app.ui.settings.SettingsDialog
import se.storleksprognosen.app.ui.theme.CardShape
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.app.ui.theme.extendedColors
import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.forecast.ForecastBasis
import se.storleksprognosen.domain.forecast.GrowthForecaster
import se.storleksprognosen.domain.forecast.MetricForecast
import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.NormalDistribution
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.model.Metric
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.sizing.ClothingSizes
import se.storleksprognosen.domain.sizing.ShoeSizes
import se.storleksprognosen.domain.sizing.SizeWindow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private sealed interface ForecastDialog {
    data object Settings : ForecastDialog
    data class EditChild(val child: Child?) : ForecastDialog
    data class EditMeasurement(val measurement: Measurement?) : ForecastDialog
}

@Composable
fun ForecastScreen(viewModel: ForecastViewModel = viewModel(factory = ForecastViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ForecastDialog?>(null) }
    var metric by rememberSaveable { mutableStateOf(Metric.HEIGHT_CM) }

    val demoNames = DemoNames(
        brother = stringResource(R.string.demo_brother),
        sister = stringResource(R.string.demo_sister),
        winterBoots = stringResource(R.string.demo_winter_boots),
        snowsuit = stringResource(R.string.demo_snowsuit),
        sandals = stringResource(R.string.demo_sandals),
        linedBoots = stringResource(R.string.demo_lined_boots),
        bodysuits = stringResource(R.string.demo_bodysuits),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_forecast), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.forecast_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { dialog = ForecastDialog.Settings }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.selected != null) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = ForecastDialog.EditMeasurement(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_measurement)) },
                    shape = PillShape,
                )
            }
        },
    ) { padding ->
        val selected = state.selected
        val forecast = state.forecast
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding))
            selected == null || forecast == null -> EmptyState(
                emoji = "🌱",
                title = stringResource(R.string.forecast_empty_title),
                message = stringResource(R.string.forecast_empty_message),
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = { dialog = ForecastDialog.EditChild(null) }, shape = PillShape) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_add_child))
                }
                OutlinedButton(onClick = { viewModel.loadDemo(demoNames) }, shape = PillShape) {
                    Text(stringResource(R.string.action_show_demo))
                }
            }
            else -> ForecastContent(
                state = state,
                child = selected,
                forecast = forecast,
                metric = metric,
                onMetricChange = { metric = it },
                onSelectChild = viewModel::selectChild,
                onAddChild = { dialog = ForecastDialog.EditChild(null) },
                onEditChild = { dialog = ForecastDialog.EditChild(selected) },
                onEditMeasurement = { dialog = ForecastDialog.EditMeasurement(it) },
                contentPadding = padding,
            )
        }
    }

    when (val current = dialog) {
        null -> Unit
        ForecastDialog.Settings -> SettingsDialog(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
            onDismiss = { dialog = null },
        )
        is ForecastDialog.EditChild -> ChildDialog(
            existing = current.child,
            today = state.today,
            onSave = {
                viewModel.saveChild(it, isNew = current.child == null)
                dialog = null
            },
            onDelete = {
                current.child?.let(viewModel::deleteChild)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is ForecastDialog.EditMeasurement -> state.selected?.let { child ->
            MeasurementDialog(
                child = child,
                existing = current.measurement,
                today = state.today,
                onSave = {
                    viewModel.saveMeasurement(it)
                    dialog = null
                },
                onDelete = {
                    current.measurement?.let(viewModel::deleteMeasurement)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }
    }
}

@Composable
private fun ForecastContent(
    state: ForecastUiState,
    child: Child,
    forecast: ChildForecast,
    metric: Metric,
    onMetricChange: (Metric) -> Unit,
    onSelectChild: (String) -> Unit,
    onAddChild: () -> Unit,
    onEditChild: () -> Unit,
    onEditMeasurement: (Measurement?) -> Unit,
    contentPadding: PaddingValues,
) {
    val todayAge = forecast.ageInMonths(state.today)
    var sliderAge by rememberSaveable(child.id) {
        mutableFloatStateOf(todayAge.toFloat().coerceIn(0f, MAX_AGE_MONTHS.toFloat()))
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "children") {
            ChildSelector(
                children = state.children,
                selectedId = child.id,
                onSelect = onSelectChild,
                onAdd = onAddChild,
            )
        }
        item(key = "header") {
            ChildHeaderCard(
                child = child,
                forecast = forecast,
                today = state.today,
                onEdit = onEditChild,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item(key = "chart") {
            ElevatedCard(
                shape = CardShape,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Metric.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = metric == option,
                                onClick = { onMetricChange(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, Metric.entries.size),
                                label = {
                                    Text(
                                        stringResource(
                                            if (option == Metric.HEIGHT_CM) R.string.metric_height_toggle else R.string.metric_foot_toggle
                                        )
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GrowthChart(
                        metric = metric,
                        gender = child.gender,
                        birthdate = child.birthdate,
                        measurements = state.measurements,
                        forecast = forecast,
                        todayAgeMonths = todayAge,
                        selectedAgeMonths = sliderAge,
                        onSelectAge = { sliderAge = it },
                        childName = child.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 8.dp),
                    )
                    AgeSlider(
                        value = sliderAge,
                        onValueChange = { sliderAge = it },
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Text(
                        stringResource(R.string.slider_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
        item(key = "preview") {
            SizePreviewCard(
                child = child,
                forecast = forecast,
                ageMonths = sliderAge,
                today = state.today,
                shoeAllowanceMm = state.sizing.shoeAllowanceMm,
                timelines = state.timelines,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item(key = "measurementsHeader") {
            SectionHeader(
                text = stringResource(R.string.measurements_title),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (state.measurements.isEmpty()) {
            item(key = "measurementsEmpty") {
                Text(
                    stringResource(R.string.measurements_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(state.measurements, key = { it.id }) { measurement ->
            MeasurementRow(
                child = child,
                measurement = measurement,
                onClick = { onEditMeasurement(measurement) },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
            )
        }
        item(key = "disclaimer") {
            Text(
                stringResource(R.string.forecast_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ChildSelector(
    children: List<Child>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(children, key = { it.id }) { child ->
            val selected = child.id == selectedId
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "chipScale",
            )
            FilterChip(
                selected = selected,
                onClick = { onSelect(child.id) },
                label = { Text(child.name) },
                leadingIcon = { Text(genderEmoji(child.gender)) },
                shape = PillShape,
                modifier = Modifier.scale(scale),
            )
        }
        item {
            AssistChip(
                onClick = onAdd,
                label = { Text(stringResource(R.string.action_add_child)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = PillShape,
            )
        }
    }
}

@Composable
private fun ChildHeaderCard(
    child: Child,
    forecast: ChildForecast,
    today: LocalDate,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChildAvatar(child, size = 52)
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(child.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        ageText(child.birthdate, today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PercentilePill("📏 ${stringResource(R.string.metric_height)}", forecast.height)
                PercentilePill("👣 ${stringResource(R.string.metric_foot)}", forecast.foot)
            }
            if (today.isAfter(forecast.horizon)) {
                Text(
                    stringResource(R.string.forecast_beyond_horizon, child.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PercentilePill(label: String, forecast: MetricForecast) {
    val basis = when (forecast.basis) {
        ForecastBasis.MEASURED -> stringResource(
            R.string.forecast_basis_measured,
            forecast.basedOn?.let { fullDate(it.date) }.orEmpty(),
        )
        ForecastBasis.FROM_HEIGHT -> stringResource(R.string.forecast_basis_from_height)
        ForecastBasis.MEDIAN -> stringResource(R.string.forecast_basis_median)
    }
    Surface(shape = PillShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
        Text(
            text = "$label ${stringResource(R.string.percentile_short, forecast.percentile.roundToInt().coerceIn(1, 99))} · $basis",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Material 3-slider med en rund, fjädrande tumme. */
@Composable
private fun AgeSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val thumbScale by animateFloatAsState(
        targetValue = if (dragged || pressed) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumbScale",
    )
    val description = stringResource(R.string.slider_description)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..MAX_AGE_MONTHS.toFloat(),
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.extendedColors.forecastLine,
            inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(thumbScale)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.extendedColors.forecastLine, CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    )
}

/** Den lekfulla rutan som visar rekommenderade storlekar vid vald ålder. */
@Composable
private fun SizePreviewCard(
    child: Child,
    forecast: ChildForecast,
    ageMonths: Float,
    today: LocalDate,
    shoeAllowanceMm: Int,
    timelines: Map<Category, List<SizeWindow>>,
    modifier: Modifier = Modifier,
) {
    val age = ageMonths.toDouble()
    val date = AgeMath.dateAtAge(child.birthdate, age)
    val period = SeasonCalendar.periodContaining(date)
    val height = forecast.valueAtAge(Metric.HEIGHT_CM, age)
    val foot = forecast.valueAtAge(Metric.FOOT_MM, age)
    val clothesSize = ClothingSizes.sizeFor(height)
    val shoeSize = ShoeSizes.sizeFor(foot, shoeAllowanceMm)

    // Ett litet haptiskt "klick" när storleken byts medan man drar.
    val haptics = LocalHapticFeedback.current
    var previousSizes by remember { mutableStateOf(clothesSize to shoeSize) }
    LaunchedEffect(clothesSize, shoeSize) {
        if (previousSizes != clothesSize to shoeSize) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            previousSizes = clothesSize to shoeSize
        }
    }

    val monthsAway = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), date.withDayOfMonth(1)).toInt()
    val relative = when {
        monthsAway > 0 -> stringResource(R.string.preview_in_future, durationText(monthsAway))
        monthsAway < 0 -> stringResource(R.string.preview_in_past, durationText(monthsAway))
        else -> stringResource(R.string.preview_now)
    }
    val extended = MaterialTheme.extendedColors
    val container by animateColorAsState(
        targetValue = when (period.season) {
            Season.WINTER -> extended.winter
            Season.SUMMER -> extended.summer
            else -> extended.springFall
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "previewColor",
    )

    ElevatedCard(
        shape = CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = period.phase,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it / 2 }) togetherWith fadeOut()
                    },
                    label = "seasonEmoji",
                ) { phase -> Text(phaseEmoji(phase), fontSize = 34.sp) }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        "${monthYear(date).replaceFirstChar { it.uppercase() }} · ${periodLabel(period)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${ageText(child.birthdate, date)} · $relative",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SizeTile(
                    label = stringResource(R.string.preview_clothes),
                    size = clothesSize,
                    detail = stringResource(R.string.preview_height, formatDecimal(height, 0)),
                    until = lastsUntil(timelines[Category.CLOTHES], clothesSize, date),
                    modifier = Modifier.weight(1f),
                )
                SizeTile(
                    label = stringResource(R.string.preview_shoes),
                    size = shoeSize,
                    detail = stringResource(R.string.preview_foot, formatDecimal(foot, 0)),
                    until = lastsUntil(timelines[Category.SHOES], shoeSize, date),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun lastsUntil(windows: List<SizeWindow>?, size: Int, date: LocalDate): String {
    val window = windows?.firstOrNull { it.size == size && !date.isBefore(it.start) && (it.endExclusive?.isAfter(date) ?: true) }
    val end = window?.endExclusive ?: return stringResource(R.string.preview_lasts_beyond)
    return stringResource(R.string.preview_lasts_until, monthYear(end))
}

@Composable
private fun SizeTile(
    label: String,
    size: Int,
    detail: String,
    until: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            AnimatedContent(
                targetState = size,
                transitionSpec = {
                    val growing = targetState > initialState
                    val spec = spring<androidx.compose.ui.unit.IntOffset>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                    (slideInVertically(spec) { if (growing) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically { if (growing) -it else it } + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "size",
            ) { value ->
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Text(
                until,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MeasurementRow(
    child: Child,
    measurement: Measurement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fullDate(measurement.date), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.measurement_at_age, ageText(child.birthdate, measurement.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                measurement.heightCm?.let { value ->
                    Text(
                        "📏 " + valueWithPercentile(child, measurement, Metric.HEIGHT_CM, stringResource(R.string.value_cm, formatDecimal(value, 1))),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                measurement.footMm?.let { value ->
                    Text(
                        "👣 " + valueWithPercentile(child, measurement, Metric.FOOT_MM, stringResource(R.string.value_mm, formatDecimal(value, 0))),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun valueWithPercentile(child: Child, measurement: Measurement, metric: Metric, formatted: String): String {
    val value = measurement.valueOf(metric) ?: return formatted
    val ageMonths = AgeMath.ageInMonths(child.birthdate, measurement.date)
    if (ageMonths > MAX_AGE_MONTHS) return formatted
    val z = GrowthForecaster.zScore(child, metric, measurement.date, value.toDouble())
    val percentile = NormalDistribution.percentile(z).roundToInt().coerceIn(1, 99)
    return stringResource(R.string.measurement_value_with_percentile, formatted, percentile)
}
