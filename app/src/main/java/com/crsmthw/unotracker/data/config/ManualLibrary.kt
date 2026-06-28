package com.crsmthw.unotracker.data.config

import android.content.Context
import com.crsmthw.unotracker.data.config.dto.ManualsRootDto
import com.crsmthw.unotracker.domain.Manual
import com.crsmthw.unotracker.domain.ManualSection
import com.google.gson.Gson

/** Loads the bundled `manuals.json` prose once and exposes each variant's rulebook by id. */
class ManualLibrary(context: Context) {

    private val byVariantId: Map<String, Manual> = run {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = Gson().fromJson(json, ManualsRootDto::class.java)
        root?.manuals.orEmpty().mapNotNull { dto ->
            val id = dto.variantId ?: return@mapNotNull null
            Manual(
                variantId = id,
                subtitle = dto.subtitle ?: "",
                sections = dto.sections.orEmpty().mapNotNull { s ->
                    val key = s.key ?: return@mapNotNull null
                    ManualSection(key = key, title = s.title ?: "", body = s.body ?: "")
                },
            )
        }.associateBy { it.variantId }
    }

    fun byId(variantId: String): Manual? = byVariantId[variantId]

    companion object {
        const val ASSET_NAME = "manuals.json"
    }
}
