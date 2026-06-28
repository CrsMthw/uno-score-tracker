package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.ui.GameDetailViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer
import com.crsmthw.unotracker.util.BiometricAuth
import com.crsmthw.unotracker.util.findFragmentActivity
import com.crsmthw.unotracker.util.reject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameDetailScreen(gameId: Long, onBack: () -> Unit, onDeleted: () -> Unit) {
    val container = rememberAppContainer()
    val vm: GameDetailViewModel = viewModel(factory = UnoViewModelFactory(container, gameId = gameId))
    val gs by vm.state.collectAsStateWithLifecycle()
    val rounds by vm.rounds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var confirm by remember { mutableStateOf(false) }

    val game = gs ?: return
    val winnerName = game.standings.firstOrNull { it.entityId in game.winnerEntityIds }?.name
    val ordered = if (game.scoringMode == ScoringMode.LOWEST_TOTAL) game.standings.sortedBy { it.total } else game.standings.sortedByDescending { it.total }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(game.variant.displayName) },
                subtitle = { Text(winnerName?.let { stringResource(R.string.won_suffix, it) } ?: stringResource(R.string.finished_game)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                actions = { IconButton(onClick = { haptics.reject(); confirm = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.cd_delete_game), tint = MaterialTheme.colorScheme.error) } },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SectionLabel(stringResource(R.string.final_standings)) }
                items(ordered, key = { it.entityId }) { s ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp)) {
                            Text(if (s.entityId in game.winnerEntityIds) stringResource(R.string.winner_tag, s.name) else s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${s.total}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { SectionLabel(stringResource(R.string.round_by_round)) }
                items(rounds, key = { it.roundId }) { round ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.round_number, round.roundNumber), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            round.entries.sortedByDescending { it.delta }.forEach { e ->
                                Row {
                                    Text(if (e.isWinner) stringResource(R.string.went_out_suffix, e.name) else e.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = if (e.isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text(if (e.delta >= 0) stringResource(R.string.delta_plus, e.delta) else stringResource(R.string.delta_value, e.delta), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            BottomFadeScrim()
        }
    }

    if (confirm) {
        val bioTitle = stringResource(R.string.biometric_delete_game)
        val bioSub = stringResource(R.string.biometric_delete_game_sub)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.delete_game_title)) },
            text = { Text(stringResource(R.string.delete_game_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    BiometricAuth.authenticate(
                        activity = context.findFragmentActivity(),
                        title = bioTitle,
                        subtitle = bioSub,
                        onSuccess = { vm.delete(onDeleted) },
                    )
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
