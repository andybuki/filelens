package com.filelens.commander.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filelens.commander.ui.screens.CompareScreen
import com.filelens.commander.ui.screens.DuplicateFinderScreen
import com.filelens.commander.ui.screens.MainScreen
import com.filelens.commander.ui.screens.OnboardingScreen
import com.filelens.commander.ui.screens.RemoteServersScreen
import com.filelens.commander.ui.screens.SettingsScreen
import com.filelens.commander.ui.screens.ShortcutsScreen
import com.filelens.commander.ui.screens.SqliteBrowserScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val COMPARE = "compare/{left}/{right}"
    const val DUPLICATES = "duplicates/{root}"
    const val SQLITE = "sqlite/{db}"
    const val SHORTCUTS = "shortcuts"
    const val REMOTE_SERVERS = "remote_servers"

    fun compare(leftPath: String, rightPath: String): String =
        "compare/${Uri.encode(leftPath)}/${Uri.encode(rightPath)}"

    fun duplicates(rootPath: String): String = "duplicates/${Uri.encode(rootPath)}"

    fun sqlite(dbPath: String): String = "sqlite/${Uri.encode(dbPath)}"
}

@Composable
fun FileLensNavGraph(
    navController: NavHostController,
    showOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Routes.ONBOARDING else Routes.MAIN,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    onOnboardingComplete()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onCompare = { left, right ->
                    navController.navigate(Routes.compare(left, right))
                },
                onFindDuplicates = { root ->
                    navController.navigate(Routes.duplicates(root))
                },
                onBrowseSqlite = { db ->
                    navController.navigate(Routes.sqlite(db))
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenShortcuts = { navController.navigate(Routes.SHORTCUTS) },
                onOpenRemoteServers = { navController.navigate(Routes.REMOTE_SERVERS) }
            )
        }
        composable(Routes.SHORTCUTS) {
            ShortcutsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REMOTE_SERVERS) {
            RemoteServersScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.COMPARE,
            arguments = listOf(
                navArgument("left") { type = NavType.StringType },
                navArgument("right") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val left = backStackEntry.arguments?.getString("left").orEmpty()
            val right = backStackEntry.arguments?.getString("right").orEmpty()
            CompareScreen(
                leftPath = left,
                rightPath = right,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.DUPLICATES,
            arguments = listOf(navArgument("root") { type = NavType.StringType })
        ) { backStackEntry ->
            val root = backStackEntry.arguments?.getString("root").orEmpty()
            DuplicateFinderScreen(
                rootPath = root,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.SQLITE,
            arguments = listOf(navArgument("db") { type = NavType.StringType })
        ) { backStackEntry ->
            val db = backStackEntry.arguments?.getString("db").orEmpty()
            SqliteBrowserScreen(
                dbPath = db,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
