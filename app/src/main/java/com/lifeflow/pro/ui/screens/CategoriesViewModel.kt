package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val selectedType = MutableStateFlow("TASK")
    private val editor = MutableStateFlow(CategoryEditorState())
    private val editorVisible = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private var cache: List<CategoryEntity> = emptyList()

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeCategories(),
        selectedType,
        editor,
        editorVisible,
        message,
    ) { categories, type, editorState, visible, msg ->
        cache = categories
        CategoriesUiState(
            selectedType = type,
            visibleCategories = categories.filter { it.type == type },
            editor = editorState,
            isEditorVisible = visible,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun selectType(type: String) { selectedType.value = type }

    fun showCreate() {
        editor.value = CategoryEditorState(type = selectedType.value, color = defaultColorFor(selectedType.value))
        editorVisible.value = true
    }

    fun showEdit(id: Long) {
        cache.firstOrNull { it.id == id }?.let {
            editor.value = CategoryEditorState.fromEntity(it)
            editorVisible.value = true
        }
    }

    fun updateEditor(newState: CategoryEditorState) { editor.value = newState }
    fun dismissEditor() { editorVisible.value = false }
    fun clearMessage() { message.value = null }

    fun save() {
        val state = editor.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = state.id,
                name = state.name,
                color = state.color.ifBlank { defaultColorFor(state.type) },
                type = state.type,
            )
            if (state.id == 0L) categoryRepository.saveCategory(entity) else categoryRepository.updateCategory(entity)
            editorVisible.value = false
        }
    }

    fun delete(id: Long) {
        val category = cache.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            runCatching { categoryRepository.deleteCategory(category) }
                .onFailure { message.value = it.message ?: "Não foi possível excluir a categoria." }
        }
    }

    private fun defaultColorFor(type: String): String = when (type) {
        "INCOME" -> "#26A69A"
        "EXPENSE" -> "#FF7043"
        else -> "#78909C"
    }
}

data class CategoriesUiState(
    val selectedType: String = "TASK",
    val visibleCategories: List<CategoryEntity> = emptyList(),
    val editor: CategoryEditorState = CategoryEditorState(),
    val isEditorVisible: Boolean = false,
    val message: String? = null,
)

data class CategoryEditorState(
    val id: Long = 0,
    val name: String = "",
    val color: String = "#78909C",
    val type: String = "TASK",
) {
    companion object {
        fun fromEntity(entity: CategoryEntity) = CategoryEditorState(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            type = entity.type,
        )
    }
}
