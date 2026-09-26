package se.storleksprognosen.app

import android.content.Context
import se.storleksprognosen.app.data.demo.DemoData
import se.storleksprognosen.app.data.local.StorleksprognosenDatabase
import se.storleksprognosen.app.data.prefs.SettingsStore
import se.storleksprognosen.app.data.repository.RoomChildRepository
import se.storleksprognosen.app.data.repository.RoomInventoryRepository
import se.storleksprognosen.domain.repository.ChildRepository
import se.storleksprognosen.domain.repository.InventoryRepository

/** Enkel manuell beroendeinjektion för hela appen. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: StorleksprognosenDatabase by lazy { StorleksprognosenDatabase.build(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val childRepository: ChildRepository by lazy { RoomChildRepository(database) }
    val inventoryRepository: InventoryRepository by lazy { RoomInventoryRepository(database) }

    val demoData: DemoData by lazy { DemoData(childRepository, inventoryRepository) }
}
