package com.lifeflow.pro.backup

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val treeUriString = inputData.getString(KEY_TREE_URI) ?: return Result.failure()
        backupManager.exportAutomaticBackupToTree(Uri.parse(treeUriString))
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_NAME = "weekly_auto_backup"
        const val KEY_TREE_URI = "tree_uri"
    }
}
