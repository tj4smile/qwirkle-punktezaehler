package com.thomas.qwirkle.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore

data class RoundWithScores(
    @Embedded val round: Round,
    @Relation(parentColumn = "id", entityColumn = "roundId")
    val scores: List<RoundScore>
)
