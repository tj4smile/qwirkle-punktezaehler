package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeExportTest {

    private val tj = Player(1, "TJ", 4)       // lila
    private val anna = Player(2, "Anna", 0)   // rot
    private val ben = Player(3, "Ben", 6)     // türkis

    private fun game(
        id: Long,
        uuid: String,
        bonus: Int = 6,
        finished: Boolean = true,
        test: Boolean = false,
        endBonusPlayerId: Long? = null,
        date: Long = 1_700_000_000_000
    ) = GameHistory(
        id = id, date = date, playerCount = 3, isTestGame = test, isFinished = finished,
        endBonusPlayerId = endBonusPlayerId, qwirkleBonus = bonus, uuid = uuid
    )

    /** Eine Runde mit den Punkten (points, qwirkles) je Spieler. */
    private fun data(
        games: List<GameHistory>,
        positions: List<Long> = listOf(1, 2, 3),
        scores: List<Triple<Long, Int, Int>>
    ): BackupData {
        val gamePlayers = games.flatMapIndexed { g, game ->
            positions.mapIndexed { i, playerId ->
                GamePlayer(g * 10L + i + 1, game.id, playerId, i)
            }
        }
        val rounds = games.mapIndexed { g, game -> Round(100L + g, game.id, 1) }
        val roundScores = games.flatMapIndexed { g, _ ->
            scores.mapIndexed { i, (playerId, points, qwirkles) ->
                RoundScore(g * 100L + i + 1, 100L + g, playerId, points, qwirkles)
            }
        }
        return BackupData(listOf(tj, anna, ben), games, gamePlayers, rounds, roundScores)
    }

    @Test
    fun finalPoints_includeQwirkleBonusSix_andEndBonus() {
        val result = ExchangeExport.build(
            data(
                listOf(game(1, "u1", endBonusPlayerId = 1)),
                scores = listOf(Triple(1, 10, 1), Triple(2, 20, 0), Triple(3, 5, 2))
            )
        ).single()

        // TJ: 10 + 6 + 6 Endbonus, Anna: 20, Ben: 5 + 12
        assertEquals(listOf(22, 20, 17), result.players.map { it.finalPoints })
        assertEquals(listOf(1, 2, 3), result.players.map { it.rank })
    }

    @Test
    fun finalPoints_useBonusTwelve_forAlternativeVariant() {
        val result = ExchangeExport.build(
            data(
                listOf(game(1, "u1", bonus = 12)),
                scores = listOf(Triple(1, 3, 1), Triple(2, 0, 0), Triple(3, 0, 0))
            )
        ).single()

        // 3 + 12 = 15, wie im Beispiel der Wunschliste
        assertEquals(15, result.players[0].finalPoints)
    }

    @Test
    fun ties_shareThePlace_nextPlaceIsSkipped() {
        val result = ExchangeExport.build(
            data(
                listOf(game(1, "u1")),
                scores = listOf(Triple(1, 30, 0), Triple(2, 30, 0), Triple(3, 10, 0))
            )
        ).single()

        assertEquals(listOf(1, 1, 3), result.players.map { it.rank })
    }

    @Test
    fun playersFollowPlayingOrder_startingAtOne() {
        val result = ExchangeExport.build(
            data(
                listOf(game(1, "u1")),
                positions = listOf(3, 1, 2),
                scores = listOf(Triple(1, 1, 0), Triple(2, 2, 0), Triple(3, 3, 0))
            )
        ).single()

        assertEquals(listOf("Ben", "TJ", "Anna"), result.players.map { it.name })
        assertEquals(listOf(1, 2, 3), result.players.map { it.order })
    }

    @Test
    fun onlyFinishedRealGamesAreExported() {
        val result = ExchangeExport.build(
            data(
                listOf(
                    game(1, "real"),
                    game(2, "test", test = true),
                    game(3, "running", finished = false)
                ),
                scores = listOf(Triple(1, 1, 0), Triple(2, 2, 0), Triple(3, 3, 0))
            )
        )

        assertEquals(listOf("real"), result.map { it.externalId })
    }

    @Test
    fun colorNames_followTheColorIndex() {
        assertEquals(
            listOf("rot", "blau", "grün", "gelb", "lila", "orange", "türkis"),
            (0..6).map { ExchangeExport.colorName(it) }
        )
        val result = ExchangeExport.build(
            data(listOf(game(1, "u1")), scores = listOf(Triple(1, 1, 0), Triple(2, 2, 0), Triple(3, 3, 0)))
        ).single()
        assertEquals(listOf("lila", "rot", "türkis"), result.players.map { it.colorName })
    }

    @Test
    fun gameKeepsUuidAndTimes() {
        val result = ExchangeExport.build(
            data(
                listOf(game(1, "abc-123", date = 1_700_000_123_000)),
                scores = listOf(Triple(1, 1, 0), Triple(2, 2, 0), Triple(3, 3, 0))
            )
        ).single()

        assertEquals("abc-123", result.externalId)
        assertEquals(1_700_000_123_000, result.startTime)
        assertTrue(result.endTime >= result.startTime)
    }

    @Test
    fun noFinishedGames_givesEmptyList() {
        val result = ExchangeExport.build(
            data(listOf(game(1, "u", finished = false)), scores = listOf(Triple(1, 1, 0)))
        )

        assertTrue(result.isEmpty())
    }
}
