package com.crsmthw.unotracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crsmthw.unotracker.data.db.PlayerEntity
import com.crsmthw.unotracker.data.model.FinishedGameSummary
import com.crsmthw.unotracker.data.model.GameState
import com.crsmthw.unotracker.data.model.HandDraft
import com.crsmthw.unotracker.data.model.LeaderboardEntry
import com.crsmthw.unotracker.data.model.NewEntity
import com.crsmthw.unotracker.data.model.RoundDraft
import com.crsmthw.unotracker.data.model.RoundSummary
import com.crsmthw.unotracker.data.repository.GameRepository
import com.crsmthw.unotracker.di.AppContainer
import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.JointWinnerSplit
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.domain.scoring.HandCard
import com.crsmthw.unotracker.domain.scoring.HandTally
import com.crsmthw.unotracker.domain.scoring.RoundInput
import com.crsmthw.unotracker.domain.scoring.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Home / variant gallery ───────────────────────────────────────────────────────────────────
class HomeViewModel(private val repo: GameRepository, val variants: List<Variant>) : ViewModel() {
    val activeGame: StateFlow<GameState?> =
        repo.observeActiveGameState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** A finished-but-unseen game routes straight to its winner screen (survives an app restart). */
    val pendingResultsGameId: StateFlow<Long?> =
        repo.observePendingResults().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

// ── Game setup ────────────────────────────────────────────────────────────────────────────────
data class SetupUiState(
    val players: List<String> = emptyList(),
    val teamsEnabled: Boolean = false,
    val teamCount: Int = 2,
    val teamAssignment: Map<String, Int> = emptyMap(),  // player -> team index
    val scoringMode: ScoringMode = ScoringMode.WINNER_TAKES,
    val challengeElimination: Boolean = false,
    val targetOverride: Int? = null,
)

class SetupViewModel(
    private val repo: GameRepository,
    val variant: Variant,
) : ViewModel() {

    val roster: StateFlow<List<PlayerEntity>> =
        repo.observePlayers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(SetupUiState())
    val ui: StateFlow<SetupUiState> = _ui.asStateFlow()

    val target: Int get() = _ui.value.targetOverride ?: variant.target

    fun addPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || _ui.value.players.any { it.equals(trimmed, true) }) return
        _ui.update { it.copy(players = it.players + trimmed) }
        viewModelScope.launch { repo.addPlayer(trimmed) }
    }

    fun removePlayer(name: String) =
        _ui.update { it.copy(players = it.players - name, teamAssignment = it.teamAssignment - name) }

    fun setTeamsEnabled(enabled: Boolean) = _ui.update { it.copy(teamsEnabled = enabled) }
    fun setTeamCount(count: Int) = _ui.update { it.copy(teamCount = count.coerceIn(2, 6)) }
    fun assignTeam(player: String, team: Int) =
        _ui.update { it.copy(teamAssignment = it.teamAssignment + (player to team)) }

    fun setScoringMode(mode: ScoringMode) = _ui.update { it.copy(scoringMode = mode) }
    fun setChallengeElimination(on: Boolean) = _ui.update { it.copy(challengeElimination = on) }
    fun setTargetOverride(target: Int?) = _ui.update { it.copy(targetOverride = target) }

    fun canStart(): Boolean {
        val s = _ui.value
        return if (s.teamsEnabled) {
            val grouped = s.players.groupBy { s.teamAssignment[it] }
            grouped.keys.filterNotNull().size >= 2 && grouped.values.all { it.isNotEmpty() }
        } else {
            s.players.size >= 2
        }
    }

    private fun buildEntities(): List<NewEntity> {
        val s = _ui.value
        if (!s.teamsEnabled) return s.players.map { NewEntity(it, false, listOf(it)) }
        val byTeam = s.players.groupBy { s.teamAssignment[it] ?: 0 }
        return byTeam.entries.sortedBy { it.key }.mapIndexed { idx, (_, members) ->
            NewEntity(name = "Team ${('A' + idx)}", isTeam = true, memberNames = members)
        }
    }

    fun start(onCreated: (Long) -> Unit) {
        val s = _ui.value
        viewModelScope.launch {
            val id = repo.createGame(
                variantId = variant.id,
                scoringMode = s.scoringMode,
                challengeElimination = s.challengeElimination,
                target = target,
                jointWinnerSplit = JointWinnerSplit.FULL_TO_EACH,
                entities = buildEntities(),
            )
            onCreated(id)
        }
    }
}

// ── Active game (The Table) ───────────────────────────────────────────────────────────────────
class ActiveGameViewModel(private val repo: GameRepository, val gameId: Long) : ViewModel() {
    val state: StateFlow<GameState?> =
        repo.observeGameState(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleDirection() = viewModelScope.launch {
        state.value?.let { repo.setDirection(gameId, !it.directionClockwise) }
    }

    fun setTurn(entityId: Long) = viewModelScope.launch { repo.setTurn(gameId, entityId) }
    fun setActiveColor(color: String?) = viewModelScope.launch { repo.setActiveColor(gameId, color) }
    fun setHandSize(entityId: Long, size: Int) = viewModelScope.launch { repo.setHandSize(entityId, size) }
    fun endGame() = viewModelScope.launch { repo.endGameManually(gameId) }
    fun cancelGame() = viewModelScope.launch { repo.cancelGame(gameId) }
    fun undoLastRound() = viewModelScope.launch { repo.undoLastRound(gameId) }
    fun markSeen() = viewModelScope.launch { repo.markResultsSeen(gameId) }
}

// ── Round entry ───────────────────────────────────────────────────────────────────────────────
enum class EntryMode { CARDS, MANUAL }

data class LoserEntry(
    val mode: EntryMode = EntryMode.CARDS,
    val manualText: String = "",
    val cards: List<HandCard> = emptyList(),
    val knockouts: Int = 0,
    val eliminated: Boolean = false,
)

data class RoundEntryUiState(
    val gameState: GameState? = null,
    val endedOnSide: FlipSide? = null,
    val winners: Set<Long> = emptySet(),
    val entries: Map<Long, LoserEntry> = emptyMap(),
)

class RoundEntryViewModel(
    private val repo: GameRepository,
    private val gameId: Long,
    private val editRoundId: Long,
) : ViewModel() {

    private val _ui = MutableStateFlow(RoundEntryUiState())
    val ui: StateFlow<RoundEntryUiState> = _ui.asStateFlow()

    val isEditing: Boolean get() = editRoundId > 0

    val gameState: StateFlow<GameState?> =
        repo.observeGameState(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Seed the editable draft once from the first non-null game state.
        viewModelScope.launch {
            gameState.collect { gs ->
                if (gs != null && _ui.value.gameState == null) {
                    // Flip scores on the side the round ended on; default to Light (where play starts).
                    val defaultSide = if (gs.variant.sideDependentScoring) FlipSide.LIGHT else null
                    _ui.update { it.copy(gameState = gs, endedOnSide = defaultSide) }
                    if (isEditing) preloadEdit(gs)
                }
            }
        }
    }

    private suspend fun preloadEdit(gs: GameState) {
        val summary = repo.getRoundSummaries(gameId).firstOrNull { it.roundId == editRoundId } ?: return
        val winners = summary.entries.filter { it.isWinner }.map { it.entityId }.toSet()
        val entries = summary.entries.filter { !it.isWinner }.associate { e ->
            e.entityId to LoserEntry(
                mode = if (e.manualTotal != null) EntryMode.MANUAL else EntryMode.CARDS,
                manualText = e.manualTotal?.toString() ?: "",
                cards = e.cards,
                knockouts = e.knockouts,
                eliminated = e.eliminatedThisRound,
            )
        }
        _ui.update { it.copy(endedOnSide = summary.endedOnSide, winners = winners, entries = entries) }
    }

    private fun entry(id: Long) = _ui.value.entries[id] ?: LoserEntry()
    private fun mutate(id: Long, block: (LoserEntry) -> LoserEntry) =
        _ui.update { it.copy(entries = it.entries + (id to block(entry(id)))) }

    fun toggleWinner(entityId: Long) = _ui.update {
        val winners = if (entityId in it.winners) it.winners - entityId else it.winners + entityId
        it.copy(winners = winners, entries = it.entries - entityId)
    }

    /** Single-winner variants: tapping selects exactly one winner (or clears it if re-tapped). */
    fun setSoleWinner(entityId: Long) = _ui.update {
        val winners = if (it.winners == setOf(entityId)) emptySet() else setOf(entityId)
        it.copy(winners = winners, entries = it.entries - entityId)
    }

    fun setSide(side: FlipSide) = _ui.update { it.copy(endedOnSide = side) }
    fun setMode(id: Long, mode: EntryMode) = mutate(id) { it.copy(mode = mode) }
    fun setManual(id: Long, text: String) = mutate(id) { it.copy(manualText = text.filter(Char::isDigit).take(4)) }
    fun addCard(id: Long, card: HandCard) = mutate(id) { it.copy(cards = it.cards + card) }
    fun removeLastCard(id: Long) = mutate(id) { it.copy(cards = it.cards.dropLast(1)) }
    fun removeCardAt(id: Long, index: Int) = mutate(id) { it.copy(cards = it.cards.filterIndexed { i, _ -> i != index }) }
    fun clearCards(id: Long) = mutate(id) { it.copy(cards = emptyList()) }
    fun setKnockouts(id: Long, n: Int) = mutate(id) { it.copy(knockouts = n.coerceAtLeast(0)) }
    fun toggleEliminated(id: Long) = mutate(id) { it.copy(eliminated = !it.eliminated) }

    /** Active, non-winner entities that still need a hand this round. */
    fun losers(gs: GameState): List<Long> =
        gs.standings.filter { !it.eliminated && it.entityId !in _ui.value.winners }.map { it.entityId }

    private fun handDraft(e: LoserEntry): HandDraft = when (e.mode) {
        EntryMode.MANUAL -> HandDraft(manualTotal = e.manualText.toIntOrNull() ?: 0)
        EntryMode.CARDS -> HandDraft(cards = e.cards)
    }

    fun buildDraft(): RoundDraft {
        val s = _ui.value
        val hands = s.entries
            .filterKeys { it !in s.winners }
            .filterValues { !it.eliminated }
            .mapValues { handDraft(it.value) }
        val knockouts = (s.winners.associateWith { entry(it).knockouts } + s.entries.mapValues { it.value.knockouts })
            .filterValues { it > 0 }
        val eliminated = s.entries.filterValues { it.eliminated }.keys
        return RoundDraft(
            endedOnSide = s.endedOnSide,
            winnerEntityIds = s.winners.toList(),
            hands = hands,
            knockouts = knockouts,
            eliminatedThisRound = eliminated,
        )
    }

    fun canSubmit(): Boolean {
        val gs = _ui.value.gameState ?: return false
        val s = _ui.value
        if (s.winners.isEmpty()) return false
        if (s.winners.size > 1 && !gs.variant.supportsJointWinners) return false
        if (gs.variant.sideDependentScoring && s.endedOnSide == null) return false
        // Every active non-winner must have a hand or be marked eliminated this round.
        return losers(gs).all { id ->
            val e = entry(id)
            e.eliminated || when (e.mode) {
                EntryMode.MANUAL -> e.manualText.isNotEmpty()
                EntryMode.CARDS -> e.cards.isNotEmpty()
            }
        }
    }

    /** Live preview of what each winner gains (Mode A) using the same engine the repository replays. */
    fun previewWinnerGain(): Int {
        val gs = _ui.value.gameState ?: return 0
        if (_ui.value.winners.isEmpty()) return 0
        val engine = ScoringEngine()
        val draft = buildDraft()
        val input = RoundInput(
            winners = draft.winnerEntityIds.map { it.toString() },
            loserHands = draft.hands.mapKeys { it.key.toString() }.mapValues { (_, h) ->
                if (h.manualTotal != null) HandTally.Manual(h.manualTotal) else HandTally.Cards(h.cards)
            },
            endedOnSide = draft.endedOnSide,
            knockouts = draft.knockouts.mapKeys { it.key.toString() },
            eliminatedThisHand = draft.eliminatedThisRound.map { it.toString() }.toSet(),
        )
        val deltas = engine.roundDeltas(gs.variant, gs.scoringMode, input).deltas
        val firstWinner = draft.winnerEntityIds.firstOrNull()?.toString() ?: return 0
        return deltas[firstWinner] ?: 0
    }

    fun submit(onDone: () -> Unit) {
        viewModelScope.launch {
            val draft = buildDraft()
            if (isEditing) repo.editRound(gameId, editRoundId, draft) else repo.addRound(gameId, draft)
            onDone()
        }
    }
}

// ── History / stats VMs ───────────────────────────────────────────────────────────────────────
class RoundHistoryViewModel(private val repo: GameRepository, val gameId: Long) : ViewModel() {
    private val _rounds = MutableStateFlow<List<RoundSummary>>(emptyList())
    val rounds: StateFlow<List<RoundSummary>> = _rounds.asStateFlow()
    val gameState: StateFlow<GameState?> =
        repo.observeGameState(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refresh() = viewModelScope.launch { _rounds.value = repo.getRoundSummaries(gameId) }
    init { refresh() }
}

class PlayerRosterViewModel(private val repo: GameRepository) : ViewModel() {
    val players: StateFlow<List<PlayerEntity>> =
        repo.observePlayers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String) = viewModelScope.launch { repo.addPlayer(name) }
    fun delete(id: Long) = viewModelScope.launch { repo.deletePlayer(id) }
}

class GameDetailViewModel(private val repo: GameRepository, val gameId: Long) : ViewModel() {
    val state: StateFlow<GameState?> =
        repo.observeGameState(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _rounds = MutableStateFlow<List<RoundSummary>>(emptyList())
    val rounds: StateFlow<List<RoundSummary>> = _rounds.asStateFlow()
    init { viewModelScope.launch { _rounds.value = repo.getRoundSummaries(gameId) } }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.deleteGame(gameId); onDone() }
}

class AboutViewModel(private val repo: GameRepository) : ViewModel() {
    fun deleteAllHistory() = viewModelScope.launch { repo.deleteAllFinished() }
}

class StatsViewModel(private val repo: GameRepository) : ViewModel() {
    val finishedGames: StateFlow<List<FinishedGameSummary>> =
        repo.observeFinishedGames().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()
    fun refreshLeaderboard() = viewModelScope.launch { _leaderboard.value = repo.getLeaderboard() }
    init { refreshLeaderboard() }
}

// ── Factory ──────────────────────────────────────────────────────────────────────────────────
class UnoViewModelFactory(
    private val container: AppContainer,
    private val gameId: Long = -1L,
    private val variantId: String = "",
    private val roundId: Long = -1L,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = container.gameRepository
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repo, container.variantCatalog.variants) as T
            modelClass.isAssignableFrom(SetupViewModel::class.java) ->
                SetupViewModel(repo, container.variantCatalog.byId(variantId)!!) as T
            modelClass.isAssignableFrom(ActiveGameViewModel::class.java) ->
                ActiveGameViewModel(repo, gameId) as T
            modelClass.isAssignableFrom(RoundEntryViewModel::class.java) ->
                RoundEntryViewModel(repo, gameId, roundId) as T
            modelClass.isAssignableFrom(RoundHistoryViewModel::class.java) ->
                RoundHistoryViewModel(repo, gameId) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(repo) as T
            modelClass.isAssignableFrom(GameDetailViewModel::class.java) ->
                GameDetailViewModel(repo, gameId) as T
            modelClass.isAssignableFrom(AboutViewModel::class.java) ->
                AboutViewModel(repo) as T
            modelClass.isAssignableFrom(PlayerRosterViewModel::class.java) ->
                PlayerRosterViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
