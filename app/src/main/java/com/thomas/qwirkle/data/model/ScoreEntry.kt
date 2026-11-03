package com.thomas.qwirkle.data.model

/** Eingabe pro Spieler auf dem Punkteerfassungs-Bildschirm. */
data class ScoreEntry(
    val playerId: Long,
    val points: Int,
    val qwirkles: Int
)
