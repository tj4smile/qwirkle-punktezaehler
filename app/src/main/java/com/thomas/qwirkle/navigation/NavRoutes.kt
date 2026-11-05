package com.thomas.qwirkle.navigation

object NavRoutes {
    const val ARG_GAME_ID = "gameId"
    const val ARG_ROUND_ID = "roundId"

    // Bildschirme ohne Argument
    const val HOME = "home"
    const val PLAYERS = "players"
    const val NEWGAME = "newgame"
    const val STATISTICS = "statistics"
    const val DATA = "data"
    const val INFO = "info"

    // Bildschirme eines laufenden Spiels (Route-Muster mit Argument)
    const val SCORING = "scoring/{$ARG_GAME_ID}"     // Punkteerfassung
    const val HISTORY = "history/{$ARG_GAME_ID}"     // Spielverlauf
    const val EDITROUND = "editround/{$ARG_GAME_ID}/{$ARG_ROUND_ID}" // Runde bearbeiten
    const val GAMEEND = "gameend/{$ARG_GAME_ID}"     // Spielende (Endspielbonus)
    const val RESULTS = "results/{$ARG_GAME_ID}"     // Endauswertung

    fun scoring(gameId: Long) = "scoring/$gameId"
    fun history(gameId: Long) = "history/$gameId"
    fun editRound(gameId: Long, roundId: Long) = "editround/$gameId/$roundId"
    fun gameEnd(gameId: Long) = "gameend/$gameId"
    fun results(gameId: Long) = "results/$gameId"
}
