package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.DebtEntity
import com.lifeflow.pro.data.db.entities.DebtInstallmentEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object DebtConstants {
    const val STATUS_OPEN = "EM_ABERTO"
    const val STATUS_IN_PAYMENT = "EM_PAGAMENTO"
    const val STATUS_SETTLED = "QUITADA"

    const val INSTALLMENT_PENDING = "PENDENTE"
    const val INSTALLMENT_PAID = "PAGA"

    const val TAB_OPEN = 0
    const val TAB_IN_PAYMENT = 1
    const val TAB_SETTLED = 2
}

data class DebtListItem(
    val debt: DebtEntity,
    val installments: List<DebtInstallmentEntity>,
) {
    val paidInstallments: Int = installments.count { it.status == DebtConstants.INSTALLMENT_PAID }
    val totalInstallments: Int = installments.size
    val progress: Float = if (installments.isEmpty()) 0f else (paidInstallments.toFloat() / installments.size.toFloat())
    val nextInstallment: DebtInstallmentEntity? = installments
        .filter { it.status != DebtConstants.INSTALLMENT_PAID }
        .minByOrNull { it.dueDate }
    val paidValue: Double = installments.sumOf { it.finalValue ?: if (it.status == DebtConstants.INSTALLMENT_PAID) it.expectedValue else 0.0 }
    val remainingValue: Double = installments.filter { it.status != DebtConstants.INSTALLMENT_PAID }.sumOf { it.expectedValue }
    val finalPaidValue: Double = installments.sumOf { it.finalValue ?: it.expectedValue }
    val daysOpen: Long = runCatching {
        ChronoUnit.DAYS.between(LocalDate.parse(debt.originDate), LocalDate.now()).coerceAtLeast(0)
    }.getOrDefault(0)
}

data class DebtInstallmentWithAccount(
    val installment: DebtInstallmentEntity,
    val account: AccountEntity?,
)

fun Float.toPercentLabel(): String = "${(this * 100).roundToInt()}%"
