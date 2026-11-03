package com.thomas.qwirkle.data.repository

import com.thomas.qwirkle.data.dao.GameHistoryDao
import com.thomas.qwirkle.data.dao.GamePlayerDao
import com.thomas.qwirkle.data.dao.PlayerDao
import com.thomas.qwirkle.data.dao.RoundDao
import com.thomas.qwirkle.data.dao.RoundScoreDao
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.RoundScore
import com.thomas.qwirkle.data.model.GameWithRounds
import com.thomas.qwirkle.data.model.RoundWithScores
import com.thomas.qwirkle.data.model.ScoreEntry
import com.thomas.qwirkle.data.model.StatisticsSummary
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.util.NameValidation
import com.thomas.qwirkle.util.PlayerColors
import com.thomas.qwirkle.util.Validation
import com.thomas.qwirkle.util.PlayerStatistics
import com.thomas.qwirkle.util.PlayerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GameRepository(
    private val playerDao: PlayerDao,
    private val gameDao: GameHistoryDao,
    private val gamePlayerDao: GamePlayerDao,
    private val roundDao: RoundDao,
    private val scoreDao: RoundScoreDao
) {

    // ---------- Spieler ----------

    fun getPlayers(): Flow<List<Player>> = playerDao.getActivePlayers()

    fun getAllPlayersIncludingInactive(): Flow<List<Player>> =
        playerDao.getAllPlayersIncludingInactive()

    suspend fun getPlayersByIds(ids: List<Long>): List<Player> =
        playerDao.getPlayersByIds(ids)

    suspend fun validateName(rawName: String, excludeId: Long = 0): NameValidation {
        val name = rawName.trim()
        val format = Validation.checkFormat(name)
        if (format != NameValidation.VALID) return format
        return if (playerDao.countActiveByName(name, excludeId) > 0) {
            NameValidation.DUPLICATE
        } else {
            NameValidation.VALID
        }
    }

    /** Legt den Spieler an, wenn der Name gültig ist. Farbe wird automatisch vergeben. */
    suspend fun addPlayer(rawName: String): NameValidation {
        val result = validateName(rawName)
        if (result != NameValidation.VALID) return result
        val used = playerDao.getActivePlayersOnce().map { it.colorIndex }.toSet()
        playerDao.insert(
            Player(name = rawName.trim(), colorIndex = PlayerColors.nextFreeIndex(used))
        )
        return NameValidation.VALID
    }

    suspend fun updatePlayer(player: Player, newName: String): NameValidation {
        val result = validateName(newName, excludeId = player.id)
        if (result != NameValidation.VALID) return result
        playerDao.update(player.copy(name = newName.trim()))
        return NameValidation.VALID
    }

    suspend fun changePlayerColor(player: Player, colorIndex: Int) =
        playerDao.update(player.copy(colorIndex = colorIndex))

    suspend fun deletePlayer(playerId: Long) = playerDao.deactivate(playerId)

    // ---------- Spiel ----------

    /**
     * Legt ein neues, noch nicht beendetes Spiel samt Teilnehmern an.
     * Die Reihenfolge von playerIds ist die Spielreihenfolge.
     * @return gameId
     */
    suspend fun startGame(playerIds: List<Long>, isTestGame: Boolean): Long {
        require(playerIds.size in GameRules.MIN_PLAYERS..GameRules.MAX_PLAYERS) {
            "Ungültige Spieleranzahl: ${playerIds.size}"
        }
        // Es gibt immer nur ein laufendes Spiel: ein altes, nicht beendetes wird ersetzt
        gameDao.deleteUnfinished()
        return gamePlayerDao.startGame(
            GameHistory(
                date = System.currentTimeMillis(),
                playerCount = playerIds.size,
                isTestGame = isTestGame
            ),
            playerIds
        )
    }

    /** Teilnehmer eines Spiels in Spielreihenfolge (auch später deaktivierte Spieler). */
    fun getGamePlayers(gameId: Long): Flow<List<Player>> =
        gamePlayerDao.getPlayersOfGame(gameId)

    suspend fun getGamePlayersOnce(gameId: Long): List<Player> =
        gamePlayerDao.getPlayersOfGameOnce(gameId)

    suspend fun saveRound(gameId: Long, entries: List<ScoreEntry>): Int =
        roundDao.saveRound(
            gameId,
            entries.map {
                RoundScore(roundId = 0, playerId = it.playerId, points = it.points, qwirkles = it.qwirkles)
            }
        )

    suspend fun deleteRound(roundId: Long) = roundDao.deleteRound(roundId)

    suspend fun getRoundWithScoresOnce(roundId: Long): RoundWithScores? =
        roundDao.getRoundWithScoresOnce(roundId)

    /**
     * Ändert Punkte und Qwirkles einer bereits gespeicherten Runde (nur in einem laufenden Spiel).
     * @throws IllegalArgumentException bei ungültigen Werten, unbekannter Runde oder Spieler
     * @throws IllegalStateException wenn das Spiel schon gespeichert (abgeschlossen) ist
     */
    suspend fun updateRound(roundId: Long, entries: List<ScoreEntry>) {
        require(entries.isNotEmpty()) { "Keine Einträge" }
        require(entries.all { it.points >= 0 && it.qwirkles >= 0 }) { "Negative Werte" }
        val round = roundDao.getRound(roundId)
            ?: throw IllegalArgumentException("Runde $roundId existiert nicht")
        val game = gameDao.getGame(round.gameId)
        check(game != null && !game.isFinished) { "Nur laufende Spiele können bearbeitet werden" }
        roundDao.updateRoundScores(
            roundId,
            entries.map {
                RoundScore(roundId = roundId, playerId = it.playerId, points = it.points, qwirkles = it.qwirkles)
            }
        )
    }

    fun getRoundsWithScores(gameId: Long): Flow<List<RoundWithScores>> =
        roundDao.getRoundsWithScores(gameId)

    fun getGameWithRounds(gameId: Long): Flow<GameWithRounds?> =
        gameDao.getGameWithRounds(gameId)

    /**
     * Schließt das Spiel ab ("Spiel speichern"): berechnet Gesamtpunkte
     * (inkl. Qwirkle-Boni und Endspielbonus) und Gesamt-Qwirkles.
     */
    suspend fun finishGame(gameId: Long, endBonusPlayerId: Long?) {
        val game = gameDao.getGame(gameId) ?: return
        val scores = scoreDao.getScoresForGame(gameId)
        val endBonus = if (endBonusPlayerId != null) GameRules.END_BONUS else 0
        gameDao.update(
            game.copy(
                totalPoints = scores.sumOf { GameRules.roundTotal(it.points, it.qwirkles) } + endBonus,
                totalQwirkles = scores.sumOf { it.qwirkles },
                endBonusPlayerId = endBonusPlayerId,
                isFinished = true
            )
        )
    }

    /** Verwirft ein Spiel (inkl. Runden und Teilnehmer per CASCADE). */
    suspend fun discardGame(gameId: Long) = gameDao.deleteById(gameId)

    suspend fun cleanUpUnfinishedGames() = gameDao.deleteUnfinished()

    // ---------- Laufendes Spiel / Spielende ----------

    fun getUnfinishedGame(): Flow<GameHistory?> = gameDao.getUnfinishedGame()

    suspend fun getGame(gameId: Long): GameHistory? = gameDao.getGame(gameId)

    /** Merkt sich, wer den Endspielbonus bekommt (null = niemand). Beendet das Spiel noch nicht. */
    suspend fun setEndBonusPlayer(gameId: Long, playerId: Long?) {
        val game = gameDao.getGame(gameId) ?: return
        gameDao.update(game.copy(endBonusPlayerId = playerId))
    }


    // ---------- Historie & Statistik ----------

    fun getFinishedGames(): Flow<List<GameHistory>> = gameDao.getFinishedGames()
    fun getLastFinishedGame(): Flow<GameHistory?> = gameDao.getLastFinishedGame()
    fun gameCount(): Flow<Int> = gameDao.gameCount()
    fun getStatistics(): Flow<StatisticsSummary> = gameDao.getStatistics()

    /** Statistik pro (aktivem) Spieler, nur gespeicherte Nicht-Testspiele. */
    fun getPlayerStatistics(): Flow<List<PlayerStats>> = combine(
        playerDao.getAllPlayersIncludingInactive(),
        gameDao.getFinishedGamesWithRounds(),
        gamePlayerDao.getParticipantsOfFinishedGames()
    ) { players, games, participants ->
        PlayerStatistics.compute(players, games, participants)
    }.flowOn(Dispatchers.Default)
    suspend fun deleteGame(gameId: Long) = gameDao.deleteById(gameId)
}
