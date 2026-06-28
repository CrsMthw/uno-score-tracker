package com.crsmthw.unotracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.ui.HomeViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer
import com.crsmthw.unotracker.ui.theme.UnoColors
import com.crsmthw.unotracker.util.press

private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    PLAY(R.string.tab_play, Icons.Filled.PlayArrow),
    RULES(R.string.tab_rules, Icons.AutoMirrored.Filled.MenuBook),
    STATS(R.string.tab_stats, Icons.Filled.EmojiEvents),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onOpenSetup: (String) -> Unit,
    onOpenGame: (Long) -> Unit,
    onOpenResults: (Long) -> Unit,
    onOpenManual: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val container = rememberAppContainer()
    val homeVm: HomeViewModel = viewModel(factory = UnoViewModelFactory(container))
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var tabIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val tab = MainTab.entries[tabIndex]

    val pendingResults by homeVm.pendingResultsGameId.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(pendingResults) { pendingResults?.let { onOpenResults(it) } }

    BackHandler(enabled = tab != MainTab.PLAY) { tabIndex = 0 }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(when (tab) { MainTab.PLAY -> R.string.main_title_play; MainTab.RULES -> R.string.main_title_rules; MainTab.STATS -> R.string.main_title_stats })) },
                subtitle = { Text(stringResource(when (tab) { MainTab.PLAY -> R.string.main_sub_play; MainTab.RULES -> R.string.main_sub_rules; MainTab.STATS -> R.string.main_sub_stats })) },
                actions = {
                    IconButton(onClick = onOpenRoster) { Icon(Icons.Filled.Group, contentDescription = stringResource(R.string.cd_players)) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = tab, label = "tab") { current ->
                when (current) {
                    MainTab.PLAY -> PlayTab(homeVm.variants, homeVm, onOpenSetup, onOpenGame)
                    MainTab.RULES -> RulesTab(homeVm.variants, onOpenManual, onOpenSetup)
                    MainTab.STATS -> StatsTab(onOpenDetail)
                }
            }
            BottomFadeScrim()
            FloatingTabBar(tab, onSelect = { if (it != tab) haptics.press(); tabIndex = it.ordinal }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingTabBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(expanded = true, modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
        MainTab.entries.forEach { item ->
            val label = stringResource(item.labelRes)
            Crossfade(targetState = item == selected, label = "tabsel") { isSelected ->
                if (isSelected) {
                    FilledIconButton(onClick = { onSelect(item) }) { Icon(item.icon, contentDescription = label) }
                } else {
                    IconButton(onClick = { onSelect(item) }) { Icon(item.icon, contentDescription = label) }
                }
            }
        }
    }
}

@Composable
private fun PlayTab(variants: List<Variant>, homeVm: HomeViewModel, onOpenSetup: (String) -> Unit, onOpenGame: (Long) -> Unit) {
    val active by homeVm.activeGame.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        active?.let { game ->
            item {
                OutlinedCard(onClick = { onOpenGame(game.gameId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.resume_game), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(game.variant.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.resume_detail, game.roundCount + 1, game.standings.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.choose_variant)) }
        items(variants, key = { it.id }) { variant -> VariantTile(variant, onClick = { onOpenSetup(variant.id) }) }
        item {
            Text(stringResource(R.string.reskins_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun VariantTile(variant: Variant, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniCardArt()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(variant.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.variant_blurb_play, variant.players, variant.deckSize, variant.target), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MiniCardArt() {
    Box(Modifier.size(40.dp, 52.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Dot(UnoColors.Red.fill); Dot(UnoColors.Yellow.fill) }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Dot(UnoColors.Green.fill); Dot(UnoColors.Blue.fill) }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}
