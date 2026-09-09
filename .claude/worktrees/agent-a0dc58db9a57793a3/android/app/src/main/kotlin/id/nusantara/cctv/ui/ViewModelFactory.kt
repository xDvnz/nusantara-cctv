package id.nusantara.cctv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import id.nusantara.cctv.AppContainer
import id.nusantara.cctv.CctvApp

/** Akses AppContainer dari ViewModel tanpa library DI. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CctvApp).container

fun factoryOf(create: (CreationExtras) -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            create(extras) as T
    }
