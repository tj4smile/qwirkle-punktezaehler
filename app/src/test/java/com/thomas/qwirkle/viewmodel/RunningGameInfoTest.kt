package com.thomas.qwirkle.viewmodel

import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import com.thomas.qwirkle.data.model.RoundWithScores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunningGameInfoTest {

    private val anna = Player(id = 1, name = "Anna", colorIndex = 0)
    private val peter = Player(id = 2, name = "Peter", colorIndex = 1)

    private fun round(number: Int, annaPts: Int, annaQ: Int, peterPts: Int, peterQ: Int) =
        RoundWithScores(
            Round(id = number.toLong(), gameId = 1, roundNumber = number),
            listOf(
                RoundScore(roundId = number.toLong(), playerId = 1, points = annaPts, qwirkles = annaQ),
                RoundScore(roundId = number.toLong(), playerId = 2, points = peterPts, qwirkles = peterQ)
            )
        )

    @Test
    fun noRounds_noLeader() {
        val info = RunningGameInfo.of(listOf(anna, peter), emptyList())
        assertEquals(0, info.rounds)
        assertNull(info.leaderName)
        assertEquals(0, info.totalPoints)
    }

    @Test
    fun leaderAndTotal_includeQwirkleBonus() {
        // Anna: 10 + (5 + 12) = 27, Peter: (20 + 6) + 0 = 26
        val rounds = listOf(round(1, 10, 0, 20, 1), round(2, 5, 2, 0, 0))
        val info = RunningGameInfo.of(listOf(anna, peter), rounds)
        assertEquals(2, info.rounds)
        assertEquals("Anna", info.leaderName)
        assertEquals(27, info.leaderPoints)
        assertEquals(53, info.totalPoints)
        assertFalse(info.isTie)
    }

    @Test
    fun tie_isReported() {
        val info = RunningGameInfo.of(listOf(anna, peter), listOf(round(1, 10, 0, 10, 0)))
        assertTrue(info.isTie)
        assertEquals(10, info.leaderPoints)
    }

    @Test
    fun longName_isShortened() {
        val long = Player(id = 1, name = "Maximilianus-Alexander", colorIndex = 0)
        val info = RunningGameInfo.of(listOf(long, peter), listOf(round(1, 30, 0, 1, 0)))
        assertEquals(RunningGameInfo.MAX_NAME_LENGTH, info.leaderName!!.length)
        assertTrue(info.leaderName!!.endsWith("…"))
    }
}
