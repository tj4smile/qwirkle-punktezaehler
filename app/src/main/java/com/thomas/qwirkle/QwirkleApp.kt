package com.thomas.qwirkle

import android.app.Application
import com.thomas.qwirkle.data.database.AppDatabase
import com.thomas.qwirkle.data.repository.BackupRepository
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.data.settings.AppSettings

/** Manuelle Dependency Injection: Datenbank, Repositories und Einstellungen werden einmal erzeugt. */
class QwirkleApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: GameRepository by lazy {
        GameRepository(
            database.playerDao(),
            database.gameHistoryDao(),
            database.gamePlayerDao(),
            database.roundDao(),
            database.roundScoreDao()
        )
    }

    val backupRepository: BackupRepository by lazy { BackupRepository(database.backupDao()) }

    val settings: AppSettings by lazy { AppSettings(this) }
}
