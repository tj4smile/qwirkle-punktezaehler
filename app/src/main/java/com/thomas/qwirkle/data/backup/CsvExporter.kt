package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.util.GameRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Beschriftungen der CSV-Datei (kommen aus den String-Ressourcen, also in der App-Sprache). */
data class CsvLabels(
    val date: String,
    val game: String,
    val testGame: String,
    val finished: String,
    val round: String,
    val player: String,
    val points: String,
    val qwirkles: String,
    val qwirkleBonus: String,
    val roundTotal: String,
    val endBonus: String,
    val yes: String,
    val no: String
)

/**
 * CSV für Excel/LibreOffice: eine Zeile pro Spieler und Runde.
 * Die Spalte "Rundenpunkte" enthält Punkte + Qwirkle-Bonus; der Endspielbonus steht in
 * einer eigenen Zeile. Die Summe der Spalte "Rundenpunkte" je Spiel entspricht damit den
 * gespeicherten Gesamtpunkten des Spiels.
 */
object CsvExporter {
    /** Byte-Order-Mark, damit Excel Umlaute richtig (UTF-8) darstellt. */
    const val BOM = "﻿"
    private const val NEWLINE = "\r\n"

    /** Deutsche Excel-Versionen erwarten ";", andere "," als Trennzeichen. */
    fun delimiterFor(locale: Locale): Char = if (locale.language == "de") ';' else ','

    fun build(data: BackupData, labels: CsvLabels, delimiter: Char): String {
        val builder = StringBuilder()
        fun addRow(cells: List<String>) {
            builder.append(cells.joinToString(delimiter.toString())).append(NEWLINE)
        }
        fun text(value: String) = textField(value, delimiter)

        addRow(
            listOf(
                labels.date, labels.game, labels.testGame, labels.finished, labels.round,
                labels.player, labels.points, labels.qwirkles, labels.qwirkleBonus,
                labels.roundTotal
            ).map(::text)
        )

        val playersById = data.players.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        fun yesNo(value: Boolean) = if (value) labels.yes else labels.no

        data.games.sortedWith(compareBy({ it.date }, { it.id })).forEach { game ->
            val day = dateFormat.format(Date(game.date))
            val order = data.gamePlayers
                .filter { it.gameId == game.id }
                .sortedBy { it.position }
                .mapIndexed { index, gamePlayer -> gamePlayer.playerId to index }
                .toMap()
            val rounds = data.rounds.filter { it.gameId == game.id }.sortedBy { it.roundNumber }

            rounds.forEach { round ->
                data.roundScores
                    .filter { it.roundId == round.id }
                    .sortedBy { order[it.playerId] ?: Int.MAX_VALUE }
                    .forEach { score ->
                        addRow(
                            listOf(
                                day,
                                game.id.toString(),
                                text(yesNo(game.isTestGame)),
                                text(yesNo(game.isFinished)),
                                round.roundNumber.toString(),
                                text(playersById[score.playerId]?.name ?: ""),
                                score.points.toString(),
                                score.qwirkles.toString(),
                                GameRules.qwirkleBonus(score.qwirkles).toString(),
                                GameRules.roundTotal(score.points, score.qwirkles).toString()
                            )
                        )
                    }
            }

            // Endspielbonus nur bei abgeschlossenen Spielen (wie in den Gesamtpunkten)
            val bonusPlayer = game.endBonusPlayerId?.let { playersById[it] }
            if (game.isFinished && bonusPlayer != null) {
                addRow(
                    listOf(
                        day,
                        game.id.toString(),
                        text(yesNo(game.isTestGame)),
                        text(yesNo(game.isFinished)),
                        text(labels.endBonus),
                        text(bonusPlayer.name),
                        "0",
                        "0",
                        "0",
                        GameRules.END_BONUS.toString()
                    )
                )
            }
        }
        return builder.toString()
    }

    /** Maskiert Trennzeichen/Anführungszeichen und schützt vor Excel-Formeln ("=...", "+...", "-...", "@..."). */
    private fun textField(value: String, delimiter: Char): String {
        var v = value
        if (v.isNotEmpty() && v[0] in "=+-@") v = "'$v"
        val needsQuotes = v.any { it == delimiter || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
}
