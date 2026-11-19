package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.security.SecurityVaultScreen
import com.example.ui.tools.BankingToolsScreen
import com.example.ui.transfers.TransfersScreen

private data class NavDestinationItem(
    val label: String,
    val route: ZarBankRoute,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    NavDestinationItem(
        label = "پیشخوان",
        route = ZarBankRoute.Dashboard,
        selectedIcon = Icons.Rounded.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance,
        testTag = "nav_overview"
    ),
    NavDestinationItem(
        label = "انتقال وجه",
        route = ZarBankRoute.Transfers,
        selectedIcon = Icons.Rounded.SwapHoriz,
        unselectedIcon = Icons.Outlined.SwapHoriz,
        testTag = "nav_transfer"
    ),
    NavDestinationItem(
        label = "ابزارها",
        route = ZarBankRoute.Tools,
        selectedIcon = Icons.Rounded.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_tools"
    ),
    NavDestinationItem(
        label = "گاوصندوق",
        route = ZarBankRoute.Security,
        selectedIcon = Icons.Rounded.Lock,
        unselectedIcon = Icons.Outlined.Lock,
        testTag = "nav_vault"
    )
)

@Composable
fun ZarBankApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateTopLevel(route: ZarBankRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        topLevelDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(item.route::class)
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        topLevelDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(item.route::class)
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ZarBankRoute.Dashboard,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<ZarBankRoute.Dashboard> {
                        DashboardScreen(
                            onNavigateToTransfers = { navigateTopLevel(ZarBankRoute.Transfers) },
                            onNavigateToTools = { navigateTopLevel(ZarBankRoute.Tools) },
                            onNavigateToSecurity = { navigateTopLevel(ZarBankRoute.Security) }
                        )
                    }
                    composable<ZarBankRoute.Transfers> {
                        TransfersScreen(
                            onNavigateToSecurity = { navigateTopLevel(ZarBankRoute.Security) }
                        )
                    }
                    composable<ZarBankRoute.Tools> {
                        BankingToolsScreen()
                    }
                    composable<ZarBankRoute.Security> {
                        SecurityVaultScreen()
                    }
                }
            }
        }
    }
}
