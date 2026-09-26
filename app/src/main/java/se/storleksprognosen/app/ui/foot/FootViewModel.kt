package se.storleksprognosen.app.ui.foot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.storleksprognosen.app.data.prefs.ScreenCalibration
import se.storleksprognosen.app.data.prefs.SettingsStore
import se.storleksprognosen.app.ui.appContainer
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.sizing.SizingSettings
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

data class FootUiState(
    val children: List<Child> = emptyList(),
    val selectedChild: Child? = null,
    /** Senaste fotmätningen per barn. */
    val latestFoot: Map<String, Measurement> = emptyMap(),
)

class FootViewModel(
    private val repository: ChildRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<FootUiState> = combine(
        repository.observeChildren(),
        repository.observeMeasurements(),
        settings.selectedChildId,
    ) { children, measurements, selectedId ->
        FootUiState(
            children = children,
            selectedChild = children.firstOrNull { it.id == selectedId } ?: children.firstOrNull(),
            latestFoot = measurements
                .filter { it.footMm != null }
                .groupBy { it.childId }
                .mapValues { (_, list) -> list.maxBy { it.date } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FootUiState())

    val calibration: StateFlow<ScreenCalibration> = settings.calibration
    val sizing: StateFlow<SizingSettings> = settings.sizing

    fun setShoeAllowance(mm: Int) = settings.setShoeAllowanceMm(mm)

    fun saveCalibration(calibration: ScreenCalibration) = settings.setCalibration(calibration)

    fun setEdgeOffset(mm: Float) = settings.setCalibration(calibration.value.copy(edgeOffsetMm = mm))

    /** Sparar en fotmätning (hela millimeter) med dagens datum. */
    fun saveFootMeasurement(child: Child, footMm: Float) {
        viewModelScope.launch {
            repository.saveMeasurement(
                Measurement(
                    id = UUID.randomUUID().toString(),
                    childId = child.id,
                    date = LocalDate.now(),
                    footMm = footMm.roundToInt().toFloat(),
                    heightCm = null,
                )
            )
            settings.setSelectedChildId(child.id)
        }
    }

    companion object {
        /** Kreditkortets (ID-1) långsida i millimeter. */
        const val CARD_LENGTH_MM = 85.6f
        const val CARD_WIDTH_MM = 53.98f
        const val CARD_CORNER_MM = 3.18f
        const val MM_PER_INCH = 25.4f

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                FootViewModel(container.childRepository, container.settings)
            }
        }
    }
}
