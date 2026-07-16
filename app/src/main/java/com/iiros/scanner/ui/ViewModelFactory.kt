package com.iiros.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Minimal `ViewModelProvider.Factory` for constructor injection without a DI framework. */
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
