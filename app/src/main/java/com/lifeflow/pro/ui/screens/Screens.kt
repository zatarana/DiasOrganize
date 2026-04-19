package com.lifeflow.pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lifeflow.pro.R

@Composable
private fun ScreenCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RootScreen(title: String, paddingValues: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding -> content(innerPadding) }
}

data class MoreMenuItem(val titleRes: Int, val subtitleRes: Int, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreScreen(
    paddingValues: PaddingValues,
    onGoalsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onReportsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onBadgesClick: () -> Unit,
) {
    val items = listOf(
        MoreMenuItem(R.string.screen_goals, R.string.more_goals_subtitle, Icons.Default.Savings, onGoalsClick),
        MoreMenuItem(R.string.screen_calendar, R.string.more_calendar_subtitle, Icons.Default.CalendarMonth, onCalendarClick),
        MoreMenuItem(R.string.screen_reports, R.string.more_reports_subtitle, Icons.Default.BarChart, onReportsClick),
        MoreMenuItem(R.string.screen_backup, R.string.more_backup_subtitle, Icons.Default.SettingsBackupRestore, onBackupClick),
        MoreMenuItem(R.string.screen_appearance, R.string.more_appearance_subtitle, Icons.Default.Palette, onAppearanceClick),
        MoreMenuItem(R.string.screen_manage_accounts, R.string.more_accounts_subtitle, Icons.Default.AccountBalanceWallet, onManageAccountsClick),
        MoreMenuItem(R.string.screen_manage_categories, R.string.more_categories_subtitle, Icons.Default.Category, onManageCategoriesClick),
        MoreMenuItem(R.string.screen_badges, R.string.more_badges_subtitle, Icons.Default.EmojiEvents, onBadgesClick),
    )
    RootScreen(title = stringResource(R.string.screen_more), paddingValues = paddingValues) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable(onClick = item.onClick)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(item.icon, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = stringResource(item.titleRes), style = MaterialTheme.typography.titleMedium)
                            Text(text = stringResource(item.subtitleRes), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericSecondaryBody(paddingValues: PaddingValues, title: String, body: String) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenCard(title = title, body = body)
    }
}

@Composable
fun ReportsScreen(onBack: () -> Unit) = ReportsScreenContent(onBack = onBack)

@Composable
fun BackupScreen(onBack: () -> Unit) = BackupScreenContent(onBack = onBack)

@Composable
fun AppearanceScreen(onBack: () -> Unit) = AppearanceScreenContent(onBack = onBack)

@Composable
fun ManageCategoriesScreen(onBack: () -> Unit) = ManageCategoriesScreenContent(onBack = onBack)

@Composable
fun BadgesScreen(onBack: () -> Unit) = BadgesScreenContent(onBack = onBack)
