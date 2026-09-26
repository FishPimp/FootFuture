package se.storleksprognosen.app.ui.foot

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.storleksprognosen.app.R
import se.storleksprognosen.app.ui.components.formatDecimal
import se.storleksprognosen.app.ui.theme.PillShape
import se.storleksprognosen.app.ui.theme.extendedColors

/**
 * Kreditkortskalibrering: ett ID-1-kort är 85,6 mm långt. Användaren lägger kortet
 * mot skärmen och drar i rutan (eller reglaget) tills rutan har exakt kortets längd.
 * Resultatet sparas som pixlar per millimeter.
 */
@Composable
fun CalibrationScreen(
    onBack: () -> Unit,
    viewModel: FootViewModel = viewModel(factory = FootViewModel.Factory),
) {
    MeasurementWindowMode(immersive = false)
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val reported = reportedPxPerMm()
    val range = reported * 0.6f..reported * 1.5f
    val fineStep = reported * 0.002f
    var pxPerMm by rememberSaveable { mutableFloatStateOf((calibration.pxPerMm ?: reported).coerceIn(range)) }
    var dragging by remember { mutableStateOf(false) }
    val edgeWidth by animateFloatAsState(
        targetValue = if (dragging) 5f else 3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardEdge",
    )

    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val textMeasurer = rememberTextMeasurer()
    val hint = stringResource(R.string.calibration_card_hint)
    val hintStyle = MaterialTheme.typography.titleSmall.copy(color = colors.onPrimaryContainer, textAlign = TextAlign.Center)
    val lengthLabel = "↕ 85,6 mm"
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = colors.primary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(stringResource(R.string.calibration_title), style = MaterialTheme.typography.titleLarge)
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, dragAmount ->
                        change.consume()
                        val newLength = pxPerMm * FootViewModel.CARD_LENGTH_MM + dragAmount
                        pxPerMm = (newLength / FootViewModel.CARD_LENGTH_MM).coerceIn(range)
                    }
                },
        ) {
            val cardHeight = FootViewModel.CARD_LENGTH_MM * pxPerMm
            val cardWidth = FootViewModel.CARD_WIDTH_MM * pxPerMm
            val topLeft = Offset((size.width - cardWidth) / 2f, 8.dp.toPx())
            val corner = CornerRadius(FootViewModel.CARD_CORNER_MM * pxPerMm)

            drawRoundRect(colors.primaryContainer.copy(alpha = 0.6f), topLeft, Size(cardWidth, cardHeight), corner)
            drawRoundRect(
                color = colors.primary,
                topLeft = topLeft,
                size = Size(cardWidth, cardHeight),
                cornerRadius = corner,
                style = Stroke(
                    width = edgeWidth.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 6.dp.toPx())),
                ),
            )
            // Handtag vid kortets nederkant.
            drawRoundRect(
                extended.forecastLine,
                Offset(size.width / 2f - 24.dp.toPx(), topLeft.y + cardHeight - 3.dp.toPx()),
                Size(48.dp.toPx(), 6.dp.toPx()),
                CornerRadius(3.dp.toPx()),
            )

            val hintLayout = textMeasurer.measure(hint, hintStyle)
            drawText(
                hintLayout,
                topLeft = Offset(
                    (size.width - hintLayout.size.width) / 2f,
                    topLeft.y + (cardHeight - hintLayout.size.height) / 2f,
                ),
            )
            val lengthLayout = textMeasurer.measure(lengthLabel, labelStyle)
            drawText(
                lengthLayout,
                topLeft = Offset(
                    topLeft.x + cardWidth + 8.dp.toPx(),
                    topLeft.y + (cardHeight - lengthLayout.size.height) / 2f,
                ),
            )
        }

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = colors.surfaceContainer,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.calibration_instructions), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { pxPerMm = (pxPerMm - fineStep).coerceIn(range) }) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.action_decrease))
                    }
                    Slider(
                        value = pxPerMm,
                        onValueChange = { pxPerMm = it },
                        valueRange = range,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { pxPerMm = (pxPerMm + fineStep).coerceIn(range) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_increase))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.calibration_value, formatDecimal(pxPerMm, 2)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { pxPerMm = reported.coerceIn(range) }) {
                        Text(stringResource(R.string.action_reset))
                    }
                    Button(
                        onClick = {
                            viewModel.saveCalibration(calibration.copy(pxPerMm = pxPerMm))
                            onBack()
                        },
                        shape = PillShape,
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}
