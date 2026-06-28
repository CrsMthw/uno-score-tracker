package com.crsmthw.unotracker.domain

/** One prose section of a variant's in-app rulebook. Point values / card glossary are NOT stored here
 *  — they render from the shared [Variant] config so the reference and the calculator can't disagree. */
data class ManualSection(val key: String, val title: String, val body: String)

/** A variant's reworded rulebook (prose only). Quick Facts, the card glossary and the scoring table all
 *  come from the [Variant] config; this supplies the booklet narrative around them. */
data class Manual(
    val variantId: String,
    val subtitle: String,
    val sections: List<ManualSection>,
) {
    fun sectionsFor(keys: List<String>): List<ManualSection> =
        keys.mapNotNull { key -> sections.firstOrNull { it.key == key } }

    companion object {
        // Canonical section order (uno-manual-library.md §1.2). The reader interleaves the
        // config-driven card glossary (after these) and scoring table at fixed points.
        val BEFORE_GLOSSARY = listOf("object", "contents", "setup", "how_to_play")
        val BEFORE_SCORING = listOf("calling_uno", "going_out")
        val AFTER_SCORING = listOf("winning", "two_player", "notes")
    }
}
