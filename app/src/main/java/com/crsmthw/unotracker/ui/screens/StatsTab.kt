package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.ui.StatsViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.rememberAppContainer

@Composable
fun StatsTab(onOpenDetail: (Long) -> Unit) {
    val container = rememberAppContainer()
    val vm: StatsViewModel = viewModel(factory = UnoViewModelFactory(container))
    val leaderboard by vm.leaderboard.collectAsStateWithLifecycle()
    val finished by vm.finishedGames.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionLabel(stringResource(R.string.leaderboard)) }
        if (leaderboard.isEmpty()) {
            item { EmptyHint(stringResource(R.string.leaderboard_empty)) }
        } else {
            items(leaderboard, key = { it.playerName }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.playerName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.win_pct, entry.winPct), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.won_of_played, entry.gamesWon, entry.gamesPlayed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.recent_games)) }
        if (finished.isEmpty()) {
            item { EmptyHint(stringResource(R.string.no_finished_games)) }
        } else {
            items(finished, key = { it.gameId }) { game ->
                Card(onClick = { onOpenDetail(game.gameId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(game.variantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.winner_rounds, game.winnerName ?: stringResource(R.string.dash), game.roundCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(game.entityNames.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}
