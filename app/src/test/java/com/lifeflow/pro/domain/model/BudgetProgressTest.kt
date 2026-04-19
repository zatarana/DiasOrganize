package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetProgressTest {

    @Test
    fun `budget status fica warning ao passar de 70 por cento`() {
        val budget = BudgetEntity(id = 1, categoryId = 10, monthYear = "2026-04", plannedValue = 1000.0, createdAt = 0)
        val category = CategoryEntity(id = 10, name = "Moradia", color = "#000000", type = "EXPENSE")
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = FinanceConstants.TYPE_EXPENSE,
                accountId = 1,
                categoryId = 10,
                description = "Aluguel",
                expectedValue = 750.0,
                finalValue = 750.0,
                expectedDate = "2026-04-10",
                paymentDate = "2026-04-10",
                status = FinanceConstants.STATUS_PAID,
                recurrenceType = FinanceConstants.RECURRENCE_NONE,
                recurrenceGroupId = null,
                economy = 0.0,
                createdAt = 0,
            )
        )

        val result = calculateBudgetProgress(budget, category, transactions)

        assertEquals(BudgetStatus.WARNING, result.status)
    }
}
