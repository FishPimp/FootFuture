package se.storleksprognosen.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.storleksprognosen.app.data.demo.DemoData
import se.storleksprognosen.app.data.demo.DemoNames
import se.storleksprognosen.app.data.prefs.SettingsStore
import se.storleksprognosen.app.data.prefs.ThemeMode
import se.storleksprognosen.app.ui.appContainer
import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.forecast.GrowthForecaster
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.sizing.SizeTimeline
import se.storleksprognosen.domain.sizing.SizeWindow
import se.storleksprognosen.domain.sizing.SizingSettings
import se.storleksprognosen.domain.model.Category
import java.time.LocalDate
import java.util.UUID

data class ForecastUiState(
    val loading: Boolean = true,
    val children: List<Child> = emptyList(),
    val selected: Child? = null,
    val forecast: ChildForecast? = null,
    /** Det valda barnets mätningar, nyast först. */
    val measurements: List<Measurement> = emptyList(),
    val sizing: SizingSettings = SizingSettings(),
    /** Storleksperioder per kategori för det valda barnet. */
    val timelines: Map<Category, List<SizeWindow>> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
)

class ForecastViewModel(
    private val repository: ChildRepository,
    private val settings: SettingsStore,
    private val demoData: DemoData,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<ForecastUiState> = combine(
        repository.observeChildren(),
        repository.observeMeasurements(),
        settings.selectedChildId,
        settings.sizing,
    ) { children, measurements, selectedId, sizing ->
        val selected = children.firstOrNull { it.id == selectedId } ?: children.firstOrNull()
        val forecast = selected?.let { GrowthForecaster.forecast(it, measurements) }
        ForecastUiState(
            loading = false,
            children = children,
            selected = selected,
            forecast = forecast,
            measurements = measurements.filter { it.childId == selected?.id },
            sizing = sizing,
            timelines = forecast?.let { f ->
                Category.entries.associateWith { SizeTimeline.build(f, it, sizing) }
            } ?: emptyMap(),
            today = today,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val dynamicColor: StateFlow<Boolean> = settings.dynamicColor

    fun selectChild(id: String) = settings.setSelectedChildId(id)

    fun saveChild(child: Child, isNew: Boolean) {
        viewModelScope.launch {
            repository.saveChild(child)
            if (isNew) settings.setSelectedChildId(child.id)
        }
    }

    fun deleteChild(child: Child) {
        viewModelScope.launch { repository.deleteChild(child.id) }
    }

    fun saveMeasurement(measurement: Measurement) {
        viewModelScope.launch { repository.saveMeasurement(measurement) }
    }

    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch { repository.deleteMeasurement(measurement.id) }
    }

    fun loadDemo(names: DemoNames) {
        viewModelScope.launch {
            val selectedId = demoData.load(names, today, settings.sizing.value)
            settings.setSelectedChildId(selectedId)
        }
    }

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ForecastViewModel(container.childRepository, container.settings, container.demoData)
            }
        }
    }
}
