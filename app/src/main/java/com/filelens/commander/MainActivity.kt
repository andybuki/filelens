package com.filelens.commander

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import app.com.filelens.R
import com.filelens.commander.data.IncomingFiles
import com.filelens.commander.domain.model.HomeCategory
import com.filelens.commander.ui.navigation.Routes
import com.filelens.commander.widget.QuickActionsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.filelens.commander.ui.navigation.FileLensNavGraph
import com.filelens.commander.ui.theme.FileLensTheme
import com.filelens.commander.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

    /** Set when another app handed us a file; replaces the normal landing screen. */
    private var incomingRoute by mutableStateOf<String?>(null)
    private var importingIncoming by mutableStateOf(false)

    /** Widget shortcut target, pushed on top of the landing screen. */
    private var shortcutRoute by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#2E1065"))
        )
        checkStoragePermission()
        if (savedInstanceState == null) {
            handleLaunchIntent(intent)
        } else {
            incomingRoute = savedInstanceState.getString(STATE_INCOMING_ROUTE)
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val isDark = when (settings.themeMode) {
                com.filelens.commander.domain.model.ThemeMode.DARK -> true
                com.filelens.commander.domain.model.ThemeMode.SYSTEM ->
                    androidx.compose.foundation.isSystemInDarkTheme()
                else -> false
            }
            androidx.compose.runtime.LaunchedEffect(isDark) {
                val navColor = if (isDark) {
                    android.graphics.Color.parseColor("#1A0E33")
                } else {
                    android.graphics.Color.parseColor("#2E1065")
                }
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(navColor)
                )
            }

            // Apply the stored language override. On API 33+ we route to the
            // platform LocaleManager so the system handles the recreate. On
            // older versions AppCompatDelegate handles it, but since this
            // activity isn't an AppCompatActivity we trigger recreate() so
            // the new locale takes effect immediately.
            androidx.compose.runtime.LaunchedEffect(settings.languageTag) {
                val desiredTags = settings.languageTag?.takeIf { it.isNotBlank() }.orEmpty()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val mgr = getSystemService(android.app.LocaleManager::class.java)
                    val current = mgr.applicationLocales
                    val desired = android.os.LocaleList.forLanguageTags(desiredTags)
                    if (current.toLanguageTags() != desired.toLanguageTags()) {
                        mgr.applicationLocales = desired
                    }
                } else {
                    val current = AppCompatDelegate.getApplicationLocales()
                    val desired = LocaleListCompat.forLanguageTags(desiredTags)
                    if (current.toLanguageTags() != desired.toLanguageTags()) {
                        AppCompatDelegate.setApplicationLocales(desired)
                        recreate()
                    }
                }
            }
            FileLensTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.filelens.commander.ui.util.LocalSizeUnits provides settings.sizeUnits
                ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (importingIncoming) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    } else if (hasStoragePermission || incomingRoute != null) {
                        val navController = rememberNavController()
                        val hasSeenOnboarding by settingsViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(innerPadding.calculateTopPadding())
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                            FileLensNavGraph(
                                navController = navController,
                                showOnboarding = !hasSeenOnboarding,
                                startFromFiles = settings.startScreen == com.filelens.commander.domain.model.StartScreen.FILES,
                                initialRoute = incomingRoute,
                                shortcutRoute = shortcutRoute,
                                onShortcutConsumed = { shortcutRoute = null },
                                onOnboardingComplete = { settingsViewModel.completeOnboarding() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Storage Access Required",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "FileLens needs access to your files to browse and inspect them.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { requestStoragePermission() }) {
                                Text("Grant Access")
                            }
                        }
                    }
                }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_INCOMING_ROUTE, incomingRoute)
    }

    private fun handleLaunchIntent(intent: Intent) {
        shortcutRoute = when (intent.getStringExtra(QuickActionsWidget.EXTRA_ACTION)) {
            QuickActionsWidget.ACTION_SEARCH -> Routes.GLOBAL_SEARCH
            QuickActionsWidget.ACTION_DOWNLOADS -> Routes.main(category = HomeCategory.DOWNLOADS)
            QuickActionsWidget.ACTION_IMAGES -> Routes.main(category = HomeCategory.IMAGES)
            QuickActionsWidget.ACTION_FILES -> Routes.main()
            else -> null
        }
        val uri = IncomingFiles.urisFrom(intent).firstOrNull() ?: return
        importingIncoming = true
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) {
                runCatching { IncomingFiles.copyToCache(this@MainActivity, uri, INCOMING_DIR) }.getOrNull()
            }
            if (copied != null) {
                incomingRoute = Routes.main(path = copied.parent, inspect = copied.absolutePath)
            } else {
                android.widget.Toast.makeText(
                    this@MainActivity, R.string.incoming_open_failed, android.widget.Toast.LENGTH_LONG
                ).show()
            }
            importingIncoming = false
        }
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private companion object {
        const val INCOMING_DIR = "incoming"
        const val STATE_INCOMING_ROUTE = "incoming_route"
    }
}
