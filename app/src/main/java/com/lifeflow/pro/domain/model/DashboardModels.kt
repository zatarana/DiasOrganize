package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import java.time.LocalDate
import java.time.YearMonth

const val DASHBOARD_MAX_SECTION_ITEMS = 3

data class DashboardTaskItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val categoryLabel: String?,
    val isCompleted: Boolean,
)

data class DashboardDueItem(
    val title: String,
    val subtitle: String,
    val amountLabel: String?,
    val target: AppSelectionTarget,
)

data class DashboardHealth(
    val score: Int,
    val levelLabel: String,
    val progressToNext: Float,
    val nextLevelLabel: String?,
)

data class DashboardUiState(
    val realBalance: Double = 0.0,
    val forecastBalance: Double = 0.0,
    val budgetItems: List<BudgetProgress> = emptyList(),
    val goals: List<GoalProgress> = emptyList(),
    val upcomingTasks: List<DashboardTaskItem> = emptyList(),
    val upcomingDueItems: List<DashboardDueItem> = emptyList(),
    val streak: Int = 0,
    val health: DashboardHealth = DashboardHealth(0, "🌱 Iniciante", 0f, "💪 Estável"),
)

enum class CalendarMarkerType {
    TASK,
    EXPENSE,
    DEBT,
    INCOME,
}

data class CalendarEventItem(
    val id: String,
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val markerType: CalendarMarkerType,
    val target: AppSelectionTarget,
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val eventsByDate: Map<LocalDate, List<CalendarEventItem>> = emptyMap(),
    val selectedDateEvents: List<CalendarEventItem> = emptyList(),
)

enum class SearchGroup(val label: String) {
    TASKS("Tarefas"),
    FINANCE("Finanças"),
    DEBTS("Dívidas"),
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val group: SearchGroup,
    val target: AppSelectionTarget,
)

data class SearchUiState(
    val query: String = "",
    val taskResults: List<SearchResultItem> = emptyList(),
    val financeResults: List<SearchResultItem> = emptyList(),
    val debtResults: List<SearchResultItem> = emptyList(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val hasResults: Boolean get() = taskResults.isNotEmpty() || financeResults.isNotEmpty() || debtResults.isNotEmpty()
}

fun calculateFinancialHealth(
    debtsSettled: Int,
    budgetsWithoutOverflow: Int,
    completedGoals: Int,
    positiveMonths: Int,
    paymentsWithEconomy: Int,
): DashboardHealth {
    val score = (debtsSettled * 20) + (budgetsWithoutOverflow * 10) + (completedGoals * 30) + (positiveMonths * 15) + (paymentsWithEconomy * 5)
    return when {
        score < 50 -> DashboardHealth(score, "🌱 Iniciante", score / 50f, "💪 Estável")
        score < 100 -> DashboardHealth(score, "💪 Estável", (score - 50) / 50f, "🏆 Saudável")
        score < 200 -> DashboardHealth(score, "🏆 Saudável", (score - 100) / 100f, "💎 Excelente")
        else -> DashboardHealth(score, "💎 Excelente", 1f, null)
    }
}

fun GoalEntity.isActiveGoal(): Boolean = status == FinanceConstants.GOAL_ACTIVE
