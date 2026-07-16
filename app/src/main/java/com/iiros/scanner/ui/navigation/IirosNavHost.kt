package com.iiros.scanner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.res.stringResource
import com.iiros.scanner.R
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.FileScanRepository
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.ui.appdetail.AppDetailScreen
import com.iiros.scanner.ui.applist.AppListScreen
import com.iiros.scanner.ui.filescanner.FileScannerScreen
import com.iiros.scanner.ui.history.HistoryScreen
import com.iiros.scanner.ui.urlscanner.UrlScannerScreen

private object Routes {
    const val APPS = "apps"
    const val URL_SCAN = "url"
    const val FILES = "files"
    const val HISTORY = "history"
    const val APP_DETAIL = "appDetail/{packageName}"
    fun appDetail(packageName: String) = "appDetail/$packageName"
}

private data class TopLevelDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.APPS, R.string.nav_apps, Icons.Filled.Apps),
    TopLevelDestination(Routes.FILES, R.string.nav_files, Icons.Filled.Folder),
    TopLevelDestination(Routes.URL_SCAN, R.string.nav_url, Icons.Filled.Link),
    TopLevelDestination(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
)

@Composable
fun IirosNavHost(
    appScannerRepository: AppScannerRepository,
    historyRepository: HistoryRepository,
    fileScanRepository: FileScanRepository,
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.APPS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.APPS) {
                AppListScreen(
                    repository = appScannerRepository,
                    historyRepository = historyRepository,
                    onAppClick = { packageName -> navController.navigate(Routes.appDetail(packageName)) },
                )
            }
            composable(Routes.URL_SCAN) {
                UrlScannerScreen(historyRepository = historyRepository)
            }
            composable(Routes.FILES) {
                FileScannerScreen(repository = fileScanRepository)
            }
            composable(Routes.HISTORY) {
                HistoryScreen(historyRepository = historyRepository)
            }
            composable(
                Routes.APP_DETAIL,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
                AppDetailScreen(
                    packageName = packageName,
                    repository = appScannerRepository,
                    historyRepository = historyRepository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
