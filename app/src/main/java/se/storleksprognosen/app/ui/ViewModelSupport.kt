package se.storleksprognosen.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import se.storleksprognosen.app.AppContainer
import se.storleksprognosen.app.StorleksprognosenApp

/** Hämtar appens beroenden inifrån en ViewModel-factory. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StorleksprognosenApp).container
