package se.storleksprognosen.app.ui.foot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.formatDecimal
import se.storleksprognosen.app.ui.components.fullDate
import se.storleksprognosen.app.ui.theme.CardShape
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.domain.sizing.ShoeSizes
import kotlin.math.roundToInt

@Composable
fun FootScreen(
    onCalibrate: () -> Unit,
    onMeasure: () -> Unit,
    viewModel: FootViewModel = viewModel(factory = FootViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val sizing by viewModel.sizing.collectAsStateWithLifecycle()
    val reported = reportedPxPerMm()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_foot), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.foot_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "calibration") {
                FootCard(title = stringResource(R.string.foot_calibration_title), emoji = "💳") {
                    val pxPerMm = calibration.pxPerMm
                    Text(
                        if (pxPerMm != null) {
                            stringResource(
                                R.string.foot_calibrated,
                                formatDecimal(pxPerMm, 2),
                                (pxPerMm * FootViewModel.MM_PER_INCH).roundToInt(),
                            )
                        } else {
                            stringResource(R.string.foot_not_calibrated, formatDecimal(reported, 2))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = onCalibrate, shape = PillShape) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.foot_calibrate_button))
                    }
                    Text(
                        stringResource(R.string.foot_edge_offset, formatDecimal(calibration.edgeOffsetMm, 1)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                        value = calibration.edgeOffsetMm,
                        onValueChange = { viewModel.setEdgeOffset((it * 2).roundToInt() / 2f) },
                        valueRange = 0f..10f,
                        steps = 19,
                    )
                    Text(
                        stringResource(R.string.foot_edge_offset_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "measure") {
                FootCard(
                    title = stringResource(R.string.foot_measure_title),
                    emoji = "🧱",
                    container = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(stringResource(R.string.foot_measure_steps), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onMeasure, shape = PillShape, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Straighten, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.foot_measure_button))
                    }
                    state.selectedChild?.let { child ->
                        state.latestFoot[child.id]?.let { measurement ->
                            val foot = measurement.footMm ?: return@let
                            Text(
                                stringResource(
                                    R.string.foot_latest,
                                    child.name,
                                    formatDecimal(foot, 0),
                                    fullDate(measurement.date),
                                    ShoeSizes.sizeFor(foot.toDouble(), sizing.shoeAllowanceMm),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            item(key = "allowance") {
                FootCard(title = stringResource(R.string.foot_allowance_title), emoji = "👟") {
                    Text(
                        stringResource(R.string.foot_allowance, sizing.shoeAllowanceMm),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val range = ShoeSizes.ALLOWANCE_RANGE_MM
                    Slider(
                        value = sizing.shoeAllowanceMm.toFloat(),
                        onValueChange = { viewModel.setShoeAllowance(it.roundToInt()) },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        steps = range.last - range.first - 1,
                    )
                    Text(
                        stringResource(R.string.foot_allowance_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val examples = listOf(120, 140, 160, 180)
                    Text(
                        examples.map { foot ->
                            stringResource(
                                R.string.foot_allowance_example,
                                foot,
                                ShoeSizes.sizeFor(foot.toDouble(), sizing.shoeAllowanceMm),
                            )
                        }.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            item(key = "tips") {
                FootCard(
                    title = stringResource(R.string.foot_tips_title),
                    emoji = null,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(stringResource(R.string.foot_tips), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun FootCard(
    title: String,
    emoji: String?,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        shape = CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                listOfNotNull(emoji, title).joinToString(" "),
                style = MaterialTheme.typography.titleLarge,
            )
            content()
        }
    }
}
