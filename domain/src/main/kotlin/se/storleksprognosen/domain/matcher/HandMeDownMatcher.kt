package se.storleksprognosen.domain.matcher

import se.storleksprognosen.domain.forecast.ChildForecast
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Season
import se.storleksprognosen.domain.season.SeasonCalendar
import se.storleksprognosen.domain.season.SeasonPeriod
import se.storleksprognosen.domain.sizing.SizeTimeline
import se.storleksprognosen.domain.sizing.SizeWindow
import se.storleksprognosen.domain.sizing.SizingSettings
import se.storleksprognosen.domain.sizing.sizeAt
import se.storleksprognosen.domain.sizing.windowFor
import java.time.LocalDate

/** Hur ett sparat plagg passar ett visst barn. */
sealed interface MatchStatus {
    val child: Child
    val size: Int

    /** 🟢 Passar barnet under [period] (null för plagg som används året runt). */
    data class Match(
        override val child: Child,
        override val size: Int,
        val period: SeasonPeriod?,
        val from: LocalDate,
        val until: LocalDate?,
    ) : MatchStatus

    /**
     * 🔴 Barnet har storleken, men inte under plaggets säsong.
     * [reachesOn] är null om barnet redan har storleken; [outgrowsOn] är null om
     * det ligger bortom prognosen.
     */
    data class WrongSeason(
        override val child: Child,
        override val size: Int,
        val reachesOn: LocalDate?,
        val outgrowsOn: LocalDate?,
    ) : MatchStatus

    /** Barnet har redan vuxit ur storleken. */
    data class Outgrown(override val child: Child, override val size: Int) : MatchStatus

    /** Storleken ligger utanför prognosens 0–6 år. */
    data class BeyondForecast(override val child: Child, override val size: Int) : MatchStatus
}

data class ItemEvaluation(val item: InventoryItem, val statuses: List<MatchStatus>)

/** Storleken ett barn behöver en viss säsong och vilka sparade plagg som täcker behovet. */
data class SeasonNeed(
    val child: Child,
    val period: SeasonPeriod,
    val category: Category,
    val size: Int,
    val coveredBy: List<InventoryItem>,
) {
    /** ⚪ Lucka: inget sparat plagg täcker behovet. */
    val isGap: Boolean get() = coveredBy.isEmpty()
}

/**
 * Syskon- och arvsmatrisen.
 *
 * Regeln är att ett plagg ska räcka hela säsongen: den storlek som behövs en säsong
 * är prognosens storlek på säsongens sista dag. Ett plagg matchar en säsong när
 * storleken är exakt den som behövs.
 */
class HandMeDownMatcher(private val settings: SizingSettings = SizingSettings()) {

    /** Utvärderar varje plagg mot syskonen (ägaren själv om det bara finns ett barn). */
    fun evaluate(
        items: List<InventoryItem>,
        forecasts: List<ChildForecast>,
        today: LocalDate,
    ): List<ItemEvaluation> {
        val timelines = forecasts.map { Timelines(it) }
        return items.map { item ->
            val candidates = timelines.filter { it.forecast.child.id != item.ownerChildId }.ifEmpty { timelines }
            ItemEvaluation(item, candidates.map { evaluate(item, it, today) })
        }
    }

    fun evaluate(item: InventoryItem, forecast: ChildForecast, today: LocalDate): MatchStatus =
        evaluate(item, Timelines(forecast), today)

    /** Behov per barn, säsong och kategori för säsonger som börjar inom [monthsAhead] månader. */
    fun seasonNeeds(
        forecasts: List<ChildForecast>,
        inventory: List<InventoryItem>,
        today: LocalDate,
        monthsAhead: Long = 12,
    ): List<SeasonNeed> {
        val periods = SeasonCalendar.upcoming(today, monthsAhead)
        return forecasts.flatMap { forecast ->
            periods
                .filter { !it.endInclusive.isAfter(forecast.horizon) }
                .flatMap { period ->
                    Category.entries.map { category ->
                        val size = neededSize(forecast, category, period)
                        SeasonNeed(
                            child = forecast.child,
                            period = period,
                            category = category,
                            size = size,
                            coveredBy = inventory.filter {
                                it.category == category && it.size == size &&
                                    (it.season == period.season || it.season == Season.ALL_YEAR)
                            },
                        )
                    }
                }
        }
    }

    /** Storleken som räcker hela säsongen. */
    fun neededSize(forecast: ChildForecast, category: Category, period: SeasonPeriod): Int =
        forecast.sizeAt(category, period.endInclusive, settings)

    private fun evaluate(item: InventoryItem, timelines: Timelines, today: LocalDate): MatchStatus {
        val forecast = timelines.forecast
        val child = forecast.child
        if (today.isAfter(forecast.horizon)) return MatchStatus.BeyondForecast(child, item.size)

        val window = timelines.windows(item.category).windowFor(item.size)
        if (window == null) {
            val sizeToday = forecast.sizeAt(item.category, today, settings)
            return if (item.size < sizeToday) MatchStatus.Outgrown(child, item.size)
            else MatchStatus.BeyondForecast(child, item.size)
        }
        val end = window.endExclusive
        if (end != null && !end.isAfter(today)) return MatchStatus.Outgrown(child, item.size)

        val from = maxOf(window.start, today)
        if (item.season == Season.ALL_YEAR) {
            return MatchStatus.Match(child, item.size, period = null, from = from, until = end)
        }

        val period = SeasonCalendar.periodsFrom(today)
            .takeWhile { !it.endInclusive.isAfter(forecast.horizon) }
            .filter { it.season == item.season }
            .firstOrNull { neededSize(forecast, item.category, it) == item.size }

        return if (period != null) {
            MatchStatus.Match(child, item.size, period, from = maxOf(from, period.start), until = end)
        } else {
            MatchStatus.WrongSeason(
                child = child,
                size = item.size,
                reachesOn = window.start.takeIf { it.isAfter(today) },
                outgrowsOn = end,
            )
        }
    }

    /** Storleksperioder per kategori, beräknade en gång per barn. */
    private inner class Timelines(val forecast: ChildForecast) {
        private val cache = mutableMapOf<Category, List<SizeWindow>>()

        fun windows(category: Category): List<SizeWindow> =
            cache.getOrPut(category) { SizeTimeline.build(forecast, category, settings) }
    }
}
