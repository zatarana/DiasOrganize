package com.lifeflow.pro.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val backupPreferences: BackupPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val pendingPreview = MutableStateFlow<BackupPreview?>(null)
    private val pendingImportUri = MutableStateFlow<Uri?>(null)
    private val lastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BackupUiState> = combine(
        busy,
        backupPreferences.autoBackupEnabledFlow,
        backupPreferences.destinationTreeUriFlow,
        pendingPreview,
        pendingImportUri,
        lastMessage,
    ) { isBusy, autoEnabled, destinationUri, preview, importUri, message ->
        BackupUiState(
            isBusy = isBusy,
            autoBackupEnabled = autoEnabled,
            destinationTreeUri = destinationUri,
            pendingPreview = preview,
            pendingImportUri = importUri,
            lastMessage = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    fun exportBackup(targetUri: Uri) {
        viewModelScope.launch {
            busy.value = true
            lastMessage.value = null
            runCatching {
                backupManager.exportBackup(targetUri)
            }.onSuccess { preview ->
                lastMessage.value = "Backup exportado: ${preview.taskCount} tarefas, ${preview.transactionCount} transações e ${preview.debtCount} dívidas."
            }.onFailure {
                lastMessage.value = "Falha ao exportar backup: ${it.message}"
            }
            busy.value = false
        }
    }

    fun inspectBackup(sourceUri: Uri) {
        viewModelScope.launch {
            busy.value = true
            lastMessage.value = null
            runCatching {
                backupManager.previewBackup(sourceUri)
            }.onSuccess { preview ->
                pendingPreview.value = preview
                pendingImportUri.value = sourceUri
            }.onFailure {
                lastMessage.value = "Falha ao ler backup: ${it.message}"
            }
            busy.value = false
        }
    }

    fun confirmRestore() {
        val sourceUri = pendingImportUri.value ?: return
        viewModelScope.launch {
            busy.value = true
            runCatching {
                backupManager.restoreBackup(sourceUri)
            }.onSuccess { preview ->
                lastMessage.value = "Backup validado e preparado. Feche e abra o app para aplicar os dados (${preview.taskCount} tarefas, ${preview.transactionCount} transações e ${preview.debtCount} dívidas)."
                pendingPreview.value = null
                pendingImportUri.value = null
            }.onFailure {
                lastMessage.value = "Falha ao restaurar backup: ${it.message}"
            }
            busy.value = false
        }
    }

    fun dismissPreview() {
        pendingPreview.value = null
        pendingImportUri.value = null
    }

    fun setDestinationTree(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            backupPreferences.setDestinationTreeUri(uri.toString())
            if (uiState.value.autoBackupEnabled) scheduleWeeklyBackup(uri.toString())
            lastMessage.value = "Destino semanal salvo com sucesso."
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setAutoBackupEnabled(enabled)
            val treeUri = uiState.value.destinationTreeUri
            if (enabled && treeUri != null) {
                scheduleWeeklyBackup(treeUri)
                lastMessage.value = "Backup semanal ativado."
            } else if (!enabled) {
                WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.UNIQUE_NAME)
                lastMessage.value = "Backup semanal desativado."
            } else {
                lastMessage.value = "Escolha uma pasta antes de ativar o backup semanal."
            }
        }
    }

    private fun scheduleWeeklyBackup(treeUri: String) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setInputData(workDataOf(AutoBackupWorker.KEY_TREE_URI to treeUri))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoBackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
