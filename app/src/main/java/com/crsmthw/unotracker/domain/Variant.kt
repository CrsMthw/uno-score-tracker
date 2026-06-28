package com.crsmthw.unotracker.domain

/** Card buckets. `points` on a [CardDef] is authoritative regardless of category. */
enum class CardCategory { NUMBER, ACTION, WILD }

/** The two official scoring methods. Mode A = winner-takes, Mode B = lowest-total. */
enum class ScoringMode(val key: String) {
    WINNER_TAKES("winner_takes"),
    LOWEST_TOTAL("lowest_total");

    companion object {
        fun fromKey(key: String?): ScoringMode =
            entries.firstOrNull { it.key == key } ?: WINNER_TAKES
    }
}

enum class DrawRule(val key: String) {
    DRAW_ONE("draw_one"),
    DRAW_UNTIL_PLAYABLE("draw_until_playable"),
    LAUNCHER("launcher");

    companion object {
        fun fromKey(key: String?): DrawRule =
            entries.firstOrNull { it.key == key } ?: DRAW_ONE
    }
}

/** How a shared (joint) win splits the loser total. Liar's Wild Liar's Challenge — fixture F11. */
enum class JointWinnerSplit { FULL_TO_EACH, SPLIT_EVENLY }

/** UNO Flip's two sides. Recorded per round; the side's cards carry their own (side-specific) keys. */
enum class FlipSide(val key: String) {
    LIGHT("light"), DARK("dark");

    companion object {
        fun fromKey(key: String?): FlipSide? = entries.firstOrNull { it.key == key }
    }
}

data class UnoFailPenalty(val type: String, val amount: Int)

data class Bonus(val key: String, val name: String, val points: Int, val manual: Boolean)

/**
 * A single card definition from the variant config. Drives both the scoring engine and the
 * card-by-card entry palette / manual glossary. [points] is null exactly when [isFaceValue] is true
 * (a number card scores its printed rank, resolved at tally time).
 */
data class CardDef(
    val key: String,
    val name: String,
    val category: CardCategory,
    val points: Int?,
    val isFaceValue: Boolean,
    val effect: String,
    val side: String?,            // "light" | "dark" | "both" | null
    val restricted: Boolean = false,
    val challengeable: Boolean = false,
    val optional: Boolean = false,
)

/**
 * One UNO variant's complete config — the single source of truth shared by the scoring engine, the
 * card palette, and the manual renderer. Parsed from `assets/variants.json`.
 */
data class Variant(
    val id: String,
    val displayName: String,
    val players: String,
    val ages: String,
    val deckSize: Int,
    val deckSizeRetail: Int?,
    val target: Int,
    val scoringMode: ScoringMode,
    val altScoringMode: ScoringMode?,
    val alternateWinCondition: String?,
    val winnerScoresZero: Boolean,
    val unoFailPenalty: UnoFailPenalty?,
    val stackingAllowed: Boolean,
    val stackingRule: String?,
    val sevenZeroBuiltIn: Boolean,
    val drawRule: DrawRule,
    val sideDependentScoring: Boolean,
    val supportsJointWinners: Boolean,
    val supportsTeams: Boolean,
    val twoPlayerReverseIsSkip: Boolean,
    val mercyRuleCardCount: Int?,
    val eliminatedHandScored: Boolean,
    val bonuses: List<Bonus>,
    val hasNumberCards: Boolean,
    val hasDrawCards: Boolean,
    val cantGoOutOn: List<String>,
    val lightColors: List<String>,
    val darkColors: List<String>,
    val cards: List<CardDef>,
    val notes: List<String>,
    val houseRules: List<String>,
    val sourceUrl: String?,
    val verifiedAgainstMattelPdf: Boolean,
) {
    fun cardByKey(key: String): CardDef? = cards.firstOrNull { it.key == key }

    /** Points awarded per player knocked out (No Mercy mercy rule); 0 when the variant has no such bonus. */
    val mercyKnockoutPoints: Int
        get() = bonuses.firstOrNull { it.key == "mercy_knockout" }?.points ?: 0

    val supportsLowestTotal: Boolean
        get() = altScoringMode == ScoringMode.LOWEST_TOTAL || scoringMode == ScoringMode.LOWEST_TOTAL

    val usesLastPlayerStanding: Boolean
        get() = alternateWinCondition == "last_player_standing"

    val hasElimination: Boolean
        get() = mercyRuleCardCount != null

    /** Non-number cards on the given side (or all, for non-Flip). Number ranks are handled separately. */
    fun paletteCards(side: FlipSide?): List<CardDef> = cards.filter { card ->
        !card.isFaceValue && when {
            !sideDependentScoring || side == null -> true
            card.side == null || card.side == "both" -> true
            else -> card.side == side.key
        }
    }
}
