package com.thomas.qwirkle.util

import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import com.thomas.qwirkle.data.model.RoundWithScores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCalculationsTest {

    private val anna = Player(id = 1, name = "Anna", colorIndex = 0)
    private val peter = Player(id = 2, name = "Peter", colorIndex = 1)

    private fun score(roundId: Long, player: Player, points: Int, qwirkles: Int = 0) =
        RoundScore(roundId = roundId, playerId = player.id, points = points, qwirkles = qwirkles)

    private fun round(number: Int, vararg scores: RoundScore) =
        RoundWithScores(
            Round(id = number.toLong(), gameId = 1, roundNumber = number),
            scores.toList()
        )

    // Runde 1: Anna 10, Peter 20 mit 1 Qwirkle
    // Runde 2: Anna 5 mit 2 Qwirkles, Peter 0
    private val twoRounds = listOf(
        round(1, score(1, anna, 10), score(1, peter, 20, 1)),
        round(2, score(2, anna, 5, 2), score(2, peter, 0))
    )

    private fun total(player: Player, points: Int, qwirkles: Int = 0) =
        PlayerTotal(player, points, qwirkles)

    // ---------- playerTotals ----------

    @Test
    fun playerTotals_sumsPointsAndBonusesOverAllRounds() {
        val totals = GameCalculations.playerTotals(listOf(anna, peter), twoRounds)

        assertEquals(27, totals[0].points)   // 10 + (5 + 12)
        assertEquals(2, totals[0].qwirkles)
        assertEquals(26, totals[1].points)   // (20 + 6) + 0
        assertEquals(1, totals[1].qwirkles)
    }

    @Test
    fun playerTotals_addsEndBonusOnlyToChosenPlayer() {
        val totals = GameCalculations.playerTotals(
            listOf(anna, peter), twoRounds, endBonusPlayerId = peter.id
        )

        assertEquals(27, totals[0].points)
        assertEquals(32, totals[1].points)   // 26 + 6
    }

    @Test
    fun playerTotals_endBonusDoesNotCountAsQwirkle() {
        val totals = GameCalculations.playerTotals(
            listOf(anna, peter), twoRounds, endBonusPlayerId = peter.id
        )

        assertEquals(1, totals[1].qwirkles)
    }

    @Test
    fun playerTotals_withoutEndBonusPlayer_addsNothing() {
        val totals = GameCalculations.playerTotals(
            listOf(anna, peter), twoRounds, endBonusPlayerId = null
        )

        assertEquals(27, totals[0].points)
        assertEquals(26, totals[1].points)
    }

    @Test
    fun playerTotals_endBonusForUnknownPlayer_isIgnored() {
        val totals = GameCalculations.playerTotals(
            listOf(anna, peter), twoRounds, endBonusPlayerId = 99L
        )

        assertEquals(27, totals[0].points)
        assertEquals(26, totals[1].points)
    }

    @Test
    fun playerTotals_keepsPlayerOrder() {
        val totals = GameCalculations.playerTotals(listOf(peter, anna), twoRounds)

        assertEquals(listOf(peter, anna), totals.map { it.player })
    }

    @Test
    fun playerTotals_playerWithoutScores_hasZero() {
        val carl = Player(id = 3, name = "Carl", colorIndex = 2)

        val totals = GameCalculations.playerTotals(listOf(anna, carl), twoRounds)

        assertEquals(0, totals[1].points)
        assertEquals(0, totals[1].qwirkles)
    }

    @Test
    fun playerTotals_noRounds_givesZeros() {
        val totals = GameCalculations.playerTotals(listOf(anna, peter), emptyList())

        assertTrue(totals.all { it.points == 0 && it.qwirkles == 0 })
    }

    @Test
    fun playerTotals_noRoundsButEndBonus_givesOnlyBonus() {
        val totals = GameCalculations.playerTotals(
            listOf(anna, peter), emptyList(), endBonusPlayerId = anna.id
        )

        assertEquals(6, totals[0].points)
        assertEquals(0, totals[1].points)
    }

    @Test
    fun playerTotals_ignoresScoresOfPlayersNotInList() {
        val totals = GameCalculations.playerTotals(listOf(anna), twoRounds)

        assertEquals(1, totals.size)
        assertEquals(27, totals[0].points)
    }

    @Test
    fun playerTotals_gameSum_isSumOfAllPlayersIncludingEndBonus() {
        // So wird totalPoints in GameRepository.finishGame definiert
        val withoutBonus = GameCalculations.playerTotals(listOf(anna, peter), twoRounds)
        val withBonus = GameCalculations.playerTotals(
            listOf(anna, peter), twoRounds, endBonusPlayerId = anna.id
        )

        assertEquals(53, withoutBonus.sumOf { it.points })
        assertEquals(59, withBonus.sumOf { it.points })
    }

    // ---------- ranking ----------

    @Test
    fun ranking_sortsByPointsDescending() {
        val ranking = GameCalculations.ranking(
            listOf(total(anna, 20), total(peter, 35))
        )

        assertEquals(listOf(peter, anna), ranking.map { it.second.player })
        assertEquals(listOf(1, 2), ranking.map { it.first })
    }

    @Test
    fun ranking_equalPointsShareRank_andNextRankIsSkipped() {
        val carl = Player(id = 3, name = "Carl", colorIndex = 2)
        val dora = Player(id = 4, name = "Dora", colorIndex = 3)

        val ranking = GameCalculations.ranking(
            listOf(total(anna, 30), total(peter, 20), total(carl, 20), total(dora, 10))
        )

        assertEquals(listOf(1, 2, 2, 4), ranking.map { it.first })
    }

    @Test
    fun ranking_allEqual_allRankOne() {
        val ranking = GameCalculations.ranking(
            listOf(total(anna, 15), total(peter, 15))
        )

        assertEquals(listOf(1, 1), ranking.map { it.first })
    }

    @Test
    fun ranking_singlePlayer_isRankOne() {
        val ranking = GameCalculations.ranking(listOf(total(anna, 0)))

        assertEquals(listOf(1), ranking.map { it.first })
    }

    @Test
    fun ranking_emptyList_isEmpty() {
        assertTrue(GameCalculations.ranking(emptyList()).isEmpty())
    }

    @Test
    fun ranking_doesNotChangeTotals() {
        val ranking = GameCalculations.ranking(listOf(total(anna, 10, 1), total(peter, 40, 3)))

        assertEquals(40, ranking[0].second.points)
        assertEquals(3, ranking[0].second.qwirkles)
        assertEquals(10, ranking[1].second.points)
        assertEquals(1, ranking[1].second.qwirkles)
    }
}
