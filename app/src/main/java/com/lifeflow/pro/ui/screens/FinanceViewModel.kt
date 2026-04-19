package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.DatabaseSeeder
import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import com.lifeflow.pro.data.repository.FinanceBundle
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.domain.model.BudgetProgress
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.FinanceRules
import com.lifeflow.pro.domain.model.FinanceTransaction
import com.lifeflow.pro.domain.model.TransactionStatus
import com.lifeflow.pro.domain.model.TransactionType
import com.lifeflow.pro.domain.model.TransactionWithRelations
import com.lifeflow.pro.domain.model.calculateBudgetProgress
import com.lifeflow.pro.domain.model.currentMonthYear
import com.lifeflow.pro.domain.model.isTransactionPaid
import com.lifeflow.pro.domain.model.nextMonthYear
import com.lifeflow.pro.domain.model.previousMonthYear
import com.lifeflow.pro.domain.model.transactionStatusForType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val databaseSeeder: DatabaseSeeder,
) : ViewModel() {

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val selectedTab = MutableStateFlow(FinanceConstants.TAB_TRANSACTIONS)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val monthYear = MutableStateFlow(currentMonthYear())
    private val transactionEditor = MutableStateFlow(TransactionEditorState())
    private val isTransactionEditorVisible = MutableStateFlow(false)
    private val budgetEditor = MutableStateFlow(BudgetEditorState())
    private val isBudgetEditorVisible = MutableStateFlow(false)
    private val paymentDialog = MutableStateFlow<PaymentDialogState?>(null)
    private var currentBundleSnapshot: FinanceBundle = FinanceBundle(emptyList(), emptyList(), emptyList(), emptyList())

    val uiState: StateFlow<FinanceUiState> = combine(
        financeRepository.observeFinanceBundle(),
        selectedAccountId,
        selectedTab,
        selectedCategoryId,
        monthYear,
        transactionEditor,
        isTransactionEditorVisible,
        budgetEditor,
        isBudgetEditorVisible,
        paymentDialog,
    ) { bundle, accountId, tab, categoryId, month, editor, editorVisible, budget, budgetVisible, payment ->
        buildUiState(bundle, accountId, tab, categoryId, month, editor, editorVisible, budget, budgetVisible, payment)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(),
    )

    init {
        viewModelScope.launch { databaseSeeder.seedIfNeeded() }
    }

    fun selectAccount(accountId: Long?) { selectedAccountId.value = accountId }
    fun selectTab(tab: Int) { selectedTab.value = tab }
    fun selectCategory(categoryId: Long?) { selectedCategoryId.value = categoryId }
    fun previousMonth() { monthYear.value = previousMonthYear(monthYear.value) }
    fun nextMonth() { monthYear.value = nextMonthYear(monthYear.value) }

    fun showCreateTransaction() {
        transactionEditor.value = TransactionEditorState(expectedDate = LocalDate.now().toString())
        isTransactionEditorVisible.value = true
    }

    fun showEditTransaction(transaction: TransactionEntity) {
        transactionEditor.value = TransactionEditorState.fromEntity(transaction)
        isTransactionEditorVisible.value = true
    }

    fun dismissTransactionEditor() { isTransactionEditorVisible.value = false }
    fun updateTransactionEditor(state: TransactionEditorState) { transactionEditor.value = state }

    fun saveTransaction() {
        val editor = transactionEditor.value
        if (!editor.isValid()) return
        viewModelScope.launch {
            if (editor.mode == FinanceConstants.TYPE_TRANSFER) {
                financeRepository.transferBetweenAccounts(
                    fromAccountId = editor.accountId ?: return@launch,
                    toAccountId = editor.transferTargetAccountId ?: return@launch,
                    value = editor.amount.toDoubleOrNull() ?: return@launch,
                    description = editor.description.ifBlank { null },
                    date = editor.expectedDate,
                )
            } else {
                val entity = editor.toEntity()
                if (editor.id == 0L) financeRepository.saveTransaction(entity) else financeRepository.updateTransaction(entity)
            }
            isTransactionEditorVisible.value = false
        }
    }

    fun focusTransaction(transactionId: Long) {
        val transaction = currentBundleSnapshot.transactions.firstOrNull { it.id == transactionId } ?: return
        monthYear.value = transaction.expectedDate.take(7)
        selectedTab.value = FinanceConstants.TAB_TRANSACTIONS
        transactionEditor.value = TransactionEditorState.fromEntity(transaction)
        isTransactionEditorVisible.value = true
    }

    fun requestConfirmTransaction(transaction: TransactionEntity) {
        paymentDialog.value = PaymentDialogState(
            transactionId = transaction.id,
            finalValue = (transaction.finalValue ?: transaction.expectedValue).toString(),
            paymentDate = transaction.paymentDate ?: LocalDate.now().toString(),
        )
    }

    fun dismissPaymentDialog() { paymentDialog.value = null }

    fun confirmTransactionPayment(finalValue: String, paymentDate: String) {
        val dialog = paymentDialog.value ?: return
        viewModelScope.launch {
            financeRepository.confirmTransaction(
                transactionId = dialog.transactionId,
                finalValue = finalValue.toDoubleOrNull() ?: return@launch,
                paymentDate = paymentDate,
            )
            paymentDialog.value = null
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { financeRepository.deleteTransaction(transaction) }
    }

    fun showCreateBudget() {
        budgetEditor.value = BudgetEditorState()
        isBudgetEditorVisible.value = true
    }

    fun showEditBudget(budget: BudgetEntity) {
        budgetEditor.value = BudgetEditorState(id = budget.id, categoryId = budget.categoryId, plannedValue = budget.plannedValue.toString())
        isBudgetEditorVisible.value = true
    }

    fun dismissBudgetEditor() { isBudgetEditorVisible.value = false }
    fun updateBudgetEditor(state: BudgetEditorState) { budgetEditor.value = state }

    fun saveBudget() {
        val editor = budgetEditor.value
        val categoryId = editor.categoryId ?: return
        val planned = editor.plannedValue.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val entity = BudgetEntity(
                id = editor.id,
                categoryId = categoryId,
                monthYear = monthYear.value,
                plannedValue = planned,
                createdAt = System.currentTimeMillis(),
            )
            if (editor.id == 0L) financeRepository.saveBudget(entity) else financeRepository.updateBudget(entity)
            isBudgetEditorVisible.value = false
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch { financeRepository.deleteBudget(budget) }
    }

    fun copyPreviousMonthBudgets() {
        viewModelScope.launch { financeRepository.copyBudgetsFromPreviousMonth(monthYear.value) }
    }

    fun generateRecurringForNextMonth() {
        viewModelScope.launch { financeRepository.generateMonthlyRecurringTransactions(nextMonthYear(monthYear.value)) }
    }

    private fun buildUiState(
        bundle: FinanceBundle,
        accountId: Long?,
        tab: Int,
        categoryId: Long?,
        month: String,
        editor: TransactionEditorState,
        editorVisible: Boolean,
        budgetEditorState: BudgetEditorState,
        budgetEditorVisible: Boolean,
        payment: PaymentDialogState?,
    ): FinanceUiState {
        currentBundleSnapshot = bundle
        val yearMonth = YearMonth.parse(month)
        val allForMonth = bundle.transactions.filter { runCatching { YearMonth.parse(it.expectedDate.substring(0, 7)) == yearMonth }.getOrDefault(false) }
        val accountFiltered = if (accountId == null) allForMonth else allForMonth.filter { it.accountId == accountId }
        val categoryFiltered = if (categoryId == null) accountFiltered else accountFiltered.filter { it.categoryId == categoryId }
        val visibleTransactions = categoryFiltered.map { transaction ->
            TransactionWithRelations(
                transaction = transaction,
                account = bundle.accounts.firstOrNull { it.id == transaction.accountId },
                category = bundle.categories.firstOrNull { it.id == transaction.categoryId },
            )
        }
        val recurringTransactions = visibleTransactions.filter { it.transaction.recurrenceType == FinanceConstants.RECURRENCE_MONTHLY }
        val budgetProgress = bundle.budgets.filter { it.monthYear == month }.map { budget ->
            calculateBudgetProgress(
                budget = budget,
                category = bundle.categories.firstOrNull { it.id == budget.categoryId },
                transactions = accountFiltered,
            )
        }
        val financeTransactions = accountFiltered.map {
            FinanceTransaction(
                type = if (it.type == FinanceConstants.TYPE_INCOME) TransactionType.RECEITA else TransactionType.DESPESA,
                status = when (it.status) {
                    FinanceConstants.STATUS_RECEIVED -> TransactionStatus.RECEBIDO
                    FinanceConstants.STATUS_TO_RECEIVE -> TransactionStatus.A_RECEBER
                    FinanceConstants.STATUS_PAID -> TransactionStatus.PAGO
                    else -> TransactionStatus.A_PAGAR
                },
                expectedValue = it.expectedValue,
                finalValue = it.finalValue,
            )
        }
        val categoryFilterName = bundle.categories.firstOrNull { it.id == categoryId }?.name
        val availableCategoriesForEditor = when (editor.mode) {
            FinanceConstants.TYPE_INCOME -> bundle.categories.filter { it.type == "INCOME" }
            FinanceConstants.TYPE_EXPENSE -> bundle.categories.filter { it.type == "EXPENSE" }
            else -> emptyList()
        }
        val filteredCategories = visibleTransactions.mapNotNull { it.category }.distinctBy { it.id }
        val openingBalance = if (accountId == null) {
            bundle.accounts.sumOf { it.initialBalance }
        } else {
            bundle.accounts.firstOrNull { it.id == accountId }?.initialBalance ?: 0.0
        }
        return FinanceUiState(
            monthYear = month,
            selectedAccountId = accountId,
            selectedTab = tab,
            accounts = bundle.accounts,
            expenseCategories = bundle.categories.filter { it.type == "EXPENSE" },
            filteredCategories = filteredCategories,
            categoryFilterName = categoryFilterName,
            visibleTransactions = visibleTransactions,
            recurringTransactions = recurringTransactions,
            budgetProgress = budgetProgress,
            realBalance = openingBalance + FinanceRules.calculateRealBalance(financeTransactions),
            forecastBalance = openingBalance + FinanceRules.calculateForecastBalance(financeTransactions),
            isTransactionEditorVisible = editorVisible,
            transactionEditor = editor,
            availableCategoriesForEditor = availableCategoriesForEditor,
            paymentDialog = payment,
            isBudgetEditorVisible = budgetEditorVisible,
            budgetEditor = budgetEditorState,
        )
    }
}

data class FinanceUiState(
    val monthYear: String = currentMonthYear(),
    val selectedAccountId: Long? = null,
    val selectedTab: Int = FinanceConstants.TAB_TRANSACTIONS,
    val accounts: List<com.lifeflow.pro.data.db.entities.AccountEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val filteredCategories: List<CategoryEntity> = emptyList(),
    val categoryFilterName: String? = null,
    val visibleTransactions: List<TransactionWithRelations> = emptyList(),
    val recurringTransactions: List<TransactionWithRelations> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val realBalance: Double = 0.0,
    val forecastBalance: Double = 0.0,
    val isTransactionEditorVisible: Boolean = false,
    val transactionEditor: TransactionEditorState = TransactionEditorState(),
    val availableCategoriesForEditor: List<CategoryEntity> = emptyList(),
    val paymentDialog: PaymentDialogState? = null,
    val isBudgetEditorVisible: Boolean = false,
    val budgetEditor: BudgetEditorState = BudgetEditorState(),
)

data class TransactionEditorState(
    val id: Long = 0,
    val mode: String = FinanceConstants.TYPE_EXPENSE,
    val type: String = FinanceConstants.TYPE_EXPENSE,
    val amount: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val description: String = "",
    val expectedDate: String = LocalDate.now().toString(),
    val recurrenceType: String = FinanceConstants.RECURRENCE_NONE,
    val transferTargetAccountId: Long? = null,
    val currentStatus: String? = null,
    val createdAt: Long? = null,
) {
    fun isValid(): Boolean {
        val amountValue = amount.toDoubleOrNull() ?: return false
        if (amountValue <= 0.0) return false
        if (accountId == null) return false
        return if (mode == FinanceConstants.TYPE_TRANSFER) {
            transferTargetAccountId != null && transferTargetAccountId != accountId
        } else {
            categoryId != null && expectedDate.isNotBlank()
        }
    }

    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        type = type,
        accountId = accountId ?: 0,
        categoryId = categoryId ?: 0,
        description = description.ifBlank { null },
        expectedValue = amount.toDoubleOrNull() ?: 0.0,
        finalValue = null,
        expectedDate = expectedDate,
        paymentDate = null,
        status = currentStatus ?: transactionStatusForType(type, paid = false),
        recurrenceType = recurrenceType,
        recurrenceGroupId = null,
        economy = 0.0,
        createdAt = createdAt ?: System.currentTimeMillis(),
    )

    companion object {
        fun fromEntity(transaction: TransactionEntity): TransactionEditorState = TransactionEditorState(
            id = transaction.id,
            mode = transaction.type,
            type = transaction.type,
            amount = transaction.expectedValue.toString(),
            accountId = transaction.accountId,
            categoryId = transaction.categoryId,
            description = transaction.description.orEmpty(),
            expectedDate = transaction.expectedDate,
            recurrenceType = transaction.recurrenceType,
            currentStatus = transaction.status,
            createdAt = transaction.createdAt,
        )
    }
}

data class PaymentDialogState(
    val transactionId: Long,
    val finalValue: String,
    val paymentDate: String,
)

data class BudgetEditorState(
    val id: Long = 0,
    val categoryId: Long? = null,
    val plannedValue: String = "",
)
