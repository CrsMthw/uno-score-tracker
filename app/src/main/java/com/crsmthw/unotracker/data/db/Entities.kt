package com.crsmthw.unotracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room schema. Design spine: in-progress scores are NEVER denormalized onto an entity — they are
 * derived by replaying [round_entries] through the scoring engine in `GameRepository.recompute`.
 * The only cached fields a replay rewrites are [RoundEntryEntity.delta] and [GameEntityRow.eliminated]
 * (both fully reconstructable from per-round facts). Finished-game outcomes ARE stored, in
 * [GameResultEntity], for a leaderboard that never replays rounds.
 */

@Entity(tableName = "players", indices = [Index(value = ["name"], unique = true)])
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

const val STATUS_ACTIVE = "ACTIVE"
const val STATUS_FINISHED = "FINISHED"
const val STATUS_CANCELLED = "CANCELLED"

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variantId: String,
    val scoringMode: String,          // ScoringMode.key
    val challengeElimination: Boolean,
    val target: Int,
    val jointWinnerSplit: String,     // JointWinnerSplit.name
    val status: String,               // STATUS_*
    val createdAt: Long,
    val finishedAt: Long?,
    val resultsSeen: Boolean,
    // Live-assist state (mutable, not part of scoring replay)
    val directionClockwise: Boolean,
    val turnEntityId: Long?,
    val activeColor: String?,         // suit color key, or null
)

@Entity(
    tableName = "game_entities",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("gameId")],
)
data class GameEntityRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val name: String,                 // player name, or team name
    val seatOrder: Int,
    val isTeam: Boolean,
    val handSize: Int,                // live assist (manual +/-)
    val eliminated: Boolean,          // derived cache — rewritten by recompute()
)

@Entity(
    tableName = "game_entity_members",
    foreignKeys = [ForeignKey(
        entity = GameEntityRow::class, parentColumns = ["id"], childColumns = ["entityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("entityId"), Index("gameId")],
)
data class GameEntityMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityId: Long,
    val gameId: Long,
    val playerName: String,           // baked name (no FK to players → deleting a player relabels history)
)

@Entity(
    tableName = "rounds",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("gameId")],
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val roundNumber: Int,
    val createdAt: Long,
    val endedOnSide: String?,         // FlipSide.key, recorded before card entry (Flip)
)

@Entity(
    tableName = "round_entries",
    foreignKeys = [
        ForeignKey(entity = RoundEntity::class, parentColumns = ["id"], childColumns = ["roundId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GameEntityRow::class, parentColumns = ["id"], childColumns = ["entityId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("roundId"), Index("gameId"), Index("entityId")],
)
data class RoundEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val gameId: Long,
    val entityId: Long,
    val roundNumber: Int,
    val isWinner: Boolean,
    val manualTotal: Int?,            // set when entered as a manual total
    val cardsJson: String?,           // set when entered card-by-card (flattened PersistedCard list)
    val knockouts: Int,               // No Mercy KOs this entity caused this round
    val eliminatedThisRound: Boolean, // per-round fact (No Mercy mercy rule) — replay reconstructs the cumulative set
    val delta: Int,                   // computed cache — rewritten by recompute()
)

@Entity(
    tableName = "game_results",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["gameId"], unique = true)],
)
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val variantId: String,
    val finishedAt: Long,
    val winnerEntityId: Long?,
    val winnerName: String?,
    val winReason: String?,           // WinReason.name
)
