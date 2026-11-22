package com.thomas.qwirkle.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein Spiel. totalPoints und totalQwirkles werden erst beim Speichern
 * (finishGame) berechnet. Bis dahin ist isFinished = false.
 *
 * qwirkleBonus: Punkte, die ein Qwirkle in diesem Spiel zählt (Standard 6; Eingabevariante
 * "Alternativ" 12). Gilt für alle Berechnungen dieses Spiels, alte Spiele haben 6.
 */
@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val playerCount: Int,
    val totalPoints: Int = 0,
    val totalQwirkles: Int = 0,
    val isTestGame: Boolean = false,
    val isFinished: Boolean = false,
    val endBonusPlayerId: Long? = null,
    @ColumnInfo(defaultValue = "6")
    val qwirkleBonus: Int = 6
)
