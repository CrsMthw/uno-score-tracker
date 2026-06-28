package com.crsmthw.unotracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crsmthw.unotracker.data.ThemeMode
import com.crsmthw.unotracker.data.ThemePreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes appearance + haptics prefs as state and persists changes. */
class ThemeViewModel(private val prefs: ThemePreferenceManager) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val amoledBlack: StateFlow<Boolean> =
        prefs.amoledBlack.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dynamicColor: StateFlow<Boolean> =
        prefs.dynamicColor.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val haptics: StateFlow<Boolean> =
        prefs.haptics.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setAmoledBlack(enabled: Boolean) = viewModelScope.launch { prefs.setAmoledBlack(enabled) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { prefs.setDynamicColor(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { prefs.setHaptics(enabled) }
}

class ThemeViewModelFactory(private val prefs: ThemePreferenceManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ThemeViewModel(prefs) as T
}
