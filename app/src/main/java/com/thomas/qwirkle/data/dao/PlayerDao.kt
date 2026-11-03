package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.thomas.qwirkle.data.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun getActivePlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE isActive = 1")
    suspend fun getActivePlayersOnce(): List<Player>

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Long>): List<Player>

    // Auch deaktivierte Spieler, für Spielverlauf und Statistik
    @Query("SELECT * FROM players")
    fun getAllPlayersIncludingInactive(): Flow<List<Player>>

    @Query(
        "SELECT COUNT(*) FROM players " +
            "WHERE isActive = 1 AND name = :name COLLATE NOCASE AND id != :excludeId"
    )
    suspend fun countActiveByName(name: String, excludeId: Long): Int

    @Insert
    suspend fun insert(player: Player): Long

    @Update
    suspend fun update(player: Player)

    // "Löschen" = deaktivieren, damit alte Spiele erhalten bleiben
    @Query("UPDATE players SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
