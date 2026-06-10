package com.filelens.commander.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filelens.commander.domain.model.HomeCategory
import com.filelens.commander.ui.screens.CompareScreen
import com.filelens.commander.ui.screens.DuplicateFinderScreen
import com.filelens.commander.ui.screens.HomeScreen
import com.filelens.commander.ui.screens.MainScreen
import com.filelens.commander.ui.screens.OnboardingScreen
import com.filelens.commander.ui.screens.AppManagerScreen
import com.filelens.commander.ui.screens.GlobalSearchScreen
import com.filelens.commander.ui.screens.RemoteServersScreen
import com.filelens.commander.ui.screens.SettingsScreen
import com.filelens.commander.ui.screens.ShortcutsScreen
import com.filelens.commander.ui.screens.SqliteBrowserScreen
import com.filelens.commander.ui.screens.TrashScreen
import com.filelens.commander.ui.screens.VaultScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    // All arguments are optional and URL-encoded; an empty value means "no
    // initial action — show the home folder as usual".
    const val MAIN = "main?path={path}&category={category}&inspect={inspect}"
    const val SETTINGS = "settings"
    const val COMPARE = "compare/{left}/{right}"
    const val DUPLICATES = "duplicates/{root}"
    const val SQLITE = "sqlite/{db}"
    const val SHORTCUTS = "shortcuts"
    const val REMOTE_SERVERS = "remote_servers"
    const val VAULT = "vault"
    const val GLOBAL_SEARCH = "global_search"
    const val APP_MANAGER = "app_manager"
    const val TRASH = "trash"

    fun main(
        path: String? = null,
        category: HomeCategory? = null,
        inspect: String? = null
    ): String {
        val pathArg = path?.let { Uri.encode(it) } ?: ""
        val categoryArg = category?.name ?: ""
        val inspectArg = inspect?.let { Uri.encode(it) } ?: ""
        return "main?path=$pathArg&category=$categoryArg&inspect=$inspectArg"
    }

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
        startDestination = if (showOnboarding) Routes.ONBOARDING else Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    onOnboardingComplete()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCategory = { category ->
                    navController.navigate(Routes.main(category = category))
                },
                onOpenPath = { path ->
                    navController.navigate(Routes.main(path = path))
                },
                onOpenFiles = { navController.navigate(Routes.main()) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenVault = { navController.navigate(Routes.VAULT) },
                onOpenGlobalSearch = { navController.navigate(Routes.GLOBAL_SEARCH) },
                onOpenAppManager = { navController.navigate(Routes.APP_MANAGER) },
                onOpenTrash = { navController.navigate(Routes.TRASH) }
            )
        }
        composable(
            route = Routes.MAIN,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("inspect") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path").orEmpty()
            val initialPath = if (rawPath.isEmpty()) null else rawPath
            val categoryName = backStackEntry.arguments?.getString("category").orEmpty()
            val initialCategory = HomeCategory.entries.firstOrNull { it.name == categoryName }
            val rawInspect = backStackEntry.arguments?.getString("inspect").orEmpty()
            val initialInspectPath = if (rawInspect.isEmpty()) null else rawInspect
            MainScreen(
                initialPath = initialPath,
                initialCategory = initialCategory,
                initialInspectPath = initialInspectPath,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onCompare = { left, right ->
                    navController.navigate(Routes.compare(left, right))
                },
                onFindDuplicates = { root ->
                    navController.navigate(Routes.duplicates(root))
                },
                onBrowseSqlite = { db ->
                    navController.navigate(Routes.sqlite(db))
                },
                onOpenHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenRemoteServers = { navController.navigate(Routes.REMOTE_SERVERS) },
                onOpenVault = { navController.navigate(Routes.VAULT) }
            )
        }
        composable(Routes.SHORTCUTS) {
            ShortcutsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REMOTE_SERVERS) {
            RemoteServersScreen(
                onBack = { navController.popBackStack() },
                onConnect = {
                    navController.navigate(Routes.main()) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.VAULT) {
            VaultScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GLOBAL_SEARCH) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onOpenFile = { item ->
                    val parent = item.path.substringBeforeLast('/', "")
                    val target = if (item.nodeType == com.filelens.commander.vfs.NodeType.DIRECTORY) {
                        item.path
                    } else {
                        parent.ifEmpty { item.path }
                    }
                    val inspect = if (item.nodeType == com.filelens.commander.vfs.NodeType.FILE) item.path else null
                    navController.navigate(Routes.main(path = target, inspect = inspect))
                }
            )
        }
        composable(Routes.APP_MANAGER) {
            AppManagerScreen(
                onBack = { navController.popBackStack() },
                onInspectApk = { apkPath ->
                    val parent = apkPath.substringBeforeLast('/', "")
                    navController.navigate(Routes.main(path = parent, inspect = apkPath))
                }
            )
        }
        composable(Routes.TRASH) {
            TrashScreen(onBack = { navController.popBackStack() })
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
