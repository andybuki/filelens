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
import com.filelens.commander.ui.screens.MainScreen
import com.filelens.commander.ui.screens.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val COMPARE = "compare/{left}/{right}"

    fun compare(leftPath: String, rightPath: String): String =
        "compare/${Uri.encode(leftPath)}/${Uri.encode(rightPath)}"
}

@Composable
fun FileLensNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onCompare = { left, right ->
                    navController.navigate(Routes.compare(left, right))
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
    }
}
