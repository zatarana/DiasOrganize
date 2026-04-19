
package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.TaskEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ReportsSummaryTest {
    @Test
    fun `buildReportsUiState aggregates current month values`() {
        val tx = listOf(
            TransactionEntity(1, FinanceConstants.TYPE_INCOME, 1, 1, "Salário", 2000.0, 2000.0, "2026-04-05", "2026-04-05", FinanceConstants.STATUS_RECEIVED, FinanceConstants.RECURRENCE_NONE, null, 0.0, 1L),
            TransactionEntity(2, FinanceConstants.TYPE_EXPENSE, 1, 2, "Aluguel", 800.0, 750.0, "2026-04-10", "2026-04-10", FinanceConstants.STATUS_PAID, FinanceConstants.RECURRENCE_NONE, null, 50.0, 1L),
        )
        val tasks = listOf(
            TaskEntity(1, "T1", null, null, TaskConstants.STATUS_COMPLETED, "2026-04-01", null, TaskConstants.RECURRENCE_NONE, null, TaskConstants.PRIORITY_HIGH, null, null, 1L, "2026-04-01T08:00:00")
        )
        val state = buildReportsUiState(YearMonth.of(2026, 4), tx, emptyList(), emptyList(), tasks)
        assertEquals(2000.0, state.totalReceived, 0.001)
        assertEquals(750.0, state.totalPaid, 0.001)
        assertEquals(50.0, state.economyGenerated, 0.001)
        assertTrue(state.taskCompletionRate > 0f)
    }
}
