package com.lifeflow.pro.domain.model

import com.lifeflow.pro.data.db.entities.AccountEntity
import com.lifeflow.pro.data.db.entities.BudgetEntity
import com.lifeflow.pro.data.db.entities.CategoryEntity
import com.lifeflow.pro.data.db.entities.GoalEntity
import com.lifeflow.pro.data.db.entities.TransactionEntity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

object FinanceConstants {
    const val TYPE_INCOME = "RECEITA"
    const val TYPE_EXPENSE = "DESPESA"
    const val TYPE_TRANSFER = "TRANSFERENCIA"

    const val STATUS_TO_RECEIVE = "A_RECEBER"
    const val STATUS_RECEIVED = "RECEBIDO"
    const val STATUS_TO_PAY = "A_PAGAR"
    const val STATUS_PAID = "PAGO"

    const val RECURRENCE_NONE = "NENHUMA"
    const val RECURRENCE_MONTHLY = "MENSAL"

    const val GOAL_ACTIVE = "ATIVA"
    const val GOAL_COMPLETED = "CONCLUIDA"

    const val TAB_TRANSACTIONS = 0
    const val TAB_FIXED = 1
    const val TAB_BUDGETS = 2
}

private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Double.toCurrencyBr(): String = currencyFormatter.format(this)
fun LocalDate.toBrDate(): String = format(dateFormatter)
fun String.isoDateToBr(): String = runCatching { LocalDate.parse(this).format(dateFormatter) }.getOrDefault(this)
fun currentMonthYear(): String = YearMonth.now().toString()
fun previousMonthYear(monthYear: String): String = YearMonth.parse(monthYear).minusMonths(1).toString()
fun nextMonthYear(monthYear: String): String = YearMonth.parse(monthYear).plusMonths(1).toString()

fun transactionStatusForType(type: String, paid: Boolean = false): String = when (type) {
    FinanceConstants.TYPE_INCOME -> if (paid) FinanceConstants.STATUS_RECEIVED else FinanceConstants.STATUS_TO_RECEIVE
    else -> if (paid) FinanceConstants.STATUS_PAID else FinanceConstants.STATUS_TO_PAY
}

fun isTransactionPaid(status: String): Boolean =
    status == FinanceConstants.STATUS_PAID || status == FinanceConstants.STATUS_RECEIVED

data class TransactionWithRelations(
    val transaction: TransactionEntity,
    val account: AccountEntity?,
    val category: CategoryEntity?,
)

data class BudgetProgress(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spentValue: Double,
    val usedPercentage: Float,
    val status: BudgetStatus,
)

enum class BudgetStatus { OK, WARNING, EXCEEDED }

fun calculateBudgetProgress(budget: BudgetEntity, category: CategoryEntity?, transactions: List<TransactionEntity>): BudgetProgress {
    val spent = transactions.filter {
        it.type == FinanceConstants.TYPE_EXPENSE &&
            it.categoryId == budget.categoryId &&
            YearMonth.parse(budget.monthYear) == YearMonth.parse(it.expectedDate.substring(0, 7))
    }.sumOf { it.finalValue ?: it.expectedValue }
    val planned = max(budget.plannedValue, 0.01)
    val ratio = (spent / planned).toFloat()
    val status = when {
        ratio >= 1f -> BudgetStatus.EXCEEDED
        ratio >= 0.7f -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
    return BudgetProgress(
        budget = budget,
        category = category,
        spentValue = spent,
        usedPercentage = ratio,
        status = status,
    )
}

data class GoalProgress(
    val goal: GoalEntity,
    val progress: Float,
)

fun calculateGoalProgress(goal: GoalEntity): GoalProgress {
    val progress = if (goal.targetValue <= 0.0) 0f else (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
    return GoalProgress(goal = goal, progress = progress)
}
