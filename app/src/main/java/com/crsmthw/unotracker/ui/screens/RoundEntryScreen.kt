package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.data.model.GameState
import com.crsmthw.unotracker.domain.CardDef
import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.domain.scoring.HandCard
import com.crsmthw.unotracker.ui.EntryMode
import com.crsmthw.unotracker.ui.LoserEntry
import com.crsmthw.unotracker.ui.RoundEntryViewModel
import com.crsmthw.unotracker.ui.UnoViewModelFactory
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer
import com.crsmthw.unotracker.ui.theme.UnoColors
import com.crsmthw.unotracker.util.confirm
import com.crsmthw.unotracker.util.tick
import com.crsmthw.unotracker.util.toggle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun RoundEntryScreen(gameId: Long, roundId: Long, onDone: () -> Unit, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val vm: RoundEntryViewModel = viewModel(factory = UnoViewModelFactory(container, gameId = gameId, roundId = roundId))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val gs = ui.gameState ?: return
    val variant = gs.variant

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(if (vm.isEditing) R.string.round_title_edit else R.string.round_title_score)) },
                subtitle = { Text(variant.displayName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (vm.canSubmit()) { haptics.confirm(); vm.submit(onDone) } },
                icon = { Icon(Icons.Filled.Check, null) },
                text = { val gain = vm.previewWinnerGain(); Text(if (gain > 0) stringResource(R.string.action_save_gain, gain) else stringResource(R.string.action_save)) },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().consumeWindowInsets(padding).imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Flip ending side — gates the palette, so capture it first.
                if (variant.sideDependentScoring) {
                    item { SectionLabel(stringResource(R.string.ending_side)) }
                    item {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf(FlipSide.LIGHT to R.string.side_light, FlipSide.DARK to R.string.side_dark).forEachIndexed { i, (side, label) ->
                                SegmentedButton(
                                    selected = ui.endedOnSide == side,
                                    onClick = { vm.setSide(side) },
                                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                                ) { Text(stringResource(label)) }
                            }
                        }
                    }
                }

                item { SectionLabel(stringResource(if (variant.supportsJointWinners) R.string.who_went_out_joint else R.string.who_went_out)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gs.standings.filter { !it.eliminated }.forEach { s ->
                            FilterChip(
                                selected = s.entityId in ui.winners,
                                onClick = {
                                    haptics.toggle(s.entityId !in ui.winners)
                                    if (variant.supportsJointWinners) vm.toggleWinner(s.entityId) else vm.setSoleWinner(s.entityId)
                                },
                                label = { Text(s.name) },
                            )
                        }
                    }
                }

                val losers = vm.losers(gs)
                if (losers.isNotEmpty()) item { SectionLabel(stringResource(R.string.remaining_hands)) }
                items(losers, key = { it }) { entityId ->
                    val name = gs.standings.first { it.entityId == entityId }.name
                    LoserCard(
                        name = name,
                        variant = variant,
                        side = ui.endedOnSide,
                        entry = ui.entries[entityId] ?: LoserEntry(),
                        showMercy = variant.hasElimination,
                        onMode = { vm.setMode(entityId, it) },
                        onManual = { vm.setManual(entityId, it) },
                        onAddCard = { haptics.tick(); vm.addCard(entityId, it) },
                        onRemoveCardAt = { idx -> vm.removeCardAt(entityId, idx) },
                        onClear = { vm.clearCards(entityId) },
                        onKnockouts = { vm.setKnockouts(entityId, it) },
                        onToggleEliminated = { vm.toggleEliminated(entityId) },
                    )
                }
            }
            BottomFadeScrim()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoserCard(
    name: String,
    variant: Variant,
    side: FlipSide?,
    entry: LoserEntry,
    showMercy: Boolean,
    onMode: (EntryMode) -> Unit,
    onManual: (String) -> Unit,
    onAddCard: (HandCard) -> Unit,
    onRemoveCardAt: (Int) -> Unit,
    onClear: () -> Unit,
    onKnockouts: (Int) -> Unit,
    onToggleEliminated: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!entry.eliminated) Text(stringResource(R.string.subtotal, subtotal(variant, entry)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (entry.eliminated) {
                Text(stringResource(R.string.eliminated_not_scored), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(EntryMode.CARDS to R.string.entry_cards, EntryMode.MANUAL to R.string.entry_manual).forEachIndexed { i, (m, label) ->
                        SegmentedButton(selected = entry.mode == m, onClick = { onMode(m) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(stringResource(label)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                when (entry.mode) {
                    EntryMode.MANUAL -> OutlinedTextField(
                        value = entry.manualText,
                        onValueChange = onManual,
                        label = { Text(stringResource(R.string.points_held)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    EntryMode.CARDS -> CardPalette(variant, side, entry, onAddCard, onRemoveCardAt, onClear)
                }
            }
            if (showMercy) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = entry.eliminated, onClick = onToggleEliminated, label = { Text(stringResource(R.string.knocked_out_25)) })
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.kos_by_them, entry.knockouts), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onKnockouts(entry.knockouts + 1) }) { Text(stringResource(R.string.cd_plus)) }
                    TextButton(onClick = { onKnockouts(entry.knockouts - 1) }) { Text(stringResource(R.string.cd_minus)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardPalette(variant: Variant, side: FlipSide?, entry: LoserEntry, onAddCard: (HandCard) -> Unit, onRemoveCardAt: (Int) -> Unit, onClear: () -> Unit) {
    Column {
        // ── Selection buttons stay in a fixed position (added cards grow BELOW them) ──
        Text(stringResource(R.string.tap_to_add), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (variant.hasNumberCards) {
            val range = if (variant.id == "flip" || variant.id == "attack") 1..9 else 0..9
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                range.forEach { n ->
                    val suit = UnoColors.lightSuits[n % 4]
                    Box(
                        Modifier.size(34.dp, 44.dp).clip(RoundedCornerShape(8.dp)).background(suit.fill).clickable { onAddCard(HandCard.Number(n)) },
                        contentAlignment = Alignment.Center,
                    ) { Text("$n", color = suit.onFill, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            variant.paletteCards(side).forEach { card ->
                ActionCardChip(card) { onAddCard(HandCard.Keyed(card.key)) }
            }
        }
        // ── Added cards, below the buttons ──
        if (entry.cards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.added_tap_remove), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                entry.cards.forEachIndexed { index, card ->
                    AddedCardChip(variant, card) { onRemoveCardAt(index) }
                }
            }
        }
    }
}

private fun cardLabel(variant: Variant, card: HandCard): String = when (card) {
    is HandCard.Number -> card.rank.toString()
    is HandCard.Keyed -> variant.cardByKey(card.key)?.name ?: card.key
}

private fun cardPoints(variant: Variant, card: HandCard): Int = when (card) {
    is HandCard.Number -> card.rank
    is HandCard.Keyed -> variant.cardByKey(card.key)?.points ?: 0
}

@Composable
private fun AddedCardChip(variant: Variant, card: HandCard, onRemove: () -> Unit) {
    val isWild = card is HandCard.Keyed && variant.cardByKey(card.key)?.category == com.crsmthw.unotracker.domain.CardCategory.WILD
    val suit = if (isWild) UnoColors.Wild else UnoColors.byKey("blue")
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(suit.fill).clickable(onClick = onRemove).padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.card_value_label, cardLabel(variant, card), cardPoints(variant, card)), color = suit.onFill, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Close, stringResource(R.string.cd_remove), tint = suit.onFill, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ActionCardChip(card: CardDef, onClick: () -> Unit) {
    val suit = if (card.category == com.crsmthw.unotracker.domain.CardCategory.WILD) UnoColors.Wild else UnoColors.byKey("green")
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(suit.fill).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(stringResource(R.string.card_value_label, card.name, card.points ?: 0), color = suit.onFill, style = MaterialTheme.typography.labelMedium)
    }
}

private fun subtotal(variant: Variant, entry: LoserEntry): Int = when (entry.mode) {
    EntryMode.MANUAL -> entry.manualText.toIntOrNull() ?: 0
    EntryMode.CARDS -> entry.cards.sumOf { card ->
        when (card) {
            is HandCard.Number -> card.rank
            is HandCard.Keyed -> variant.cardByKey(card.key)?.points ?: 0
        }
    }
}
