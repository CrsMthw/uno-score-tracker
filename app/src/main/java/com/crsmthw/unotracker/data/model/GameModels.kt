package com.crsmthw.unotracker.data.model

import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.domain.scoring.HandCard
import com.crsmthw.unotracker.domain.scoring.WinReason

/** A scored entity as entered at setup — a solo player (one member) or a team (several). */
data class NewEntity(
    val name: String,
    val isTeam: Boolean,
    val memberNames: List<String>,
)

/** Live snapshot of an active (or just-finished) game, assembled by the repository for the board. */
data class GameState(
    val gameId: Long,
    val variant: Variant,
    val scoringMode: ScoringMode,
    val challengeElimination: Boolean,
    val target: Int,
    val isFinished: Boolean,
    val isCancelled: Boolean,
    val resultsSeen: Boolean,
    val roundCount: Int,
    val standings: List<EntityStanding>,
    val directionClockwise: Boolean,
    val turnEntityId: Long?,
    val activeColor: String?,
    val winnerEntityIds: List<Long>,
    val winReason: WinReason?,
)

data class EntityStanding(
    val entityId: Long,
    val name: String,
    val isTeam: Boolean,
    val memberNames: List<String>,
    val seatOrder: Int,
    val total: Int,
    val handSize: Int,
    val eliminated: Boolean,
) {
    /** Points remaining to the target (never negative); winner-takes only. */
    fun pointsToTarget(target: Int): Int = (target - total).coerceAtLeast(0)
}

/** One row of a round, for history display and re-opening an edit. */
data class RoundEntrySummary(
    val entryId: Long,
    val entityId: Long,
    val name: String,
    val delta: Int,
    val isWinner: Boolean,
    val eliminatedThisRound: Boolean,
    val knockouts: Int,
    val manualTotal: Int?,
    val cards: List<HandCard>,
)

data class RoundSummary(
    val roundId: Long,
    val roundNumber: Int,
    val endedOnSide: FlipSide?,
    val entries: List<RoundEntrySummary>,
)

/** One non-winner's hand as entered on the round screen — exactly one of [manualTotal]/[cards] is used. */
data class HandDraft(
    val manualTotal: Int? = null,
    val cards: List<HandCard> = emptyList(),
)

/** The full input the round screen hands to the repository to record (or replace) a round. */
data class RoundDraft(
    val endedOnSide: FlipSide? = null,
    val winnerEntityIds: List<Long>,
    val hands: Map<Long, HandDraft>,
    val knockouts: Map<Long, Int> = emptyMap(),
    val eliminatedThisRound: Set<Long> = emptySet(),
)

/** Browse-history row for a finished game. */
data class FinishedGameSummary(
    val gameId: Long,
    val variantId: String,
    val variantName: String,
    val finishedAt: Long,
    val winnerName: String?,
    val entityNames: List<String>,
    val roundCount: Int,
)

data class LeaderboardEntry(
    val playerName: String,
    val gamesPlayed: Int,
    val gamesWon: Int,
) {
    val winPct: Int get() = if (gamesPlayed == 0) 0 else (gamesWon * 100) / gamesPlayed
}
