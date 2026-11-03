package com.thomas.qwirkle.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Einfache App-Einstellungen in SharedPreferences.
 *
 * darkModePreferred: Wunsch des Nutzers für das dunkle Design. Wirksam ist er nur,
 * wenn das System hell ist. Ist das System dunkel, ist die App immer dunkel
 * (siehe MainActivity).
 *
 * Sicherungs-Erinnerung: lastBackupAt (0 = noch nie), gamesAtBackup (gespeicherte
 * Partien zu diesem Zeitpunkt) und backupSnoozedUntil ("Später" gewählt).
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _darkModePreferred = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val darkModePreferred: StateFlow<Boolean> = _darkModePreferred

    fun setDarkModePreferred(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
        _darkModePreferred.value = value
    }

    private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L))
    val lastBackupAt: StateFlow<Long> = _lastBackupAt

    private val _gamesAtBackup = MutableStateFlow(prefs.getInt(KEY_GAMES_AT_BACKUP, 0))
    val gamesAtBackup: StateFlow<Int> = _gamesAtBackup

    private val _backupSnoozedUntil = MutableStateFlow(prefs.getLong(KEY_SNOOZE, 0L))
    val backupSnoozedUntil: StateFlow<Long> = _backupSnoozedUntil

    /** Merkt eine erfolgreiche Sicherung (oder Wiederherstellung). */
    fun recordBackup(time: Long, gameCount: Int) {
        prefs.edit()
            .putLong(KEY_LAST_BACKUP, time)
            .putInt(KEY_GAMES_AT_BACKUP, gameCount)
            .putLong(KEY_SNOOZE, 0L)
            .apply()
        _lastBackupAt.value = time
        _gamesAtBackup.value = gameCount
        _backupSnoozedUntil.value = 0L
    }

    fun snoozeBackupReminder(until: Long) {
        prefs.edit().putLong(KEY_SNOOZE, until).apply()
        _backupSnoozedUntil.value = until
    }

    private companion object {
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_GAMES_AT_BACKUP = "games_at_backup"
        const val KEY_SNOOZE = "backup_snoozed_until"
        const val PREFS_NAME = "qwirkle_settings"
        const val KEY_DARK_MODE = "dark_mode_preferred"
    }
}
