package com.thomas.qwirkle.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * points = vom Spieler eingegebene Punkte OHNE Qwirkle-Bonus.
 * Der Bonus (6 pro Qwirkle) wird per GameRules berechnet, nicht gespeichert.
 */
@Entity(
    tableName = "round_scores",
    foreignKeys = [
        ForeignKey(
            entity = Round::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"]
        )
    ],
    indices = [Index("roundId"), Index("playerId")]
)
data class RoundScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roundId: Long,
    val playerId: Long,
    val points: Int,
    val qwirkles: Int
)
