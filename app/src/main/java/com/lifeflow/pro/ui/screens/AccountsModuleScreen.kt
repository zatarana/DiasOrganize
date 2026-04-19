package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.domain.model.toCurrencyBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gerenciar contas") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreate) {
                Icon(Icons.Default.Add, contentDescription = "Nova conta")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.accounts, key = { it.id }) { account ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${account.icon} ${account.name}", style = MaterialTheme.typography.titleMedium)
                            Text("Saldo inicial: ${account.initialBalance.toCurrencyBr()}")
                            Text("Cor: ${account.color}")
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.showEdit(account.id) }) { Text("Editar") }
                                TextButton(onClick = { viewModel.deleteAccount(account.id) }) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        AccountEditorDialog(state = uiState.editor, onDismiss = viewModel::dismissEditor, onChange = viewModel::updateEditor, onSave = viewModel::saveAccount)
    }
}

@Composable
private fun AccountEditorDialog(
    state: AccountEditorState,
    onDismiss: () -> Unit,
    onChange: (AccountEditorState) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onSave, enabled = state.name.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.name, onValueChange = { onChange(state.copy(name = it)) }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.icon, onValueChange = { onChange(state.copy(icon = it)) }, label = { Text("Ícone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.color, onValueChange = { onChange(state.copy(color = it)) }, label = { Text("Cor") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.initialBalance, onValueChange = { onChange(state.copy(initialBalance = it)) }, label = { Text("Saldo inicial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
