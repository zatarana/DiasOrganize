
package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.data.repository.TaskRepository
import com.lifeflow.pro.domain.model.ReportsUiState
import com.lifeflow.pro.domain.model.buildReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportsViewModel @Inject constructor(
    financeRepository: FinanceRepository,
    taskRepository: TaskRepository,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ReportsUiState> = combine(
        selectedMonth,
        financeRepository.observeFinanceBundle(),
        taskRepository.observeTasksWithCategories(),
    ) { month, finance, taskPair ->
        buildReportsUiState(
            month = month,
            transactions = finance.transactions,
            categories = finance.categories,
            budgets = finance.budgets,
            tasks = taskPair.first,
            openingBalance = finance.accounts.sumOf { it.initialBalance },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun previousMonth() { selectedMonth.value = selectedMonth.value.minusMonths(1) }
    fun nextMonth() { selectedMonth.value = selectedMonth.value.plusMonths(1) }
}
