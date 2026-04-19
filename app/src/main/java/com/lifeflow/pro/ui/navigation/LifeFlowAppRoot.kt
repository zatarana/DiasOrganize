package com.lifeflow.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeflow.pro.ui.onboarding.OnboardingPreferences
import com.lifeflow.pro.ui.onboarding.OnboardingScreen
import com.lifeflow.pro.ui.screens.AppearanceScreen
import com.lifeflow.pro.ui.screens.BackupScreen
import com.lifeflow.pro.ui.screens.BadgesScreen
import com.lifeflow.pro.ui.screens.CalendarScreen
import com.lifeflow.pro.ui.screens.DashboardScreen
import com.lifeflow.pro.ui.screens.DebtsScreen
import com.lifeflow.pro.ui.screens.FinanceScreen
import com.lifeflow.pro.ui.screens.GoalsScreen
import com.lifeflow.pro.ui.screens.ManageAccountsScreen
import com.lifeflow.pro.ui.screens.ManageCategoriesScreen
import com.lifeflow.pro.ui.screens.MoreScreen
import com.lifeflow.pro.ui.screens.ReportsScreen
import com.lifeflow.pro.ui.screens.SearchScreen
import com.lifeflow.pro.ui.screens.TasksScreen

@Composable
fun LifeFlowAppRoot() {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    var onboardingCompleted by remember { mutableStateOf(onboardingPreferences.isCompleted()) }

    if (!onboardingCompleted) {
        OnboardingScreen(
            onFinish = {
                onboardingPreferences.setCompleted(true)
                onboardingCompleted = true
            },
        )
    } else {
        MainAppShell()
    }
}

@Composable
private fun MainAppShell() {
    val navController = rememberNavController()
    val items = listOf(
        AppDestination.Home,
        AppDestination.Tasks,
        AppDestination.Finance,
        AppDestination.Debts,
        AppDestination.More,
    )

    fun openSelectionTarget(target: AppSelectionTarget) {
        val route = when (target) {
            is AppSelectionTarget.Task -> AppDestination.Tasks.route
            is AppSelectionTarget.Transaction -> AppDestination.Finance.route
            is AppSelectionTarget.Debt -> AppDestination.Debts.route
        }
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val showBottomBar = items.any { destination ->
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
            }

            if (showBottomBar) {
                NavigationBar {
                    items.forEach { destination ->
                        val icon = when (destination) {
                            AppDestination.Home -> Icons.Default.Home
                            AppDestination.Tasks -> Icons.Default.CheckCircle
                            AppDestination.Finance -> Icons.Default.AccountBalanceWallet
                            AppDestination.Debts -> Icons.Default.ReceiptLong
                            AppDestination.More -> Icons.Default.Menu
                            else -> Icons.Default.Home
                        }
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(destination.labelRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
        ) {
            composable(AppDestination.Home.route) {
                DashboardScreen(
                    paddingValues = innerPadding,
                    onOpenSearch = { navController.navigate(AppDestination.Search.route) },
                    onOpenTasks = { navController.navigate(AppDestination.Tasks.route) },
                    onOpenFinance = { navController.navigate(AppDestination.Finance.route) },
                    onOpenGoals = { navController.navigate(AppDestination.Goals.route) },
                    onOpenDebts = { navController.navigate(AppDestination.Debts.route) },
                )
            }
            composable(AppDestination.Tasks.route) { TasksScreen(innerPadding) }
            composable(AppDestination.Finance.route) { FinanceScreen(innerPadding) }
            composable(AppDestination.Debts.route) { DebtsScreen(innerPadding) }
            composable(AppDestination.More.route) {
                MoreScreen(
                    paddingValues = innerPadding,
                    onGoalsClick = { navController.navigate(AppDestination.Goals.route) },
                    onCalendarClick = { navController.navigate(AppDestination.Calendar.route) },
                    onReportsClick = { navController.navigate(AppDestination.Reports.route) },
                    onBackupClick = { navController.navigate(AppDestination.Backup.route) },
                    onAppearanceClick = { navController.navigate(AppDestination.Appearance.route) },
                    onManageAccountsClick = { navController.navigate(AppDestination.ManageAccounts.route) },
                    onManageCategoriesClick = { navController.navigate(AppDestination.ManageCategories.route) },
                    onBadgesClick = { navController.navigate(AppDestination.Badges.route) },
                )
            }
            composable(AppDestination.Goals.route) { GoalsScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.Calendar.route) {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTarget = ::openSelectionTarget,
                )
            }
            composable(AppDestination.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTarget = ::openSelectionTarget,
                )
            }
            composable(AppDestination.Reports.route) { ReportsScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.Backup.route) { BackupScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.Appearance.route) { AppearanceScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.ManageAccounts.route) { ManageAccountsScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.ManageCategories.route) { ManageCategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(AppDestination.Badges.route) { BadgesScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
