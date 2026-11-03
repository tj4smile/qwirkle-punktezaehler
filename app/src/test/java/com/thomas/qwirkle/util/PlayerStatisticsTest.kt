package com.thomas.qwirkle.util

import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import com.thomas.qwirkle.data.model.GameWithRounds
import com.thomas.qwirkle.data.model.RoundWithScores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStatisticsTest {

    private val anna = Player(1, "Anna", 0)
    private val peter = Player(2, "Peter", 1)
    private val klaus = Player(3, "Klaus", 2, isActive = false)
    private val lena = Player(4, "Lena", 3)
    private val all = listOf(anna, peter, klaus, lena)

    /** Eintrag pro Spieler: (playerId, Punkte ohne Bonus, Qwirkles). */
    private fun round(id: Long, gameId: Long, number: Int, vararg scores: Triple<Long, Int, Int>) =
        RoundWithScores(
            Round(id, gameId, number),
            scores.mapIndexed { i, (player, points, qwirkles) ->
                RoundScore(id * 100 + i, id, player, points, qwirkles)
            }
        )

    private fun game(id: Long, endBonus: Long?, vararg rounds: RoundWithScores) = GameWithRounds(
        GameHistory(id = id, date = 0, playerCount = 2, isFinished = true, endBonusPlayerId = endBonus),
        rounds.toList()
    )

    private fun participants(gameId: Long, vararg playerIds: Long) =
        playerIds.mapIndexed { i, p -> GamePlayer(gameId * 10 + i, gameId, p, i) }

    private fun stats(list: List<PlayerStats>, name: String) = list.first { it.player.name == name }

    /**
     * Partie 10: Anna 10 | 5+2 Qwirkles = 27, Peter 20+1 Qwirkle | 0 = 26, Endbonus Peter -> 32. Peter gewinnt.
     * Partie 11: Anna 40, Peter 30. Anna gewinnt.
     */
    private val twoGames = listOf(
        game(
            10, endBonus = 2,
            round(100, 10, 1, Triple(1, 10, 0), Triple(2, 20, 1)),
            round(101, 10, 2, Triple(1, 5, 2), Triple(2, 0, 0))
        ),
        game(11, endBonus = null, round(110, 11, 1, Triple(1, 40, 0), Triple(2, 30, 0)))
    )
    private val twoGamesParticipants = participants(10, 1, 2) + participants(11, 1, 2)

    @Test
    fun computesGamesWinsPointsAndQwirkles() {
        val result = PlayerStatistics.compute(all, twoGames, twoGamesParticipants)

        val a = stats(result, "Anna")
        assertEquals(2, a.games)
        assertEquals(1, a.wins)
        assertEquals(67, a.totalPoints)
        assertEquals(40, a.bestPoints)
        assertEquals(2, a.totalQwirkles)

        val p = stats(result, "Peter")
        assertEquals(2, p.games)
        assertEquals(1, p.wins)
        assertEquals(62, p.totalPoints)
        assertEquals(32, p.bestPoints)
        assertEquals(1, p.totalQwirkles)
    }

    @Test
    fun endBonusDecidesTheWinner() {
        val withBonus = PlayerStatistics.compute(all, twoGames.take(1), participants(10, 1, 2))
        val withoutBonus = PlayerStatistics.compute(
            all,
            listOf(twoGames[0].copy(game = twoGames[0].game.copy(endBonusPlayerId = null))),
            participants(10, 1, 2)
        )

        assertEquals(1, stats(withBonus, "Peter").wins)   // 32 gegen 27
        assertEquals(1, stats(withoutBonus, "Anna").wins) // 27 gegen 26
        assertEquals(0, stats(withoutBonus, "Peter").wins)
    }

    @Test
    fun averagesAndWinRate() {
        val a = stats(PlayerStatistics.compute(all, twoGames, twoGamesParticipants), "Anna")

        assertEquals(33.5, a.averagePoints, 0.0001)
        assertEquals(1.0, a.averageQwirkles, 0.0001)
        assertEquals(0.5, a.winRate, 0.0001)
    }

    @Test
    fun sortedByWinsThenWinRateThenAveragePoints() {
        // Anna und Peter: je 1 Sieg, gleiche Quote, Anna hat den besseren Schnitt
        val result = PlayerStatistics.compute(all, twoGames, twoGamesParticipants)

        assertEquals(listOf("Anna", "Peter"), result.map { it.player.name })
    }

    @Test
    fun moreWinsComeFirstEvenWithLowerAverage() {
        val games = listOf(
            game(20, null, round(200, 20, 1, Triple(1, 100, 0), Triple(2, 1, 0))),
            game(21, null, round(210, 21, 1, Triple(2, 5, 0), Triple(1, 4, 0))),
            game(22, null, round(220, 22, 1, Triple(2, 6, 0), Triple(1, 4, 0)))
        )
        val parts = participants(20, 1, 2) + participants(21, 1, 2) + participants(22, 1, 2)

        val result = PlayerStatistics.compute(all, games, parts)

        assertEquals("Peter", result[0].player.name) // 2 Siege
        assertEquals("Anna", result[1].player.name)  // 1 Sieg, aber höherer Schnitt
    }

    @Test
    fun tieForFirstPlaceCountsAsWinForAll() {
        val games = listOf(game(30, null, round(300, 30, 1, Triple(1, 10, 0), Triple(2, 10, 0))))

        val result = PlayerStatistics.compute(all, games, participants(30, 1, 2))

        assertEquals(1, stats(result, "Anna").wins)
        assertEquals(1, stats(result, "Peter").wins)
    }

    @Test
    fun inactivePlayerIsHiddenButStillTakesPartInRanking() {
        val games = listOf(game(40, null, round(400, 40, 1, Triple(1, 20, 0), Triple(3, 50, 0))))

        val result = PlayerStatistics.compute(all, games, participants(40, 1, 3))

        assertEquals(listOf("Anna"), result.map { it.player.name })
        assertEquals(0, result[0].wins) // Klaus (inaktiv) hat gewonnen
        assertEquals(1, result[0].games)
    }

    @Test
    fun playersWithoutGamesAreNotListed() {
        val result = PlayerStatistics.compute(all, twoGames, twoGamesParticipants)

        assertNull(result.firstOrNull { it.player.name == "Lena" })
    }

    @Test
    fun onlyParticipantsAreCounted() {
        // Lena sitzt nicht in der Partie, auch wenn sie aktiv ist
        val result = PlayerStatistics.compute(all, twoGames.take(1), participants(10, 1, 2))

        assertEquals(2, result.size)
    }

    @Test
    fun gameWithoutRoundsIsIgnored() {
        val empty = listOf(game(50, null))

        assertTrue(PlayerStatistics.compute(all, empty, participants(50, 1, 2)).isEmpty())
    }

    @Test
    fun noDataGivesEmptyList() {
        assertTrue(PlayerStatistics.compute(emptyList(), emptyList(), emptyList()).isEmpty())
        assertTrue(PlayerStatistics.compute(all, emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun threePlayerGameHasOneWinner() {
        val games = listOf(
            game(60, null, round(600, 60, 1, Triple(1, 10, 0), Triple(2, 30, 0), Triple(4, 20, 0)))
        )

        val result = PlayerStatistics.compute(all, games, participants(60, 1, 2, 4))

        assertEquals(listOf("Peter"), result.filter { it.wins == 1 }.map { it.player.name })
        assertEquals(3, result.size)
    }

    @Test
    fun winRateWithoutGamesIsZero() {
        val empty = PlayerStats(anna, 0, 0, 0, 0, 0)

        assertEquals(0.0, empty.winRate, 0.0)
        assertEquals(0.0, empty.averagePoints, 0.0)
        assertEquals(0.0, empty.averageQwirkles, 0.0)
    }
}
