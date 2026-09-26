package se.storleksprognosen.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import se.storleksprognosen.domain.sizing.ShoeSizes
import se.storleksprognosen.domain.sizing.SizingSettings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Skärmkalibrering för fotmätaren. */
data class ScreenCalibration(
    /** Pixlar per millimeter, uppmätt med kreditkort; null = inte kalibrerad. */
    val pxPerMm: Float?,
    /** Avstånd från mobilens nederkant (mot väggen) till skärmens första pixel. */
    val edgeOffsetMm: Float,
)

/** Små lokala inställningar som inte hör hemma i Room-databasen. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("storleksprognosen_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _selectedChildId = MutableStateFlow(prefs.getString(KEY_SELECTED_CHILD, null))
    val selectedChildId: StateFlow<String?> = _selectedChildId.asStateFlow()

    private val _sizing = MutableStateFlow(
        SizingSettings(shoeAllowanceMm = prefs.getInt(KEY_SHOE_ALLOWANCE, ShoeSizes.DEFAULT_ALLOWANCE_MM))
    )
    val sizing: StateFlow<SizingSettings> = _sizing.asStateFlow()

    private val _calibration = MutableStateFlow(
        ScreenCalibration(
            pxPerMm = prefs.getFloat(KEY_PX_PER_MM, 0f).takeIf { it > 0f },
            edgeOffsetMm = prefs.getFloat(KEY_EDGE_OFFSET, DEFAULT_EDGE_OFFSET_MM),
        )
    )
    val calibration: StateFlow<ScreenCalibration> = _calibration.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setSelectedChildId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_CHILD, id).apply()
        _selectedChildId.value = id
    }

    fun setShoeAllowanceMm(mm: Int) {
        val value = mm.coerceIn(ShoeSizes.ALLOWANCE_RANGE_MM)
        prefs.edit().putInt(KEY_SHOE_ALLOWANCE, value).apply()
        _sizing.value = _sizing.value.copy(shoeAllowanceMm = value)
    }

    fun setCalibration(calibration: ScreenCalibration) {
        prefs.edit()
            .putFloat(KEY_PX_PER_MM, calibration.pxPerMm ?: 0f)
            .putFloat(KEY_EDGE_OFFSET, calibration.edgeOffsetMm)
            .apply()
        _calibration.value = calibration
    }

    companion object {
        const val DEFAULT_EDGE_OFFSET_MM = 3f

        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SELECTED_CHILD = "selected_child"
        private const val KEY_SHOE_ALLOWANCE = "shoe_allowance_mm"
        private const val KEY_PX_PER_MM = "px_per_mm"
        private const val KEY_EDGE_OFFSET = "edge_offset_mm"
    }
}
