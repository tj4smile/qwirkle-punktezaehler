package com.thomas.qwirkle.util

object PlayerColors {
    const val COUNT = 7

    // ARGB-Werte, in Compose: Color(PlayerColors.argb(index))
    private val colors: List<Long> = listOf(
        0xFFE53935, // Rot
        0xFF1E88E5, // Blau
        0xFF43A047, // Grün
        0xFFFDD835, // Gelb
        0xFF8E24AA, // Lila
        0xFFFB8C00, // Orange
        0xFF00ACC1  // Türkis
    )

    fun argb(index: Int): Long = colors[index.mod(COUNT)]

    /** Erste Farbe, die noch kein aktiver Spieler hat; sonst reihum. */
    fun nextFreeIndex(used: Set<Int>): Int =
        (0 until COUNT).firstOrNull { it !in used } ?: (used.size % COUNT)

    /**
     * Macht die Farben eindeutig: die erste Verwendung einer Farbe bleibt,
     * jede weitere bekommt eine noch freie. Bei höchstens [COUNT] Spielern
     * gibt es immer genug freie Farben.
     */
    fun uniqueColors(colors: List<Int>): List<Int> {
        val seen = mutableSetOf<Int>()
        val isDuplicate = colors.map { !seen.add(it) }
        val free = (0 until COUNT).filter { it !in seen }.iterator()
        return colors.mapIndexed { i, c -> if (isDuplicate[i] && free.hasNext()) free.next() else c }
    }
}
