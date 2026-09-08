package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ShowDetailScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.viewmodel.TvViewModel

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Dashboard : BottomNavScreen("dashboard", "My Series", Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_dashboard")
    object Explore : BottomNavScreen("explore", "Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_explore")
    object Stats : BottomNavScreen("stats", "Statistics", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_stats")
}

val bottomNavItems = listOf(
    BottomNavScreen.Dashboard,
    BottomNavScreen.Explore,
    BottomNavScreen.Stats
)

@Composable
fun TvApp(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val isDetailScreen = currentRoute?.startsWith("detail") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isDetailScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_navigation_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToShowDetail = { showId ->
                        viewModel.loadShowDetails(showId, null)
                        navController.navigate("detail/$showId")
                    },
                    onNavigateToSearch = {
                        navController.navigate(BottomNavScreen.Explore.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(BottomNavScreen.Explore.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToShowDetail = { showId, showDto ->
                        viewModel.loadShowDetails(showId, showDto)
                        navController.navigate("detail/$showId")
                    }
                )
            }

            composable(BottomNavScreen.Stats.route) {
                StatsScreen(
                    viewModel = viewModel
                )
            }

            composable(
                route = "detail/{showId}",
                arguments = listOf(navArgument("showId") { type = NavType.LongType })
            ) { backStackEntry ->
                val showId = backStackEntry.arguments?.getLong("showId") ?: 0L
                ShowDetailScreen(
                    showId = showId,
                    fallbackShowDto = viewModel.remoteShowDto.value,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
