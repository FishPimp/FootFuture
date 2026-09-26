package se.storleksprognosen.app.ui.foot

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.genderEmoji
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.app.ui.theme.extendedColors
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.sizing.ShoeSizes
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val MIN_FOOT_MM = 40f
private const val FINE_STEP_MM = 0.5f
private val HandleHeight = 56.dp

/**
 * Väggmetoden: mobilen ligger plant med nederkanten mot väggen och barnets häl mot
 * väggen. Linjen dras till stortåns spets; avståndet räknas från skärmens nederkant
 * plus kantavståndet till väggen.
 */
@Composable
fun MeasureScreen(
    onClose: () -> Unit,
    viewModel: FootViewModel = viewModel(factory = FootViewModel.Factory),
) {
    MeasurementWindowMode(immersive = true)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val sizing by viewModel.sizing.collectAsStateWithLifecycle()
    val pxPerMm = calibration.pxPerMm ?: reportedPxPerMm()
    val edgeOffset = calibration.edgeOffsetMm

    var footMm by rememberSaveable { mutableFloatStateOf(130f) }
    var dragging by remember { mutableStateOf(false) }
    var showSave by rememberSaveable { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val lineColor = MaterialTheme.extendedColors.forecastLine
    val textMeasurer = rememberTextMeasurer()
    val rulerStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val heelText = stringResource(R.string.measure_heel)
    val heelStyle = MaterialTheme.typography.labelLarge.copy(color = colors.primary)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val displayedMm = footMm.roundToInt()
    val shoeSize = ShoeSizes.sizeFor(displayedMm.toDouble(), sizing.shoeAllowanceMm)
    LaunchedEffect(displayedMm) {
        if (dragging) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val handleScale by animateFloatAsState(
        targetValue = if (dragging) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "handleScale",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val heightPx = constraints.maxHeight.toFloat()
        val minMm = maxOf(edgeOffset, MIN_FOOT_MM)
        val maxMm = maxOf(minMm + 1f, edgeOffset + heightPx / pxPerMm - 4f)
        fun yOf(mm: Float): Float = heightPx - (mm - edgeOffset) * pxPerMm
        LaunchedEffect(maxMm, minMm) { footMm = footMm.coerceIn(minMm, maxMm) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pxPerMm, maxMm, minMm) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, dragAmount ->
                        change.consume()
                        footMm = (footMm - dragAmount.y / pxPerMm).coerceIn(minMm, maxMm)
                    }
                },
        ) {
            val lineY = yOf(footMm)

            // Fotens yta från hälen (väggen) upp till linjen.
            drawRect(colors.primary.copy(alpha = 0.08f), Offset(0f, lineY), Size(size.width, size.height - lineY))

            // Linjal längs vänsterkanten, i millimeter från väggen.
            var mm = ceil(edgeOffset).toInt()
            while (yOf(mm.toFloat()) >= 0f) {
                val y = yOf(mm.toFloat())
                val length = when {
                    mm % 10 == 0 -> 26.dp.toPx()
                    mm % 5 == 0 -> 16.dp.toPx()
                    else -> 8.dp.toPx()
                }
                drawLine(colors.onSurfaceVariant, Offset(0f, y), Offset(length, y), strokeWidth = 1.dp.toPx())
                if (mm % 10 == 0) {
                    val label = textMeasurer.measure((mm / 10).toString(), rulerStyle)
                    drawText(label, topLeft = Offset(length + 4.dp.toPx(), y - label.size.height / 2f))
                }
                mm++
            }

            // Hälmarkering vid väggen.
            val heel = textMeasurer.measure(heelText, heelStyle)
            drawText(heel, topLeft = Offset((size.width - heel.size.width) / 2f, size.height - heel.size.height - 12.dp.toPx()))

            // Skjutmåttslinjen.
            drawLine(
                lineColor,
                Offset(0f, lineY),
                Offset(size.width, lineY),
                strokeWidth = if (dragging) 4.dp.toPx() else 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Handtag med avläsning, centrerat på linjen vid högerkanten.
        val handleOffsetPx = with(density) { (yOf(footMm) - HandleHeight.toPx() / 2f).roundToInt() }
        Surface(
            shape = PillShape,
            color = lineColor,
            contentColor = colors.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(-12.dp.roundToPx(), handleOffsetPx) }
                .height(HandleHeight)
                .scale(handleScale),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                IconButton(onClick = { footMm = (footMm - FINE_STEP_MM).coerceIn(minMm, maxMm) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.measure_fine_down))
                }
                Text(
                    stringResource(R.string.measure_reading, displayedMm.toString(), shoeSize),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { footMm = (footMm + FINE_STEP_MM).coerceIn(minMm, maxMm) }) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.measure_fine_up))
                }
            }
        }

        // Stäng och spara.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = !dragging, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    stringResource(R.string.measure_drag_hint),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(colors.surfaceContainerHigh, PillShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { showSave = true }, shape = PillShape) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showSave) {
        SaveFootDialog(
            footMm = displayedMm,
            children = state.children,
            preselected = state.selectedChild,
            onSave = { child ->
                viewModel.saveFootMeasurement(child, displayedMm.toFloat())
                Toast.makeText(context, context.getString(R.string.measure_saved, child.name), Toast.LENGTH_SHORT).show()
                showSave = false
                onClose()
            },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun SaveFootDialog(
    footMm: Int,
    children: List<Child>,
    preselected: Child?,
    onSave: (Child) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf(preselected?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measure_save_title, footMm.toString())) },
        text = {
            if (children.isEmpty()) {
                Text(stringResource(R.string.measure_no_children))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    children.forEach { child ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedId == child.id,
                                    onClick = { selectedId = child.id },
                                    role = Role.RadioButton,
                                ),
                        ) {
                            RadioButton(selected = selectedId == child.id, onClick = null)
                            Spacer(Modifier.size(8.dp))
                            Text("${genderEmoji(child.gender)} ${child.name}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val child = children.firstOrNull { it.id == selectedId }
            TextButton(onClick = { child?.let(onSave) }, enabled = child != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
