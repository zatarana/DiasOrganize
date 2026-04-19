package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.domain.model.GoalProgress
import com.lifeflow.pro.domain.model.toCurrencyBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Metas") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreate) {
                Icon(Icons.Default.Add, contentDescription = "Nova meta")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.goals, key = { it.goal.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${item.goal.icon} ${item.goal.name}", style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                            Text("${item.goal.currentValue.toCurrencyBr()} de ${item.goal.targetValue.toCurrencyBr()}")
                            Text("Status: ${item.goal.status}")
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.showEdit(item.goal.id) }) { Text("Editar") }
                                TextButton(onClick = { viewModel.deleteGoal(item.goal.id) }) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        GoalEditorDialog(
            state = uiState.editor,
            onDismiss = viewModel::dismissEditor,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveGoal,
        )
    }
}

@Composable
private fun GoalEditorDialog(
    state: GoalEditorState,
    onDismiss: () -> Unit,
    onChange: (GoalEditorState) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onSave, enabled = state.name.isNotBlank() && state.targetValue.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.name, onValueChange = { onChange(state.copy(name = it)) }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.icon, onValueChange = { onChange(state.copy(icon = it)) }, label = { Text("Emoji") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.targetValue, onValueChange = { onChange(state.copy(targetValue = it)) }, label = { Text("Valor alvo") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.currentValue, onValueChange = { onChange(state.copy(currentValue = it)) }, label = { Text("Valor atual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.targetDate, onValueChange = { onChange(state.copy(targetDate = it)) }, label = { Text("Data alvo (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
