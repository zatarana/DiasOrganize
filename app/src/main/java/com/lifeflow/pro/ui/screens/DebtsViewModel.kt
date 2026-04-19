package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.DatabaseSeeder
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.DebtInstallmentEntity
import com.lifeflow.pro.data.repository.DebtBundle
import com.lifeflow.pro.data.repository.DebtRepository
import com.lifeflow.pro.domain.model.DebtConstants
import com.lifeflow.pro.domain.model.DebtListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val databaseSeeder: DatabaseSeeder,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(DebtConstants.TAB_OPEN)
    private val editor = MutableStateFlow(DebtEditorState())
    private val isEditorVisible = MutableStateFlow(false)
    private val settlementDialog = MutableStateFlow<SettleDebtDialogState?>(null)
    private val negotiationDialog = MutableStateFlow<NegotiateDebtDialogState?>(null)
    private val installmentDialog = MutableStateFlow<InstallmentPaymentDialogState?>(null)
    private var currentBundle: DebtBundle = DebtBundle(emptyList(), emptyList(), emptyList())

    init {
        viewModelScope.launch { databaseSeeder.seedIfNeeded() }
    }

    val uiState: StateFlow<DebtsUiState> = combine(
        debtRepository.observeDebtBundle(),
        selectedTab,
        editor,
        isEditorVisible,
        settlementDialog,
        negotiationDialog,
        installmentDialog,
    ) { bundle, tab, editorState, editorVisible, settleState, negotiateState, installmentState ->
        currentBundle = bundle
        val items = bundle.debts.map { debt ->
            DebtListItem(
                debt = debt,
                installments = bundle.installments.filter { it.debtId == debt.id }.sortedBy { it.installmentNumber },
            )
        }.sortedBy { it.debt.originDate }
        DebtsUiState(
            selectedTab = tab,
            accounts = bundle.accounts,
            openDebts = items.filter { it.debt.status == DebtConstants.STATUS_OPEN },
            paymentDebts = items.filter { it.debt.status == DebtConstants.STATUS_IN_PAYMENT },
            settledDebts = items.filter { it.debt.status == DebtConstants.STATUS_SETTLED },
            openTotal = items.filter { it.debt.status == DebtConstants.STATUS_OPEN }.sumOf { it.debt.originalValue },
            settledTotal = items.filter { it.debt.status == DebtConstants.STATUS_SETTLED }.sumOf { it.debt.originalValue },
            settledEconomy = items.filter { it.debt.status == DebtConstants.STATUS_SETTLED }.sumOf { it.debt.totalEconomy },
            editor = editorState,
            isEditorVisible = editorVisible,
            settlementDialog = settleState,
            negotiationDialog = negotiateState,
            installmentDialog = installmentState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsUiState())

    fun selectTab(tab: Int) {
        selectedTab.value = tab
    }

    fun showCreate() {
        editor.value = DebtEditorState(originDate = LocalDate.now().toString())
        isEditorVisible.value = true
    }

    fun showEdit(debtId: Long) {
        val debt = currentBundle.debts.firstOrNull { it.id == debtId } ?: return
        editor.value = DebtEditorState.fromEntity(debt)
        isEditorVisible.value = true
    }

    fun dismissEditor() {
        isEditorVisible.value = false
    }

    fun updateEditor(state: DebtEditorState) {
        editor.value = state
    }

    fun saveDebt() {
        val state = editor.value
        val originalValue = state.originalValue.toDoubleOrNull() ?: return
        if (state.creditor.isBlank() || originalValue <= 0.0) return
        viewModelScope.launch {
            val entity = DebtEntity(
                id = state.id,
                creditor = state.creditor,
                description = state.description.ifBlank { null },
                originalValue = originalValue,
                negotiatedValue = state.negotiatedValue.toDoubleOrNull(),
                originDate = state.originDate,
                status = state.status,
                totalEconomy = state.totalEconomy.toDoubleOrNull() ?: 0.0,
                createdAt = state.createdAt ?: System.currentTimeMillis(),
            )
            if (state.id == 0L) debtRepository.saveDebt(entity) else debtRepository.updateDebt(entity)
            isEditorVisible.value = false
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch { debtRepository.deleteDebt(debtId) }
    }

    fun showSettleDebt(debtId: Long) {
        val debt = currentBundle.debts.firstOrNull { it.id == debtId } ?: return
        settlementDialog.value = SettleDebtDialogState(
            debtId = debtId,
            creditor = debt.creditor,
            finalValue = debt.originalValue.toString(),
            paymentDate = LocalDate.now().toString(),
            accountId = currentBundle.accounts.firstOrNull()?.id,
        )
    }

    fun dismissSettleDebt() {
        settlementDialog.value = null
    }

    fun updateSettleDebt(state: SettleDebtDialogState) {
        settlementDialog.value = state
    }

    fun confirmSettleDebt() {
        val state = settlementDialog.value ?: return
        val finalValue = state.finalValue.toDoubleOrNull() ?: return
        val accountId = state.accountId ?: return
        viewModelScope.launch {
            debtRepository.settleDebt(
                debtId = state.debtId,
                finalValue = finalValue,
                accountId = accountId,
                paymentDate = state.paymentDate,
            )
            settlementDialog.value = null
            selectedTab.value = DebtConstants.TAB_SETTLED
        }
    }

    fun showNegotiateDebt(debtId: Long) {
        val debt = currentBundle.debts.firstOrNull { it.id == debtId } ?: return
        negotiationDialog.value = NegotiateDebtDialogState(
            debtId = debtId,
            creditor = debt.creditor,
            negotiatedValue = debt.originalValue.toString(),
            firstPaymentDate = LocalDate.now().toString(),
            installmentCount = "3",
            accountId = currentBundle.accounts.firstOrNull()?.id,
        )
    }

    fun dismissNegotiateDebt() {
        negotiationDialog.value = null
    }

    fun updateNegotiateDebt(state: NegotiateDebtDialogState) {
        negotiationDialog.value = state
    }

    fun confirmNegotiateDebt() {
        val state = negotiationDialog.value ?: return
        val negotiatedValue = state.negotiatedValue.toDoubleOrNull() ?: return
        val installmentCount = state.installmentCount.toIntOrNull() ?: return
        val accountId = state.accountId ?: return
        if (installmentCount <= 0 || negotiatedValue <= 0.0) return
        viewModelScope.launch {
            debtRepository.negotiateDebt(
                debtId = state.debtId,
                negotiatedValue = negotiatedValue,
                firstPaymentDate = state.firstPaymentDate,
                installmentCount = installmentCount,
                accountId = accountId,
            )
            negotiationDialog.value = null
            selectedTab.value = DebtConstants.TAB_IN_PAYMENT
        }
    }

    fun showInstallmentPayment(installmentId: Long) {
        val installment = currentBundle.installments.firstOrNull { it.id == installmentId } ?: return
        val debt = currentBundle.debts.firstOrNull { it.id == installment.debtId }
        installmentDialog.value = InstallmentPaymentDialogState(
            installmentId = installmentId,
            debtId = installment.debtId,
            creditor = debt?.creditor.orEmpty(),
            installmentNumber = installment.installmentNumber,
            finalValue = installment.expectedValue.toString(),
            paymentDate = LocalDate.now().toString(),
        )
    }

    fun dismissInstallmentPayment() {
        installmentDialog.value = null
    }

    fun updateInstallmentPayment(state: InstallmentPaymentDialogState) {
        installmentDialog.value = state
    }

    fun confirmInstallmentPayment() {
        val state = installmentDialog.value ?: return
        val finalValue = state.finalValue.toDoubleOrNull() ?: return
        viewModelScope.launch {
            debtRepository.confirmInstallmentPayment(
                installmentId = state.installmentId,
                finalValue = finalValue,
                paymentDate = state.paymentDate,
            )
            installmentDialog.value = null
        }
    }
}

data class DebtsUiState(
    val selectedTab: Int = DebtConstants.TAB_OPEN,
    val accounts: List<AccountEntity> = emptyList(),
    val openDebts: List<DebtListItem> = emptyList(),
    val paymentDebts: List<DebtListItem> = emptyList(),
    val settledDebts: List<DebtListItem> = emptyList(),
    val openTotal: Double = 0.0,
    val settledTotal: Double = 0.0,
    val settledEconomy: Double = 0.0,
    val editor: DebtEditorState = DebtEditorState(),
    val isEditorVisible: Boolean = false,
    val settlementDialog: SettleDebtDialogState? = null,
    val negotiationDialog: NegotiateDebtDialogState? = null,
    val installmentDialog: InstallmentPaymentDialogState? = null,
)

data class DebtEditorState(
    val id: Long = 0,
    val creditor: String = "",
    val description: String = "",
    val originalValue: String = "",
    val originDate: String = LocalDate.now().toString(),
    val negotiatedValue: String = "",
    val status: String = DebtConstants.STATUS_OPEN,
    val totalEconomy: String = "0.0",
    val createdAt: Long? = null,
) {
    companion object {
        fun fromEntity(entity: DebtEntity) = DebtEditorState(
            id = entity.id,
            creditor = entity.creditor,
            description = entity.description.orEmpty(),
            originalValue = entity.originalValue.toString(),
            originDate = entity.originDate,
            negotiatedValue = entity.negotiatedValue?.toString().orEmpty(),
            status = entity.status,
            totalEconomy = entity.totalEconomy.toString(),
            createdAt = entity.createdAt,
        )
    }
}

data class SettleDebtDialogState(
    val debtId: Long,
    val creditor: String,
    val finalValue: String,
    val paymentDate: String,
    val accountId: Long?,
)

data class NegotiateDebtDialogState(
    val debtId: Long,
    val creditor: String,
    val negotiatedValue: String,
    val firstPaymentDate: String,
    val installmentCount: String,
    val accountId: Long?,
)

data class InstallmentPaymentDialogState(
    val installmentId: Long,
    val debtId: Long,
    val creditor: String,
    val installmentNumber: Int,
    val finalValue: String,
    val paymentDate: String,
)
