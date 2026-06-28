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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.ui.RoundHistoryViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoundHistoryScreen(gameId: Long, onBack: () -> Unit, onEditRound: (Long) -> Unit) {
    val container = rememberAppContainer()
    val vm: RoundHistoryViewModel = viewModel(factory = UnoViewModelFactory(container, gameId = gameId))
    val rounds by vm.rounds.collectAsStateWithLifecycle()
    val gs by vm.gameState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(gs?.roundCount) { vm.refresh() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.round_history)) },
                subtitle = { Text(stringResource(R.string.round_history_sub)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
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
                if (rounds.isEmpty()) item { EmptyHint(stringResource(R.string.no_rounds)) }
                items(rounds.reversed(), key = { it.roundId }) { round ->
                    Card(onClick = { onEditRound(round.roundId) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row {
                                Text(stringResource(R.string.round_number, round.roundNumber), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                round.endedOnSide?.let { Text(it.name.lowercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            round.entries.sortedByDescending { it.delta }.forEach { e ->
                                Row {
                                    Text(
                                        if (e.isWinner) stringResource(R.string.went_out_suffix, e.name) else e.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (e.isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
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
}
