package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.model.GameWithRounds
import com.thomas.qwirkle.data.model.StatisticsSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface GameHistoryDao {

    @Insert
    suspend fun insert(game: GameHistory): Long

    @Update
    suspend fun update(game: GameHistory)

    @Query("DELETE FROM game_history WHERE id = :gameId")
    suspend fun deleteById(gameId: Long)

    // Aufräumen: abgebrochene Spiele, die nie gespeichert wurden
    @Query("DELETE FROM game_history WHERE isFinished = 0")
    suspend fun deleteUnfinished()

    // Laufendes (noch nicht gespeichertes) Spiel, für "Spiel fortsetzen"
    @Query("SELECT * FROM game_history WHERE isFinished = 0 ORDER BY date DESC LIMIT 1")
    fun getUnfinishedGame(): Flow<GameHistory?>

    @Query("SELECT * FROM game_history WHERE id = :gameId")
    suspend fun getGame(gameId: Long): GameHistory?

    // Historie: nur beendete Echtspiele
    @Query("SELECT * FROM game_history WHERE isFinished = 1 AND isTestGame = 0 ORDER BY date DESC")
    fun getFinishedGames(): Flow<List<GameHistory>>

    @Query(
        "SELECT * FROM game_history WHERE isFinished = 1 AND isTestGame = 0 " +
            "ORDER BY date DESC LIMIT 1"
    )
    fun getLastFinishedGame(): Flow<GameHistory?>

    @Query("SELECT COUNT(*) FROM game_history WHERE isFinished = 1 AND isTestGame = 0")
    fun gameCount(): Flow<Int>

    // Für die Spielerstatistik: alle gespeicherten Echtspiele samt Runden und Punkten
    @Transaction
    @Query("SELECT * FROM game_history WHERE isFinished = 1 AND isTestGame = 0")
    fun getFinishedGamesWithRounds(): Flow<List<GameWithRounds>>

    @Transaction
    @Query("SELECT * FROM game_history WHERE id = :gameId")
    fun getGameWithRounds(gameId: Long): Flow<GameWithRounds?>

    @Query(
        "SELECT COUNT(*) AS totalGames, " +
            "COALESCE(SUM(totalPoints), 0) AS totalPoints, " +
            "COALESCE(SUM(totalQwirkles), 0) AS totalQwirkles, " +
            "COALESCE(AVG(totalPoints), 0.0) AS averagePoints, " +
            "COALESCE(AVG(totalQwirkles), 0.0) AS averageQwirkles, " +
            "COALESCE(MAX(totalPoints), 0) AS maxPoints, " +
            "COALESCE(MAX(totalQwirkles), 0) AS maxQwirkles " +
            "FROM game_history WHERE isFinished = 1 AND isTestGame = 0"
    )
    fun getStatistics(): Flow<StatisticsSummary>
}
