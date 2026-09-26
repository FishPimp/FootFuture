package se.storleksprognosen.app.data.local

import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Measurement
import java.time.LocalDate

fun ChildEntity.toDomain() = Child(
    id = id,
    name = name,
    gender = gender,
    birthdate = LocalDate.ofEpochDay(birthdateEpochDays),
)

fun Child.toEntity() = ChildEntity(
    id = id,
    name = name,
    gender = gender,
    birthdateEpochDays = birthdate.toEpochDay(),
)

fun MeasurementEntity.toDomain() = Measurement(
    id = id,
    childId = childId,
    date = LocalDate.ofEpochDay(dateEpochDays),
    footMm = footMm,
    heightCm = heightCm,
)

fun Measurement.toEntity() = MeasurementEntity(
    id = id,
    childId = childId,
    dateEpochDays = date.toEpochDay(),
    footMm = footMm,
    heightCm = heightCm,
)

fun InventoryEntity.toDomain() = InventoryItem(
    id = id,
    ownerChildId = ownerChildId,
    title = title,
    category = category,
    size = size,
    season = season,
)

fun InventoryItem.toEntity() = InventoryEntity(
    id = id,
    ownerChildId = ownerChildId,
    title = title,
    category = category,
    size = size,
    season = season,
)
