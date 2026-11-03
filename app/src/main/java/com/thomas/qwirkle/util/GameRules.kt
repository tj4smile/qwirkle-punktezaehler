package com.thomas.qwirkle.util

object GameRules {
    const val QWIRKLE_BONUS = 6
    const val END_BONUS = 6
    const val MIN_PLAYERS = 2
    const val MAX_PLAYERS = 7

    fun qwirkleBonus(qwirkles: Int): Int = qwirkles * QWIRKLE_BONUS

    /** Rundenpunkte inklusive Qwirkle-Bonus. */
    fun roundTotal(points: Int, qwirkles: Int): Int = points + qwirkleBonus(qwirkles)
}
