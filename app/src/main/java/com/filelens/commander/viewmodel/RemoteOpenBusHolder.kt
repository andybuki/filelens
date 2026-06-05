package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import com.filelens.commander.data.RemoteOpenBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin wrapper so [com.filelens.commander.ui.screens.MainScreen] can collect
 * [RemoteOpenBus] events without needing a Hilt EntryPoint. The bus itself is
 * the singleton; this holder just routes it through `hiltViewModel()`.
 */
@HiltViewModel
class RemoteOpenBusHolder @Inject constructor(
    val bus: RemoteOpenBus
) : ViewModel()
