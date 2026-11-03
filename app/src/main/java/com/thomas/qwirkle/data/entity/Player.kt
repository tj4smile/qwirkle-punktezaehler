package com.thomas.qwirkle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Spieler. Wird nie hart gelöscht, sondern nur deaktiviert (isActive = false),
 * damit alte Spiele und Statistiken erhalten bleiben.
 */
@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val isActive: Boolean = true
)
