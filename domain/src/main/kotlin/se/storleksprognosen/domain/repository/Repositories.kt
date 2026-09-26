package se.storleksprognosen.domain.repository

import kotlinx.coroutines.flow.Flow
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Measurement

interface ChildRepository {
    fun observeChildren(): Flow<List<Child>>
    fun observeMeasurements(): Flow<List<Measurement>>

    suspend fun saveChild(child: Child)

    /** Tar även bort barnets mätningar och sparade plagg. */
    suspend fun deleteChild(childId: String)

    suspend fun saveMeasurement(measurement: Measurement)
    suspend fun deleteMeasurement(measurementId: String)
}

interface InventoryRepository {
    fun observeInventory(): Flow<List<InventoryItem>>

    suspend fun saveItem(item: InventoryItem)
    suspend fun deleteItem(itemId: String)
}
