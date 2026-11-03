package com.thomas.qwirkle.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.Round

data class GameWithRounds(
    @Embedded val game: GameHistory,
    @Relation(entity = Round::class, parentColumn = "id", entityColumn = "gameId")
    val rounds: List<RoundWithScores>
)
