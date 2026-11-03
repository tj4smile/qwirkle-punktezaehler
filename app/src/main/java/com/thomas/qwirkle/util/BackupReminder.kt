package com.thomas.qwirkle.util

/**
 * Entscheidet, ob die Startseite an eine Sicherung erinnert.
 * Reine Rechenfunktion (ohne Android), damit sie testbar ist.
 */
object BackupReminder {
    /** So viele neue Partien seit der letzten Sicherung lösen eine Erinnerung aus. */
    const val NEW_GAMES_THRESHOLD = 5

    /** Nach so vielen Tagen erinnert die App, sofern es neue Partien gibt. */
    const val MAX_AGE_DAYS = 30

    /** "Später" blendet die Erinnerung so viele Tage aus. */
    const val SNOOZE_DAYS = 7

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun snoozeUntil(now: Long): Long = now + SNOOZE_DAYS * DAY_MS

    /**
     * @param lastBackupAt Zeitpunkt der letzten Sicherung (0 = noch nie)
     * @param gamesAtBackup gespeicherte Partien zum Zeitpunkt dieser Sicherung
     * @param gameCount aktuell gespeicherte Partien
     * @param snoozedUntil bis wann der Nutzer "Später" gewählt hat (0 = nicht)
     */
    fun shouldRemind(
        now: Long,
        lastBackupAt: Long,
        gamesAtBackup: Int,
        gameCount: Int,
        snoozedUntil: Long
    ): Boolean {
        if (gameCount <= 0) return false
        if (now < snoozedUntil) return false
        if (lastBackupAt <= 0L) return true
        val newGames = gameCount - gamesAtBackup
        if (newGames >= NEW_GAMES_THRESHOLD) return true
        val ageDays = (now - lastBackupAt) / DAY_MS
        return newGames > 0 && ageDays >= MAX_AGE_DAYS
    }
}
