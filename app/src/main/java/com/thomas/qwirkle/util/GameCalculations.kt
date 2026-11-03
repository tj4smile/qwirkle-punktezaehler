package com.thomas.qwirkle.util

import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.RoundWithScores

data class PlayerTotal(
    val player: Player,
    val points: Int,
    val qwirkles: Int
)

object GameCalculations {

    /** Gesamtpunkte (inkl. Qwirkle-Boni und optionalem Endspielbonus) und Qwirkles pro Spieler. */
    fun playerTotals(
        players: List<Player>,
        rounds: List<RoundWithScores>,
        endBonusPlayerId: Long? = null
    ): List<PlayerTotal> {
        val scores = rounds.flatMap { it.scores }
        return players.map { player ->
            val own = scores.filter { it.playerId == player.id }
            val endBonus = if (player.id == endBonusPlayerId) GameRules.END_BONUS else 0
            PlayerTotal(
                player = player,
                points = own.sumOf { GameRules.roundTotal(it.points, it.qwirkles) } + endBonus,
                qwirkles = own.sumOf { it.qwirkles }
            )
        }
    }

    /** Platzierungen, absteigend nach Punkten. Gleiche Punkte = gleicher Rang. */
    fun ranking(totals: List<PlayerTotal>): List<Pair<Int, PlayerTotal>> {
        val sorted = totals.sortedByDescending { it.points }
        val result = mutableListOf<Pair<Int, PlayerTotal>>()
        sorted.forEachIndexed { index, total ->
            val rank = if (index > 0 && total.points == sorted[index - 1].points) {
                result[index - 1].first
            } else {
                index + 1
            }
            result.add(rank to total)
        }
        return result
    }
}
