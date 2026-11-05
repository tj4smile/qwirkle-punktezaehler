package com.thomas.qwirkle.util

/** Reine Logik des Nummernpads (ohne Android), damit sie sich testen lässt. */
object ScoreInputLogic {
    const val MAX_DIGITS = 2

    /**
     * Hängt eine Ziffer an. [replace] = true ersetzt den bisherigen Wert (erste Ziffer
     * nach dem Setzen des Cursors); eine einzelne "0" wird ebenfalls ersetzt.
     * Höchstens [MAX_DIGITS] Stellen, keine führende Null.
     */
    fun appendDigit(current: String, digit: Int, replace: Boolean): String {
        require(digit in 0..9)
        val base = if (replace || current.isEmpty() || current == "0") "" else current
        val result = (base + digit).take(MAX_DIGITS)
        return result.trimStart('0').ifEmpty { "0" }
    }

    /** Löscht die letzte Stelle; nichts mehr übrig = "0". */
    fun deleteLast(current: String): String =
        current.dropLast(1).ifEmpty { "0" }

    /** Nächster Spieler; nach dem letzten bleibt der Cursor beim letzten. */
    fun nextCursor(active: Int, lastIndex: Int): Int =
        if (lastIndex < 0) 0 else (active + 1).coerceAtMost(lastIndex)
}
