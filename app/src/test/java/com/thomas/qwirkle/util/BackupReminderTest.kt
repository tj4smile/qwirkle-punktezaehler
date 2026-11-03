package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReminderTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    @Test fun noGames_noReminder() =
        assertFalse(BackupReminder.shouldRemind(now, 0, 0, 0, 0))

    @Test fun neverBackedUp_withGames_reminds() =
        assertTrue(BackupReminder.shouldRemind(now, 0, 0, 1, 0))

    @Test fun recentBackup_fewNewGames_noReminder() =
        assertFalse(BackupReminder.shouldRemind(now, now - 2 * day, 10, 12, 0))

    @Test fun fiveNewGames_reminds() =
        assertTrue(BackupReminder.shouldRemind(now, now - 2 * day, 10, 15, 0))

    @Test fun oldBackup_withNewGame_reminds() =
        assertTrue(BackupReminder.shouldRemind(now, now - 31 * day, 10, 11, 0))

    @Test fun oldBackup_noNewGames_noReminder() =
        assertFalse(BackupReminder.shouldRemind(now, now - 90 * day, 10, 10, 0))

    @Test fun snoozed_noReminder() =
        assertFalse(BackupReminder.shouldRemind(now, 0, 0, 3, now + day))

    @Test fun snoozeExpired_reminds() =
        assertTrue(BackupReminder.shouldRemind(now, 0, 0, 3, now - 1))

    @Test fun snoozeUntil_addsSevenDays() =
        assertEquals(now + 7 * day, BackupReminder.snoozeUntil(now))
}
