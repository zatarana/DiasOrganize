package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.R
import com.lifeflow.pro.domain.model.BudgetProgress
import com.lifeflow.pro.domain.model.CalendarEventItem
import com.lifeflow.pro.domain.model.CalendarMarkerType
import com.lifeflow.pro.domain.model.DashboardDueItem
import com.lifeflow.pro.domain.model.DashboardTaskItem
import com.lifeflow.pro.domain.model.GoalProgress
import com.lifeflow.pro.domain.model.SearchResultItem
import com.lifeflow.pro.domain.model.SearchUiState
import com.lifeflow.pro.domain.model.isoDateToBr
import com.lifeflow.pro.domain.model.toCurrencyBr
import com.lifeflow.pro.ui.navigation.AppSelectionTarget
import com.lifeflow.pro.ui.navigation.SelectionCoordinator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    paddingValues: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenDebts: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_home)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.dashboard_search_cd))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.dashboard_balance_title), style = MaterialTheme.typography.titleLarge)
                        Text(text = stringResource(R.string.dashboard_real_balance, uiState.realBalance.toCurrencyBr()), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(R.string.dashboard_forecast_balance, uiState.forecastBalance.toCurrencyBr()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.dashboard_health_title), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(R.string.dashboard_health_score, uiState.health.score), style = MaterialTheme.typography.bodyLarge)
                        Text(text = uiState.health.levelLabel, style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(progress = { uiState.health.progressToNext.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        uiState.health.nextLevelLabel?.let { next ->
                            Text(text = stringResource(R.string.dashboard_next_level, next), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.dashboard_budgets_title), actionLabel = stringResource(R.string.action_view_all), onAction = onOpenFinance)
                Spacer(modifier = Modifier.height(8.dp))
                SectionColumn {
                    if (uiState.budgetItems.isEmpty()) {
                        EmptySectionCard(stringResource(R.string.dashboard_budgets_empty))
                    } else {
                        uiState.budgetItems.forEach { BudgetSummaryCard(it) }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.dashboard_goals_title), actionLabel = stringResource(R.string.action_view_all), onAction = onOpenGoals)
                Spacer(modifier = Modifier.height(8.dp))
                SectionColumn {
                    if (uiState.goals.isEmpty()) {
                        EmptySectionCard(stringResource(R.string.dashboard_goals_empty))
                    } else {
                        uiState.goals.forEach { GoalSummaryCard(it) }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.dashboard_tasks_title), actionLabel = stringResource(R.string.action_view_all), onAction = onOpenTasks)
                Spacer(modifier = Modifier.height(8.dp))
                SectionColumn {
                    if (uiState.upcomingTasks.isEmpty()) {
                        EmptySectionCard(stringResource(R.string.dashboard_tasks_empty))
                    } else {
                        uiState.upcomingTasks.forEach { task ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = task.isCompleted, onCheckedChange = { viewModel.toggleTask(task.id) })
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                                        Text(text = task.subtitle, style = MaterialTheme.typography.bodySmall)
                                        task.categoryLabel?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = stringResource(R.string.dashboard_due_title), style = MaterialTheme.typography.titleMedium)
                        if (uiState.streak >= 3) {
                            Text(text = stringResource(R.string.dashboard_streak_label, uiState.streak), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = onOpenDebts) { Text(stringResource(R.string.dashboard_open_debts_action)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SectionColumn {
                    if (uiState.upcomingDueItems.isEmpty()) {
                        EmptySectionCard(stringResource(R.string.dashboard_due_empty))
                    } else {
                        uiState.upcomingDueItems.forEach { dueItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SelectionCoordinator.focus(dueItem.target)
                                        when (dueItem.target) {
                                            is AppSelectionTarget.Transaction -> onOpenFinance()
                                            is AppSelectionTarget.Debt -> onOpenDebts()
                                            is AppSelectionTarget.Task -> onOpenTasks()
                                        }
                                    },
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = dueItem.title, style = MaterialTheme.typography.titleMedium)
                                    Text(text = dueItem.subtitle, style = MaterialTheme.typography.bodySmall)
                                    dueItem.amountLabel?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun SectionColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun EmptySectionCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(text = message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BudgetSummaryCard(item: BudgetProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = item.category?.name ?: stringResource(R.string.dashboard_budget_generic), style = MaterialTheme.typography.titleMedium)
                Text(text = item.budget.plannedValue.toCurrencyBr(), style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = { item.usedPercentage.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text(text = stringResource(R.string.dashboard_budget_spent, item.spentValue.toCurrencyBr()))
        }
    }
}

@Composable
private fun GoalSummaryCard(item: GoalProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "${item.goal.icon} ${item.goal.name}", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
            Text(text = stringResource(R.string.dashboard_goal_progress, item.goal.currentValue.toCurrencyBr(), item.goal.targetValue.toCurrencyBr()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTarget: (AppSelectionTarget) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                label = { Text(stringResource(R.string.search_field_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            when {
                !uiState.hasQuery -> {
                    EmptySectionCard(stringResource(R.string.search_hint))
                }
                !uiState.hasResults -> {
                    EmptySectionCard(stringResource(R.string.search_empty))
                }
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        searchSection(uiState.taskResults, stringResource(R.string.search_group_tasks), onOpenTarget)
                        searchSection(uiState.financeResults, stringResource(R.string.search_group_finance), onOpenTarget)
                        searchSection(uiState.debtResults, stringResource(R.string.search_group_debts), onOpenTarget)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchSection(
    items: List<SearchResultItem>,
    title: String,
    onOpenTarget: (AppSelectionTarget) -> Unit,
) {
    if (items.isNotEmpty()) {
        item {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SelectionCoordinator.focus(item.target)
                        onOpenTarget(item.target)
                    },
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenTarget: (AppSelectionTarget) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cells = rememberCalendarCells(uiState.month)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_calendar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::previousMonth) { Text(stringResource(R.string.calendar_prev_month)) }
                Text(text = uiState.month.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } + " ${uiState.month.year}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::nextMonth) { Text(stringResource(R.string.calendar_next_month)) }
            }

            WeekdayHeaderRow()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                isSelected = date == uiState.selectedDate,
                                markerTypes = uiState.eventsByDate[date].orEmpty().map { it.markerType }.distinct(),
                                onClick = { selected -> viewModel.selectDate(selected) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Text(text = stringResource(R.string.calendar_events_for_day, uiState.selectedDate.isoDateToBr()), style = MaterialTheme.typography.titleMedium)

            if (uiState.selectedDateEvents.isEmpty()) {
                EmptySectionCard(stringResource(R.string.calendar_empty_day))
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.selectedDateEvents, key = { it.id }) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SelectionCoordinator.focus(event.target)
                                    onOpenTarget(event.target)
                                },
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(markerColor(event.markerType), CircleShape))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                                    Text(text = event.subtitle, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val locale = Locale("pt", "BR")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.values().forEach { day ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = day.getDisplayName(TextStyle.SHORT, locale).take(3), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    markerTypes: List<CalendarMarkerType>,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .height(68.dp)
            .background(background, RoundedCornerShape(12.dp))
            .let {
                if (date != null) it.clickable { onClick(date) } else it
            }
            .padding(8.dp),
    ) {
        if (date != null) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    markerTypes.take(4).forEach { type ->
                        Box(modifier = Modifier.size(8.dp).background(markerColor(type), CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun markerColor(type: CalendarMarkerType): Color = when (type) {
    CalendarMarkerType.TASK -> Color(0xFF4A90E2)
    CalendarMarkerType.EXPENSE -> Color(0xFFE67E22)
    CalendarMarkerType.DEBT -> Color(0xFF8E44AD)
    CalendarMarkerType.INCOME -> Color(0xFF27AE60)
}

private fun rememberCalendarCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()
    return buildList {
        repeat(startOffset) { add(null) }
        repeat(totalDays) { add(month.atDay(it + 1)) }
        while (size % 7 != 0) add(null)
    }
}
