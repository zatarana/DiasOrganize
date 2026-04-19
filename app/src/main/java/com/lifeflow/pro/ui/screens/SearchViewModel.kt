package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.DatabaseSeeder
import com.lifeflow.pro.data.repository.DebtRepository
import com.lifeflow.pro.data.repository.FinanceRepository
import com.lifeflow.pro.data.repository.TaskRepository
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.domain.model.SearchGroup
import com.lifeflow.pro.domain.model.SearchResultItem
import com.lifeflow.pro.domain.model.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    taskRepository: TaskRepository,
    financeRepository: FinanceRepository,
    debtRepository: DebtRepository,
    private val databaseSeeder: DatabaseSeeder,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        query.debounce(300),
        taskRepository.observeTasksWithCategories(),
        financeRepository.observeFinanceBundle(),
        debtRepository.observeDebtBundle(),
    ) { rawQuery, debouncedQuery, taskPair, financeBundle, debtBundle ->
        val normalized = debouncedQuery.trim().lowercase()
        val categoryById = financeBundle.categories.associateBy { it.id }
        val taskCategories = taskPair.second.associateBy { it.id }
        if (normalized.isBlank()) {
            SearchUiState(query = rawQuery)
        } else {
            val tasks = taskPair.first
                .filter {
                    it.title.lowercase().contains(normalized) ||
                        it.description.orEmpty().lowercase().contains(normalized) ||
                        taskCategories[it.categoryId]?.name.orEmpty().lowercase().contains(normalized)
                }
                .take(8)
                .map {
                    SearchResultItem(
                        id = "task-${it.id}",
                        title = it.title,
                        subtitle = buildString {
                            append(taskCategories[it.categoryId]?.name ?: "Tarefa")
                            it.dueDate?.let { date -> append(" • ").append(date) }
                        },
                        group = SearchGroup.TASKS,
                        target = AppSelectionTarget.Task(it.id),
                    )
                }

            val finances = financeBundle.transactions
                .filter {
                    it.description.orEmpty().lowercase().contains(normalized) ||
                        categoryById[it.categoryId]?.name.orEmpty().lowercase().contains(normalized)
                }
                .take(8)
                .map {
                    SearchResultItem(
                        id = "transaction-${it.id}",
                        title = it.description ?: categoryById[it.categoryId]?.name ?: "Transação",
                        subtitle = buildString {
                            append(categoryById[it.categoryId]?.name ?: "Finanças")
                            append(" • ")
                            append(it.expectedDate)
                        },
                        group = SearchGroup.FINANCE,
                        target = AppSelectionTarget.Transaction(it.id),
                    )
                }

            val debts = debtBundle.debts
                .filter {
                    it.creditor.lowercase().contains(normalized) ||
                        it.description.orEmpty().lowercase().contains(normalized)
                }
                .take(8)
                .map {
                    SearchResultItem(
                        id = "debt-${it.id}",
                        title = it.creditor,
                        subtitle = buildString {
                            append(it.description ?: "Dívida")
                            append(" • ")
                            append(it.originDate)
                        },
                        group = SearchGroup.DEBTS,
                        target = AppSelectionTarget.Debt(it.id),
                    )
                }

            SearchUiState(
                query = rawQuery,
                taskResults = tasks,
                financeResults = finances,
                debtResults = debts,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        viewModelScope.launch { databaseSeeder.seedIfNeeded() }
    }

    fun updateQuery(value: String) {
        query.value = value
    }
}
