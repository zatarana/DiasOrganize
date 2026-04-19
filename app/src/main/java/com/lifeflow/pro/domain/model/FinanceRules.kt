package com.lifeflow.pro.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object FinanceRules {
    fun calculateRealBalance(transactions: List<FinanceTransaction>): Double {
        val received = transactions
            .filter { it.type == TransactionType.RECEITA && it.status == TransactionStatus.RECEBIDO }
            .sumOf { it.finalValue ?: it.expectedValue }

        val paid = transactions
            .filter { it.type == TransactionType.DESPESA && it.status == TransactionStatus.PAGO }
            .sumOf { it.finalValue ?: it.expectedValue }

        return received - paid
    }

    fun calculateForecastBalance(transactions: List<FinanceTransaction>): Double {
        val real = calculateRealBalance(transactions)
        val receivable = transactions
            .filter { it.type == TransactionType.RECEITA && it.status == TransactionStatus.A_RECEBER }
            .sumOf { it.expectedValue }
        val payable = transactions
            .filter { it.type == TransactionType.DESPESA && it.status == TransactionStatus.A_PAGAR }
            .sumOf { it.expectedValue }
        return real + receivable - payable
    }

    fun calculateEconomy(expectedValue: Double, finalValue: Double): Double = expectedValue - finalValue

    fun generateDebtInstallments(
        totalValue: Double,
        installmentCount: Int,
        firstDueDate: LocalDate,
    ): List<InstallmentPlanItem> {
        require(installmentCount > 0) { "installmentCount must be greater than zero" }
        val total = BigDecimal.valueOf(totalValue).setScale(2, RoundingMode.HALF_UP)
        val count = BigDecimal.valueOf(installmentCount.toLong())
        val base = total.divide(count, 2, RoundingMode.DOWN)
        var accumulated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

        return (1..installmentCount).map { number ->
            val value = if (number == installmentCount) {
                total.subtract(accumulated)
            } else {
                base
            }
            accumulated = accumulated.add(value)
            InstallmentPlanItem(
                installmentNumber = number,
                value = value.toDouble(),
                dueDate = firstDueDate.plusMonths((number - 1).toLong()),
            )
        }
    }
}

data class FinanceTransaction(
    val type: TransactionType,
    val status: TransactionStatus,
    val expectedValue: Double,
    val finalValue: Double? = null,
)

data class InstallmentPlanItem(
    val installmentNumber: Int,
    val value: Double,
    val dueDate: LocalDate,
)

enum class TransactionType { RECEITA, DESPESA }
enum class TransactionStatus { A_RECEBER, RECEBIDO, A_PAGAR, PAGO }
