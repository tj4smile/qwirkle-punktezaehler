package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.data.database.DATABASE_VERSION
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupFormatException(val reason: Reason, message: String) : Exception(message) {
    enum class Reason { NOT_A_BACKUP, NEWER_VERSION, INCONSISTENT }
}

/**
 * Sicherung als JSON-Datei.
 *
 * FORMAT_VERSION beschreibt den AUFBAU der Datei. Ändert sich die Struktur der
 * Daten (neue Spalten, neue Tabellen), muss sie erhöht und decode() so erweitert
 * werden, dass auch alte Sicherungen noch gelesen werden können.
 */
object BackupJson {
    const val FORMAT = "qwirkle-backup"
    const val FORMAT_VERSION = 1

    fun encode(data: BackupData, exportedAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("schemaVersion", DATABASE_VERSION)
            .put("exportedAt", exportedAt)

        root.put("players", JSONArray().also { array ->
            data.players.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("colorIndex", it.colorIndex)
                        .put("isActive", it.isActive)
                )
            }
        })
        root.put("games", JSONArray().also { array ->
            data.games.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("date", it.date)
                        .put("playerCount", it.playerCount)
                        .put("totalPoints", it.totalPoints)
                        .put("totalQwirkles", it.totalQwirkles)
                        .put("isTestGame", it.isTestGame)
                        .put("isFinished", it.isFinished)
                        // JSONObject.NULL, sonst würde das Feld ganz weggelassen
                        .put("endBonusPlayerId", it.endBonusPlayerId ?: JSONObject.NULL)
                )
            }
        })
        root.put("gamePlayers", JSONArray().also { array ->
            data.gamePlayers.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("gameId", it.gameId)
                        .put("playerId", it.playerId)
                        .put("position", it.position)
                )
            }
        })
        root.put("rounds", JSONArray().also { array ->
            data.rounds.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("gameId", it.gameId)
                        .put("roundNumber", it.roundNumber)
                )
            }
        })
        root.put("roundScores", JSONArray().also { array ->
            data.roundScores.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("roundId", it.roundId)
                        .put("playerId", it.playerId)
                        .put("points", it.points)
                        .put("qwirkles", it.qwirkles)
                )
            }
        })
        return root.toString(2)
    }

    /** Liest und PRÜFT eine Sicherung. Wirft BackupFormatException, wenn sie nicht brauchbar ist. */
    fun decode(rawText: String): BackupData {
        val text = rawText.removePrefix("﻿")
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupFormatException(
                BackupFormatException.Reason.NOT_A_BACKUP, "Keine gültige JSON-Datei"
            )
        }

        if (root.optString("format") != FORMAT) {
            throw BackupFormatException(
                BackupFormatException.Reason.NOT_A_BACKUP, "Keine Qwirkle-Sicherung"
            )
        }
        val version = root.optInt("formatVersion", -1)
        if (version < 1) {
            throw BackupFormatException(
                BackupFormatException.Reason.NOT_A_BACKUP, "Formatversion fehlt"
            )
        }
        if (version > FORMAT_VERSION) {
            throw BackupFormatException(
                BackupFormatException.Reason.NEWER_VERSION, "Sicherung aus neuerer App-Version"
            )
        }

        val data = try {
            BackupData(
                players = root.getJSONArray("players").objects().map {
                    Player(
                        id = it.getLong("id"),
                        name = it.getString("name"),
                        colorIndex = it.getInt("colorIndex"),
                        isActive = it.getBoolean("isActive")
                    )
                },
                games = root.getJSONArray("games").objects().map {
                    GameHistory(
                        id = it.getLong("id"),
                        date = it.getLong("date"),
                        playerCount = it.getInt("playerCount"),
                        totalPoints = it.getInt("totalPoints"),
                        totalQwirkles = it.getInt("totalQwirkles"),
                        isTestGame = it.getBoolean("isTestGame"),
                        isFinished = it.getBoolean("isFinished"),
                        endBonusPlayerId = if (it.isNull("endBonusPlayerId")) null
                        else it.getLong("endBonusPlayerId")
                    )
                },
                gamePlayers = root.getJSONArray("gamePlayers").objects().map {
                    GamePlayer(
                        id = it.getLong("id"),
                        gameId = it.getLong("gameId"),
                        playerId = it.getLong("playerId"),
                        position = it.getInt("position")
                    )
                },
                rounds = root.getJSONArray("rounds").objects().map {
                    Round(
                        id = it.getLong("id"),
                        gameId = it.getLong("gameId"),
                        roundNumber = it.getInt("roundNumber")
                    )
                },
                roundScores = root.getJSONArray("roundScores").objects().map {
                    RoundScore(
                        id = it.getLong("id"),
                        roundId = it.getLong("roundId"),
                        playerId = it.getLong("playerId"),
                        points = it.getInt("points"),
                        qwirkles = it.getInt("qwirkles")
                    )
                }
            )
        } catch (e: JSONException) {
            throw BackupFormatException(
                BackupFormatException.Reason.NOT_A_BACKUP, "Felder fehlen oder sind fehlerhaft"
            )
        }

        validate(data)
        return data
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }

    private fun inconsistent(message: String): Nothing =
        throw BackupFormatException(BackupFormatException.Reason.INCONSISTENT, message)

    /** Prüft Eindeutigkeit der IDs, gültige Verweise und sinnvolle Werte, bevor etwas gelöscht wird. */
    private fun validate(data: BackupData) {
        fun requireUnique(ids: List<Long>, table: String) {
            if (ids.toSet().size != ids.size) inconsistent("Doppelte IDs in $table")
        }
        requireUnique(data.players.map { it.id }, "players")
        requireUnique(data.games.map { it.id }, "games")
        requireUnique(data.gamePlayers.map { it.id }, "gamePlayers")
        requireUnique(data.rounds.map { it.id }, "rounds")
        requireUnique(data.roundScores.map { it.id }, "roundScores")

        val playerIds = data.players.map { it.id }.toSet()
        val gameIds = data.games.map { it.id }.toSet()
        val roundIds = data.rounds.map { it.id }.toSet()

        if (data.players.any { it.name.isBlank() }) inconsistent("Spieler ohne Namen")
        data.games.forEach {
            if (it.endBonusPlayerId != null && it.endBonusPlayerId !in playerIds) {
                inconsistent("Endbonus-Spieler unbekannt")
            }
        }
        data.gamePlayers.forEach {
            if (it.gameId !in gameIds || it.playerId !in playerIds) {
                inconsistent("Teilnehmer verweist auf unbekanntes Spiel oder Spieler")
            }
        }
        data.rounds.forEach {
            if (it.gameId !in gameIds) inconsistent("Runde verweist auf unbekanntes Spiel")
            if (it.roundNumber < 1) inconsistent("Ungültige Rundennummer")
        }
        data.roundScores.forEach {
            if (it.roundId !in roundIds || it.playerId !in playerIds) {
                inconsistent("Punkte verweisen auf unbekannte Runde oder Spieler")
            }
            if (it.points < 0 || it.qwirkles < 0) inconsistent("Negative Punkte")
        }
    }
}
