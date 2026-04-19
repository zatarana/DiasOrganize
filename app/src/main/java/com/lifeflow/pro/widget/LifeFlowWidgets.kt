
package com.lifeflow.pro.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import com.lifeflow.pro.data.db.AppDatabase
import com.lifeflow.pro.domain.model.DebtConstants
import com.lifeflow.pro.domain.model.FinanceConstants
import com.lifeflow.pro.domain.model.TaskConstants
import com.lifeflow.pro.domain.model.toCurrencyBr
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.ui.unit.dp

data class WidgetSnapshot(
    val realBalance: String = "R$ 0,00",
    val forecastBalance: String = "R$ 0,00",
    val nextDue: String = "Nenhum vencimento",
    val todayTasks: List<String> = emptyList(),
    val todayDueItems: List<String> = emptyList(),
    val oldestDebt: String = "Nenhuma dívida em aberto",
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDatabaseEntryPoint {
    fun database(): AppDatabase
}

object LifeFlowWidgets {
    suspend fun refreshAll(context: Context) {
        runCatching { FinanceSummaryWidget().updateAll(context) }
        runCatching { TodayAgendaWidget().updateAll(context) }
        runCatching { DebtHighlightWidget().updateAll(context) }
    }
}

private suspend fun loadSnapshot(context: Context): WidgetSnapshot = withContext(Dispatchers.IO) {
    val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetDatabaseEntryPoint::class.java)
    val db = entry.database()
    val accounts = db.accountDao().getAll()
    val tx = db.transactionDao().getAll()
    val tasks = db.taskDao().getAllOnce()
    val debts = db.debtDao().getAll()
    val installments = db.debtInstallmentDao().getAllSnapshot()

    val openingBalance = accounts.sumOf { it.initialBalance }
    val real = openingBalance + tx.filter { it.status == FinanceConstants.STATUS_RECEIVED }.sumOf { it.finalValue ?: it.expectedValue } - tx.filter { it.status == FinanceConstants.STATUS_PAID }.sumOf { it.finalValue ?: it.expectedValue }
    val forecast = real + tx.filter { it.status == FinanceConstants.STATUS_TO_RECEIVE }.sumOf { it.expectedValue } - tx.filter { it.status == FinanceConstants.STATUS_TO_PAY }.sumOf { it.expectedValue }
    val nextDueTx = tx.filter { it.status == FinanceConstants.STATUS_TO_PAY || it.status == FinanceConstants.STATUS_TO_RECEIVE }.minByOrNull { it.expectedDate }
    val today = LocalDate.now().toString()
    val todayTasks = tasks.filter { it.status != TaskConstants.STATUS_COMPLETED && it.dueDate == today }.take(3).map { it.title }
    val todayDue = buildList {
        addAll(tx.filter { it.expectedDate == today && (it.status == FinanceConstants.STATUS_TO_PAY || it.status == FinanceConstants.STATUS_TO_RECEIVE) }.take(3).map { it.description ?: "Transação" })
        addAll(installments.filter { it.dueDate == today && it.status != DebtConstants.INSTALLMENT_PAID }.take(3).map { "Parcela ${it.installmentNumber}" })
    }.take(3)
    val oldestDebt = debts.filter { it.status == DebtConstants.STATUS_OPEN }.minByOrNull { it.originDate }?.let { "${it.creditor} • ${it.originalValue.toCurrencyBr()}" } ?: "Nenhuma dívida em aberto"

    WidgetSnapshot(
        realBalance = real.toCurrencyBr(),
        forecastBalance = forecast.toCurrencyBr(),
        nextDue = nextDueTx?.let { (it.description ?: "Transação") + " • " + it.expectedDate.split("-").reversed().joinToString("/") } ?: "Nenhum vencimento",
        todayTasks = todayTasks,
        todayDueItems = todayDue,
        oldestDebt = oldestDebt,
    )
}

class FinanceSummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val data = loadSnapshot(context)
        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                Text("Saldo real: ${data.realBalance}")
                Text("Saldo previsto: ${data.forecastBalance}")
                Text("Próximo: ${data.nextDue}")
            }
        }
    }
}

class TodayAgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val data = loadSnapshot(context)
        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                Text("Agenda do dia")
                data.todayTasks.forEach { Text("• $it") }
                data.todayDueItems.forEach { Text("◦ $it") }
                if (data.todayTasks.isEmpty() && data.todayDueItems.isEmpty()) {
                    Text("Nada pendente hoje")
                }
            }
        }
    }
}

class DebtHighlightWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val data = loadSnapshot(context)
        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                Text("Dívida em destaque")
                Text(data.oldestDebt)
            }
        }
    }
}

class FinanceSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanceSummaryWidget()
}

class TodayAgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayAgendaWidget()
}

class DebtHighlightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DebtHighlightWidget()
}
