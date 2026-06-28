package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.data.model.EntityStanding
import com.crsmthw.unotracker.data.model.GameState
import com.crsmthw.unotracker.ui.ActiveGameViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.components.WavyRing
import com.crsmthw.unotracker.ui.components.isWidePane
import com.crsmthw.unotracker.ui.rememberAppContainer
import com.crsmthw.unotracker.ui.theme.UnoColors
import com.crsmthw.unotracker.util.confirm
import com.crsmthw.unotracker.util.press
import com.crsmthw.unotracker.util.reject
import com.crsmthw.unotracker.util.toggle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveGameScreen(
    gameId: Long,
    onBack: () -> Unit,
    onAddRound: () -> Unit,
    onEditRound: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenManual: (String) -> Unit,
    onGameEnded: () -> Unit,
    onAbandoned: () -> Unit,
) {
    val container = rememberAppContainer()
    val vm: ActiveGameViewModel = viewModel(factory = UnoViewModelFactory(container, gameId = gameId))
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var view by rememberSaveable { mutableIntStateOf(0) } // 0 = Table, 1 = Standings
    var showEnd by remember { mutableStateOf(false) }

    // Route to the winner screen once, when the game first finishes (not again on back-navigation).
    LaunchedEffect(state?.isFinished, state?.resultsSeen) {
        val s = state
        if (s != null && s.isFinished && !s.resultsSeen) onGameEnded()
    }
    // An abandoned (all-zero) game is cancelled with no winner — just return home.
    LaunchedEffect(state?.isCancelled) {
        if (state?.isCancelled == true) onAbandoned()
    }

    val gs = state ?: return
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(gs.variant.displayName) },
                subtitle = { Text(stringResource(R.string.active_subtitle, gs.roundCount + 1, gs.target)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.cd_round_history)) }
                    IconButton(onClick = { onOpenManual(gs.variant.id) }) { Icon(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.cd_rules)) }
                    IconButton(onClick = { showEnd = true }) { Icon(Icons.Filled.Flag, stringResource(R.string.cd_end_game)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.press(); onAddRound() },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_round)) },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isWidePane()) {
                // Unfolded: The Table and the standings sit side by side.
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TheTable(gs, onTapPlayer = { haptics.press(); vm.setTurn(it) })
                        LiveControls(gs, onToggleDirection = { haptics.toggle(!gs.directionClockwise); vm.toggleDirection() }, onSetColor = { haptics.press(); vm.setActiveColor(it) })
                    }
                    LazyColumn(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { SectionLabel(stringResource(R.string.standings)) }
                        items(gs.standings.sortedByDescending { it.total }, key = { it.entityId }) { s ->
                            StandingRow(s, gs, onHand = { delta -> haptics.press(); vm.setHandSize(s.entityId, s.handSize + delta) })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf(R.string.view_table, R.string.view_standings).forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = view == index,
                                    onClick = { haptics.press(); view = index },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                                ) { Text(stringResource(label)) }
                            }
                        }
                    }
                    if (view == 0) {
                        item { TheTable(gs, onTapPlayer = { haptics.press(); vm.setTurn(it) }) }
                        item {
                            LiveControls(
                                gs,
                                onToggleDirection = { haptics.toggle(!gs.directionClockwise); vm.toggleDirection() },
                                onSetColor = { haptics.press(); vm.setActiveColor(it) },
                            )
                        }
                    } else {
                        items(gs.standings.sortedByDescending { it.total }, key = { it.entityId }) { s ->
                            StandingRow(s, gs, onHand = { delta -> haptics.press(); vm.setHandSize(s.entityId, (s.handSize + delta)) })
                        }
                    }
                }
            }
            BottomFadeScrim()
        }
    }

    if (showEnd) {
        val allZero = gs.standings.all { it.total == 0 }
        AlertDialog(
            onDismissRequest = { showEnd = false },
            title = { Text(stringResource(if (allZero) R.string.abandon_game_title else R.string.end_game_title)) },
            text = { Text(stringResource(if (allZero) R.string.abandon_game_body else R.string.end_game_body)) },
            confirmButton = {
                TextButton(onClick = { haptics.confirm(); showEnd = false; vm.endGame() }) {
                    Text(stringResource(if (allZero) R.string.abandon_game_confirm else R.string.end_game_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showEnd = false }) { Text(stringResource(R.string.end_game_dismiss)) } },
        )
    }
}

// ── The Table — the live board ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TheTable(gs: GameState, onTapPlayer: (Long) -> Unit) {
    val players = gs.standings.sortedBy { it.seatOrder }
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp)) {
        val board = min(maxWidth.value, 360f).dp
        val center = board / 2
        val radius = center - 52.dp

        Box(Modifier.size(board).align(Alignment.Center)) {
            // Wavy direction ring — a slow wave that travels in the direction of play.
            WavyRing(
                clockwise = gs.directionClockwise,
                modifier = Modifier.size(radius * 2 + 28.dp).align(Alignment.Center),
            )
            CenterDisc(gs, Modifier.align(Alignment.Center))
            players.forEachIndexed { i, s ->
                val angle = Math.toRadians((-90.0 + i * 360.0 / players.size))
                val dx = (radius.value * cos(angle)).toFloat().dp
                val dy = (radius.value * sin(angle)).toFloat().dp
                Box(
                    Modifier.align(Alignment.Center).offset(x = dx, y = dy).size(78.dp, 86.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayerNode(s, isTurn = gs.turnEntityId == s.entityId, onClick = { onTapPlayer(s.entityId) })
                }
            }
        }
    }
}

@Composable
private fun CenterDisc(gs: GameState, modifier: Modifier = Modifier) {
    val hasColor = gs.activeColor != null
    val suit = gs.activeColor?.let { UnoColors.byKey(it) } ?: UnoColors.Wild
    Box(
        modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (hasColor) suit.fill else MaterialTheme.colorScheme.surfaceVariant)
            .border(if (hasColor) 0.dp else 1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasColor) Text(stringResource(R.string.set_color), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun PlayerNode(s: EntityStanding, isTurn: Boolean, onClick: () -> Unit) {
    val border = if (isTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (isTurn) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(if (s.eliminated) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(s.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Text(s.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text("${s.total}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(if (s.eliminated) stringResource(R.string.out) else stringResource(R.string.cards_count, s.handSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LiveControls(gs: GameState, onToggleDirection: () -> Unit, onSetColor: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = onToggleDirection,
            label = { Text(stringResource(if (gs.directionClockwise) R.string.direction_cw else R.string.direction_ccw)) },
            leadingIcon = { Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)) },
        )
        Text(stringResource(R.string.active_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val colors = (gs.variant.lightColors + gs.variant.darkColors).distinct()
            colors.forEach { key ->
                val suit = UnoColors.byKey(key)
                Box(
                    Modifier
                        .size(34.dp).clip(CircleShape).background(suit.fill)
                        .border(if (gs.activeColor == key) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { onSetColor(if (gs.activeColor == key) null else key) },
                )
            }
        }
    }
}

@Composable
private fun StandingRow(s: EntityStanding, gs: GameState, onHand: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(s.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (s.isTeam) Text(s.memberNames.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (s.eliminated) stringResource(R.string.eliminated) else stringResource(R.string.points_to_target, s.pointsToTarget(gs.target)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${s.total}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearWavyProgressIndicator(progress = { (s.total.toFloat() / gs.target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.hand_label, s.handSize), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                FilledIconButton(onClick = { onHand(-1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, stringResource(R.string.cd_minus)) }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { onHand(1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, stringResource(R.string.cd_plus)) }
            }
        }
    }
}
