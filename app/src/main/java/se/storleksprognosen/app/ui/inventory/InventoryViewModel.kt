package se.storleksprognosen.app.ui.inventory

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
import se.storleksprognosen.app.data.prefs.SettingsStore
import se.storleksprognosen.app.ui.appContainer
import se.storleksprognosen.domain.forecast.GrowthForecaster
import se.storleksprognosen.domain.matcher.HandMeDownMatcher
import se.storleksprognosen.domain.matcher.ItemEvaluation
import se.storleksprognosen.domain.matcher.SeasonNeed
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.repository.InventoryRepository
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.season.SeasonPeriod
import java.time.LocalDate
import java.util.UUID

data class InventoryUiState(
    val loading: Boolean = true,
    val children: List<Child> = emptyList(),
    val evaluations: List<ItemEvaluation> = emptyList(),
    val needs: List<SeasonNeed> = emptyList(),
    val periods: List<SeasonPeriod> = emptyList(),
)

class InventoryViewModel(
    private val childRepository: ChildRepository,
    private val inventoryRepository: InventoryRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<InventoryUiState> = combine(
        childRepository.observeChildren(),
        childRepository.observeMeasurements(),
        inventoryRepository.observeInventory(),
        settings.sizing,
    ) { children, measurements, inventory, sizing ->
        val matcher = HandMeDownMatcher(sizing)
        val forecasts = children.map { GrowthForecaster.forecast(it, measurements) }
        InventoryUiState(
            loading = false,
            children = children,
            evaluations = matcher.evaluate(inventory, forecasts, today),
            needs = matcher.seasonNeeds(forecasts, inventory, today, MONTHS_AHEAD),
            periods = SeasonCalendar.upcoming(today, MONTHS_AHEAD),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun saveItem(item: InventoryItem) {
        viewModelScope.launch { inventoryRepository.saveItem(item) }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch { inventoryRepository.deleteItem(item.id) }
    }

    companion object {
        const val MONTHS_AHEAD = 12L

        fun newId(): String = UUID.randomUUID().toString()

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                InventoryViewModel(container.childRepository, container.inventoryRepository, container.settings)
            }
        }
    }
}
