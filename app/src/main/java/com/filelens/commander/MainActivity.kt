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
import com.filelens.commander.ui.navigation.FileLensNavGraph
import com.filelens.commander.ui.theme.FileLensTheme
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

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

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
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
            FileLensTheme(themeMode = settings.themeMode) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.filelens.commander.ui.util.LocalSizeUnits provides settings.sizeUnits
                ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasStoragePermission) {
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
}
