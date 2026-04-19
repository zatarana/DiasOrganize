
package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.R
import com.lifeflow.pro.domain.model.BadgeUiItem
import com.lifeflow.pro.domain.model.BudgetProgress
import com.lifeflow.pro.domain.model.BudgetStatus
import com.lifeflow.pro.domain.model.CountItem
import com.lifeflow.pro.domain.model.ReportAmountItem
import com.lifeflow.pro.domain.model.ReportsUiState
import com.lifeflow.pro.domain.model.WeeklyFinanceItem
import com.lifeflow.pro.domain.model.toCurrencyBr
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenContent(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_reports)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) { Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.calendar_prev_month)) }
                    Text(text = uiState.month.toString(), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = viewModel::nextMonth) { Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.calendar_next_month)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SummaryGrid(uiState)
            }
            item {
                ComparisonCard(uiState)
            }
            item {
                SectionCard(stringResource(R.string.reports_expense_chart_title)) {
                    if (uiState.expenseByCategory.isEmpty()) {
                        Text(stringResource(R.string.reports_empty))
                    } else {
                        HorizontalAmountChart(uiState.expenseByCategory)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.reports_weekly_chart_title)) {
                    if (uiState.weeklyFinance.all { it.income == 0.0 && it.expense == 0.0 }) {
                        Text(stringResource(R.string.reports_empty))
                    } else {
                        WeeklyFinanceChart(uiState.weeklyFinance)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.reports_balance_chart_title)) {
                    if (uiState.balanceSeries.size < 2) {
                        Text(stringResource(R.string.reports_empty))
                    } else {
                        BalanceLineChart(uiState)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.reports_budgets_title)) {
                    if (uiState.budgetRows.isEmpty()) {
                        Text(stringResource(R.string.reports_empty))
                    } else {
                        BudgetRows(uiState.budgetRows)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.reports_tasks_title)) {
                    TaskReport(uiState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreenContent(
    onBack: () -> Unit,
    viewModel: BadgesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_badges)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.dashboard_health_title), style = MaterialTheme.typography.titleMedium)
                        Text(text = uiState.health.levelLabel, style = MaterialTheme.typography.headlineSmall)
                        Text(text = stringResource(R.string.dashboard_health_score, uiState.health.score))
                        LinearProgressIndicator(progress = { uiState.health.progressToNext }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(uiState.badges, key = { it.id }) { badge -> BadgeCard(badge) }
        }
    }
}

@Composable
private fun SummaryGrid(uiState: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.reports_total_received), uiState.totalReceived.toCurrencyBr(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.reports_total_paid), uiState.totalPaid.toCurrencyBr(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.reports_final_balance), uiState.finalBalance.toCurrencyBr(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.reports_economy_generated), uiState.economyGenerated.toCurrencyBr(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ComparisonCard(uiState: ReportsUiState) {
    SectionCard(stringResource(R.string.reports_comparison_title)) {
        ComparisonRow(stringResource(R.string.reports_total_received), uiState.comparison.receivedDeltaPct)
        ComparisonRow(stringResource(R.string.reports_total_paid), uiState.comparison.paidDeltaPct)
        ComparisonRow(stringResource(R.string.reports_economy_generated), uiState.comparison.economyDeltaPct)
        ComparisonRow(stringResource(R.string.reports_final_balance), uiState.comparison.finalBalanceDeltaPct)
    }
}

@Composable
private fun ComparisonRow(label: String, value: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(String.format("%+.1f%%", value), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun HorizontalAmountChart(items: List<ReportAmountItem>) {
    val maxValue = items.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.label)
                    Text(item.value.toCurrencyBr(), fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(progress = { (item.value / maxValue).toFloat() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WeeklyFinanceChart(items: List<WeeklyFinanceItem>) {
    val maxValue = items.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Receitas: ${item.income.toCurrencyBr()}")
                LinearProgressIndicator(progress = { (item.income / maxValue).toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("Despesas: ${item.expense.toCurrencyBr()}")
                LinearProgressIndicator(progress = { (item.expense / maxValue).toFloat() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BalanceLineChart(uiState: ReportsUiState) {
    val realPoints = uiState.balanceSeries.map { it.realBalance }
    val forecastPoints = uiState.balanceSeries.map { it.forecastBalance }
    val maxY = max(realPoints.maxOrNull() ?: 0.0, forecastPoints.maxOrNull() ?: 0.0)
    val minY = minOf(realPoints.minOrNull() ?: 0.0, forecastPoints.minOrNull() ?: 0.0)
    val range = (maxY - minY).takeIf { it > 0.0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val points = uiState.balanceSeries
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        fun y(value: Double): Float = ((maxY - value) / range).toFloat() * size.height
        for (i in 0 until points.lastIndex) {
            drawLine(
                color = Color(0xFF1565C0),
                start = Offset(stepX * i, y(points[i].realBalance)),
                end = Offset(stepX * (i + 1), y(points[i + 1].realBalance)),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF6A1B9A),
                start = Offset(stepX * i, y(points[i].forecastBalance)),
                end = Offset(stepX * (i + 1), y(points[i + 1].forecastBalance)),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.reports_real_balance_line))
        Text(stringResource(R.string.reports_forecast_balance_line))
    }
}

@Composable
private fun BudgetRows(items: List<BudgetProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.category?.name ?: stringResource(R.string.dashboard_budget_generic))
                    Text(item.spentValue.toCurrencyBr())
                }
                LinearProgressIndicator(progress = { item.usedPercentage.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(
                    when (item.status) {
                        BudgetStatus.OK -> stringResource(R.string.budget_status_ok)
                        BudgetStatus.WARNING -> stringResource(R.string.budget_status_warning)
                        BudgetStatus.EXCEEDED -> stringResource(R.string.budget_status_exceeded)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TaskReport(uiState: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reports_task_completion, uiState.completedTasks, uiState.totalTasks))
        LinearProgressIndicator(progress = { uiState.taskCompletionRate }, modifier = Modifier.fillMaxWidth())
        if (uiState.tasksByPriority.isNotEmpty()) {
            Text(stringResource(R.string.reports_tasks_by_priority), fontWeight = FontWeight.SemiBold)
            CountList(uiState.tasksByPriority)
        }
        if (uiState.tasksByCategory.isNotEmpty()) {
            Text(stringResource(R.string.reports_tasks_by_category), fontWeight = FontWeight.SemiBold)
            CountList(uiState.tasksByCategory)
        }
        if (uiState.streakHistory.isNotEmpty()) {
            Text(stringResource(R.string.reports_streak_history), fontWeight = FontWeight.SemiBold)
            CountList(uiState.streakHistory)
        }
    }
}

@Composable
private fun CountList(items: List<CountItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label)
                Text(item.count.toString(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeUiItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = if (badge.unlocked) badge.icon else "🔒", style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = badge.title, style = MaterialTheme.typography.titleMedium)
                Text(text = badge.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (badge.unlocked) stringResource(R.string.badge_unlocked_on, badge.unlockedDateLabel ?: "—") else stringResource(R.string.badge_locked),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
