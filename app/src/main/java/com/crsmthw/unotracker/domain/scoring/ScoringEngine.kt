package com.crsmthw.unotracker.domain.scoring

import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.JointWinnerSplit
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.Variant

/** One card held in a loser's hand at round end. Numbers carry their rank; other cards a config key. */
sealed interface HandCard {
    /** A number card 0-9 — scores its [rank] (FACE_VALUE). */
    data class Number(val rank: Int) : HandCard
    /** An action/wild card, referenced by its `variants.json` `cards[].key` (Flip keys are side-specific). */
    data class Keyed(val key: String) : HandCard
}

/** How a loser's held-card value is supplied: a fast manual total, or a card-by-card breakdown. */
sealed interface HandTally {
    data class Manual(val total: Int) : HandTally
    data class Cards(val cards: List<HandCard>) : HandTally
}

/**
 * Everything one finished round needs to score. The caller supplies winner(s) and each non-winner's
 * hand; the engine never derives hand sizes itself. `eliminatedThisHand` are players whose hand is
 * NOT scored (No Mercy mercy rule); `knockouts` credits +250-style bonuses to whoever caused them.
 */
data class RoundInput(
    val winners: List<String>,
    val loserHands: Map<String, HandTally>,
    val endedOnSide: FlipSide? = null,
    val knockouts: Map<String, Int> = emptyMap(),
    val eliminatedThisHand: Set<String> = emptySet(),
)

/** Per-entity points awarded this round (the deltas added to running totals). */
data class RoundResult(val deltas: Map<String, Int>)

enum class WinReason { TARGET, LOWEST_TOTAL, LAST_STANDING }

/**
 * Outcome of checking whether the game is over after a round's deltas are applied.
 * [newlyEliminated] is only populated in the elimination "Challenge Game" reading.
 */
data class GameEndEval(
    val gameOver: Boolean,
    val winners: List<String>,
    val reason: WinReason?,
    val newlyEliminated: Set<String>,
)

/**
 * The universal, config-driven UNO scoring engine. Pure (no Android, no I/O). One algorithm for every
 * variant; behaviour differs only by the [Variant] config + the chosen [ScoringMode]. Validated
 * against `docs/uno-scoring-test-fixtures.md` (F1-F14 + the cross-variant invariants).
 */
class ScoringEngine(private val jointWinnerSplit: JointWinnerSplit = JointWinnerSplit.FULL_TO_EACH) {

    /** Resolve a single held card to its point value. */
    fun cardValue(variant: Variant, card: HandCard): Int = when (card) {
        is HandCard.Number -> card.rank.coerceIn(0, 9)
        is HandCard.Keyed -> variant.cardByKey(card.key)?.points ?: 0
    }

    /** Resolve a loser's whole hand to a point total. */
    fun handValue(variant: Variant, tally: HandTally): Int = when (tally) {
        is HandTally.Manual -> tally.total
        is HandTally.Cards -> tally.cards.sumOf { cardValue(variant, it) }
    }

    /**
     * Per-round point deltas for every entity involved. Mode A: winner(s) gain the summed value of all
     * non-eliminated losers' hands (split per [jointWinnerSplit] if >1 winner). Mode B: each loser
     * accrues their own hand value; winners score 0. Knockout bonuses are additive and independent of
     * the card totals (No Mercy +250 per knockout).
     */
    fun roundDeltas(variant: Variant, mode: ScoringMode, input: RoundInput): RoundResult {
        val deltas = HashMap<String, Int>()
        fun add(entity: String, points: Int) { deltas[entity] = (deltas[entity] ?: 0) + points }

        // Seed everyone involved at 0 so the map is complete.
        input.winners.forEach { add(it, 0) }
        input.loserHands.keys.forEach { add(it, 0) }
        input.knockouts.keys.forEach { add(it, 0) }

        // Knockout bonuses — credited to whoever caused them, winner or not.
        for ((causer, count) in input.knockouts) {
            if (count > 0) add(causer, count * variant.mercyKnockoutPoints)
        }

        val scoredLosers = input.loserHands.filterKeys {
            it !in input.winners && it !in input.eliminatedThisHand
        }

        when (mode) {
            ScoringMode.WINNER_TAKES -> {
                val loserTotal = scoredLosers.values.sumOf { handValue(variant, it) }
                val winners = input.winners
                if (winners.isNotEmpty()) {
                    val perWinner = when (jointWinnerSplit) {
                        JointWinnerSplit.FULL_TO_EACH -> loserTotal
                        JointWinnerSplit.SPLIT_EVENLY -> loserTotal / winners.size
                    }
                    winners.forEach { add(it, perWinner) }
                }
            }
            ScoringMode.LOWEST_TOTAL -> {
                // Winner scores 0; each remaining loser accrues their own held-card value.
                for ((entity, tally) in scoredLosers) add(entity, handValue(variant, tally))
            }
        }
        return RoundResult(deltas)
    }

    /**
     * Decide whether the game is over after [totals] have been updated.
     *
     * - Last-player-standing (No Mercy mercy rule, or any elimination reading): if exactly one entity
     *   is still in the game, they win.
     * - Mode A (winner-takes): once any active entity reaches [Variant.target], the highest total wins.
     * - Mode B (lowest-total): once any active entity reaches the target, the **lowest** total wins —
     *   unless [challengeElimination] is on, where crossing the target eliminates you and play continues
     *   until one entity remains.
     */
    fun evaluateGameEnd(
        variant: Variant,
        mode: ScoringMode,
        target: Int,
        totals: Map<String, Int>,
        allEntities: Set<String>,
        eliminatedFromGame: Set<String> = emptySet(),
        challengeElimination: Boolean = false,
    ): GameEndEval {
        fun total(e: String) = totals[e] ?: 0

        val activeBefore = allEntities - eliminatedFromGame
        val targetCrossers =
            if (challengeElimination) activeBefore.filter { total(it) >= target }.toSet()
            else emptySet()
        val remaining = activeBefore - targetCrossers

        val lastStandingApplies = challengeElimination || variant.usesLastPlayerStanding
        if (lastStandingApplies && allEntities.size > 1 && remaining.size == 1) {
            return GameEndEval(true, listOf(remaining.first()), WinReason.LAST_STANDING, targetCrossers)
        }

        if (!challengeElimination) {
            val crossers = activeBefore.filter { total(it) >= target }
            if (crossers.isNotEmpty()) {
                val winner = when (mode) {
                    ScoringMode.WINNER_TAKES -> activeBefore.maxByOrNull { total(it) }
                    ScoringMode.LOWEST_TOTAL -> activeBefore.minByOrNull { total(it) }
                }
                if (winner != null) {
                    val reason =
                        if (mode == ScoringMode.LOWEST_TOTAL) WinReason.LOWEST_TOTAL else WinReason.TARGET
                    return GameEndEval(true, listOf(winner), reason, emptySet())
                }
            }
        }

        return GameEndEval(false, emptyList(), null, targetCrossers)
    }
}
