package com.lifeflow.pro.data.db

import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity

object SeedData {
    fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Alimentação", color = "#FF7043", type = "EXPENSE"),
        CategoryEntity(name = "Transporte", color = "#29B6F6", type = "EXPENSE"),
        CategoryEntity(name = "Saúde", color = "#66BB6A", type = "EXPENSE"),
        CategoryEntity(name = "Lazer", color = "#AB47BC", type = "EXPENSE"),
        CategoryEntity(name = "Moradia", color = "#8D6E63", type = "EXPENSE"),
        CategoryEntity(name = "Salário", color = "#26A69A", type = "INCOME"),
        CategoryEntity(name = "Outros", color = "#78909C", type = "TASK"),
    )

    fun defaultAccount(now: Long = System.currentTimeMillis()): AccountEntity =
        AccountEntity(
            name = "Carteira",
            icon = "wallet",
            color = "#4CAF50",
            initialBalance = 0.0,
            createdAt = now,
        )
}
