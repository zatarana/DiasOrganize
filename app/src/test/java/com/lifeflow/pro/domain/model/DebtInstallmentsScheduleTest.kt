package com.lifeflow.pro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DebtInstallmentsScheduleTest {

    @Test
    fun `gera datas mensais corretas a partir da data inicial`() {
        val start = LocalDate.of(2026, 5, 10)

        val installments = FinanceRules.generateDebtInstallments(
            totalValue = 900.0,
            installmentCount = 4,
            firstDueDate = start,
        )

        assertEquals(4, installments.size)
        assertEquals(LocalDate.of(2026, 5, 10), installments[0].dueDate)
        assertEquals(LocalDate.of(2026, 6, 10), installments[1].dueDate)
        assertEquals(LocalDate.of(2026, 7, 10), installments[2].dueDate)
        assertEquals(LocalDate.of(2026, 8, 10), installments[3].dueDate)
    }
}
