package com.thomas.qwirkle.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Läuft auf dem Gerät/Emulator, weil org.json dort echt vorhanden ist. */
@RunWith(AndroidJUnit4::class)
class ExchangeJsonTest {

    private val games = listOf(
        ExchangeGame(
            externalId = "11111111-2222-3333-4444-555555555555",
            startTime = 1_700_000_000_000,
            endTime = 1_700_000_000_000,
            players = listOf(
                ExchangePlayer("TJ", "lila", 1, 3, 2),
                ExchangePlayer("Jörg \"J\"", "grün", 2, 9, 1)
            )
        )
    )

    @Test
    fun encode_producesTheAgreedStructure() {
        val root = JSONObject(ExchangeExport.encode(games, exportedAt = 1_700_000_999_000))

        assertEquals("punkte-austausch", root.getString("format"))
        assertEquals(1, root.getInt("formatVersion"))
        assertEquals("com.thomas.qwirkle", root.getString("quelle"))
        assertEquals(1_700_000_999_000, root.getLong("exportiertAm"))

        val game = root.getJSONArray("partien").getJSONObject(0)
        assertEquals("11111111-2222-3333-4444-555555555555", game.getString("externeId"))
        val spiel = game.getJSONObject("spiel")
        assertEquals("Qwirkle", spiel.getString("name"))
        assertTrue(spiel.getBoolean("gewinntHoechste"))
        assertEquals("PUNKTE", spiel.getString("modus"))
        assertEquals(1_700_000_000_000, game.getLong("startZeit"))
        assertEquals(1_700_000_000_000, game.getLong("endZeit"))

        val players = game.getJSONArray("spieler")
        assertEquals(2, players.length())
        val first = players.getJSONObject(0)
        assertEquals("TJ", first.getString("name"))
        assertEquals("lila", first.getString("farbe"))
        assertEquals(1, first.getInt("reihenfolge"))
        assertEquals(3, first.getInt("endpunkte"))
        assertEquals(2, first.getInt("platz"))
        assertEquals("Jörg \"J\"", players.getJSONObject(1).getString("name"))
    }

    @Test
    fun encode_keepsUmlautsReadable() {
        val text = ExchangeExport.encode(games)

        assertTrue(text.contains("grün"))
    }
}
