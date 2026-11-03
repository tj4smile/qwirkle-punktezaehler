package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import com.thomas.qwirkle.data.model.RoundWithScores
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoundDao {

    @Insert
    protected abstract suspend fun insertRound(round: Round): Long

    @Insert
    protected abstract suspend fun insertScores(scores: List<RoundScore>)

    @Query("SELECT COALESCE(MAX(roundNumber), 0) FROM rounds WHERE gameId = :gameId")
    protected abstract suspend fun lastRoundNumber(gameId: Long): Int

    /**
     * Speichert Runde + alle Spielerpunkte atomar.
     * Die roundId in den übergebenen Scores wird ersetzt (kann 0 sein).
     * @return Rundennummer der neuen Runde
     */
    @Transaction
    open suspend fun saveRound(gameId: Long, scores: List<RoundScore>): Int {
        val number = lastRoundNumber(gameId) + 1
        val roundId = insertRound(Round(gameId = gameId, roundNumber = number))
        insertScores(scores.map { it.copy(roundId = roundId) })
        return number
    }

    @Query("SELECT * FROM rounds WHERE id = :roundId")
    abstract suspend fun getRound(roundId: Long): Round?

    @Transaction
    @Query("SELECT * FROM rounds WHERE id = :roundId")
    abstract suspend fun getRoundWithScoresOnce(roundId: Long): RoundWithScores?

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    protected abstract suspend fun scoresOfRound(roundId: Long): List<RoundScore>

    @Update
    protected abstract suspend fun updateScores(scores: List<RoundScore>)

    /**
     * Ändert Punkte und Qwirkles der bestehenden Spielerzeilen einer Runde atomar.
     * Die Runde behält ihre Nummer und ihre Zeilen (IDs). Gehört ein Spieler nicht zur
     * Runde, wird nichts geändert und IllegalArgumentException geworfen.
     */
    @Transaction
    open suspend fun updateRoundScores(roundId: Long, newScores: List<RoundScore>) {
        val existing = scoresOfRound(roundId).associateBy { it.playerId }
        val updated = newScores.map { new ->
            val old = existing[new.playerId]
                ?: throw IllegalArgumentException("Spieler ${new.playerId} gehört nicht zu Runde $roundId")
            old.copy(points = new.points, qwirkles = new.qwirkles)
        }
        updateScores(updated)
    }

    @Query("DELETE FROM rounds WHERE id = :roundId")
    abstract suspend fun deleteRound(roundId: Long)

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber")
    abstract fun getRounds(gameId: Long): Flow<List<Round>>

    @Transaction
    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber")
    abstract fun getRoundsWithScores(gameId: Long): Flow<List<RoundWithScores>>
}
