package com.budgetapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.budgetapp.ui.accounts.AccountsScreen
import com.budgetapp.ui.accounts.AddAccountScreen
import com.budgetapp.ui.auth.AuthScreen
import com.budgetapp.ui.budgets.BudgetsScreen
import com.budgetapp.ui.dashboard.DashboardScreen
import com.budgetapp.ui.onboarding.TrackingMethodScreen
import com.budgetapp.ui.settings.SettingsScreen
import com.budgetapp.ui.transactions.AddTransactionScreen
import com.budgetapp.ui.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object TrackingMethod : Screen("tracking_method")
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object AddTransaction : Screen("add_transaction")
    data object Budgets : Screen("budgets")
    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home", Icons.Default.Dashboard),
    BottomNavItem(Screen.Transactions, "Transactions", Icons.Default.Receipt),
    BottomNavItem(Screen.Budgets, "Budgets", Icons.Default.PieChart),
    BottomNavItem(Screen.Accounts, "Accounts", Icons.Default.AccountBalance),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onFirstTimeSetup = {
                        navController.navigate(Screen.TrackingMethod.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.TrackingMethod.route) {
                TrackingMethodScreen(
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.TrackingMethod.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) }
                )
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Budgets.route) {
                BudgetsScreen()
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onAddAccount = { navController.navigate(Screen.AddAccount.route) }
                )
            }

            composable(Screen.AddAccount.route) {
                AddAccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
