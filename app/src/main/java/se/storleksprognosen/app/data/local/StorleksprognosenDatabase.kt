package se.storleksprognosen.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChildEntity::class,
        MeasurementEntity::class,
        InventoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StorleksprognosenDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        fun build(context: Context): StorleksprognosenDatabase =
            Room.databaseBuilder(context, StorleksprognosenDatabase::class.java, "storleksprognosen.db")
                .build()
    }
}
