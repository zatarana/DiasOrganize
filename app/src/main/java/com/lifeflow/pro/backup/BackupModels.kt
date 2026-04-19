package com.lifeflow.pro.backup

import android.net.Uri

data class BackupPreview(
    val createdAtEpochMillis: Long,
    val schemaVersion: Int,
    val taskCount: Int,
    val transactionCount: Int,
    val debtCount: Int,
    val checksum: String,
    val sourceName: String? = null,
)

data class BackupUiState(
    val isBusy: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val destinationTreeUri: String? = null,
    val pendingPreview: BackupPreview? = null,
    val pendingImportUri: Uri? = null,
    val lastMessage: String? = null,
)
