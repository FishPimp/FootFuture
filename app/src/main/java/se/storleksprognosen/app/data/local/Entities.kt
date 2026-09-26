package se.storleksprognosen.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import se.storleksprognosen.domain.model.Category
import se.storleksprognosen.domain.model.Gender
import se.storleksprognosen.domain.model.Season

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gender: Gender,
    val birthdateEpochDays: Long,
)

/** Fot- och kroppslängd är valfria var för sig: fotmätaren sparar t.ex. bara foten. */
@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("childId")],
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val dateEpochDays: Long,
    val footMm: Float?,
    val heightCm: Float?,
)

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerChildId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ownerChildId")],
)
data class InventoryEntity(
    @PrimaryKey val id: String,
    val ownerChildId: String,
    val title: String,
    val category: Category,
    val size: Int, // EU-storlek eller centilong
    val season: Season,
)
