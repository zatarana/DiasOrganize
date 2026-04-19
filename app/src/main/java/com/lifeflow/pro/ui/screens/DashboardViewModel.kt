package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.DatabaseSeeder
import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.data.repository.DebtRepository
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.data.repository.GoalRepository
import com.lifeflow.pro.data.repository.TaskRepository
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.domain.model.BudgetProgress
import com.lifeflow.pro.domain.model.CalendarEventItem
import com.lifeflow.pro.domain.model.CalendarMarkerType
import com.lifeflow.pro.domain.model.DASHBOARD_MAX_SECTION_ITEMS
import com.lifeflow.pro.domain.model.DashboardDueItem
import com.lifeflow.pro.domain.model.DashboardTaskItem
import com.lifeflow.pro.domain.model.DashboardUiState
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.FinanceRules
import com.lifeflow.pro.domain.model.FinanceTransaction
import com.lifeflow.pro.domain.model.GoalProgress
import com.lifeflow.pro.domain.model.SearchGroup
import com.lifeflow.pro.domain.model.SearchResultItem
import com.lifeflow.pro.domain.model.TaskConstants
import com.lifeflow.pro.domain.model.TaskStreakCalculator
import com.lifeflow.pro.domain.model.TransactionStatus
import com.lifeflow.pro.domain.model.TransactionType
import com.lifeflow.pro.domain.model.calculateBudgetProgress
import com.lifeflow.pro.domain.model.calculateFinancialHealth
import com.lifeflow.pro.domain.model.calculateGoalProgress
import com.lifeflow.pro.domain.model.currentMonthYear
import com.lifeflow.pro.domain.model.dueDateSummary
import com.lifeflow.pro.domain.model.isoDateToBr
import com.lifeflow.pro.domain.model.isActiveGoal
import com.lifeflow.pro.domain.model.toCurrencyBr
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val financeRepository: FinanceRepository,
    private val goalRepository: GoalRepository,
    private val debtRepository: DebtRepository,
    private val databaseSeeder: DatabaseSeeder,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        taskRepository.observeTasksWithCategories(),
        financeRepository.observeFinanceBundle(),
        goalRepository.observeGoals(),
        debtRepository.observeDebtBundle(),
    ) { taskPair, financeBundle, goals, debtBundle ->
        val tasks = taskPair.first
        val taskCategories = taskPair.second.associateBy { it.id }
        val categories = financeBundle.categories.associateBy { it.id }
        val currentMonth = currentMonthYear()

        val financeTransactions = financeBundle.transactions.map {
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

        val budgets = financeBundle.budgets
            .filter { it.monthYear == currentMonth }
            .map { budget -> calculateBudgetProgress(budget, categories[budget.categoryId], financeBundle.transactions) }
            .sortedByDescending { it.usedPercentage }
            .take(DASHBOARD_MAX_SECTION_ITEMS)

        val activeGoals = goals
            .filter { it.isActiveGoal() }
            .map(::calculateGoalProgress)
            .sortedByDescending { it.progress }
            .take(DASHBOARD_MAX_SECTION_ITEMS)

        val upcomingTasks = tasks
            .filter { it.status != TaskConstants.STATUS_COMPLETED }
            .sortedWith(compareBy<TaskEntity> { it.dueDate ?: "9999-12-31" }.thenBy { it.dueTime ?: "99:99" }.thenByDescending { it.createdAt })
            .take(DASHBOARD_MAX_SECTION_ITEMS)
            .map { task ->
                DashboardTaskItem(
                    id = task.id,
                    title = task.title,
                    subtitle = dueDateSummary(task.dueDate, task.dueTime),
                    categoryLabel = task.categoryId?.let { taskCategories[it]?.name },
                    isCompleted = task.status == TaskConstants.STATUS_COMPLETED,
                )
            }

        val pendingFinance = financeBundle.transactions
            .filter { it.status == FinanceConstants.STATUS_TO_PAY || it.status == FinanceConstants.STATUS_TO_RECEIVE }
            .sortedBy { it.expectedDate }
            .map { transaction ->
                DashboardDueItem(
                    title = transaction.description ?: categories[transaction.categoryId]?.name ?: "Transação",
                    subtitle = buildString {
                        append(transaction.expectedDate.isoDateToBr())
                        append(" • ")
                        append(if (transaction.type == FinanceConstants.TYPE_INCOME) "Receita" else "Despesa")
                    },
                    amountLabel = transaction.expectedValue.toCurrencyBr(),
                    target = AppSelectionTarget.Transaction(transaction.id),
                )
            }

        val pendingInstallments = debtBundle.installments
            .filter { it.status != "PAGA" }
            .sortedBy { it.dueDate }
            .mapNotNull { installment ->
                val debt = debtBundle.debts.firstOrNull { it.id == installment.debtId } ?: return@mapNotNull null
                DashboardDueItem(
                    title = "Parcela ${installment.installmentNumber} • ${debt.creditor}",
                    subtitle = installment.dueDate.isoDateToBr(),
                    amountLabel = installment.expectedValue.toCurrencyBr(),
                    target = AppSelectionTarget.Debt(debt.id),
                )
            }

        val openingBalance = financeBundle.accounts.sumOf { it.initialBalance }
        val paidWithEconomy = financeBundle.transactions.count { (it.economy > 0.0) && (it.status == FinanceConstants.STATUS_PAID || it.status == FinanceConstants.STATUS_RECEIVED) }
        val allBudgetProgress = financeBundle.budgets.filter { it.monthYear == currentMonth }.map { budget ->
            calculateBudgetProgress(budget, categories[budget.categoryId], financeBundle.transactions)
        }
        val monthsPositive = if (openingBalance + FinanceRules.calculateRealBalance(financeTransactions) > 0.0) 1 else 0
        val health = calculateFinancialHealth(
            debtsSettled = debtBundle.debts.count { it.status == "QUITADA" },
            budgetsWithoutOverflow = if (allBudgetProgress.isNotEmpty() && allBudgetProgress.none { it.status.name == "EXCEEDED" }) 1 else 0,
            completedGoals = goals.count { it.status == FinanceConstants.GOAL_COMPLETED },
            positiveMonths = monthsPositive,
            paymentsWithEconomy = paidWithEconomy,
        )

        DashboardUiState(
            realBalance = openingBalance + FinanceRules.calculateRealBalance(financeTransactions),
            forecastBalance = openingBalance + FinanceRules.calculateForecastBalance(financeTransactions),
            budgetItems = budgets,
            goals = activeGoals,
            upcomingTasks = upcomingTasks,
            upcomingDueItems = (pendingFinance + pendingInstallments).sortedWith(compareBy<DashboardDueItem> { parseDueDateOrMax(it.subtitle) }.thenBy { it.title }).take(DASHBOARD_MAX_SECTION_ITEMS),
            streak = TaskStreakCalculator.calculateStreak(tasks),
            health = health,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        viewModelScope.launch { databaseSeeder.seedIfNeeded() }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.markCompleted(taskId)
        }
    }
}


private fun parseDueDateOrMax(label: String): LocalDate {
    val candidate = label.substringBefore(" • ").trim()
    return runCatching {
        val parts = candidate.split("/")
        if (parts.size == 3) {
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else {
            LocalDate.MAX
        }
    }.getOrDefault(LocalDate.MAX)
}
