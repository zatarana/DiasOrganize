package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import com.lifeflow.pro.domain.model.BudgetProgress
import com.lifeflow.pro.domain.model.BudgetStatus
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.TransactionWithRelations
import com.lifeflow.pro.domain.model.isoDateToBr
import com.lifeflow.pro.domain.model.toCurrencyBr
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.ui.navigation.SelectionCoordinator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
    paddingValues: PaddingValues,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by SelectionCoordinator.target.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        val currentTarget = target
        if (currentTarget is AppSelectionTarget.Transaction) {
            viewModel.focusTransaction(currentTarget.transactionId)
            SelectionCoordinator.clear()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Nova transação")
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Finanças", style = MaterialTheme.typography.headlineMedium)
                    Text("Saldo real: ${uiState.realBalance.toCurrencyBr()}", style = MaterialTheme.typography.titleMedium)
                    Text("Saldo previsto: ${uiState.forecastBalance.toCurrencyBr()}", style = MaterialTheme.typography.bodyLarge)
                    Text("Mês: ${uiState.monthYear}", style = MaterialTheme.typography.bodySmall)
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedAccountId == null,
                    onClick = { viewModel.selectAccount(null) },
                    label = { Text("Todas") },
                )
                uiState.accounts.forEach { account ->
                    FilterChip(
                        selected = uiState.selectedAccountId == account.id,
                        onClick = { viewModel.selectAccount(account.id) },
                        label = { Text(account.name) },
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::previousMonth, label = { Text("Mês anterior") })
                AssistChip(onClick = viewModel::nextMonth, label = { Text("Próximo mês") })
                if (uiState.categoryFilterName != null) {
                    AssistChip(onClick = { viewModel.selectCategory(null) }, label = { Text("Categoria: ${uiState.categoryFilterName} ✕") })
                }
            }

            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(selected = uiState.selectedTab == FinanceConstants.TAB_TRANSACTIONS, onClick = { viewModel.selectTab(FinanceConstants.TAB_TRANSACTIONS) }, text = { Text("Transações") })
                Tab(selected = uiState.selectedTab == FinanceConstants.TAB_FIXED, onClick = { viewModel.selectTab(FinanceConstants.TAB_FIXED) }, text = { Text("Contas fixas") })
                Tab(selected = uiState.selectedTab == FinanceConstants.TAB_BUDGETS, onClick = { viewModel.selectTab(FinanceConstants.TAB_BUDGETS) }, text = { Text("Orçamentos") })
            }

            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    FinanceConstants.TAB_TRANSACTIONS -> TransactionsTab(
                        items = uiState.visibleTransactions,
                        categories = uiState.filteredCategories,
                        onCategoryClick = viewModel::selectCategory,
                        onEdit = viewModel::showEditTransaction,
                        onDelete = viewModel::deleteTransaction,
                        onRequestPay = viewModel::requestConfirmTransaction,
                    )
                    FinanceConstants.TAB_FIXED -> FixedTransactionsTab(
                        items = uiState.recurringTransactions,
                        onGenerate = viewModel::generateRecurringForNextMonth,
                        onRequestPay = viewModel::requestConfirmTransaction,
                    )
                    else -> BudgetsTab(
                        items = uiState.budgetProgress,
                        onCreate = viewModel::showCreateBudget,
                        onEdit = viewModel::showEditBudget,
                        onDelete = viewModel::deleteBudget,
                        onCopyPrevious = viewModel::copyPreviousMonthBudgets,
                    )
                }
            }
        }
    }

    if (uiState.isTransactionEditorVisible) {
        TransactionEditorDialog(
            state = uiState.transactionEditor,
            accounts = uiState.accounts,
            categories = uiState.availableCategoriesForEditor,
            onDismiss = viewModel::dismissTransactionEditor,
            onChange = viewModel::updateTransactionEditor,
            onSave = viewModel::saveTransaction,
        )
    }

    if (uiState.paymentDialog != null) {
        PaymentConfirmationDialog(
            state = uiState.paymentDialog,
            onDismiss = viewModel::dismissPaymentDialog,
            onConfirm = viewModel::confirmTransactionPayment,
        )
    }

    if (uiState.isBudgetEditorVisible) {
        BudgetEditorDialog(
            state = uiState.budgetEditor,
            categories = uiState.expenseCategories,
            onDismiss = viewModel::dismissBudgetEditor,
            onChange = viewModel::updateBudgetEditor,
            onSave = viewModel::saveBudget,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionsTab(
    items: List<TransactionWithRelations>,
    categories: List<CategoryEntity>,
    onCategoryClick: (Long?) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onRequestPay: (TransactionEntity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (categories.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onCategoryClick(null) }, label = { Text("Todas categorias") })
                categories.forEach { category ->
                    AssistChip(onClick = { onCategoryClick(category.id) }, label = { Text(category.name) })
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.transaction.id }) { item ->
                TransactionCard(item = item, onEdit = { onEdit(item.transaction) }, onDelete = { onDelete(item.transaction) }, onRequestPay = { onRequestPay(item.transaction) })
            }
        }
    }
}

@Composable
private fun FixedTransactionsTab(
    items: List<TransactionWithRelations>,
    onGenerate: () -> Unit,
    onRequestPay: (TransactionEntity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGenerate) { Text("Gerar próximo mês") }
        }
        if (items.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nenhuma conta fixa cadastrada.")
                    Text("Use recorrência mensal ao criar uma despesa para ela aparecer aqui.")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.transaction.id }) { item ->
                    TransactionCard(item = item, onEdit = {}, onDelete = {}, onRequestPay = { onRequestPay(item.transaction) })
                }
            }
        }
    }
}

@Composable
private fun BudgetsTab(
    items: List<BudgetProgress>,
    onCreate: () -> Unit,
    onEdit: (BudgetEntity) -> Unit,
    onDelete: (BudgetEntity) -> Unit,
    onCopyPrevious: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate) { Text("Novo orçamento") }
            TextButton(onClick = onCopyPrevious) { Text("Copiar do mês anterior") }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.budget.id }) { item ->
                val progress = item.usedPercentage.coerceIn(0f, 1f)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.category?.name ?: "Categoria", style = MaterialTheme.typography.titleMedium)
                            Text(item.budget.plannedValue.toCurrencyBr())
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("Gasto atual: ${item.spentValue.toCurrencyBr()}")
                        Text(
                            when (item.status) {
                                BudgetStatus.OK -> "Abaixo de 70%"
                                BudgetStatus.WARNING -> "Entre 70% e 99%"
                                BudgetStatus.EXCEEDED -> "Orçamento ultrapassado"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onEdit(item.budget) }) { Text("Editar") }
                            TextButton(onClick = { onDelete(item.budget) }) { Text("Excluir") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    item: TransactionWithRelations,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestPay: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.transaction.description ?: item.category?.name ?: "Transação", style = MaterialTheme.typography.titleMedium)
                    Text("${item.account?.name ?: "Conta"} • ${item.category?.name ?: "Sem categoria"}")
                }
                Text((item.transaction.finalValue ?: item.transaction.expectedValue).toCurrencyBr())
            }
            Text("Data: ${item.transaction.expectedDate.isoDateToBr()}")
            Text("Status: ${item.transaction.status}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                if (item.transaction.status == FinanceConstants.STATUS_TO_PAY || item.transaction.status == FinanceConstants.STATUS_TO_RECEIVE) {
                    TextButton(onClick = onRequestPay) { Text("Confirmar") }
                }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionEditorDialog(
    state: TransactionEditorState,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onChange: (TransactionEditorState) -> Unit,
    onSave: () -> Unit,
) {
    var accountExpanded by rememberSaveable { mutableStateOf(false) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var targetAccountExpanded by rememberSaveable { mutableStateOf(false) }
    var recurrenceExpanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onSave, enabled = state.isValid()) { Text(if (state.id == 0L) "Salvar" else "Atualizar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.mode == FinanceConstants.TYPE_EXPENSE, onClick = { onChange(state.copy(mode = FinanceConstants.TYPE_EXPENSE, type = FinanceConstants.TYPE_EXPENSE, categoryId = null, transferTargetAccountId = null)) }, label = { Text("Despesa") })
                    FilterChip(selected = state.mode == FinanceConstants.TYPE_INCOME, onClick = { onChange(state.copy(mode = FinanceConstants.TYPE_INCOME, type = FinanceConstants.TYPE_INCOME, categoryId = null, transferTargetAccountId = null)) }, label = { Text("Receita") })
                    FilterChip(selected = state.mode == FinanceConstants.TYPE_TRANSFER, onClick = { onChange(state.copy(mode = FinanceConstants.TYPE_TRANSFER, type = FinanceConstants.TYPE_EXPENSE, categoryId = null)) }, label = { Text("Transferência") }, leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) })
                }
                OutlinedTextField(value = state.amount, onValueChange = { onChange(state.copy(amount = it)) }, label = { Text("Valor") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.description, onValueChange = { onChange(state.copy(description = it)) }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.expectedDate, onValueChange = { onChange(state.copy(expectedDate = it)) }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                    OutlinedTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true, value = accounts.firstOrNull { it.id == state.accountId }?.name ?: "Selecione a conta", onValueChange = {}, label = { Text("Conta") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) })
                    androidx.compose.material3.ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.name) }, onClick = { onChange(state.copy(accountId = account.id)); accountExpanded = false })
                        }
                    }
                }

                if (state.mode == FinanceConstants.TYPE_TRANSFER) {
                    ExposedDropdownMenuBox(expanded = targetAccountExpanded, onExpandedChange = { targetAccountExpanded = !targetAccountExpanded }) {
                        OutlinedTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true, value = accounts.firstOrNull { it.id == state.transferTargetAccountId }?.name ?: "Conta de destino", onValueChange = {}, label = { Text("Destino") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetAccountExpanded) })
                        androidx.compose.material3.ExposedDropdownMenu(expanded = targetAccountExpanded, onDismissRequest = { targetAccountExpanded = false }) {
                            accounts.filter { it.id != state.accountId }.forEach { account ->
                                DropdownMenuItem(text = { Text(account.name) }, onClick = { onChange(state.copy(transferTargetAccountId = account.id)); targetAccountExpanded = false })
                            }
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                        OutlinedTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true, value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "Selecione a categoria", onValueChange = {}, label = { Text("Categoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) })
                        androidx.compose.material3.ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(text = { Text(category.name) }, onClick = { onChange(state.copy(categoryId = category.id)); categoryExpanded = false })
                            }
                        }
                    }
                }

                if (state.mode != FinanceConstants.TYPE_TRANSFER) {
                    ExposedDropdownMenuBox(expanded = recurrenceExpanded, onExpandedChange = { recurrenceExpanded = !recurrenceExpanded }) {
                        OutlinedTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true, value = state.recurrenceType, onValueChange = {}, label = { Text("Recorrência") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) })
                        androidx.compose.material3.ExposedDropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                            listOf(FinanceConstants.RECURRENCE_NONE, FinanceConstants.RECURRENCE_MONTHLY).forEach { recurrence ->
                                DropdownMenuItem(text = { Text(recurrence) }, onClick = { onChange(state.copy(recurrenceType = recurrence)); recurrenceExpanded = false })
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PaymentConfirmationDialog(
    state: PaymentDialogState,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var value by rememberSaveable(state.transactionId) { mutableStateOf(state.finalValue) }
    var date by rememberSaveable(state.transactionId) { mutableStateOf(state.paymentDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onConfirm(value, date) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Valor final") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Valor final pago/recebido") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data do pagamento") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorDialog(
    state: BudgetEditorState,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onChange: (BudgetEditorState) -> Unit,
    onSave: () -> Unit,
) {
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onSave, enabled = state.categoryId != null && state.plannedValue.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true, value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "Categoria", onValueChange = {}, label = { Text("Categoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) })
                    androidx.compose.material3.ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = { onChange(state.copy(categoryId = category.id)); categoryExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = state.plannedValue, onValueChange = { onChange(state.copy(plannedValue = it)) }, label = { Text("Valor planejado") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
