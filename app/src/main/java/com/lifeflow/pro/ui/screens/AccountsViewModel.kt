package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    private val editor = MutableStateFlow(AccountEditorState())
    private val isEditorVisible = MutableStateFlow(false)
    private var accountsCache: List<AccountEntity> = emptyList()

    val uiState: StateFlow<AccountsUiState> = combine(
        financeRepository.observeAccounts(),
        editor,
        isEditorVisible,
    ) { accounts, editorState, visible ->
        accountsCache = accounts
        AccountsUiState(accounts = accounts, editor = editorState, isEditorVisible = visible)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun showCreate() {
        editor.value = AccountEditorState(icon = "💳", color = "#4CAF50")
        isEditorVisible.value = true
    }

    fun showEdit(accountId: Long) {
        val account = accountsCache.firstOrNull { it.id == accountId } ?: return
        editor.value = AccountEditorState.fromEntity(account)
        isEditorVisible.value = true
    }

    fun dismissEditor() { isEditorVisible.value = false }
    fun updateEditor(newState: AccountEditorState) { editor.value = newState }

    fun saveAccount() {
        val state = editor.value
        viewModelScope.launch {
            val entity = AccountEntity(
                id = state.id,
                name = state.name,
                icon = state.icon.ifBlank { "💳" },
                color = state.color.ifBlank { "#4CAF50" },
                initialBalance = state.initialBalance.toDoubleOrNull() ?: 0.0,
                createdAt = state.createdAt ?: System.currentTimeMillis(),
            )
            if (state.id == 0L) financeRepository.saveAccount(entity) else financeRepository.updateAccount(entity)
            isEditorVisible.value = false
        }
    }

    fun deleteAccount(accountId: Long) {
        val account = accountsCache.firstOrNull { it.id == accountId } ?: return
        if (account.name == "Carteira") return
        viewModelScope.launch { financeRepository.deleteAccount(account) }
    }
}

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val editor: AccountEditorState = AccountEditorState(),
    val isEditorVisible: Boolean = false,
)

data class AccountEditorState(
    val id: Long = 0,
    val name: String = "",
    val icon: String = "💳",
    val color: String = "#4CAF50",
    val initialBalance: String = "0",
    val createdAt: Long? = null,
) {
    companion object {
        fun fromEntity(account: AccountEntity) = AccountEditorState(
            id = account.id,
            name = account.name,
            icon = account.icon,
            color = account.color,
            initialBalance = account.initialBalance.toString(),
            createdAt = account.createdAt,
        )
    }
}
