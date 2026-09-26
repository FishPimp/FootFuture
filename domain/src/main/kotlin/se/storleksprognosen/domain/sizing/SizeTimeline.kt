package se.storleksprognosen.domain.sizing

import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.model.Category
import java.time.LocalDate

/** Perioden då barnet enligt prognosen har en viss storlek. */
data class SizeWindow(
    val size: Int,
    val start: LocalDate,
    /** Dagen då nästa storlek behövs, eller null om det ligger bortom prognosen. */
    val endExclusive: LocalDate?,
)

object SizeTimeline {

    /** Storleksperioder från födseln till prognosens slut, dag för dag. */
    fun build(forecast: ChildForecast, category: Category, settings: SizingSettings): List<SizeWindow> {
        val windows = mutableListOf<SizeWindow>()
        var start = forecast.child.birthdate
        var currentSize = forecast.sizeAt(category, start, settings)
        var day = start.plusDays(1)
        while (!day.isAfter(forecast.horizon)) {
            val size = forecast.sizeAt(category, day, settings)
            if (size != currentSize) {
                windows += SizeWindow(currentSize, start, day)
                currentSize = size
                start = day
            }
            day = day.plusDays(1)
        }
        windows += SizeWindow(currentSize, start, null)
        return windows
    }
}

/** Hela perioden för en storlek (första start till sista slut), eller null om barnet aldrig har den. */
fun List<SizeWindow>.windowFor(size: Int): SizeWindow? {
    val matching = filter { it.size == size }
    if (matching.isEmpty()) return null
    return SizeWindow(size, matching.first().start, matching.last().endExclusive)
}
