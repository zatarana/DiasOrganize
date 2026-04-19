package com.lifeflow.pro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.backup.BackupPreview
import com.lifeflow.pro.backup.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreenContent(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        uri?.let(viewModel::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::inspectBackup)
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let(viewModel::setDestinationTree)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup e restauração") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backup manual")
                    Text("Exporte um arquivo .lfpbak com banco compactado, checksum e metadados para conferência.")
                    Button(onClick = {
                        val name = "lifeflow_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.lfpbak"
                        exportLauncher.launch(name)
                    }, enabled = !uiState.isBusy) {
                        Text("Exportar backup")
                    }
                    Button(onClick = { importLauncher.launch(arrayOf("*/*")) }, enabled = !uiState.isBusy) {
                        Text("Importar backup")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backup automático semanal")
                    Text(uiState.destinationTreeUri?.let { "Destino salvo: $it" } ?: "Nenhuma pasta selecionada ainda.")
                    Button(onClick = { treeLauncher.launch(null) }, enabled = !uiState.isBusy) {
                        Text("Escolher pasta de destino")
                    }
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ativar semanalmente", modifier = Modifier.weight(1f))
                        Switch(
                            checked = uiState.autoBackupEnabled,
                            onCheckedChange = viewModel::setAutoBackupEnabled,
                        )
                    }
                }
            }

            uiState.lastMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(message, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    uiState.pendingPreview?.let { preview ->
        RestorePreviewDialog(
            preview = preview,
            onDismiss = viewModel::dismissPreview,
            onConfirm = viewModel::confirmRestore,
        )
    }
}

@Composable
private fun RestorePreviewDialog(
    preview: BackupPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm) { Text("Restaurar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.sourceName?.let { Text("Arquivo: $it") }
                Text("Este backup contém ${preview.taskCount} tarefas, ${preview.transactionCount} transações e ${preview.debtCount} dívidas.")
                Text("Criado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(preview.createdAtEpochMillis))}")
                Text("Schema: v${preview.schemaVersion}")
                Text("Checksum: ${preview.checksum.take(12)}...")
            }
        },
    )
}
