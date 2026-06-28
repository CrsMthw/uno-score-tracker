package com.crsmthw.unotracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/** App-wide appearance + haptics prefs, persisted in DataStore. */
class ThemePreferenceManager(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        private val KEY_AMOLED = booleanPreferencesKey("amoled_black")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE] ?: 0) {
            1 -> ThemeMode.LIGHT
            2 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val amoledBlack: Flow<Boolean> = context.themeDataStore.data.map { it[KEY_AMOLED] ?: false }
    val dynamicColor: Flow<Boolean> = context.themeDataStore.data.map { it[KEY_DYNAMIC] ?: true }
    val haptics: Flow<Boolean> = context.themeDataStore.data.map { it[KEY_HAPTICS] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
                ThemeMode.SYSTEM -> 0
            }
        }
    }

    suspend fun setAmoledBlack(enabled: Boolean) =
        context.themeDataStore.edit { it[KEY_AMOLED] = enabled }.let {}

    suspend fun setDynamicColor(enabled: Boolean) =
        context.themeDataStore.edit { it[KEY_DYNAMIC] = enabled }.let {}

    suspend fun setHaptics(enabled: Boolean) =
        context.themeDataStore.edit { it[KEY_HAPTICS] = enabled }.let {}
}
