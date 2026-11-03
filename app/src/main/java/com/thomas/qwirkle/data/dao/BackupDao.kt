package com.thomas.qwirkle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.thomas.qwirkle.data.backup.BackupData
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore

/** Liest und ersetzt den kompletten Datenbestand (für Sicherung und Wiederherstellung). */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM players ORDER BY id")
    abstract suspend fun allPlayers(): List<Player>

    @Query("SELECT * FROM game_history ORDER BY id")
    abstract suspend fun allGames(): List<GameHistory>

    @Query("SELECT * FROM game_players ORDER BY id")
    abstract suspend fun allGamePlayers(): List<GamePlayer>

    @Query("SELECT * FROM rounds ORDER BY id")
    abstract suspend fun allRounds(): List<Round>

    @Query("SELECT * FROM round_scores ORDER BY id")
    abstract suspend fun allRoundScores(): List<RoundScore>

    @Insert
    abstract suspend fun insertPlayers(items: List<Player>)

    @Insert
    abstract suspend fun insertGames(items: List<GameHistory>)

    @Insert
    abstract suspend fun insertGamePlayers(items: List<GamePlayer>)

    @Insert
    abstract suspend fun insertRounds(items: List<Round>)

    @Insert
    abstract suspend fun insertRoundScores(items: List<RoundScore>)

    @Query("DELETE FROM round_scores")
    abstract suspend fun clearRoundScores()

    @Query("DELETE FROM rounds")
    abstract suspend fun clearRounds()

    @Query("DELETE FROM game_players")
    abstract suspend fun clearGamePlayers()

    @Query("DELETE FROM game_history")
    abstract suspend fun clearGames()

    @Query("DELETE FROM players")
    abstract suspend fun clearPlayers()

    /** Konsistenter Schnappschuss aller Tabellen. */
    @Transaction
    open suspend fun snapshot(): BackupData = BackupData(
        players = allPlayers(),
        games = allGames(),
        gamePlayers = allGamePlayers(),
        rounds = allRounds(),
        roundScores = allRoundScores()
    )

    /**
     * Ersetzt ALLE Daten durch data. Alles läuft in einer Transaktion: Schlägt
     * irgendein Schritt fehl (z. B. ungültiger Verweis), wird nichts geändert.
     * Löschen: von den abhängigen zu den Haupttabellen, Einfügen: umgekehrt.
     */
    @Transaction
    open suspend fun replaceAll(data: BackupData) {
        clearRoundScores()
        clearRounds()
        clearGamePlayers()
        clearGames()
        clearPlayers()

        insertPlayers(data.players)
        insertGames(data.games)
        insertGamePlayers(data.gamePlayers)
        insertRounds(data.rounds)
        insertRoundScores(data.roundScores)
    }
}
