package com.thomas.qwirkle.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Welche Spieler nehmen an welchem Spiel teil (und in welcher Reihenfolge). */
@Entity(
    tableName = "game_players",
    foreignKeys = [
        ForeignKey(
            entity = GameHistory::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"]
        )
    ],
    indices = [Index("gameId"), Index("playerId")]
)
data class GamePlayer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val position: Int
)
