package com.crsmthw.unotracker.data.config

import android.content.Context
import com.crsmthw.unotracker.domain.Variant

/**
 * Loads the bundled `variants.json` once and exposes the variants by id. Built in the AppContainer;
 * the scoring engine and the manual renderer both read from here so point values never diverge.
 */
class VariantCatalog(context: Context) {

    val variants: List<Variant> = run {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        VariantParser.parse(json)
    }

    private val byId: Map<String, Variant> = variants.associateBy { it.id }

    fun byId(id: String): Variant? = byId[id]

    companion object {
        const val ASSET_NAME = "variants.json"
    }
}
