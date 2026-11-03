package com.thomas.qwirkle.data.repository

import com.thomas.qwirkle.data.backup.BackupData
import com.thomas.qwirkle.data.dao.BackupDao

class BackupRepository(private val dao: BackupDao) {

    suspend fun createSnapshot(): BackupData = dao.snapshot()

    /** Ersetzt alle Daten atomar. Bei einem Fehler bleiben die bisherigen Daten unverändert. */
    suspend fun restore(data: BackupData) = dao.replaceAll(data)
}
