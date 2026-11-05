package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GamePlayerDao {

    @Insert
    protected abstract suspend fun insertGame(game: GameHistory): Long

    @Insert
    protected abstract suspend fun insertGamePlayers(entries: List<GamePlayer>)

    /** Legt Spiel und Teilnehmer atomar an. @return gameId */
    @Transaction
    open suspend fun startGame(game: GameHistory, playerIds: List<Long>): Long {
        val gameId = insertGame(game)
        insertGamePlayers(
            playerIds.mapIndexed { index, playerId ->
                GamePlayer(gameId = gameId, playerId = playerId, position = index)
            }
        )
        return gameId
    }

    // Auch deaktivierte Spieler: sie gehören weiterhin zu diesem Spiel
    @Query(
        "SELECT p.* FROM players p INNER JOIN game_players gp ON gp.playerId = p.id " +
            "WHERE gp.gameId = :gameId ORDER BY gp.position"
    )
    abstract fun getPlayersOfGame(gameId: Long): Flow<List<Player>>

    // Teilnehmer aller gespeicherten Echtspiele (für die Spielerstatistik)
    @Query(
        "SELECT gp.* FROM game_players gp INNER JOIN game_history g ON g.id = gp.gameId " +
            "WHERE g.isFinished = 1 AND g.isTestGame = 0"
    )
    abstract fun getParticipantsOfFinishedGames(): Flow<List<GamePlayer>>

    @Query(
        "SELECT p.* FROM players p INNER JOIN game_players gp ON gp.playerId = p.id " +
            "WHERE gp.gameId = :gameId ORDER BY gp.position"
    )
    abstract suspend fun getPlayersOfGameOnce(gameId: Long): List<Player>

    @Query("UPDATE game_players SET position = :position WHERE gameId = :gameId AND playerId = :playerId")
    abstract suspend fun updatePosition(gameId: Long, playerId: Long, position: Int)

    /** Speichert eine neue Spielreihenfolge (Liste der Spieler-IDs in der neuen Reihenfolge). */
    @Transaction
    open suspend fun reorder(gameId: Long, playerIds: List<Long>) {
        playerIds.forEachIndexed { index, id -> updatePosition(gameId, id, index) }
    }
}
