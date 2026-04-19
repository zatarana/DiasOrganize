package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.domain.model.DebtConstants
import com.lifeflow.pro.domain.model.DebtListItem
import com.lifeflow.pro.domain.model.isoDateToBr
import com.lifeflow.pro.domain.model.toCurrencyBr
import com.lifeflow.pro.domain.model.toPercentLabel
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.ui.navigation.SelectionCoordinator

@Composable
fun DebtsScreen(
    paddingValues: PaddingValues,
    viewModel: DebtsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by SelectionCoordinator.target.collectAsStateWithLifecycle()

    LaunchedEffect(target, uiState.openDebts, uiState.paymentDebts, uiState.settledDebts) {
        val currentTarget = target
        if (currentTarget is AppSelectionTarget.Debt) {
            when {
                uiState.openDebts.any { it.debt.id == currentTarget.debtId } -> {
                    viewModel.selectTab(DebtConstants.TAB_OPEN)
                    viewModel.showEdit(currentTarget.debtId)
                }
                uiState.paymentDebts.any { it.debt.id == currentTarget.debtId } -> viewModel.selectTab(DebtConstants.TAB_IN_PAYMENT)
                uiState.settledDebts.any { it.debt.id == currentTarget.debtId } -> viewModel.selectTab(DebtConstants.TAB_SETTLED)
            }
            SelectionCoordinator.clear()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreate) {
                Icon(Icons.Default.Add, contentDescription = "Nova dívida")
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
            SummaryCard(uiState = uiState)

            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == DebtConstants.TAB_OPEN,
                    onClick = { viewModel.selectTab(DebtConstants.TAB_OPEN) },
                    text = { Text("Em aberto") },
                )
                Tab(
                    selected = uiState.selectedTab == DebtConstants.TAB_IN_PAYMENT,
                    onClick = { viewModel.selectTab(DebtConstants.TAB_IN_PAYMENT) },
                    text = { Text("Em pagamento") },
                )
                Tab(
                    selected = uiState.selectedTab == DebtConstants.TAB_SETTLED,
                    onClick = { viewModel.selectTab(DebtConstants.TAB_SETTLED) },
                    text = { Text("Quitadas") },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    DebtConstants.TAB_OPEN -> OpenDebtsTab(
                        items = uiState.openDebts,
                        onEdit = viewModel::showEdit,
                        onSettle = viewModel::showSettleDebt,
                        onNegotiate = viewModel::showNegotiateDebt,
                        onDelete = viewModel::deleteDebt,
                    )
                    DebtConstants.TAB_IN_PAYMENT -> PaymentDebtsTab(
                        items = uiState.paymentDebts,
                        onPayInstallment = { item -> item.nextInstallment?.id?.let(viewModel::showInstallmentPayment) },
                        onDelete = viewModel::deleteDebt,
                    )
                    else -> SettledDebtsTab(items = uiState.settledDebts)
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        DebtEditorDialog(
            state = uiState.editor,
            onDismiss = viewModel::dismissEditor,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveDebt,
        )
    }

    uiState.settlementDialog?.let { dialogState ->
        SettleDebtDialog(
            state = dialogState,
            accounts = uiState.accounts,
            onDismiss = viewModel::dismissSettleDebt,
            onChange = viewModel::updateSettleDebt,
            onConfirm = viewModel::confirmSettleDebt,
        )
    }

    uiState.negotiationDialog?.let { dialogState ->
        NegotiateDebtDialog(
            state = dialogState,
            accounts = uiState.accounts,
            onDismiss = viewModel::dismissNegotiateDebt,
            onChange = viewModel::updateNegotiateDebt,
            onConfirm = viewModel::confirmNegotiateDebt,
        )
    }

    uiState.installmentDialog?.let { dialogState ->
        InstallmentPaymentDialog(
            state = dialogState,
            onDismiss = viewModel::dismissInstallmentPayment,
            onChange = viewModel::updateInstallmentPayment,
            onConfirm = viewModel::confirmInstallmentPayment,
        )
    }
}

@Composable
private fun SummaryCard(uiState: DebtsUiState) {
    val title = when (uiState.selectedTab) {
        DebtConstants.TAB_OPEN -> "Você tem ${uiState.openTotal.toCurrencyBr()} em dívidas em aberto"
        DebtConstants.TAB_IN_PAYMENT -> "${uiState.paymentDebts.size} dívidas em pagamento"
        else -> "Você zerou ${uiState.settledTotal.toCurrencyBr()} e economizou ${uiState.settledEconomy.toCurrencyBr()}"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Dívidas", style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun OpenDebtsTab(
    items: List<DebtListItem>,
    onEdit: (Long) -> Unit,
    onSettle: (Long) -> Unit,
    onNegotiate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyDebtState("Nenhuma dívida em aberto cadastrada.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.debt.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.debt.creditor, style = MaterialTheme.typography.titleMedium)
                    item.debt.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    Text("Valor original: ${item.debt.originalValue.toCurrencyBr()}")
                    Text("Data de origem: ${item.debt.originDate.isoDateToBr()}")
                    Text("Há ${item.daysOpen} dias em aberto")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onEdit(item.debt.id) }) { Text("Editar") }
                        TextButton(onClick = { onSettle(item.debt.id) }) { Text("Quitar") }
                        TextButton(onClick = { onNegotiate(item.debt.id) }) { Text("Negociar") }
                        TextButton(onClick = { onDelete(item.debt.id) }) { Text("Excluir") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentDebtsTab(
    items: List<DebtListItem>,
    onPayInstallment: (DebtListItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyDebtState("Nenhuma dívida em pagamento no momento.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.debt.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.debt.creditor, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${item.paidInstallments}/${item.totalInstallments} parcelas pagas • ${item.progress.toPercentLabel()}")
                    Text("Pago: ${item.paidValue.toCurrencyBr()} • Restante: ${item.remainingValue.toCurrencyBr()}")
                    item.nextInstallment?.let {
                        Text("Próxima parcela: ${it.expectedValue.toCurrencyBr()} em ${it.dueDate.isoDateToBr()}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onPayInstallment(item) }, enabled = item.nextInstallment != null) { Text("Registrar parcela") }
                        TextButton(onClick = { onDelete(item.debt.id) }) { Text("Excluir") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettledDebtsTab(items: List<DebtListItem>) {
    if (items.isEmpty()) {
        EmptyDebtState("Nenhuma dívida quitada ainda.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.debt.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.debt.creditor, style = MaterialTheme.typography.titleMedium)
                    Text("Valor original: ${item.debt.originalValue.toCurrencyBr()}")
                    Text("Valor pago: ${item.finalPaidValue.toCurrencyBr()}")
                    Text("Economia gerada: ${item.debt.totalEconomy.toCurrencyBr()}")
                }
            }
        }
    }
}

@Composable
private fun EmptyDebtState(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DebtEditorDialog(
    state: DebtEditorState,
    onDismiss: () -> Unit,
    onChange: (DebtEditorState) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onSave, enabled = state.creditor.isNotBlank() && (state.originalValue.toDoubleOrNull() ?: 0.0) > 0.0) {
                Text("Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.creditor,
                    onValueChange = { onChange(state.copy(creditor = it)) },
                    label = { Text("Credor") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onChange(state.copy(description = it)) },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.originalValue,
                    onValueChange = { onChange(state.copy(originalValue = it)) },
                    label = { Text("Valor original") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.originDate,
                    onValueChange = { onChange(state.copy(originDate = it)) },
                    label = { Text("Data de origem (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun SettleDebtDialog(
    state: SettleDebtDialogState,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onChange: (SettleDebtDialogState) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm, enabled = (state.finalValue.toDoubleOrNull() ?: 0.0) > 0.0 && state.accountId != null) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quitar dívida de ${state.creditor}", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.finalValue,
                    onValueChange = { onChange(state.copy(finalValue = it)) },
                    label = { Text("Valor final pago") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.paymentDate,
                    onValueChange = { onChange(state.copy(paymentDate = it)) },
                    label = { Text("Data do pagamento") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Conta de débito")
                AccountSelector(
                    accounts = accounts,
                    selectedAccountId = state.accountId,
                    onSelect = { onChange(state.copy(accountId = it)) },
                )
            }
        },
    )
}

@Composable
private fun NegotiateDebtDialog(
    state: NegotiateDebtDialogState,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onChange: (NegotiateDebtDialogState) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = (state.negotiatedValue.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    (state.installmentCount.toIntOrNull() ?: 0) > 0 &&
                    state.accountId != null,
            ) { Text("Gerar parcelas") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Negociar dívida de ${state.creditor}", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.negotiatedValue,
                    onValueChange = { onChange(state.copy(negotiatedValue = it)) },
                    label = { Text("Novo valor total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.firstPaymentDate,
                    onValueChange = { onChange(state.copy(firstPaymentDate = it)) },
                    label = { Text("Data da 1ª parcela") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.installmentCount,
                    onValueChange = { onChange(state.copy(installmentCount = it)) },
                    label = { Text("Número de parcelas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Conta de débito")
                AccountSelector(
                    accounts = accounts,
                    selectedAccountId = state.accountId,
                    onSelect = { onChange(state.copy(accountId = it)) },
                )
            }
        },
    )
}

@Composable
private fun InstallmentPaymentDialog(
    state: InstallmentPaymentDialogState,
    onDismiss: () -> Unit,
    onChange: (InstallmentPaymentDialogState) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm, enabled = (state.finalValue.toDoubleOrNull() ?: 0.0) > 0.0) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Parcela ${state.installmentNumber} • ${state.creditor}", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.finalValue,
                    onValueChange = { onChange(state.copy(finalValue = it)) },
                    label = { Text("Valor final pago") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.paymentDate,
                    onValueChange = { onChange(state.copy(paymentDate = it)) },
                    label = { Text("Data do pagamento") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun AccountSelector(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { account ->
            FilterChip(
                selected = selectedAccountId == account.id,
                onClick = { onSelect(account.id) },
                label = { Text("${account.icon} ${account.name}") },
            )
        }
    }
}
