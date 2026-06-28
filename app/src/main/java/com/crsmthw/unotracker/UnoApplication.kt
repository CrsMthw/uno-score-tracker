package com.crsmthw.unotracker

import android.app.Application
import com.crsmthw.unotracker.di.AppContainer

class UnoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
