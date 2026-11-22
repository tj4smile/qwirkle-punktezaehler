package com.thomas.qwirkle.util

object GameRules {
    /** Qwirkle-Bonus der Standard-Eingabevariante. */
    const val QWIRKLE_BONUS = 6
    /** Eingabevariante "Alternativ": ein Qwirkle zählt 12, eingetragen werden nur die Querreihen. */
    const val ALTERNATIVE_QWIRKLE_BONUS = 12
    const val END_BONUS = 6
    const val MIN_PLAYERS = 2
    const val MAX_PLAYERS = 7

    fun qwirkleBonus(qwirkles: Int, bonusPerQwirkle: Int = QWIRKLE_BONUS): Int =
        qwirkles * bonusPerQwirkle

    /** Rundenpunkte inklusive Qwirkle-Bonus. */
    fun roundTotal(points: Int, qwirkles: Int, bonusPerQwirkle: Int = QWIRKLE_BONUS): Int =
        points + qwirkleBonus(qwirkles, bonusPerQwirkle)
}
