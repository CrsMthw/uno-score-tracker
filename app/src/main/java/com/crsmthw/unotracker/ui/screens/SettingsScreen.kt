package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.data.ThemeMode
import com.crsmthw.unotracker.ui.ThemeViewModel
import com.crsmthw.unotracker.util.press
import com.crsmthw.unotracker.util.toggle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(themeVm: ThemeViewModel, onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()
    val amoled by themeVm.amoledBlack.collectAsStateWithLifecycle()
    val dynamic by themeVm.dynamicColor.collectAsStateWithLifecycle()
    val haptics by themeVm.haptics.collectAsStateWithLifecycle()
    val hf = LocalHapticFeedback.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionLabel(stringResource(R.string.appearance)) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(selected = themeMode == mode, onClick = { hf.press(); themeVm.setThemeMode(mode) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(stringResource(label)) }
                    }
                }
            }
            item { ToggleRow(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_sub), dynamic) { hf.toggle(it); themeVm.setDynamicColor(it) } }
            item { ToggleRow(stringResource(R.string.amoled), stringResource(R.string.amoled_sub), amoled) { hf.toggle(it); themeVm.setAmoledBlack(it) } }
            item { SectionLabel(stringResource(R.string.feedback)) }
            item { ToggleRow(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), haptics) { hf.toggle(it); themeVm.setHaptics(it) } }
            item { SectionLabel(stringResource(R.string.settings_app)) }
            item {
                TextButton(onClick = onOpenAbout) { Text(stringResource(R.string.about_and_data), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
