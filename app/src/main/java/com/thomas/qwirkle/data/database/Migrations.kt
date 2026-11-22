package com.thomas.qwirkle.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Alle Datenbank-Migrationen in aufsteigender Reihenfolge.
 *
 * Version 2: neue Spalte game_history.qwirkleBonus (Eingabevariante "Alternativ", 12 je Qwirkle).
 * Alle bisherigen Spiele bekommen 6 und rechnen unverändert weiter.
 *
 * SO FÜHRST DU EINE SCHEMA-ÄNDERUNG DURCH (z. B. neue Spalte in "players"):
 *  1. Entity ändern.
 *  2. DATABASE_VERSION in AppDatabase.kt um 1 erhöhen (1 -> 2).
 *  3. Hier eine Migration ergänzen und in ALL eintragen, zum Beispiel:
 *
 *       val MIGRATION_1_2 = object : Migration(1, 2) {
 *           override fun migrate(db: SupportSQLiteDatabase) {
 *               db.execSQL("ALTER TABLE players ADD COLUMN note TEXT NOT NULL DEFAULT ''")
 *           }
 *       }
 *       val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
 *
 *     (Import: androidx.sqlite.db.SupportSQLiteDatabase)
 *  4. Projekt bauen: Room schreibt schemas/.../2.json. Beide JSON-Dateien
 *     (1.json UND 2.json) im Projekt behalten und mit versionieren.
 *  5. MigrationTest um einen Test 1 -> 2 erweitern.
 *  6. Backup-Format prüfen: Hat sich die Struktur der Daten geändert, muss
 *     BackupJson.FORMAT_VERSION erhöht und das Lesen alter Sicherungen angepasst werden.
 *
 * NIEMALS fallbackToDestructiveMigration() verwenden: das würde bei einem Update
 * alle Spiele der Nutzer löschen.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE game_history ADD COLUMN qwirkleBonus INTEGER NOT NULL DEFAULT 6")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
