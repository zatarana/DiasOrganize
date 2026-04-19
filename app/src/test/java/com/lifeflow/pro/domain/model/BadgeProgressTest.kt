
package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeProgressTest {
    @Test
    fun `buildBadgeItems unlocks first debt and first goal`() {
        val badges = buildBadgeItems(
            tasks = emptyList(),
            debts = listOf(DebtEntity(1, "Banco", null, 100.0, 90.0, "2026-04-01", DebtConstants.STATUS_SETTLED, 10.0, 1L)),
            goals = listOf(GoalEntity(1, "Reserva", "💰", 1000.0, 1000.0, null, FinanceConstants.GOAL_COMPLETED, 2L, 1L)),
            budgets = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
        )
        assertTrue(badges.first { it.id == "first_debt" }.unlocked)
        assertTrue(badges.first { it.id == "first_goal" }.unlocked)
    }

    @Test
    fun `countMonthsWithoutBudgetOverflow counts valid months`() {
        val budgets = listOf(
            BudgetEntity(1, 1, "2026-02", 100.0, 1L),
            BudgetEntity(2, 1, "2026-03", 100.0, 1L),
            BudgetEntity(3, 1, "2026-04", 100.0, 1L),
        )
        val tx = listOf(
            TransactionEntity(1, FinanceConstants.TYPE_EXPENSE, 1, 1, null, 50.0, 50.0, "2026-02-05", "2026-02-05", FinanceConstants.STATUS_PAID, FinanceConstants.RECURRENCE_NONE, null, 0.0, 1L),
            TransactionEntity(2, FinanceConstants.TYPE_EXPENSE, 1, 1, null, 60.0, 60.0, "2026-03-05", "2026-03-05", FinanceConstants.STATUS_PAID, FinanceConstants.RECURRENCE_NONE, null, 0.0, 1L),
            TransactionEntity(3, FinanceConstants.TYPE_EXPENSE, 1, 1, null, 70.0, 70.0, "2026-04-05", "2026-04-05", FinanceConstants.STATUS_PAID, FinanceConstants.RECURRENCE_NONE, null, 0.0, 1L),
        )
        assertEquals(3, countMonthsWithoutBudgetOverflow(budgets, tx))
    }
}
