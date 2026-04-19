package com.lifeflow.pro.data.repository

import com.lifeflow.pro.data.db.dao.AccountDao
import com.lifeflow.pro.data.db.dao.BudgetDao
import com.lifeflow.pro.data.db.dao.CategoryDao
import com.lifeflow.pro.data.db.dao.DebtDao
import com.lifeflow.pro.data.db.dao.DebtInstallmentDao
import com.lifeflow.pro.data.db.dao.GoalDao
import com.lifeflow.pro.data.db.dao.TransactionDao
import com.lifeflow.pro.data.db.dao.TransferDao
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.DebtInstallmentEntity
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import com.lifeflow.pro.data.db.entities.TransferEntity
import com.lifeflow.pro.domain.model.DebtConstants
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.FinanceRules
import com.lifeflow.pro.domain.model.currentMonthYear
import com.lifeflow.pro.domain.model.nextMonthYear
import com.lifeflow.pro.domain.model.previousMonthYear
import com.lifeflow.pro.domain.model.transactionStatusForType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.lifeflow.pro.widget.LifeFlowWidgets

@Singleton
class FinanceRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val transferDao: TransferDao,
    @ApplicationContext private val context: Context,
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()
    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAll()
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeExpenseCategories(): Flow<List<CategoryEntity>> = categoryDao.observeByType("EXPENSE")
    fun observeIncomeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeByType("INCOME")
    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    fun observeFinanceBundle(): Flow<FinanceBundle> = combine(
        transactionDao.observeAll(),
        accountDao.observeAll(),
        categoryDao.observeAll(),
        budgetDao.observeAll(),
    ) { transactions, accounts, categories, budgets ->
        FinanceBundle(transactions, accounts, categories, budgets)
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long = transactionDao.insert(transaction).also { LifeFlowWidgets.refreshAll(context) }
    suspend fun updateTransaction(transaction: TransactionEntity) { transactionDao.update(transaction); LifeFlowWidgets.refreshAll(context) }
    suspend fun deleteTransaction(transaction: TransactionEntity) { transactionDao.delete(transaction); LifeFlowWidgets.refreshAll(context) }

    suspend fun confirmTransaction(transactionId: Long, finalValue: Double, paymentDate: String) {
        val current = transactionDao.getById(transactionId) ?: return
        transactionDao.update(
            current.copy(
                finalValue = finalValue,
                paymentDate = paymentDate,
                status = transactionStatusForType(current.type, paid = true),
                economy = current.expectedValue - finalValue,
            )
        )
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun saveAccount(account: AccountEntity): Long = accountDao.insert(account).also { LifeFlowWidgets.refreshAll(context) }
    suspend fun updateAccount(account: AccountEntity) { accountDao.update(account); LifeFlowWidgets.refreshAll(context) }
    suspend fun deleteAccount(account: AccountEntity) { accountDao.delete(account); LifeFlowWidgets.refreshAll(context) }

    suspend fun saveBudget(budget: BudgetEntity): Long = budgetDao.insert(budget).also { LifeFlowWidgets.refreshAll(context) }
    suspend fun updateBudget(budget: BudgetEntity) { budgetDao.update(budget); LifeFlowWidgets.refreshAll(context) }
    suspend fun deleteBudget(budget: BudgetEntity) { budgetDao.delete(budget); LifeFlowWidgets.refreshAll(context) }

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        value: Double,
        description: String?,
        date: String,
    ) {
        val categories = categoryDao.getAll()
        val expenseCategory = categories.firstOrNull { it.type == "EXPENSE" } ?: return
        val incomeCategory = categories.firstOrNull { it.type == "INCOME" } ?: return
        val groupId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    type = FinanceConstants.TYPE_EXPENSE,
                    accountId = fromAccountId,
                    categoryId = expenseCategory.id,
                    description = description ?: "Transferência enviada",
                    expectedValue = value,
                    finalValue = value,
                    expectedDate = date,
                    paymentDate = date,
                    status = FinanceConstants.STATUS_PAID,
                    recurrenceType = FinanceConstants.RECURRENCE_NONE,
                    recurrenceGroupId = groupId,
                    economy = 0.0,
                    createdAt = now,
                ),
                TransactionEntity(
                    type = FinanceConstants.TYPE_INCOME,
                    accountId = toAccountId,
                    categoryId = incomeCategory.id,
                    description = description ?: "Transferência recebida",
                    expectedValue = value,
                    finalValue = value,
                    expectedDate = date,
                    paymentDate = date,
                    status = FinanceConstants.STATUS_RECEIVED,
                    recurrenceType = FinanceConstants.RECURRENCE_NONE,
                    recurrenceGroupId = groupId,
                    economy = 0.0,
                    createdAt = now,
                ),
            )
        )
        transferDao.insert(
            TransferEntity(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                value = value,
                description = description,
                date = date,
                createdAt = now,
            )
        )
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun copyBudgetsFromPreviousMonth(targetMonthYear: String) {
        val allBudgets = budgetDao.observeAllSnapshot()
        val targetSet = allBudgets.filter { it.monthYear == targetMonthYear }.map { it.categoryId }.toSet()
        val previous = previousMonthYear(targetMonthYear)
        allBudgets.filter { it.monthYear == previous && it.categoryId !in targetSet }
            .forEach { budget ->
                budgetDao.insert(
                    budget.copy(
                        id = 0,
                        monthYear = targetMonthYear,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun generateMonthlyRecurringTransactions(targetMonthYear: String = nextMonthYear(currentMonthYear())) {
        val all = transactionDao.getAll()
        val yearMonth = YearMonth.parse(targetMonthYear)
        val existing = all.filter { YearMonth.parse(it.expectedDate.substring(0, 7)) == yearMonth }
        val recurring = all.filter { it.recurrenceType == FinanceConstants.RECURRENCE_MONTHLY }
        recurring.forEach { original ->
            val sourceMonth = YearMonth.parse(original.expectedDate.substring(0, 7))
            if (!sourceMonth.isBefore(yearMonth)) return@forEach
            val alreadyExists = existing.any {
                it.recurrenceGroupId != null &&
                    it.recurrenceGroupId == (original.recurrenceGroupId ?: original.id.toString()) &&
                    it.accountId == original.accountId &&
                    it.type == original.type
            }
            if (alreadyExists) return@forEach
            val day = original.expectedDate.substringAfterLast('-').toIntOrNull() ?: 1
            val safeDay = day.coerceAtMost(yearMonth.lengthOfMonth())
            val expectedDate = yearMonth.atDay(safeDay).toString()
            transactionDao.insert(
                original.copy(
                    id = 0,
                    finalValue = null,
                    paymentDate = null,
                    status = transactionStatusForType(original.type, paid = false),
                    expectedDate = expectedDate,
                    economy = 0.0,
                    createdAt = System.currentTimeMillis(),
                    recurrenceGroupId = original.recurrenceGroupId ?: original.id.toString(),
                )
            )
        }
        LifeFlowWidgets.refreshAll(context)
    }
}

data class FinanceBundle(
    val transactions: List<TransactionEntity>,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
)

@Singleton
class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val debtInstallmentDao: DebtInstallmentDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    @ApplicationContext private val context: Context,
) {
    fun observeDebtBundle(): Flow<DebtBundle> = combine(
        debtDao.observeAll(),
        debtInstallmentDao.observeAll(),
        accountDao.observeAll(),
    ) { debts, installments, accounts ->
        DebtBundle(debts = debts, installments = installments, accounts = accounts)
    }

    fun observeDebts(): Flow<List<DebtEntity>> = debtDao.observeAll()
    fun observeInstallments(debtId: Long): Flow<List<DebtInstallmentEntity>> = debtInstallmentDao.observeByDebtId(debtId)
    suspend fun saveDebt(debt: DebtEntity): Long = debtDao.insert(debt).also { LifeFlowWidgets.refreshAll(context) }
    suspend fun updateDebt(debt: DebtEntity) { debtDao.update(debt); LifeFlowWidgets.refreshAll(context) }
    suspend fun saveInstallments(items: List<DebtInstallmentEntity>) { debtInstallmentDao.insertAll(items); LifeFlowWidgets.refreshAll(context) }

    suspend fun deleteDebt(debtId: Long) {
        val debt = debtDao.getById(debtId) ?: return
        val installments = debtInstallmentDao.getByDebtIdOnce(debtId)
        installments.forEach { installment ->
            installment.transactionId?.let { txId ->
                transactionDao.getById(txId)?.let { transactionDao.delete(it) }
            }
            debtInstallmentDao.delete(installment)
        }
        debtDao.delete(debt)
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun settleDebt(debtId: Long, finalValue: Double, accountId: Long, paymentDate: String) {
        val debt = debtDao.getById(debtId) ?: return
        val expenseCategoryId = resolveExpenseCategoryId() ?: return
        transactionDao.insert(
            TransactionEntity(
                type = FinanceConstants.TYPE_EXPENSE,
                accountId = accountId,
                categoryId = expenseCategoryId,
                description = "Quitação de dívida - ${debt.creditor}",
                expectedValue = finalValue,
                finalValue = finalValue,
                expectedDate = paymentDate,
                paymentDate = paymentDate,
                status = FinanceConstants.STATUS_PAID,
                recurrenceType = FinanceConstants.RECURRENCE_NONE,
                recurrenceGroupId = null,
                economy = 0.0,
                createdAt = System.currentTimeMillis(),
            )
        )
        debtDao.update(
            debt.copy(
                status = DebtConstants.STATUS_SETTLED,
                negotiatedValue = finalValue,
                totalEconomy = debt.originalValue - finalValue,
            )
        )
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun negotiateDebt(
        debtId: Long,
        negotiatedValue: Double,
        firstPaymentDate: String,
        installmentCount: Int,
        accountId: Long,
    ) {
        val debt = debtDao.getById(debtId) ?: return
        val expenseCategoryId = resolveExpenseCategoryId() ?: return
        val plan = FinanceRules.generateDebtInstallments(
            totalValue = negotiatedValue,
            installmentCount = installmentCount,
            firstDueDate = LocalDate.parse(firstPaymentDate),
        )
        val groupId = UUID.randomUUID().toString()
        plan.forEach { item ->
            val transactionId = transactionDao.insert(
                TransactionEntity(
                    type = FinanceConstants.TYPE_EXPENSE,
                    accountId = accountId,
                    categoryId = expenseCategoryId,
                    description = "Parcela ${item.installmentNumber}/$installmentCount - ${debt.creditor}",
                    expectedValue = item.value,
                    finalValue = null,
                    expectedDate = item.dueDate.toString(),
                    paymentDate = null,
                    status = FinanceConstants.STATUS_TO_PAY,
                    recurrenceType = FinanceConstants.RECURRENCE_NONE,
                    recurrenceGroupId = groupId,
                    economy = 0.0,
                    createdAt = System.currentTimeMillis(),
                )
            )
            debtInstallmentDao.insert(
                DebtInstallmentEntity(
                    debtId = debt.id,
                    installmentNumber = item.installmentNumber,
                    expectedValue = item.value,
                    finalValue = null,
                    dueDate = item.dueDate.toString(),
                    paymentDate = null,
                    status = DebtConstants.INSTALLMENT_PENDING,
                    economy = 0.0,
                    transactionId = transactionId,
                )
            )
        }
        debtDao.update(
            debt.copy(
                status = DebtConstants.STATUS_IN_PAYMENT,
                negotiatedValue = negotiatedValue,
                totalEconomy = debt.originalValue - negotiatedValue,
            )
        )
        LifeFlowWidgets.refreshAll(context)
    }

    suspend fun confirmInstallmentPayment(installmentId: Long, finalValue: Double, paymentDate: String) {
        val installment = debtInstallmentDao.getById(installmentId) ?: return
        installment.transactionId?.let { transactionId ->
            val transaction = transactionDao.getById(transactionId)
            if (transaction != null) {
                transactionDao.update(
                    transaction.copy(
                        finalValue = finalValue,
                        paymentDate = paymentDate,
                        status = FinanceConstants.STATUS_PAID,
                        economy = installment.expectedValue - finalValue,
                    )
                )
            }
        }
        debtInstallmentDao.update(
            installment.copy(
                finalValue = finalValue,
                paymentDate = paymentDate,
                status = DebtConstants.INSTALLMENT_PAID,
                economy = installment.expectedValue - finalValue,
            )
        )
        recalculateDebt(installment.debtId)
        LifeFlowWidgets.refreshAll(context)
    }

    private suspend fun recalculateDebt(debtId: Long) {
        val debt = debtDao.getById(debtId) ?: return
        val installments = debtInstallmentDao.getByDebtIdOnce(debtId)
        val baseEconomy = if (debt.negotiatedValue != null) debt.originalValue - debt.negotiatedValue else 0.0
        val adjustments = installments.sumOf { it.economy }
        val allPaid = installments.isNotEmpty() && installments.all { it.status == DebtConstants.INSTALLMENT_PAID }
        debtDao.update(
            debt.copy(
                status = if (allPaid) DebtConstants.STATUS_SETTLED else DebtConstants.STATUS_IN_PAYMENT,
                totalEconomy = baseEconomy + adjustments,
            )
        )
    }

    private suspend fun resolveExpenseCategoryId(): Long? = categoryDao.getAll().firstOrNull { it.type == "EXPENSE" }?.id
}

data class DebtBundle(
    val debts: List<DebtEntity>,
    val installments: List<DebtInstallmentEntity>,
    val accounts: List<AccountEntity>,
)

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    @ApplicationContext private val context: Context,
) {
    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()
    suspend fun saveBudget(budget: BudgetEntity): Long = budgetDao.insert(budget).also { LifeFlowWidgets.refreshAll(context) }
}

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    @ApplicationContext private val context: Context,
) {
    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()
    suspend fun saveGoal(goal: GoalEntity): Long = goalDao.insert(goal).also { LifeFlowWidgets.refreshAll(context) }
    suspend fun updateGoal(goal: GoalEntity) { goalDao.update(goal); LifeFlowWidgets.refreshAll(context) }
    suspend fun deleteGoal(goal: GoalEntity) { goalDao.delete(goal); LifeFlowWidgets.refreshAll(context) }
}
