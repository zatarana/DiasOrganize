
package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.repository.DebtRepository
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.data.repository.GoalRepository
import com.lifeflow.pro.data.repository.TaskRepository
import com.lifeflow.pro.domain.model.BadgesUiState
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.buildBadgeItems
import com.lifeflow.pro.domain.model.buildReportsUiState
import com.lifeflow.pro.domain.model.calculateFinancialHealth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BadgesViewModel @Inject constructor(
    taskRepository: TaskRepository,
    financeRepository: FinanceRepository,
    goalRepository: GoalRepository,
    debtRepository: DebtRepository,
) : ViewModel() {
    val uiState: StateFlow<BadgesUiState> = combine(
        taskRepository.observeTasks(),
        financeRepository.observeFinanceBundle(),
        goalRepository.observeGoals(),
        debtRepository.observeDebtBundle(),
    ) { tasks, finance, goals, debts ->
        val paidWithEconomy = finance.transactions.count { (it.economy > 0.0) && (it.status == FinanceConstants.STATUS_PAID || it.status == FinanceConstants.STATUS_RECEIVED) }
        val health = calculateFinancialHealth(
            debtsSettled = debts.debts.count { it.status == "QUITADA" },
            budgetsWithoutOverflow = com.lifeflow.pro.domain.model.countMonthsWithoutBudgetOverflow(finance.budgets, finance.transactions),
            completedGoals = goals.count { it.status == FinanceConstants.GOAL_COMPLETED },
            positiveMonths = if (buildReportsUiState(java.time.YearMonth.now(), finance.transactions, finance.categories, finance.budgets, tasks).finalBalance > 0.0) 1 else 0,
            paymentsWithEconomy = paidWithEconomy,
        )
        BadgesUiState(
            badges = buildBadgeItems(tasks, debts.debts, goals, finance.budgets, finance.categories, finance.transactions),
            health = health,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BadgesUiState())
}
