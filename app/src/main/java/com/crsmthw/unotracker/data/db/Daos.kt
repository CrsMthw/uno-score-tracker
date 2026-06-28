package com.crsmthw.unotracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(player: PlayerEntity): Long

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = '$STATUS_ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveGame(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = '$STATUS_ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveGame(): GameEntity?

    @Query("SELECT * FROM games WHERE status = '$STATUS_FINISHED' ORDER BY finishedAt DESC")
    fun observeFinishedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = '$STATUS_FINISHED' AND resultsSeen = 0 ORDER BY finishedAt DESC LIMIT 1")
    fun observePendingResults(): Flow<GameEntity?>

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM games WHERE status = '$STATUS_FINISHED'")
    suspend fun deleteAllFinished()
}

@Dao
interface GameEntityDao {
    @Insert
    suspend fun insert(entity: GameEntityRow): Long

    @Update
    suspend fun update(entity: GameEntityRow)

    @Query("SELECT * FROM game_entities WHERE gameId = :gameId ORDER BY seatOrder ASC")
    fun observeByGame(gameId: Long): Flow<List<GameEntityRow>>

    @Query("SELECT * FROM game_entities WHERE gameId = :gameId ORDER BY seatOrder ASC")
    suspend fun getByGame(gameId: Long): List<GameEntityRow>

    @Query("UPDATE game_entities SET handSize = :handSize WHERE id = :entityId")
    suspend fun updateHandSize(entityId: Long, handSize: Int)

    @Query("UPDATE game_entities SET eliminated = :eliminated WHERE id = :entityId")
    suspend fun updateEliminated(entityId: Long, eliminated: Boolean)
}

@Dao
interface MemberDao {
    @Insert
    suspend fun insert(member: GameEntityMember): Long

    @Query("SELECT * FROM game_entity_members WHERE gameId = :gameId")
    suspend fun getByGame(gameId: Long): List<GameEntityMember>

    @Query("SELECT * FROM game_entity_members")
    suspend fun getAll(): List<GameEntityMember>
}

@Dao
interface RoundDao {
    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun observeByGame(gameId: Long): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getByGame(gameId: Long): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber DESC LIMIT 1")
    suspend fun getLastRound(gameId: Long): RoundEntity?

    @Query("UPDATE rounds SET endedOnSide = :side WHERE id = :roundId")
    suspend fun updateSide(roundId: Long, side: String?)

    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteById(roundId: Long)
}

@Dao
interface RoundEntryDao {
    @Insert
    suspend fun insert(entry: RoundEntryEntity): Long

    @Update
    suspend fun update(entry: RoundEntryEntity)

    @Query("SELECT * FROM round_entries WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun observeByGame(gameId: Long): Flow<List<RoundEntryEntity>>

    @Query("SELECT * FROM round_entries WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getByGame(gameId: Long): List<RoundEntryEntity>

    @Query("SELECT * FROM round_entries WHERE roundId = :roundId")
    suspend fun getByRound(roundId: Long): List<RoundEntryEntity>

    @Query("UPDATE round_entries SET delta = :delta WHERE id = :entryId")
    suspend fun updateDelta(entryId: Long, delta: Int)

    @Query("DELETE FROM round_entries WHERE roundId = :roundId")
    suspend fun deleteByRound(roundId: Long)
}

@Dao
interface GameResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: GameResultEntity): Long

    @Query("SELECT * FROM game_results WHERE gameId = :gameId")
    suspend fun getByGame(gameId: Long): GameResultEntity?

    @Query("SELECT * FROM game_results")
    suspend fun getAll(): List<GameResultEntity>

    @Query("DELETE FROM game_results WHERE gameId = :gameId")
    suspend fun deleteByGame(gameId: Long)
}
