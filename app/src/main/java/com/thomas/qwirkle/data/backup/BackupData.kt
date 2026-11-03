package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore

/** Der komplette Datenbestand der App (alle Tabellen). */
data class BackupData(
    val players: List<Player>,
    val games: List<GameHistory>,
    val gamePlayers: List<GamePlayer>,
    val rounds: List<Round>,
    val roundScores: List<RoundScore>
)
