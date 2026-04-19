package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.domain.model.TaskConstants
import com.lifeflow.pro.domain.model.TaskEditorState
import com.lifeflow.pro.domain.model.TaskWithCategory
import com.lifeflow.pro.domain.model.dueDateSummary
import com.lifeflow.pro.domain.model.recurrenceSummary
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.ui.navigation.SelectionCoordinator

@Composable
fun TasksScreen(
    paddingValues: PaddingValues,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by SelectionCoordinator.target.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(target, uiState.tasks) {
        val currentTarget = target
        if (currentTarget is AppSelectionTarget.Task) {
            uiState.tasks.firstOrNull { it.task.id == currentTarget.taskId }?.let { item ->
                viewModel.focusTask(currentTarget.taskId)
                SelectionCoordinator.clear()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreate) {
                Icon(Icons.Default.Add, contentDescription = "Nova tarefa")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Tarefas", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = if (uiState.streak >= 3) "Streak atual: ${uiState.streak} dias 🔥" else "Conclua tarefas por alguns dias seguidos para ativar o streak.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.filters.forEach { filter ->
                            FilterChip(
                                selected = filter.key == uiState.selectedFilter,
                                onClick = { viewModel.selectFilter(filter.key) },
                                label = { Text(filter.label) },
                            )
                        }
                    }
                }
                item { Box(modifier = Modifier.height(8.dp)) }
                if (uiState.tasks.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Nenhuma tarefa encontrada", style = MaterialTheme.typography.titleMedium)
                                Text("Crie sua primeira tarefa pelo botão +.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(uiState.tasks, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        onToggleDone = { viewModel.toggleTaskDone(item.task) },
                        onEdit = { viewModel.showEdit(item.task) },
                        onDelete = {
                            viewModel.deleteTask(item.task)
                        },
                    )
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        TaskEditorDialog(
            state = uiState.editor,
            categories = uiState.categories,
            isEditing = uiState.isEditing,
            onDismiss = viewModel::dismissEditor,
            onStateChange = viewModel::updateEditor,
            onSave = viewModel::saveTask,
        )
    }

    LaunchedEffect(uiState.tasks.size) {
        // reservado para snackbar/undo na próxima iteração do módulo
    }
}

@Composable
private fun TaskCard(
    item: TaskWithCategory,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val task = item.task
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onEdit,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.status == TaskConstants.STATUS_COMPLETED, onCheckedChange = { onToggleDone() })
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Text(dueDateSummary(task.dueDate, task.dueTime), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityDot(task.priority)
                AssistChip(onClick = onEdit, label = { Text(item.category?.name ?: "Sem categoria") })
                if (task.recurrenceType != TaskConstants.RECURRENCE_NONE) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text(recurrenceSummary(task.recurrenceType, task.recurrenceConfig)) },
                        leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                    )
                }
            }
            if (!task.description.isNullOrBlank()) {
                Text(task.description ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PriorityDot(priority: String) {
    val color = when (priority) {
        TaskConstants.PRIORITY_HIGH -> Color(0xFFD32F2F)
        TaskConstants.PRIORITY_MEDIUM -> Color(0xFFF9A825)
        else -> Color(0xFF388E3C)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier
            .background(color, CircleShape)
            .height(10.dp)
            .width(10.dp))
        Text(priority.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    state: TaskEditorState,
    categories: List<CategoryEntity>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onStateChange: ((TaskEditorState) -> TaskEditorState) -> Unit,
    onSave: () -> Unit,
) {
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var priorityExpanded by rememberSaveable { mutableStateOf(false) }
    var recurrenceExpanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onSave, enabled = state.title.isNotBlank()) { Text(if (isEditing) "Salvar" else "Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.title,
                    onValueChange = { value -> onStateChange { it.copy(title = value) } },
                    label = { Text("Título") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.description,
                    onValueChange = { value -> onStateChange { it.copy(description = value) } },
                    label = { Text("Descrição") },
                    minLines = 2,
                )
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "Sem categoria",
                        onValueChange = {},
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DropdownMenuItem(text = { Text("Sem categoria") }, onClick = {
                            onStateChange { it.copy(categoryId = null) }
                            categoryExpanded = false
                        })
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                onStateChange { it.copy(categoryId = category.id) }
                                categoryExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.dueDate,
                    onValueChange = { value -> onStateChange { it.copy(dueDate = value) } },
                    label = { Text("Data (AAAA-MM-DD)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.dueTime,
                    onValueChange = { value -> onStateChange { it.copy(dueTime = value) } },
                    label = { Text("Hora (HH:MM)") },
                    singleLine = true,
                )
                ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = !priorityExpanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = state.priority,
                        onValueChange = {},
                        label = { Text("Prioridade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    )
                    ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        listOf(TaskConstants.PRIORITY_LOW, TaskConstants.PRIORITY_MEDIUM, TaskConstants.PRIORITY_HIGH).forEach { priority ->
                            DropdownMenuItem(text = { Text(priority) }, onClick = {
                                onStateChange { it.copy(priority = priority) }
                                priorityExpanded = false
                            })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = recurrenceExpanded, onExpandedChange = { recurrenceExpanded = !recurrenceExpanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = state.recurrenceType,
                        onValueChange = {},
                        label = { Text("Recorrência") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                    )
                    ExposedDropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                        listOf(
                            TaskConstants.RECURRENCE_NONE,
                            TaskConstants.RECURRENCE_DAILY,
                            TaskConstants.RECURRENCE_WEEKLY,
                            TaskConstants.RECURRENCE_MONTHLY,
                            TaskConstants.RECURRENCE_CUSTOM,
                        ).forEach { recurrence ->
                            DropdownMenuItem(text = { Text(recurrence) }, onClick = {
                                onStateChange { it.copy(recurrenceType = recurrence) }
                                recurrenceExpanded = false
                            })
                        }
                    }
                }
                if (state.recurrenceType == TaskConstants.RECURRENCE_CUSTOM) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.recurrenceConfig,
                        onValueChange = { value -> onStateChange { it.copy(recurrenceConfig = value) } },
                        label = { Text("Configuração personalizada") },
                        supportingText = { Text("Ex.: interval:15 ou days:MONDAY,WEDNESDAY") },
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.linkedTransactionId?.toString().orEmpty(),
                    onValueChange = { value -> onStateChange { it.copy(linkedTransactionId = value.toLongOrNull()) } },
                    label = { Text("Transação vinculada (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        title = { Text(if (isEditing) "Editar tarefa" else "Nova tarefa") },
    )
}
