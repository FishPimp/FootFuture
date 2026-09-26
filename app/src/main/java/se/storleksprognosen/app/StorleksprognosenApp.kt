package se.storleksprognosen.app

import android.app.Application

class StorleksprognosenApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
