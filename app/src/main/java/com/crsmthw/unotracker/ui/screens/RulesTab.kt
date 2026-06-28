package com.crsmthw.unotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.ui.components.isWidePane
import com.crsmthw.unotracker.ui.rememberAppContainer

@Composable
fun RulesTab(variants: List<Variant>, onOpenManual: (String) -> Unit, onOpenSetup: (String) -> Unit) {
    val container = rememberAppContainer()
    if (isWidePane()) {
        var selectedId by rememberSaveable { mutableStateOf(variants.firstOrNull()?.id ?: "") }
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionLabel(androidx.compose.ui.res.stringResource(com.crsmthw.unotracker.R.string.pocket_rulebook)) }
                items(variants, key = { it.id }) { variant ->
                    VariantListRow(variant, selected = variant.id == selectedId) { selectedId = variant.id }
                }
            }
            val variant = container.variantCatalog.byId(selectedId)
            if (variant != null) {
                val manual = container.manualLibrary.byId(variant.id)
                ManualContent(variant, manual, onStartGame = { onOpenSetup(variant.id) }, modifier = Modifier.weight(0.58f).fillMaxHeight())
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionLabel("Pocket rulebook") }
            items(variants, key = { it.id }) { variant ->
                Card(onClick = { onOpenManual(variant.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        VariantBlurb(variant, Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantListRow(variant: Variant, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp)) { VariantBlurb(variant, Modifier.weight(1f)) }
        }
    } else {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp)) { VariantBlurb(variant, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun VariantBlurb(variant: Variant, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(variant.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            androidx.compose.ui.res.stringResource(com.crsmthw.unotracker.R.string.variant_blurb_rules, variant.players, variant.ages, variant.deckSize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
