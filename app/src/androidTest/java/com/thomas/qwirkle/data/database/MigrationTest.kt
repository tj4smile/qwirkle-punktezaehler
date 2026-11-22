package com.thomas.qwirkle.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft, dass eine Datenbank der Version 1 (aus schemas/.../1.json) mit der aktuellen
 * App geöffnet werden kann und ihre Daten behält, und dass die Migration 1 -> 2
 * (neue Spalte qwirkleBonus) korrekt ist. Dafür muss schemas/.../2.json im Projekt liegen
 * (wird beim ersten Bauen von Room erzeugt).
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
        // Bisherige Spiele rechnen weiter mit 6 je Qwirkle
        assertEquals(6, snapshot.games.single().qwirkleBonus)
    }

    @Test
    fun migration1To2_addsQwirkleBonusWithDefaultSix() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO game_history (id, date, playerCount, totalPoints, totalQwirkles, " +
                    "isTestGame, isFinished, endBonusPlayerId) " +
                    "VALUES (10, 1700000000000, 2, 59, 3, 0, 1, NULL)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT qwirkleBonus, totalPoints FROM game_history WHERE id = 10").use {
            assertTrue(it.moveToFirst())
            assertEquals(6, it.getInt(0))
            assertEquals(59, it.getInt(1))
        }
    }

    @Test
    fun migration2To3_givesEveryExistingGameItsOwnUuid() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO game_history (id, date, playerCount, totalPoints, totalQwirkles, " +
                    "isTestGame, isFinished, endBonusPlayerId, qwirkleBonus) " +
                    "VALUES (10, 1700000000000, 2, 59, 3, 0, 1, NULL, 12)"
            )
            execSQL(
                "INSERT INTO game_history (id, date, playerCount, totalPoints, totalQwirkles, " +
                    "isTestGame, isFinished, endBonusPlayerId, qwirkleBonus) " +
                    "VALUES (11, 1700100000000, 3, 70, 2, 0, 1, NULL, 6)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, Migrations.MIGRATION_2_3)

        val uuids = mutableListOf<String>()
        db.query("SELECT uuid, totalPoints, qwirkleBonus FROM game_history ORDER BY id").use {
            while (it.moveToNext()) {
                uuids.add(it.getString(0))
            }
        }
        assertEquals(2, uuids.size)
        assertTrue(uuids.all { it.length == 36 })
        assertTrue(uuids[0] != uuids[1])
        // Alte Werte bleiben unverändert
        db.query("SELECT totalPoints, qwirkleBonus FROM game_history WHERE id = 10").use {
            assertTrue(it.moveToFirst())
            assertEquals(59, it.getInt(0))
            assertEquals(12, it.getInt(1))
        }
    }

    @Test
    fun allMigrations_fromVersion1_reachCurrentVersion() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO game_history (id, date, playerCount, totalPoints, totalQwirkles, " +
                    "isTestGame, isFinished, endBonusPlayerId) " +
                    "VALUES (10, 1700000000000, 2, 59, 3, 0, 1, NULL)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, *Migrations.ALL)

        db.query("SELECT uuid, qwirkleBonus FROM game_history WHERE id = 10").use {
            assertTrue(it.moveToFirst())
            assertEquals(36, it.getString(0).length)
            assertEquals(6, it.getInt(1))
        }
    }
}
