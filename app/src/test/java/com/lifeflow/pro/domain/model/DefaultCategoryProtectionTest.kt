package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.repository.CategoryRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoryProtectionTest {
    @Test
    fun `seed categories are treated as protected`() {
        val repo = CategoryRepositoryFake()
        assertTrue(repo.isDefault(CategoryEntity(name = "Alimentação", color = "#fff", type = "EXPENSE")))
        assertTrue(repo.isDefault(CategoryEntity(name = "Salário", color = "#fff", type = "INCOME")))
        assertFalse(repo.isDefault(CategoryEntity(name = "Freela", color = "#fff", type = "INCOME")))
    }
}

private class CategoryRepositoryFake {
    fun isDefault(category: CategoryEntity): Boolean {
        return com.lifeflow.pro.data.db.SeedData.defaultCategories().any { it.name == category.name && it.type == category.type }
    }
}
