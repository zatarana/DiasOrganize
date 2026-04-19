package com.lifeflow.pro.ui.navigation

import androidx.annotation.StringRes
import com.lifeflow.pro.R

sealed class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    data object Home : AppDestination("home", R.string.nav_home)
    data object Tasks : AppDestination("tasks", R.string.nav_tasks)
    data object Finance : AppDestination("finance", R.string.nav_finance)
    data object Debts : AppDestination("debts", R.string.nav_debts)
    data object More : AppDestination("more", R.string.nav_more)

    data object Goals : AppDestination("goals", R.string.screen_goals)
    data object Calendar : AppDestination("calendar", R.string.screen_calendar)
    data object Search : AppDestination("search", R.string.screen_search)
    data object Reports : AppDestination("reports", R.string.screen_reports)
    data object Backup : AppDestination("backup", R.string.screen_backup)
    data object Appearance : AppDestination("appearance", R.string.screen_appearance)
    data object ManageAccounts : AppDestination("manage_accounts", R.string.screen_manage_accounts)
    data object ManageCategories : AppDestination("manage_categories", R.string.screen_manage_categories)
    data object Badges : AppDestination("badges", R.string.screen_badges)
}
