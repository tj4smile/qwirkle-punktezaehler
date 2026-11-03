package com.thomas.qwirkle.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Läuft auf dem Gerät/Emulator, weil org.json dort echt vorhanden ist. */
@RunWith(AndroidJUnit4::class)
class BackupJsonTest {

    private val sample = BackupData(
        players = listOf(
            Player(1, "Anna", 0, true),
            Player(2, "Jörg \"J\" Müller", 3, false)
        ),
        games = listOf(
            GameHistory(10, 1_700_000_000_000, 2, 59, 3, false, true, 2L),
            GameHistory(11, 1_700_100_000_000, 2, 0, 0, true, false, null)
        ),
        gamePlayers = listOf(
            GamePlayer(1, 10, 1, 0), GamePlayer(2, 10, 2, 1),
            GamePlayer(3, 11, 2, 0), GamePlayer(4, 11, 1, 1)
        ),
        rounds = listOf(Round(100, 10, 1), Round(101, 10, 2)),
        roundScores = listOf(
            RoundScore(1000, 100, 1, 10, 0), RoundScore(1001, 100, 2, 20, 1),
            RoundScore(1002, 101, 1, 5, 2), RoundScore(1003, 101, 2, 0, 0)
        )
    )

    private fun expectReason(reason: BackupFormatException.Reason, text: String) {
        try {
            BackupJson.decode(text)
            fail("BackupFormatException erwartet")
        } catch (e: BackupFormatException) {
            assertEquals(reason, e.reason)
        }
    }

    private fun modified(change: (JSONObject) -> Unit): String {
        val root = JSONObject(BackupJson.encode(sample))
        change(root)
        return root.toString()
    }

    @Test
    fun roundTrip_keepsAllData() {
        assertEquals(sample, BackupJson.decode(BackupJson.encode(sample)))
    }

    @Test
    fun roundTrip_keepsNullEndBonusAndFlags() {
        val decoded = BackupJson.decode(BackupJson.encode(sample))

        assertEquals(null, decoded.games[1].endBonusPlayerId)
        assertTrue(decoded.games[1].isTestGame)
        assertEquals(false, decoded.players[1].isActive)
    }

    @Test
    fun roundTrip_keepsSpecialCharactersInNames() {
        val decoded = BackupJson.decode(BackupJson.encode(sample))

        assertEquals("Jörg \"J\" Müller", decoded.players[1].name)
    }

    @Test
    fun roundTrip_emptyData() {
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(empty, BackupJson.decode(BackupJson.encode(empty)))
    }

    @Test
    fun decode_acceptsLeadingByteOrderMark() {
        assertEquals(sample, BackupJson.decode("﻿" + BackupJson.encode(sample)))
    }

    @Test
    fun decode_noJson_isNotABackup() {
        expectReason(BackupFormatException.Reason.NOT_A_BACKUP, "Das ist kein JSON")
    }

    @Test
    fun decode_otherJson_isNotABackup() {
        expectReason(BackupFormatException.Reason.NOT_A_BACKUP, "{\"hello\": 1}")
    }

    @Test
    fun decode_newerFormatVersion_isReported() {
        val text = modified { it.put("formatVersion", BackupJson.FORMAT_VERSION + 1) }

        expectReason(BackupFormatException.Reason.NEWER_VERSION, text)
    }

    @Test
    fun decode_missingFormatVersion_isNotABackup() {
        val text = modified { it.remove("formatVersion") }

        expectReason(BackupFormatException.Reason.NOT_A_BACKUP, text)
    }

    @Test
    fun decode_missingTable_isNotABackup() {
        val text = modified { it.remove("rounds") }

        expectReason(BackupFormatException.Reason.NOT_A_BACKUP, text)
    }

    @Test
    fun decode_duplicatePlayerIds_areInconsistent() {
        val text = modified {
            it.getJSONArray("players").getJSONObject(1).put("id", 1)
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }

    @Test
    fun decode_scoreOfUnknownPlayer_isInconsistent() {
        val text = modified {
            it.getJSONArray("roundScores").getJSONObject(0).put("playerId", 99)
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }

    @Test
    fun decode_roundOfUnknownGame_isInconsistent() {
        val text = modified {
            it.getJSONArray("rounds").getJSONObject(0).put("gameId", 99)
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }

    @Test
    fun decode_unknownEndBonusPlayer_isInconsistent() {
        val text = modified {
            it.getJSONArray("games").getJSONObject(0).put("endBonusPlayerId", 99)
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }

    @Test
    fun decode_negativePoints_areInconsistent() {
        val text = modified {
            it.getJSONArray("roundScores").getJSONObject(0).put("points", -5)
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }

    @Test
    fun decode_blankPlayerName_isInconsistent() {
        val text = modified {
            it.getJSONArray("players").getJSONObject(0).put("name", "  ")
        }

        expectReason(BackupFormatException.Reason.INCONSISTENT, text)
    }
}
