package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.DatabaseSeeder
import com.lifeflow.pro.data.repository.DebtRepository
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.data.repository.TaskRepository
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.domain.model.CalendarEventItem
import com.lifeflow.pro.domain.model.CalendarMarkerType
import com.lifeflow.pro.domain.model.CalendarUiState
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.TaskConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    taskRepository: TaskRepository,
    financeRepository: FinanceRepository,
    debtRepository: DebtRepository,
    private val databaseSeeder: DatabaseSeeder,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        taskRepository.observeTasksWithCategories(),
        financeRepository.observeFinanceBundle(),
        debtRepository.observeDebtBundle(),
        month,
        selectedDate,
    ) { taskPair, financeBundle, debtBundle, visibleMonth, chosenDate ->
        val taskCategories = taskPair.second.associateBy { it.id }
        val financeCategories = financeBundle.categories.associateBy { it.id }
        val debtById = debtBundle.debts.associateBy { it.id }

        val taskEvents = taskPair.first.mapNotNull { task ->
            val dueDate = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
            if (task.status == TaskConstants.STATUS_COMPLETED) return@mapNotNull null
            CalendarEventItem(
                id = "task-${task.id}",
                date = dueDate,
                title = task.title,
                subtitle = taskCategories[task.categoryId]?.name ?: "Tarefa",
                markerType = CalendarMarkerType.TASK,
                target = AppSelectionTarget.Task(task.id),
            )
        }

        val financeEvents = financeBundle.transactions.mapNotNull { transaction ->
            val dueDate = runCatching { LocalDate.parse(transaction.expectedDate) }.getOrNull() ?: return@mapNotNull null
            val markerType = when {
                transaction.type == FinanceConstants.TYPE_INCOME && transaction.status == FinanceConstants.STATUS_TO_RECEIVE -> CalendarMarkerType.INCOME
                transaction.type == FinanceConstants.TYPE_EXPENSE && transaction.status == FinanceConstants.STATUS_TO_PAY -> CalendarMarkerType.EXPENSE
                else -> return@mapNotNull null
            }
            CalendarEventItem(
                id = "transaction-${transaction.id}",
                date = dueDate,
                title = transaction.description ?: financeCategories[transaction.categoryId]?.name ?: "Financeiro",
                subtitle = financeCategories[transaction.categoryId]?.name ?: "Movimento financeiro",
                markerType = markerType,
                target = AppSelectionTarget.Transaction(transaction.id),
            )
        }

        val debtEvents = debtBundle.installments.mapNotNull { installment ->
            val dueDate = runCatching { LocalDate.parse(installment.dueDate) }.getOrNull() ?: return@mapNotNull null
            if (installment.status == "PAGA") return@mapNotNull null
            val debt = debtById[installment.debtId] ?: return@mapNotNull null
            CalendarEventItem(
                id = "debt-${installment.id}",
                date = dueDate,
                title = "Parcela ${installment.installmentNumber}",
                subtitle = debt.creditor,
                markerType = CalendarMarkerType.DEBT,
                target = AppSelectionTarget.Debt(debt.id),
            )
        }

        val monthEvents = (taskEvents + financeEvents + debtEvents)
            .filter { YearMonth.from(it.date) == visibleMonth }
            .groupBy { it.date }

        val safeSelectedDate = if (YearMonth.from(chosenDate) == visibleMonth) chosenDate else visibleMonth.atDay(1)

        CalendarUiState(
            month = visibleMonth,
            selectedDate = safeSelectedDate,
            eventsByDate = monthEvents,
            selectedDateEvents = monthEvents[safeSelectedDate].orEmpty().sortedBy { it.title },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch { databaseSeeder.seedIfNeeded() }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selectedDate.value = month.value.atDay(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selectedDate.value = month.value.atDay(1)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }
}
