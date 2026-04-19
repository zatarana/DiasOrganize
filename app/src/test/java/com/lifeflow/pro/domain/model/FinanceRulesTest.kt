package com.lifeflow.pro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceRulesTest {

    @Test
    fun `calcula saldo real com receitas recebidas menos despesas pagas`() {
        val transactions = listOf(
            FinanceTransaction(TransactionType.RECEITA, TransactionStatus.RECEBIDO, 1500.0),
            FinanceTransaction(TransactionType.DESPESA, TransactionStatus.PAGO, 200.0),
            FinanceTransaction(TransactionType.RECEITA, TransactionStatus.A_RECEBER, 300.0),
            FinanceTransaction(TransactionType.DESPESA, TransactionStatus.A_PAGAR, 100.0),
        )

        val result = FinanceRules.calculateRealBalance(transactions)

        assertEquals(1300.0, result, 0.0001)
    }

    @Test
    fun `calcula saldo previsto a partir do saldo real mais pendencias`() {
        val transactions = listOf(
            FinanceTransaction(TransactionType.RECEITA, TransactionStatus.RECEBIDO, 1000.0),
            FinanceTransaction(TransactionType.DESPESA, TransactionStatus.PAGO, 400.0),
            FinanceTransaction(TransactionType.RECEITA, TransactionStatus.A_RECEBER, 200.0),
            FinanceTransaction(TransactionType.DESPESA, TransactionStatus.A_PAGAR, 50.0),
        )

        val result = FinanceRules.calculateForecastBalance(transactions)

        assertEquals(750.0, result, 0.0001)
    }

    @Test
    fun `calcula economia positiva quando valor final e menor`() {
        assertEquals(20.0, FinanceRules.calculateEconomy(100.0, 80.0), 0.0001)
    }

    @Test
    fun `calcula economia zero quando valor final e igual`() {
        assertEquals(0.0, FinanceRules.calculateEconomy(100.0, 100.0), 0.0001)
    }

    @Test
    fun `calcula economia negativa quando valor final e maior`() {
        assertEquals(-15.0, FinanceRules.calculateEconomy(100.0, 115.0), 0.0001)
    }

    @Test
    fun `gera parcelas negociadas com datas mensais sequenciais e soma exata`() {
        val firstDate = LocalDate.of(2026, 4, 19)

        val installments = FinanceRules.generateDebtInstallments(
            totalValue = 1000.0,
            installmentCount = 3,
            firstDueDate = firstDate,
        )

        assertEquals(3, installments.size)
        assertEquals(firstDate, installments[0].dueDate)
        assertEquals(firstDate.plusMonths(1), installments[1].dueDate)
        assertEquals(firstDate.plusMonths(2), installments[2].dueDate)
        assertEquals(1000.0, installments.sumOf { it.value }, 0.0001)
        assertTrue(installments[2].value >= installments[0].value)
    }
}
