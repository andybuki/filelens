package com.filelens.commander.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.ThemeMode
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportToString()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    snackbar.showSnackbar("Backup saved")
                } catch (e: Exception) {
                    snackbar.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: return@launch
                    viewModel.importFromJson(json)
                } catch (e: Exception) {
                    snackbar.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SectionHeader("Appearance")
            SettingsCard {
                ChoiceRow(
                    label = "Theme",
                    value = settings.themeMode.displayName,
                    onClick = { /* handled by inline picker */ },
                    trailing = null,
                    inlinePicker = {
                        ThemePicker(current = settings.themeMode, onSelect = viewModel::setThemeMode)
                    }
                )
            }

            SectionHeader("Browsing")
            SettingsCard {
                SwitchRow(
                    label = "Show hidden files",
                    description = "Display dotfiles and folders that start with a period.",
                    checked = settings.showHidden,
                    onCheckedChange = viewModel::setShowHidden
                )
                Divider()
                SwitchRow(
                    label = "Hide empty folders",
                    description = "Don't show folders that contain no visible files.",
                    checked = settings.hideEmptyFolders,
                    onCheckedChange = viewModel::setHideEmptyFolders
                )
                Divider()
                ChoiceRow(
                    label = "File size units",
                    value = settings.sizeUnits.displayName,
                    inlinePicker = {
                        UnitsPicker(current = settings.sizeUnits, onSelect = viewModel::setSizeUnits)
                    }
                )
            }

            SectionHeader("Backup & restore")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Save your settings, bookmarks, recent files, and frequent folders to a JSON file you can restore later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlInk3
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportLauncher.launch("filelens-backup.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Export") }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Import") }
                    }
                }
            }

            SectionHeader("About")
            SettingsCard {
                StaticRow("FileLens Commander", "Version 0.1.0")
                Divider()
                StaticRow(
                    "Vision",
                    "Browse files, peek inside, understand before opening. Offline, on-device, no accounts."
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = Eyebrow,
        color = FlInk3,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
private fun SwitchRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = FlAccent
            )
        )
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    inlinePicker: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, color = FlInk3)
            }
            if (trailing != null) trailing()
        }
        if (inlinePicker != null) {
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                inlinePicker()
            }
        }
    }
}

@Composable
private fun StaticRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = FlInk3)
    }
}

@Composable
private fun ThemePicker(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SegmentRow(
        options = ThemeMode.entries.map { it to it.displayName },
        current = current,
        onSelect = onSelect
    )
}

@Composable
private fun UnitsPicker(current: SizeUnits, onSelect: (SizeUnits) -> Unit) {
    SegmentRow(
        options = SizeUnits.entries.map { it to it.displayName },
        current = current,
        onSelect = onSelect
    )
}

@Composable
private fun <T> SegmentRow(
    options: List<Pair<T, String>>,
    current: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val selected = value == current
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) FlAccent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
