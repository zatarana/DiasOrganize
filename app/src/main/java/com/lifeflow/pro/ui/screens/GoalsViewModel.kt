package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.repository.GoalRepository
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.GoalProgress
import com.lifeflow.pro.domain.model.calculateGoalProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val editor = MutableStateFlow(GoalEditorState())
    private val isEditorVisible = MutableStateFlow(false)
    private var currentGoals: List<GoalEntity> = emptyList()

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeGoals(),
        editor,
        isEditorVisible,
    ) { goals, editorState, visible ->
        currentGoals = goals
        GoalsUiState(
            goals = goals.map(::calculateGoalProgress),
            editor = editorState,
            isEditorVisible = visible,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun showCreate() {
        editor.value = GoalEditorState(icon = "💰")
        isEditorVisible.value = true
    }

    fun showEdit(goalId: Long) {
        val goal = currentGoals.firstOrNull { it.id == goalId } ?: return
        editor.value = GoalEditorState.fromEntity(goal)
        isEditorVisible.value = true
    }

    fun dismissEditor() { isEditorVisible.value = false }
    fun updateEditor(newState: GoalEditorState) { editor.value = newState }

    fun saveGoal() {
        val state = editor.value
        viewModelScope.launch {
            val target = state.targetValue.toDoubleOrNull() ?: return@launch
            val current = state.currentValue.toDoubleOrNull() ?: 0.0
            val status = if (current >= target && target > 0.0) FinanceConstants.GOAL_COMPLETED else FinanceConstants.GOAL_ACTIVE
            val entity = GoalEntity(
                id = state.id,
                name = state.name,
                icon = state.icon.ifBlank { "💰" },
                targetValue = target,
                currentValue = current,
                targetDate = state.targetDate.ifBlank { null },
                status = status,
                completedAt = if (status == FinanceConstants.GOAL_COMPLETED) System.currentTimeMillis() else null,
                createdAt = state.createdAt ?: System.currentTimeMillis(),
            )
            if (state.id == 0L) goalRepository.saveGoal(entity) else goalRepository.updateGoal(entity)
            isEditorVisible.value = false
        }
    }

    fun deleteGoal(goalId: Long) {
        val goal = currentGoals.firstOrNull { it.id == goalId } ?: return
        viewModelScope.launch { goalRepository.deleteGoal(goal) }
    }
}

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val editor: GoalEditorState = GoalEditorState(),
    val isEditorVisible: Boolean = false,
)

data class GoalEditorState(
    val id: Long = 0,
    val name: String = "",
    val icon: String = "💰",
    val targetValue: String = "",
    val currentValue: String = "",
    val targetDate: String = "",
    val createdAt: Long? = null,
) {
    companion object {
        fun fromEntity(goal: GoalEntity) = GoalEditorState(
            id = goal.id,
            name = goal.name,
            icon = goal.icon,
            targetValue = goal.targetValue.toString(),
            currentValue = goal.currentValue.toString(),
            targetDate = goal.targetDate.orEmpty(),
            createdAt = goal.createdAt,
        )
    }
}
