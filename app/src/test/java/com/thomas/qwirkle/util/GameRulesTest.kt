package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GameRulesTest {

    @Test
    fun constants_matchGameRules() {
        assertEquals(6, GameRules.QWIRKLE_BONUS)
        assertEquals(6, GameRules.END_BONUS)
        assertEquals(2, GameRules.MIN_PLAYERS)
        assertEquals(7, GameRules.MAX_PLAYERS)
    }

    @Test
    fun qwirkleBonus_isSixPerQwirkle() {
        assertEquals(0, GameRules.qwirkleBonus(0))
        assertEquals(6, GameRules.qwirkleBonus(1))
        assertEquals(12, GameRules.qwirkleBonus(2))
        assertEquals(54, GameRules.qwirkleBonus(9))
    }

    @Test
    fun roundTotal_addsBonusToPoints() {
        assertEquals(30, GameRules.roundTotal(points = 18, qwirkles = 2))
    }

    @Test
    fun roundTotal_withoutQwirkles_equalsPoints() {
        assertEquals(18, GameRules.roundTotal(points = 18, qwirkles = 0))
    }

    @Test
    fun roundTotal_zeroPointsWithQwirkle_isOnlyBonus() {
        assertEquals(6, GameRules.roundTotal(points = 0, qwirkles = 1))
    }

    @Test
    fun roundTotal_zeroEverything_isZero() {
        assertEquals(0, GameRules.roundTotal(points = 0, qwirkles = 0))
    }
}
