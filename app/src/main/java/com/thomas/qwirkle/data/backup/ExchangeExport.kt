package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.data.model.RoundWithScores
import com.thomas.qwirkle.util.GameCalculations
import org.json.JSONArray
import org.json.JSONObject

/** Ein Spieler in einer Partie des Austauschformats. */
data class ExchangePlayer(
    val name: String,
    val colorName: String,
    /** Reihenfolge ab 1. */
    val order: Int,
    /** Punkte inklusive Qwirkle-Bonus und Endspielbonus. */
    val finalPoints: Int,
    /** Platz; bei Gleichstand gleicher Platz (1, 1, 3). */
    val rank: Int
)

data class ExchangeGame(
    val externalId: String,
    val startTime: Long,
    val endTime: Long,
    val players: List<ExchangePlayer>
)

/**
 * Austauschformat "punkte-austausch" (formatVersion 1) für die App "Punktezähler".
 * Enthält nur abgeschlossene Partien, keine Testspiele und keine laufenden Spiele.
 * Die Endpunkte rechnet diese App selbst (Punkte + Qwirkle-Bonus + Endspielbonus).
 */
object ExchangeExport {
    const val FORMAT = "punkte-austausch"
    const val FORMAT_VERSION = 1
    const val SOURCE = "com.thomas.qwirkle"
    const val MIME_TYPE = "application/json"

    // Reihenfolge = colorIndex der Spieler (siehe PlayerColors)
    private val colorNames = listOf("rot", "blau", "grün", "gelb", "lila", "orange", "türkis")

    fun colorName(colorIndex: Int): String = colorNames[colorIndex.mod(colorNames.size)]

    /** Wählt die abgeschlossenen Nicht-Testspiele aus und berechnet Endpunkte und Plätze. */
    fun build(data: BackupData): List<ExchangeGame> {
        val playersById = data.players.associateBy { it.id }
        val scoresByRound = data.roundScores.groupBy { it.roundId }
        return data.games
            .filter { it.isFinished && !it.isTestGame }
            .sortedBy { it.date }
            .map { game ->
                val participants = data.gamePlayers
                    .filter { it.gameId == game.id }
                    .sortedBy { it.position }
                val players = participants.mapNotNull { playersById[it.playerId] }
                val rounds = data.rounds
                    .filter { it.gameId == game.id }
                    .sortedBy { it.roundNumber }
                    .map { RoundWithScores(it, scoresByRound[it.id].orEmpty()) }
                val totals = GameCalculations.playerTotals(
                    players, rounds, game.endBonusPlayerId, game.qwirkleBonus
                )
                val rankByPlayer = GameCalculations.ranking(totals)
                    .associate { (rank, total) -> total.player.id to rank }
                ExchangeGame(
                    externalId = game.uuid,
                    startTime = game.date,
                    // Die App speichert nur den Startzeitpunkt einer Partie
                    endTime = game.date,
                    players = totals.mapIndexed { index, total ->
                        ExchangePlayer(
                            name = total.player.name,
                            colorName = colorName(total.player.colorIndex),
                            order = index + 1,
                            finalPoints = total.points,
                            rank = rankByPlayer.getValue(total.player.id)
                        )
                    }
                )
            }
    }

    fun encode(games: List<ExchangeGame>, exportedAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("quelle", SOURCE)
            .put("exportiertAm", exportedAt)
        root.put("partien", JSONArray().also { array ->
            games.forEach { game ->
                array.put(
                    JSONObject()
                        .put("externeId", game.externalId)
                        .put(
                            "spiel",
                            JSONObject()
                                .put("name", "Qwirkle")
                                .put("gewinntHoechste", true)
                                .put("modus", "PUNKTE")
                        )
                        .put("startZeit", game.startTime)
                        .put("endZeit", game.endTime)
                        .put("spieler", JSONArray().also { players ->
                            game.players.forEach {
                                players.put(
                                    JSONObject()
                                        .put("name", it.name)
                                        .put("farbe", it.colorName)
                                        .put("reihenfolge", it.order)
                                        .put("endpunkte", it.finalPoints)
                                        .put("platz", it.rank)
                                )
                            }
                        })
                )
            }
        })
        return root.toString(2)
    }
}
