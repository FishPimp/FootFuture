package se.storleksprognosen.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY birthdateEpochDays ASC, name ASC")
    fun observeAll(): Flow<List<ChildEntity>>

    @Upsert
    suspend fun upsert(child: ChildEntity)

    @Query("DELETE FROM children WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY dateEpochDays DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY category ASC, size ASC, title ASC")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Upsert
    suspend fun upsert(item: InventoryEntity)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun delete(id: String)
}
