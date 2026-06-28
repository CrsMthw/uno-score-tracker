package com.crsmthw.unotracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crsmthw.unotracker.R
import com.crsmthw.unotracker.domain.CardCategory
import com.crsmthw.unotracker.domain.Manual
import com.crsmthw.unotracker.domain.ManualSection
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.ui.components.BottomFadeScrim
import com.crsmthw.unotracker.ui.rememberAppContainer
import kotlinx.coroutines.launch

private sealed interface Block
private data class ProseBlock(val section: ManualSection) : Block
private data object GlossaryBlock : Block
private data object ScoringBlock : Block

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManualScreen(variantId: String, onBack: () -> Unit, onStartGame: () -> Unit) {
    val container = rememberAppContainer()
    val variant = container.variantCatalog.byId(variantId) ?: return
    val manual = container.manualLibrary.byId(variantId)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(variant.displayName) },
                subtitle = { Text(manual?.subtitle?.ifEmpty { null } ?: stringResource(R.string.pocket_rulebook)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ManualContent(variant, manual, onStartGame, Modifier.fillMaxSize())
            BottomFadeScrim()
        }
    }
}

/** The booklet body (no Scaffold chrome) — shared by the standalone reader and the wide-pane Rules layout. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManualContent(variant: Variant, manual: Manual?, onStartGame: () -> Unit, modifier: Modifier = Modifier) {
    val blocks = remember(variant.id) {
        buildList {
            manual?.sectionsFor(Manual.BEFORE_GLOSSARY)?.forEach { add(ProseBlock(it)) }
            add(GlossaryBlock)
            manual?.sectionsFor(Manual.BEFORE_SCORING)?.forEach { add(ProseBlock(it)) }
            add(ScoringBlock)
            manual?.sectionsFor(Manual.AFTER_SCORING)?.forEach { add(ProseBlock(it)) }
        }
    }
    val scoringListIndex = blocks.indexOfFirst { it is ScoringBlock } + 1 // +1 for the header item
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                QuickFacts(variant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { scope.launch { listState.animateScrollToItem(scoringListIndex) } }) { Text(stringResource(R.string.jump_to_scoring)) }
                    FilledTonalButton(onClick = onStartGame) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.height(18.dp)); Text(stringResource(R.string.action_start_game_lead))
                    }
                }
            }
        }
        itemsIndexed(blocks) { _, block ->
            when (block) {
                is ProseBlock -> CollapsibleSection(block.section.title, defaultExpanded = true) {
                    Text(block.section.body, style = MaterialTheme.typography.bodyMedium)
                }
                GlossaryBlock -> CollapsibleSection(stringResource(R.string.action_special_cards), defaultExpanded = true) { CardGlossary(variant) }
                ScoringBlock -> CollapsibleSection(stringResource(R.string.scoring), defaultExpanded = true) { ScoringTable(variant) }
            }
        }
        variant.sourceUrl?.let { url ->
            item {
                Text(
                    stringResource(R.string.source_note, url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSection(title: String, defaultExpanded: Boolean, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(defaultExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand), Modifier.rotate(rotation))
            }
            AnimatedVisibility(expanded) { Column(Modifier.padding(top = 8.dp)) { content() } }
        }
    }
}

@Composable
private fun CardGlossary(variant: Variant) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardCategory.entries.forEach { category ->
            val cards = variant.cards.filter { it.category == category }
            if (cards.isNotEmpty()) {
                Text(stringResource(category.glossaryLabelRes()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                cards.forEach { card ->
                    Column(Modifier.padding(bottom = 4.dp)) {
                        Text(card.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(card.effect, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringTable(variant: Variant) {
    Column {
        Text(
            stringResource(R.string.scoring_summary, variant.target) +
                if (variant.altScoringMode != null) stringResource(R.string.scoring_alt) else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        variant.cards.forEach { card ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(card.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(if (card.isFaceValue) stringResource(R.string.face_value) else stringResource(R.string.points_value, card.points ?: 0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        variant.bonuses.forEach { bonus ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(bonus.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.bonus_value, bonus.points), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickFacts(variant: Variant) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.fact_players, variant.players)) })
        SuggestionChip(onClick = {}, label = { Text(variant.ages) })
        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.fact_cards, variant.deckSize)) })
        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.fact_first_to, variant.target)) })
        if (variant.stackingAllowed) AssistChip(onClick = {}, label = { Text(stringResource(R.string.fact_stacking_chip)) })
        if (variant.sevenZeroBuiltIn) AssistChip(onClick = {}, label = { Text(stringResource(R.string.fact_seven_zero_chip)) })
    }
}

private fun CardCategory.glossaryLabelRes(): Int = when (this) {
    CardCategory.NUMBER -> R.string.glossary_numbers
    CardCategory.ACTION -> R.string.glossary_actions
    CardCategory.WILD -> R.string.glossary_wilds
}
