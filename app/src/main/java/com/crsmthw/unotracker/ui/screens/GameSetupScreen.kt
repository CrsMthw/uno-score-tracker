package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.ui.SetupViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer
import com.crsmthw.unotracker.util.confirm

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun GameSetupScreen(variantId: String, onBack: () -> Unit, onStarted: (Long) -> Unit) {
    val container = rememberAppContainer()
    val vm: SetupViewModel = viewModel(factory = UnoViewModelFactory(container, variantId = variantId))
    val variant = vm.variant
    val ui by vm.ui.collectAsStateWithLifecycle()
    val roster by vm.roster.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var newName by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(variant.displayName) },
                subtitle = { Text(stringResource(R.string.setup_subtitle)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (vm.canStart()) { haptics.confirm(); vm.start(onStarted) } },
                expanded = vm.canStart(),
                icon = { Icon(Icons.Filled.Check, null) },
                text = { Text(stringResource(R.string.setup_start)) },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionLabel(stringResource(R.string.setup_players)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.setup_add_player)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { vm.addPlayer(newName); newName = "" }) { Icon(Icons.Filled.Add, stringResource(R.string.cd_add)) }
                }
            }
            if (ui.players.isNotEmpty()) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ui.players.forEach { name ->
                            InputChip(
                                selected = true,
                                onClick = { vm.removePlayer(name) },
                                label = { Text(name) },
                                trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.cd_remove), modifier = Modifier.width(16.dp)) },
                            )
                        }
                    }
                }
            }
            val suggestions = roster.map { it.name }.filter { sug -> ui.players.none { it.equals(sug, true) } }
            if (suggestions.isNotEmpty()) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.take(12).forEach { name ->
                            AssistChip(onClick = { vm.addPlayer(name) }, label = { Text(name) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.setup_teams), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.setup_teams_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = ui.teamsEnabled, onCheckedChange = { vm.setTeamsEnabled(it) })
                }
            }
            if (ui.teamsEnabled) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.setup_team_count, ui.teamCount), modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.setTeamCount(ui.teamCount - 1) }) { Text("–", style = MaterialTheme.typography.titleLarge) }
                        IconButton(onClick = { vm.setTeamCount(ui.teamCount + 1) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
                items(ui.players.size) { i ->
                    val name = ui.players[i]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (0 until ui.teamCount).forEach { t ->
                                FilterChip(
                                    selected = ui.teamAssignment[name] == t,
                                    onClick = { vm.assignTeam(name, t) },
                                    label = { Text("${('A' + t)}") },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel(stringResource(R.string.setup_scoring)) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(ScoringMode.WINNER_TAKES to R.string.mode_winner_takes, ScoringMode.LOWEST_TOTAL to R.string.mode_lowest_total)
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = ui.scoringMode == mode,
                            onClick = { vm.setScoringMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            if (ui.scoringMode == ScoringMode.LOWEST_TOTAL) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.setup_elimination), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.setup_elimination_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = ui.challengeElimination, onCheckedChange = { vm.setChallengeElimination(it) })
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.setup_score_limit)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = {
                            targetText = it.filter(Char::isDigit).take(5)
                            vm.setTargetOverride(targetText.toIntOrNull())
                        },
                        label = { Text(stringResource(R.string.setup_custom_limit, variant.target)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                val notes = buildList {
                    add(stringResource(R.string.fact_first_to, vm.target))
                    if (variant.sevenZeroBuiltIn) add(stringResource(R.string.fact_seven_zero))
                    if (variant.stackingAllowed) add(stringResource(R.string.fact_stacking))
                }
                Text(stringResource(R.string.dot_join, notes.joinToString("  ·  ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BottomFadeScrim()
        }
    }
}
