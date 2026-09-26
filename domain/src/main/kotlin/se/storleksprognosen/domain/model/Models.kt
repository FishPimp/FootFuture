package se.storleksprognosen.domain.model

import java.time.LocalDate

enum class Gender { GIRL, BOY }

/** Säsong som ett plagg är tänkt för. [SPRING_FALL] täcker både vår och höst. */
enum class Season { WINTER, SUMMER, SPRING_FALL, ALL_YEAR }

enum class Category { SHOES, CLOTHES }

/** Måtten som appen följer och prognostiserar. */
enum class Metric { HEIGHT_CM, FOOT_MM }

data class Child(
    val id: String,
    val name: String,
    val gender: Gender,
    val birthdate: LocalDate,
)

/** En mätning; minst ett av måtten är satt (fotmätaren sparar t.ex. bara fotlängd). */
data class Measurement(
    val id: String,
    val childId: String,
    val date: LocalDate,
    val footMm: Float?,
    val heightCm: Float?,
) {
    fun valueOf(metric: Metric): Float? = when (metric) {
        Metric.HEIGHT_CM -> heightCm
        Metric.FOOT_MM -> footMm
    }
}

/** Ett sparat plagg i förrådet. [size] är EU-storlek för skor och centilong för kläder. */
data class InventoryItem(
    val id: String,
    val ownerChildId: String,
    val title: String,
    val category: Category,
    val size: Int,
    val season: Season,
)
