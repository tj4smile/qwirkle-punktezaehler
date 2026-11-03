package com.thomas.qwirkle.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thomas.qwirkle.data.dao.BackupDao
import com.thomas.qwirkle.data.dao.GameHistoryDao
import com.thomas.qwirkle.data.dao.GamePlayerDao
import com.thomas.qwirkle.data.dao.PlayerDao
import com.thomas.qwirkle.data.dao.RoundDao
import com.thomas.qwirkle.data.dao.RoundScoreDao
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore

/** Aktuelle Datenbank-Version. Bei jeder Schema-Änderung erhöhen (siehe Migrations.kt). */
const val DATABASE_VERSION = 1

@Database(
    entities = [
        Player::class,
        GameHistory::class,
        GamePlayer::class,
        Round::class,
        RoundScore::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun gamePlayerDao(): GamePlayerDao
    abstract fun roundDao(): RoundDao
    abstract fun roundScoreDao(): RoundScoreDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qwirkle.db"
                )
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
