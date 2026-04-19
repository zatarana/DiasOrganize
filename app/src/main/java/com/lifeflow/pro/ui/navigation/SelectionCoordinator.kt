package com.lifeflow.pro.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppSelectionTarget {
    data class Task(val taskId: Long) : AppSelectionTarget
    data class Transaction(val transactionId: Long) : AppSelectionTarget
    data class Debt(val debtId: Long) : AppSelectionTarget
}

object SelectionCoordinator {
    private val _target = MutableStateFlow<AppSelectionTarget?>(null)
    val target: StateFlow<AppSelectionTarget?> = _target.asStateFlow()

    fun focus(target: AppSelectionTarget) {
        _target.value = target
    }

    fun clear() {
        _target.value = null
    }
}
