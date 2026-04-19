
package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import kotlin.math.abs

data class BadgeUiItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean,
    val unlockedDateLabel: String? = null,
)

data class BadgesUiState(
    val badges: List<BadgeUiItem> = emptyList(),
    val health: DashboardHealth = DashboardHealth(0, "🌱 Iniciante", 0f, "💪 Estável"),
)

private fun Long.toBrDateFromEpochMillis(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .toBrDate()

fun buildBadgeItems(
    tasks: List<TaskEntity>,
    debts: List<DebtEntity>,
    goals: List<GoalEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    today: LocalDate = LocalDate.now(),
): List<BadgeUiItem> {
    val streak = TaskStreakCalculator.calculateStreak(tasks, today)
    val completedGoals = goals.filter { it.status == FinanceConstants.GOAL_COMPLETED }
    val settledDebts = debts.filter { it.status == DebtConstants.STATUS_SETTLED }
    val monthsWithinBudget = countMonthsWithoutBudgetOverflow(budgets, transactions)
    val allDebtsSettled = debts.isNotEmpty() && debts.all { it.status == DebtConstants.STATUS_SETTLED }

    return listOf(
        BadgeUiItem(
            id = "first_debt",
            title = "Livre de uma!",
            description = "Quitou a primeira dívida registrada.",
            icon = "🎉",
            unlocked = settledDebts.isNotEmpty(),
            unlockedDateLabel = settledDebts.minByOrNull { it.createdAt }?.createdAt?.toBrDateFromEpochMillis(),
        ),
        BadgeUiItem(
            id = "all_debts",
            title = "Zerou tudo!",
            description = "Todas as dívidas cadastradas foram quitadas.",
            icon = "🏁",
            unlocked = allDebtsSettled,
            unlockedDateLabel = if (allDebtsSettled) (settledDebts.maxOfOrNull { it.createdAt }?.toBrDateFromEpochMillis()) else null,
        ),
        BadgeUiItem(
            id = "first_goal",
            title = "Guardador!",
            description = "Concluiu a primeira meta de economia.",
            icon = "💰",
            unlocked = completedGoals.isNotEmpty(),
            unlockedDateLabel = completedGoals.minByOrNull { it.completedAt ?: Long.MAX_VALUE }?.completedAt?.toBrDateFromEpochMillis(),
        ),
        BadgeUiItem(
            id = "seven_day_streak",
            title = "Imparável!",
            description = "Manteve streak de 7 dias com tarefas concluídas.",
            icon = "🔥",
            unlocked = streak >= 7,
            unlockedDateLabel = if (streak >= 7) today.toBrDate() else null,
        ),
        BadgeUiItem(
            id = "budget_control",
            title = "Dentro do limite!",
            description = "Fechou 3 meses sem estourar orçamentos.",
            icon = "📊",
            unlocked = monthsWithinBudget >= 3,
            unlockedDateLabel = if (monthsWithinBudget >= 3) today.toBrDate() else null,
        ),
    )
}

fun countMonthsWithoutBudgetOverflow(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
): Int {
    if (budgets.isEmpty()) return 0
    return budgets.groupBy { it.monthYear }.count { (month, monthlyBudgets) ->
        monthlyBudgets.map { budget -> calculateBudgetProgress(budget, null, transactions) }.none { it.status == BudgetStatus.EXCEEDED }
    }
}

data class ReportAmountItem(val label: String, val value: Double)
data class WeeklyFinanceItem(val label: String, val income: Double, val expense: Double)
data class BalancePoint(val label: String, val realBalance: Double, val forecastBalance: Double)
data class CountItem(val label: String, val count: Int)
data class MonthlyComparison(val receivedDeltaPct: Double, val paidDeltaPct: Double, val economyDeltaPct: Double, val finalBalanceDeltaPct: Double)

data class ReportsUiState(
    val month: YearMonth = YearMonth.now(),
    val totalReceived: Double = 0.0,
    val totalPaid: Double = 0.0,
    val finalBalance: Double = 0.0,
    val economyGenerated: Double = 0.0,
    val expenseByCategory: List<ReportAmountItem> = emptyList(),
    val weeklyFinance: List<WeeklyFinanceItem> = emptyList(),
    val balanceSeries: List<BalancePoint> = emptyList(),
    val budgetRows: List<BudgetProgress> = emptyList(),
    val taskCompletionRate: Float = 0f,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val tasksByPriority: List<CountItem> = emptyList(),
    val tasksByCategory: List<CountItem> = emptyList(),
    val streakHistory: List<CountItem> = emptyList(),
    val comparison: MonthlyComparison = MonthlyComparison(0.0, 0.0, 0.0, 0.0),
)

private fun pctChange(current: Double, previous: Double): Double {
    if (abs(previous) < 0.0001) return if (abs(current) < 0.0001) 0.0 else 100.0
    return ((current - previous) / previous) * 100.0
}

fun buildReportsUiState(
    month: YearMonth,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    budgets: List<BudgetEntity>,
    tasks: List<TaskEntity>,
    openingBalance: Double = 0.0,
): ReportsUiState {
    val monthPrefix = month.toString()
    val currentTransactions = transactions.filter { it.expectedDate.startsWith(monthPrefix) }
    val previousTransactions = transactions.filter { it.expectedDate.startsWith(month.minusMonths(1).toString()) }
    val paidCurrent = currentTransactions.filter { it.status == FinanceConstants.STATUS_PAID }
    val receivedCurrent = currentTransactions.filter { it.status == FinanceConstants.STATUS_RECEIVED }

    val totalReceived = receivedCurrent.sumOf { it.finalValue ?: it.expectedValue }
    val totalPaid = paidCurrent.sumOf { it.finalValue ?: it.expectedValue }
    val finalBalance = openingBalance + totalReceived - totalPaid
    val economyGenerated = currentTransactions.sumOf { it.economy }

    val categoriesById = categories.associateBy { it.id }
    val expenseByCategory = currentTransactions
        .filter { it.type == FinanceConstants.TYPE_EXPENSE }
        .groupBy { categoriesById[it.categoryId]?.name ?: "Sem categoria" }
        .map { (label, txs) -> ReportAmountItem(label, txs.sumOf { it.finalValue ?: it.expectedValue }) }
        .sortedByDescending { it.value }

    val weeklyFinance = (1..5).map { week ->
        val start = month.atDay(1).plusDays(((week - 1) * 7).toLong())
        val end = if (week == 5) month.atEndOfMonth() else start.plusDays(6)
        val txs = currentTransactions.filter { tx ->
            val date = LocalDate.parse(tx.expectedDate)
            !date.isBefore(start) && !date.isAfter(end)
        }
        WeeklyFinanceItem(
            label = "Sem $week",
            income = txs.filter { it.type == FinanceConstants.TYPE_INCOME }.sumOf { it.finalValue ?: it.expectedValue },
            expense = txs.filter { it.type == FinanceConstants.TYPE_EXPENSE }.sumOf { it.finalValue ?: it.expectedValue },
        )
    }

    var runningReal = openingBalance
    var runningForecast = 0.0
    val sortedDates = currentTransactions.map { it.expectedDate }.distinct().sorted()
    val balanceSeries = sortedDates.map { date ->
        val onDate = currentTransactions.filter { it.expectedDate == date }
        runningReal += onDate.filter { it.status == FinanceConstants.STATUS_RECEIVED }.sumOf { it.finalValue ?: it.expectedValue }
        runningReal -= onDate.filter { it.status == FinanceConstants.STATUS_PAID }.sumOf { it.finalValue ?: it.expectedValue }
        runningForecast += onDate.filter { it.type == FinanceConstants.TYPE_INCOME }.sumOf { if (it.status == FinanceConstants.STATUS_RECEIVED) 0.0 else it.expectedValue }
        runningForecast -= onDate.filter { it.type == FinanceConstants.TYPE_EXPENSE }.sumOf { if (it.status == FinanceConstants.STATUS_PAID) 0.0 else it.expectedValue }
        BalancePoint(label = LocalDate.parse(date).dayOfMonth.toString(), realBalance = runningReal, forecastBalance = runningReal + runningForecast)
    }

    val monthBudgets = budgets.filter { it.monthYear == month.toString() }.map { budget ->
        calculateBudgetProgress(budget, categoriesById[budget.categoryId], currentTransactions)
    }

    val monthTasks = tasks.filter { it.dueDate?.startsWith(monthPrefix) == true || it.completedAt?.startsWith(monthPrefix) == true }
    val completedTasks = monthTasks.count { it.status == TaskConstants.STATUS_COMPLETED }
    val taskCompletionRate = if (monthTasks.isEmpty()) 0f else completedTasks.toFloat() / monthTasks.size.toFloat()
    val tasksByPriority = monthTasks.groupBy { it.priority }.map { CountItem(it.key, it.value.size) }.sortedByDescending { it.count }
    val tasksByCategory = monthTasks.groupBy { task -> categoriesById[task.categoryId]?.name ?: "Sem categoria" }.map { CountItem(it.key, it.value.size) }.sortedByDescending { it.count }
    val streakHistory = monthTasks.mapNotNull { it.completedAt?.substring(0, 10) }
        .groupingBy { it.substring(8, 10).toIntOrNull() ?: 0 }
        .eachCount()
        .toSortedMap()
        .map { CountItem(it.key.toString().padStart(2, '0'), it.value) }

    val prevReceived = previousTransactions.filter { it.status == FinanceConstants.STATUS_RECEIVED }.sumOf { it.finalValue ?: it.expectedValue }
    val prevPaid = previousTransactions.filter { it.status == FinanceConstants.STATUS_PAID }.sumOf { it.finalValue ?: it.expectedValue }
    val prevEconomy = previousTransactions.sumOf { it.economy }
    val prevFinal = openingBalance + prevReceived - prevPaid

    return ReportsUiState(
        month = month,
        totalReceived = totalReceived,
        totalPaid = totalPaid,
        finalBalance = finalBalance,
        economyGenerated = economyGenerated,
        expenseByCategory = expenseByCategory,
        weeklyFinance = weeklyFinance,
        balanceSeries = balanceSeries,
        budgetRows = monthBudgets,
        taskCompletionRate = taskCompletionRate,
        completedTasks = completedTasks,
        totalTasks = monthTasks.size,
        tasksByPriority = tasksByPriority,
        tasksByCategory = tasksByCategory,
        streakHistory = streakHistory,
        comparison = MonthlyComparison(
            receivedDeltaPct = pctChange(totalReceived, prevReceived),
            paidDeltaPct = pctChange(totalPaid, prevPaid),
            economyDeltaPct = pctChange(economyGenerated, prevEconomy),
            finalBalanceDeltaPct = pctChange(finalBalance, prevFinal),
        ),
    )
}
