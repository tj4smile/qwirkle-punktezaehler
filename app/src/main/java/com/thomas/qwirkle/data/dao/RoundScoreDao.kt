package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.thomas.qwirkle.data.entity.RoundScore
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundScoreDao {

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    fun getScores(roundId: Long): Flow<List<RoundScore>>

    @Query(
        "SELECT rs.* FROM round_scores rs " +
            "INNER JOIN rounds r ON rs.roundId = r.id WHERE r.gameId = :gameId"
    )
    suspend fun getScoresForGame(gameId: Long): List<RoundScore>
}
