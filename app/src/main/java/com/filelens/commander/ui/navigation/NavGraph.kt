package com.filelens.commander.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.filelens.commander.ui.screens.MainScreen
import com.filelens.commander.ui.screens.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
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
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
