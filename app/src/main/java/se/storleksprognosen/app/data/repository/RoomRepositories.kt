package se.storleksprognosen.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.storleksprognosen.app.data.local.StorleksprognosenDatabase
import se.storleksprognosen.app.data.local.toDomain
import se.storleksprognosen.app.data.local.toEntity
import se.storleksprognosen.domain.model.Child
import se.storleksprognosen.domain.model.InventoryItem
import se.storleksprognosen.domain.model.Measurement
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.repository.InventoryRepository

class RoomChildRepository(database: StorleksprognosenDatabase) : ChildRepository {

    private val childDao = database.childDao()
    private val measurementDao = database.measurementDao()

    override fun observeChildren(): Flow<List<Child>> =
        childDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMeasurements(): Flow<List<Measurement>> =
        measurementDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveChild(child: Child) = childDao.upsert(child.toEntity())

    // Mätningar och plagg tas bort via ForeignKey.CASCADE.
    override suspend fun deleteChild(childId: String) = childDao.delete(childId)

    override suspend fun saveMeasurement(measurement: Measurement) =
        measurementDao.upsert(measurement.toEntity())

    override suspend fun deleteMeasurement(measurementId: String) = measurementDao.delete(measurementId)
}

class RoomInventoryRepository(database: StorleksprognosenDatabase) : InventoryRepository {

    private val dao = database.inventoryDao()

    override fun observeInventory(): Flow<List<InventoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveItem(item: InventoryItem) = dao.upsert(item.toEntity())

    override suspend fun deleteItem(itemId: String) = dao.delete(itemId)
}
