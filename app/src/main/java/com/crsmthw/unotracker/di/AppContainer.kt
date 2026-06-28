package com.crsmthw.unotracker.di

import android.content.Context
import com.crsmthw.unotracker.data.ThemePreferenceManager
import com.crsmthw.unotracker.data.config.ManualLibrary
import com.crsmthw.unotracker.data.config.VariantCatalog
import com.crsmthw.unotracker.data.db.UnoDatabase
import com.crsmthw.unotracker.data.repository.GameRepository

/**
 * Manual dependency container, built once in [com.crsmthw.unotracker.UnoApplication.onCreate]. Holds
 * the app-scoped singletons: the variant config catalog, the Room db, and the game repository.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val themePrefs: ThemePreferenceManager = ThemePreferenceManager(appContext)
    val variantCatalog: VariantCatalog = VariantCatalog(appContext)
    val manualLibrary: ManualLibrary = ManualLibrary(appContext)
    private val database: UnoDatabase = UnoDatabase.get(appContext)
    val gameRepository: GameRepository = GameRepository(database, variantCatalog)
}
