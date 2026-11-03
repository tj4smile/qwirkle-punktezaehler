package com.thomas.qwirkle.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft, dass eine Datenbank der Version 1 (aus schemas/.../1.json) mit der aktuellen
 * App geöffnet werden kann und ihre Daten behält.
 *
 * Bei jeder neuen Version hier einen Test "n -> n+1" ergänzen
 * (helper.runMigrationsAndValidate(DB_NAME, n+1, true, Migrations.MIGRATION_n_n+1)).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    @org.junit.Ignore("Room-Testhelfer vs. kotlinx-serialization, später klären")
    @Test
    fun version1_opensWithCurrentApp_andKeepsData() = runBlocking {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO players (id, name, colorIndex, isActive) VALUES (1, 'Anna', 0, 1)")
            execSQL(
                "INSERT INTO game_history (id, date, playerCount, totalPoints, totalQwirkles, " +
                    "isTestGame, isFinished, endBonusPlayerId) " +
                    "VALUES (10, 1700000000000, 2, 59, 3, 0, 1, NULL)"
            )
            close()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .build()
        helper.closeWhenFinished(db)

        val snapshot = db.backupDao().snapshot()
        assertEquals(listOf("Anna"), snapshot.players.map { it.name })
        assertEquals(59, snapshot.games.single().totalPoints)
    }
}
