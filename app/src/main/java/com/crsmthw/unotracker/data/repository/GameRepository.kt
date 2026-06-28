package com.crsmthw.unotracker.data.repository

import com.crsmthw.unotracker.data.config.VariantCatalog
import com.crsmthw.unotracker.data.db.GameEntity
import com.crsmthw.unotracker.data.db.GameEntityMember
import com.crsmthw.unotracker.data.db.GameEntityRow
import com.crsmthw.unotracker.data.db.GameResultEntity
import com.crsmthw.unotracker.data.db.RoundEntity
import com.crsmthw.unotracker.data.db.RoundEntryEntity
import com.crsmthw.unotracker.data.db.STATUS_ACTIVE
import com.crsmthw.unotracker.data.db.STATUS_CANCELLED
import com.crsmthw.unotracker.data.db.STATUS_FINISHED
import com.crsmthw.unotracker.data.db.UnoDatabase
import com.crsmthw.unotracker.data.model.EntityStanding
import com.crsmthw.unotracker.data.model.FinishedGameSummary
import com.crsmthw.unotracker.data.model.GameState
import com.crsmthw.unotracker.data.model.HandCardJson
import com.crsmthw.unotracker.data.model.LeaderboardEntry
import com.crsmthw.unotracker.data.model.NewEntity
import com.crsmthw.unotracker.data.model.RoundDraft
import com.crsmthw.unotracker.data.model.RoundEntrySummary
import com.crsmthw.unotracker.data.model.RoundSummary
import com.crsmthw.unotracker.data.model.tallyFrom
import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.JointWinnerSplit
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.scoring.RoundInput
import com.crsmthw.unotracker.domain.scoring.ScoringEngine
import com.crsmthw.unotracker.domain.scoring.WinReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * All game logic. The spine is [recompute]: every add / edit / undo writes raw round facts, then a
 * full replay through [ScoringEngine] rewrites every round's delta cache, the cumulative elimination
 * flags, and the finished/active status. Nothing denormalises an in-progress cumulative score.
 */
class GameRepository(
    private val db: UnoDatabase,
    private val catalog: VariantCatalog,
) {
    private val gameDao = db.gameDao()
    private val entityDao = db.gameEntityDao()
    private val memberDao = db.memberDao()
    private val roundDao = db.roundDao()
    private val entryDao = db.roundEntryDao()
    private val resultDao = db.gameResultDao()
    private val playerDao = db.playerDao()

    // ── Roster ─────────────────────────────────────────────────────────────────────────────────
    fun observePlayers() = playerDao.observeAll()

    suspend fun addPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) playerDao.insert(playerEntity(trimmed))
    }

    suspend fun deletePlayer(id: Long) = playerDao.deleteById(id)

    private fun playerEntity(name: String) =
        com.crsmthw.unotracker.data.db.PlayerEntity(name = name, createdAt = now())

    // ── Game lifecycle ───────────────────────────────────────────────────────────────────────────
    suspend fun createGame(
        variantId: String,
        scoringMode: ScoringMode,
        challengeElimination: Boolean,
        target: Int,
        jointWinnerSplit: JointWinnerSplit,
        entities: List<NewEntity>,
    ): Long {
        val gameId = gameDao.insert(
            GameEntity(
                variantId = variantId,
                scoringMode = scoringMode.key,
                challengeElimination = challengeElimination,
                target = target,
                jointWinnerSplit = jointWinnerSplit.name,
                status = STATUS_ACTIVE,
                createdAt = now(),
                finishedAt = null,
                resultsSeen = false,
                directionClockwise = true,
                turnEntityId = null,
                activeColor = null,
            ),
        )
        var firstEntityId: Long? = null
        entities.forEachIndexed { index, entity ->
            val entityId = entityDao.insert(
                GameEntityRow(
                    gameId = gameId, name = entity.name, seatOrder = index,
                    isTeam = entity.isTeam, handSize = 7, eliminated = false,
                ),
            )
            if (firstEntityId == null) firstEntityId = entityId
            entity.memberNames.forEach { member ->
                val name = member.trim()
                if (name.isNotEmpty()) {
                    playerDao.insert(playerEntity(name)) // ensure roster has them (IGNORE on dup)
                    memberDao.insert(GameEntityMember(entityId = entityId, gameId = gameId, playerName = name))
                }
            }
        }
        // Seat the turn pointer on the first player.
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(turnEntityId = firstEntityId)) }
        return gameId
    }

    suspend fun endGameManually(gameId: Long) {
        // If no one has scored a single point, the game is abandoned — cancel it (no winner, no
        // leaderboard record, excluded from history), like Phase 10. Otherwise finish with the leader.
        val anyScore = entryDao.getByGame(gameId).any { it.delta != 0 }
        if (anyScore) recompute(gameId, forceFinish = true) else cancelGame(gameId)
    }

    suspend fun cancelGame(gameId: Long) {
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(status = STATUS_CANCELLED, finishedAt = now())) }
    }

    suspend fun deleteGame(gameId: Long) = gameDao.deleteById(gameId)

    /** Cascade clears entities/rounds/entries/results for every finished game. */
    suspend fun deleteAllFinished() = gameDao.deleteAllFinished()

    suspend fun markResultsSeen(gameId: Long) {
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(resultsSeen = true)) }
    }

    // ── Live-assist state (no scoring impact) ──────────────────────────────────────────────────
    suspend fun setDirection(gameId: Long, clockwise: Boolean) {
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(directionClockwise = clockwise)) }
    }

    suspend fun setTurn(gameId: Long, entityId: Long?) {
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(turnEntityId = entityId)) }
    }

    suspend fun setActiveColor(gameId: Long, color: String?) {
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(activeColor = color)) }
    }

    suspend fun setHandSize(entityId: Long, handSize: Int) =
        entityDao.updateHandSize(entityId, handSize.coerceAtLeast(0))

    // ── Rounds ───────────────────────────────────────────────────────────────────────────────────
    suspend fun addRound(gameId: Long, draft: RoundDraft) {
        val nextNumber = (roundDao.getLastRound(gameId)?.roundNumber ?: 0) + 1
        val roundId = roundDao.insert(
            RoundEntity(gameId = gameId, roundNumber = nextNumber, createdAt = now(), endedOnSide = draft.endedOnSide?.key),
        )
        writeRoundEntries(gameId, roundId, nextNumber, draft)
        recompute(gameId)
    }

    suspend fun editRound(gameId: Long, roundId: Long, draft: RoundDraft) {
        val round = roundDao.getByGame(gameId).firstOrNull { it.id == roundId } ?: return
        entryDao.deleteByRound(roundId)
        roundDao.updateSide(roundId, draft.endedOnSide?.key)
        writeRoundEntries(gameId, roundId, round.roundNumber, draft)
        recompute(gameId)
    }

    suspend fun undoLastRound(gameId: Long) {
        roundDao.getLastRound(gameId)?.let { roundDao.deleteById(it.id) }
        recompute(gameId)
    }

    private suspend fun writeRoundEntries(gameId: Long, roundId: Long, roundNumber: Int, draft: RoundDraft) {
        val involved = (draft.winnerEntityIds + draft.hands.keys + draft.eliminatedThisRound).toSet()
        for (entityId in involved) {
            val isWinner = entityId in draft.winnerEntityIds
            val hand = draft.hands[entityId]
            entryDao.insert(
                RoundEntryEntity(
                    roundId = roundId,
                    gameId = gameId,
                    entityId = entityId,
                    roundNumber = roundNumber,
                    isWinner = isWinner,
                    manualTotal = if (!isWinner) hand?.manualTotal else null,
                    cardsJson = if (!isWinner && hand?.cards?.isNotEmpty() == true) HandCardJson.encode(hand.cards) else null,
                    knockouts = draft.knockouts[entityId] ?: 0,
                    eliminatedThisRound = entityId in draft.eliminatedThisRound,
                    delta = 0,
                ),
            )
        }
    }

    /**
     * Replay spine. Re-folds every round through the engine in order, rewriting each entry's delta and
     * each entity's cumulative elimination flag, then decides finished/active status and the result row.
     */
    suspend fun recompute(gameId: Long, forceFinish: Boolean = false) {
        val game = gameDao.getById(gameId) ?: return
        val variant = catalog.byId(game.variantId) ?: return
        val mode = ScoringMode.fromKey(game.scoringMode)
        val split = runCatching { JointWinnerSplit.valueOf(game.jointWinnerSplit) }.getOrDefault(JointWinnerSplit.FULL_TO_EACH)
        val engine = ScoringEngine(split)

        val entities = entityDao.getByGame(gameId)
        val allIds = entities.map { it.id.toString() }.toSet()
        val rounds = roundDao.getByGame(gameId)
        val allEntries = entryDao.getByGame(gameId)
        val entriesByRound = allEntries.groupBy { it.roundId }

        val totals = HashMap<String, Int>().apply { allIds.forEach { put(it, 0) } }
        val eliminatedFromGame = HashSet<String>()
        var lastEnd: com.crsmthw.unotracker.domain.scoring.GameEndEval? = null

        for (round in rounds) {
            val entries = entriesByRound[round.id].orEmpty()
            val winners = entries.filter { it.isWinner }.map { it.entityId.toString() }
            val eliminatedThisHand = entries.filter { it.eliminatedThisRound }.map { it.entityId.toString() }.toSet()
            val knockouts = entries.filter { it.knockouts > 0 }.associate { it.entityId.toString() to it.knockouts }
            val loserHands = entries
                .filter { !it.isWinner && !it.eliminatedThisRound && it.entityId.toString() !in eliminatedFromGame }
                .mapNotNull { e -> tallyFrom(e.manualTotal, e.cardsJson)?.let { e.entityId.toString() to it } }
                .toMap()

            val input = RoundInput(
                winners = winners,
                loserHands = loserHands,
                endedOnSide = FlipSide.fromKey(round.endedOnSide),
                knockouts = knockouts,
                eliminatedThisHand = eliminatedThisHand,
            )
            val deltas = engine.roundDeltas(variant, mode, input).deltas
            entries.forEach { entry -> entryDao.updateDelta(entry.id, deltas[entry.entityId.toString()] ?: 0) }
            deltas.forEach { (id, d) -> totals[id] = (totals[id] ?: 0) + d }

            eliminatedFromGame += eliminatedThisHand
            lastEnd = engine.evaluateGameEnd(
                variant, mode, game.target, totals, allIds, eliminatedFromGame, game.challengeElimination,
            )
            eliminatedFromGame += lastEnd.newlyEliminated
        }

        // Rewrite derived elimination flags.
        entities.forEach { e ->
            val nowEliminated = e.id.toString() in eliminatedFromGame
            if (e.eliminated != nowEliminated) entityDao.updateEliminated(e.id, nowEliminated)
        }

        val gameOver = forceFinish || (lastEnd?.gameOver == true)
        if (gameOver) {
            val winners = lastEnd?.winners.orEmpty()
                .ifEmpty { listOfNotNull(leaderId(totals, mode, eliminatedFromGame, allIds)) }
            val reason = lastEnd?.reason ?: if (mode == ScoringMode.LOWEST_TOTAL) WinReason.LOWEST_TOTAL else WinReason.TARGET
            val winnerEntityId = winners.firstOrNull()?.toLongOrNull()
            val winnerName = winners.mapNotNull { id -> entities.firstOrNull { it.id.toString() == id }?.name }
                .joinToString(", ").ifEmpty { null }
            resultDao.insert(
                GameResultEntity(
                    gameId = gameId, variantId = game.variantId, finishedAt = game.finishedAt ?: now(),
                    winnerEntityId = winnerEntityId, winnerName = winnerName, winReason = reason.name,
                ),
            )
            if (game.status != STATUS_FINISHED) {
                gameDao.update(game.copy(status = STATUS_FINISHED, finishedAt = game.finishedAt ?: now()))
            }
        } else if (game.status == STATUS_FINISHED) {
            // An edit/undo un-finished the game.
            resultDao.deleteByGame(gameId)
            gameDao.update(game.copy(status = STATUS_ACTIVE, finishedAt = null, resultsSeen = false))
        }
    }

    private fun leaderId(totals: Map<String, Int>, mode: ScoringMode, eliminated: Set<String>, allIds: Set<String>): String? {
        val pool = (allIds - eliminated).ifEmpty { allIds }
        return when (mode) {
            ScoringMode.WINNER_TAKES -> pool.maxByOrNull { totals[it] ?: 0 }
            ScoringMode.LOWEST_TOTAL -> pool.minByOrNull { totals[it] ?: 0 }
        }
    }

    // ── Observation ────────────────────────────────────────────────────────────────────────────
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeActiveGameState(): Flow<GameState?> =
        gameDao.observeActiveGame().flatMapLatest { game ->
            if (game == null) flowOf(null) else observeGameState(game.id)
        }

    /** A finished game whose winner screen was never shown — for the pending-results safety net. */
    fun observePendingResults(): Flow<Long?> = gameDao.observePendingResults().map { it?.id }

    fun observeGameState(gameId: Long): Flow<GameState?> = combine(
        gameDao.observeById(gameId),
        entityDao.observeByGame(gameId),
        entryDao.observeByGame(gameId),
        roundDao.observeByGame(gameId),
    ) { game, entities, entries, rounds ->
        if (game == null) return@combine null
        val variant = catalog.byId(game.variantId) ?: return@combine null
        val members = memberDao.getByGame(gameId).groupBy { it.entityId }
        val result = resultDao.getByGame(gameId)
        val standings = entities.map { e ->
            EntityStanding(
                entityId = e.id, name = e.name, isTeam = e.isTeam,
                memberNames = members[e.id].orEmpty().map { it.playerName },
                seatOrder = e.seatOrder,
                total = entries.filter { it.entityId == e.id }.sumOf { it.delta },
                handSize = e.handSize, eliminated = e.eliminated,
            )
        }
        GameState(
            gameId = game.id, variant = variant, scoringMode = ScoringMode.fromKey(game.scoringMode),
            challengeElimination = game.challengeElimination, target = game.target,
            isFinished = game.status == STATUS_FINISHED, isCancelled = game.status == STATUS_CANCELLED,
            resultsSeen = game.resultsSeen,
            roundCount = rounds.size, standings = standings,
            directionClockwise = game.directionClockwise, turnEntityId = game.turnEntityId, activeColor = game.activeColor,
            winnerEntityIds = listOfNotNull(result?.winnerEntityId),
            winReason = result?.winReason?.let { runCatching { WinReason.valueOf(it) }.getOrNull() },
        )
    }

    suspend fun getRoundSummaries(gameId: Long): List<RoundSummary> {
        val entities = entityDao.getByGame(gameId).associateBy { it.id }
        val rounds = roundDao.getByGame(gameId)
        val entriesByRound = entryDao.getByGame(gameId).groupBy { it.roundId }
        return rounds.map { round ->
            RoundSummary(
                roundId = round.id, roundNumber = round.roundNumber,
                endedOnSide = FlipSide.fromKey(round.endedOnSide),
                entries = entriesByRound[round.id].orEmpty().map { e ->
                    RoundEntrySummary(
                        entryId = e.id, entityId = e.entityId,
                        name = entities[e.entityId]?.name ?: "?",
                        delta = e.delta, isWinner = e.isWinner, eliminatedThisRound = e.eliminatedThisRound,
                        knockouts = e.knockouts, manualTotal = e.manualTotal,
                        cards = HandCardJson.decode(e.cardsJson),
                    )
                },
            )
        }
    }

    // ── History & leaderboard ──────────────────────────────────────────────────────────────────
    fun observeFinishedGames(): Flow<List<FinishedGameSummary>> =
        gameDao.observeFinishedGames().map { games ->
            games.map { game ->
                val entities = entityDao.getByGame(game.id)
                val result = resultDao.getByGame(game.id)
                val rounds = roundDao.getByGame(game.id)
                FinishedGameSummary(
                    gameId = game.id, variantId = game.variantId,
                    variantName = catalog.byId(game.variantId)?.displayName ?: game.variantId,
                    finishedAt = game.finishedAt ?: game.createdAt,
                    winnerName = result?.winnerName,
                    entityNames = entities.map { it.name },
                    roundCount = rounds.size,
                )
            }
        }

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        val results = resultDao.getAll().associateBy { it.gameId }
        val members = memberDao.getAll().groupBy { it.gameId }
        val played = HashMap<String, Int>()
        val won = HashMap<String, Int>()
        for ((gameId, gameMembers) in members) {
            val result = results[gameId] ?: continue // only finished games have a result
            val winnerEntityId = result.winnerEntityId
            gameMembers.forEach { played[it.playerName] = (played[it.playerName] ?: 0) + 1 }
            gameMembers.filter { it.entityId == winnerEntityId }
                .forEach { won[it.playerName] = (won[it.playerName] ?: 0) + 1 }
        }
        return played.keys.map { name ->
            LeaderboardEntry(name, played[name] ?: 0, won[name] ?: 0)
        }.sortedWith(compareByDescending<LeaderboardEntry> { it.winPct }.thenByDescending { it.gamesWon })
    }

    private fun now() = System.currentTimeMillis()
}
