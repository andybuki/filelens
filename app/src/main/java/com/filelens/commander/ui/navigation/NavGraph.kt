package com.filelens.commander.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filelens.commander.domain.model.HomeCategory
import com.filelens.commander.domain.model.RemoteConnection
import com.filelens.commander.domain.model.RemoteProtocol
import com.filelens.commander.ui.screens.CompareScreen
import com.filelens.commander.ui.screens.HomeScreen
import com.filelens.commander.ui.screens.MainScreen
import com.filelens.commander.ui.screens.OnboardingScreen
import com.filelens.commander.ui.screens.AppManagerScreen
import com.filelens.commander.ui.screens.GlobalSearchScreen
import com.filelens.commander.ui.screens.RemoteServersScreen
import com.filelens.commander.ui.screens.SettingsScreen
import com.filelens.commander.ui.screens.SqliteBrowserScreen
import com.filelens.commander.ui.screens.StorageUsageScreen
import com.filelens.commander.ui.screens.TrashScreen
import com.filelens.commander.ui.screens.VaultScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    // All arguments are optional and URL-encoded; an empty value means "no
    // initial action — show the home folder as usual".
    const val MAIN = "main?path={path}&category={category}&inspect={inspect}&remoteId={remoteId}"
    const val SETTINGS = "settings"
    const val COMPARE = "compare/{left}/{right}"
    const val SQLITE = "sqlite/{db}"
    const val REMOTE_SERVERS = "remote_servers"
    const val VAULT = "vault"
    const val GLOBAL_SEARCH = "global_search"
    const val APP_MANAGER = "app_manager"
    const val TRASH = "trash"
    const val STORAGE_USAGE = "storage_usage"

    fun main(
        path: String? = null,
        category: HomeCategory? = null,
        inspect: String? = null,
        remoteId: String? = null
    ): String {
        val pathArg = path?.let { Uri.encode(it) } ?: ""
        val categoryArg = category?.name ?: ""
        val inspectArg = inspect?.let { Uri.encode(it) } ?: ""
        val remoteArg = remoteId?.let { Uri.encode(it) } ?: ""
        return "main?path=$pathArg&category=$categoryArg&inspect=$inspectArg&remoteId=$remoteArg"
    }

    fun compare(leftPath: String, rightPath: String): String =
        "compare/${Uri.encode(leftPath)}/${Uri.encode(rightPath)}"

    fun sqlite(dbPath: String): String = "sqlite/${Uri.encode(dbPath)}"
}

@Composable
fun FileLensNavGraph(
    navController: NavHostController,
    showOnboarding: Boolean = false,
    startFromFiles: Boolean = false,
    initialRoute: String? = null,
    shortcutRoute: String? = null,
    onShortcutConsumed: () -> Unit = {},
    onOnboardingComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val landing = when {
        initialRoute != null -> initialRoute
        showOnboarding -> Routes.ONBOARDING
        startFromFiles -> Routes.main()
        else -> Routes.HOME
    }
    NavHost(
        navController = navController,
        startDestination = landing,
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
            val remoteServersVM: com.filelens.commander.viewmodel.RemoteServersViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val savedRemotes by remoteServersVM.connections.collectAsStateWithLifecycle()
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
                onOpenTrash = { navController.navigate(Routes.TRASH) },
                onOpenStorageUsage = { navController.navigate(Routes.STORAGE_USAGE) },
                onOpenRecentFile = { path ->
                    routeRecentFile(navController, path, savedRemotes)
                }
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
                },
                navArgument("remoteId") {
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
            val rawRemoteId = backStackEntry.arguments?.getString("remoteId").orEmpty()
            val initialRemoteId = if (rawRemoteId.isEmpty()) null else rawRemoteId
            MainScreen(
                initialPath = initialPath,
                initialCategory = initialCategory,
                initialInspectPath = initialInspectPath,
                initialRemoteId = initialRemoteId,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onCompare = { left, right ->
                    navController.navigate(Routes.compare(left, right))
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
        composable(Routes.REMOTE_SERVERS) {
            RemoteServersScreen(
                onBack = { navController.popBackStack() },
                onConnect = { connectionId ->
                    navController.navigate(Routes.main(remoteId = connectionId)) {
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
        composable(Routes.STORAGE_USAGE) {
            StorageUsageScreen(onBack = { navController.popBackStack() })
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
    // After NavHost so its graph is set before we navigate.
    androidx.compose.runtime.LaunchedEffect(shortcutRoute) {
        if (shortcutRoute != null && landing != Routes.ONBOARDING) {
            navController.navigate(shortcutRoute)
        }
        if (shortcutRoute != null) onShortcutConsumed()
    }
}

/**
 * Route a tap on a Recent files row to the right destination. Local paths land
 * on the file's parent folder with the file pre-selected in the inspector;
 * remote URLs (ftp://, sftp://, smb://) reconnect to the matching saved server
 * before applying the inspect target so the user lands on the actual file
 * instead of being bounced back to the home root.
 */
private fun routeRecentFile(
    navController: NavHostController,
    rawPath: String,
    savedRemotes: List<RemoteConnection>
) {
    val schemeSep = rawPath.indexOf("://")
    if (schemeSep > 0) {
        val scheme = rawPath.substring(0, schemeSep).lowercase()
        val rest = rawPath.substring(schemeSep + 3)
        val authority = rest.substringBefore('/')
        // Strip the user@ prefix and any :port suffix; we only match by host.
        val host = authority.substringAfterLast('@').substringBefore(':').lowercase()
        val match = savedRemotes.firstOrNull { conn ->
            conn.protocol.matchesScheme(scheme) && conn.host.equals(host, ignoreCase = true)
        }
        if (match != null) {
            navController.navigate(
                Routes.main(remoteId = match.id, inspect = rawPath)
            )
        } else {
            // No saved server matches — drop the user in Remote Servers so
            // they can add or pick one. Better than silently navigating home.
            navController.navigate(Routes.REMOTE_SERVERS)
        }
        return
    }
    val parent = rawPath.substringBeforeLast('/', "")
    val target = parent.ifEmpty { rawPath }
    navController.navigate(Routes.main(path = target, inspect = rawPath))
}

private fun RemoteProtocol.matchesScheme(scheme: String): Boolean = when (this) {
    RemoteProtocol.FTP -> scheme == "ftp"
    RemoteProtocol.SFTP -> scheme == "sftp"
    RemoteProtocol.SMB -> scheme == "smb" || scheme == "cifs"
}
