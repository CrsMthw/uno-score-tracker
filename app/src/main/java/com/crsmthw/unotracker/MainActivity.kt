package com.crsmthw.unotracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crsmthw.unotracker.ui.ThemeViewModel
import com.crsmthw.unotracker.ui.ThemeViewModelFactory
import com.crsmthw.unotracker.ui.UnoApp
import com.crsmthw.unotracker.ui.theme.UnoTheme
import com.crsmthw.unotracker.util.HapticsConfig

class MainActivity : FragmentActivity() {

    private val container by lazy { (application as UnoApplication).container }

    private val themeVm: ThemeViewModel by viewModels {
        ThemeViewModelFactory(container.themePrefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Truly transparent bars so every screen's background shows through (re-asserted on rotation
        // by the theme's SideEffect) — playbook §6.
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        setContent {
            val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()
            val amoledBlack by themeVm.amoledBlack.collectAsStateWithLifecycle()
            val dynamicColor by themeVm.dynamicColor.collectAsStateWithLifecycle()
            val haptics by themeVm.haptics.collectAsStateWithLifecycle()

            LaunchedEffect(haptics) { HapticsConfig.enabled = haptics }

            UnoTheme(themeMode = themeMode, amoledBlack = amoledBlack, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Inset all content clear of the landscape side nav bar (foldable cover screen).
                    // Consumed here once, so nested navigationBarsPadding() (FAB / toolbar) only adds the
                    // bottom inset. The Surface still fills behind the transparent bar. Playbook §6.
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))) {
                        UnoApp(themeVm = themeVm)
                    }
                }
            }
        }
    }
}
