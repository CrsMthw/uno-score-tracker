package com.crsmthw.unotracker.data.model

import com.crsmthw.unotracker.domain.scoring.HandCard
import com.crsmthw.unotracker.domain.scoring.HandTally
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Flattened, type-tagged representation of a [HandCard] so a card-by-card hand can round-trip through
 * Gson (a sealed interface can't). Stored in `round_entries.cardsJson`; lets a round be re-opened and
 * edited. All fields nullable for the same Unsafe-deserialization safety as the variant DTOs.
 */
data class PersistedCard(
    val kind: String? = null,   // "number" | "keyed"
    val rank: Int? = null,
    val key: String? = null,
)

private val gson = Gson()
private val listType = object : TypeToken<List<PersistedCard>>() {}.type

fun HandCard.toPersisted(): PersistedCard = when (this) {
    is HandCard.Number -> PersistedCard(kind = "number", rank = rank)
    is HandCard.Keyed -> PersistedCard(kind = "keyed", key = key)
}

fun PersistedCard.toHandCard(): HandCard? = when (kind) {
    "number" -> HandCard.Number(rank ?: 0)
    "keyed" -> key?.let { HandCard.Keyed(it) }
    else -> null
}

object HandCardJson {
    fun encode(cards: List<HandCard>): String = gson.toJson(cards.map { it.toPersisted() })

    fun decode(json: String?): List<HandCard> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<PersistedCard>>(json, listType).orEmpty().mapNotNull { it.toHandCard() }
        }.getOrDefault(emptyList())
    }
}

/** Build a [HandTally] from a stored round entry's manual total / cards json. */
fun tallyFrom(manualTotal: Int?, cardsJson: String?): HandTally? = when {
    manualTotal != null -> HandTally.Manual(manualTotal)
    cardsJson != null -> HandTally.Cards(HandCardJson.decode(cardsJson))
    else -> null
}
