package com.thomas.qwirkle.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thomas.qwirkle.QwirkleApp
import com.thomas.qwirkle.data.repository.BackupRepository

@Composable
fun rememberBackupRepository(): BackupRepository {
    val app = LocalContext.current.applicationContext as QwirkleApp
    return app.backupRepository
}
