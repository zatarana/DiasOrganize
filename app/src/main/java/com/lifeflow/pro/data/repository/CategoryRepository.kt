package com.lifeflow.pro.data.repository

import com.lifeflow.pro.data.db.SeedData
import com.lifeflow.pro.data.db.dao.CategoryDao
import com.lifeflow.pro.data.db.entities.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun saveCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        require(!isDefaultCategory(category)) { "Categorias padrão não podem ser excluídas." }
        categoryDao.delete(category)
    }

    fun isDefaultCategory(category: CategoryEntity): Boolean {
        return SeedData.defaultCategories().any { it.name == category.name && it.type == category.type }
    }
}
