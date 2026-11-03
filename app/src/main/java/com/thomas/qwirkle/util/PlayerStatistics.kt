package com.thomas.qwirkle.util

import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.GameWithRounds

/** Auswertung eines Spielers über alle gespeicherten Partien. */
data class PlayerStats(
    val player: Player,
    val games: Int,
    val wins: Int,
    val totalPoints: Int,
    val bestPoints: Int,
    val totalQwirkles: Int
) {
    /** Anteil gewonnener Partien, 0.0 bis 1.0. */
    val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
    val averagePoints: Double get() = if (games == 0) 0.0 else totalPoints.toDouble() / games
    val averageQwirkles: Double get() = if (games == 0) 0.0 else totalQwirkles.toDouble() / games
}

/**
 * Berechnet die Spielerstatistik rein aus den Daten (keine Datenbank, gut testbar).
 *
 * Regeln:
 *  - Es zählen nur die übergebenen Partien (die Datenbankabfrage liefert nur gespeicherte
 *    Nicht-Testspiele). Partien ohne Runden werden übersprungen.
 *  - Punkte einer Partie = alle Rundenpunkte inkl. Qwirkle-Boni plus Endspielbonus.
 *  - Sieg = Platz 1. Bei Punktgleichstand an der Spitze zählt der Sieg für alle Beteiligten.
 *  - Auch deaktivierte ("gelöschte") Spieler nehmen am Ranking ihrer alten Partien teil,
 *    erscheinen aber selbst nicht in der Ausgabe.
 *  - Sortierung: Siege, dann Siegquote, dann Ø Punkte (jeweils absteigend), dann Name.
 */
object PlayerStatistics {

    fun compute(
        players: List<Player>,
        games: List<GameWithRounds>,
        participants: List<GamePlayer>
    ): List<PlayerStats> {
        val playersById = players.associateBy { it.id }
        val participantsByGame = participants.groupBy { it.gameId }

        class Acc(val player: Player) {
            var games = 0
            var wins = 0
            var totalPoints = 0
            var bestPoints = 0
            var totalQwirkles = 0
        }

        val accs = mutableMapOf<Long, Acc>()

        for (entry in games) {
            if (entry.rounds.isEmpty()) continue
            val gamePlayers = participantsByGame[entry.game.id].orEmpty()
                .sortedBy { it.position }
                .mapNotNull { playersById[it.playerId] }
            if (gamePlayers.isEmpty()) continue

            val totals = GameCalculations.playerTotals(
                gamePlayers, entry.rounds, entry.game.endBonusPlayerId
            )
            for ((rank, total) in GameCalculations.ranking(totals)) {
                val acc = accs.getOrPut(total.player.id) { Acc(total.player) }
                acc.games += 1
                if (rank == 1) acc.wins += 1
                acc.totalPoints += total.points
                if (total.points > acc.bestPoints) acc.bestPoints = total.points
                acc.totalQwirkles += total.qwirkles
            }
        }

        return accs.values
            .filter { it.player.isActive && it.games > 0 }
            .map {
                PlayerStats(
                    player = it.player,
                    games = it.games,
                    wins = it.wins,
                    totalPoints = it.totalPoints,
                    bestPoints = it.bestPoints,
                    totalQwirkles = it.totalQwirkles
                )
            }
            .sortedWith(
                compareByDescending<PlayerStats> { it.wins }
                    .thenByDescending { it.winRate }
                    .thenByDescending { it.averagePoints }
                    .thenBy { it.player.name.lowercase() }
            )
    }
}
