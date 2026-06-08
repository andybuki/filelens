package com.filelens.commander.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.com.filelens.R
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlBlue50
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.viewmodel.SettingsViewModel

private val HeaderInk = FlOnAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenRemoteServers: () -> Unit = {},
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
                    snackbar.showSnackbar(context.getString(R.string.settings_backup_saved))
                } catch (e: Exception) {
                    snackbar.showSnackbar(context.getString(R.string.settings_export_failed, e.message))
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
                    snackbar.showSnackbar(context.getString(R.string.settings_import_failed, e.message))
                }
            }
        }
    }

    val licensesIntro = stringResource(R.string.settings_licenses_intro)

    Column(modifier = Modifier.fillMaxSize()) {
        NavyHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 36.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_section_browsing))
            SettingsCard {
                SwitchRow(
                    label = stringResource(R.string.settings_show_hidden_files),
                    description = stringResource(R.string.settings_show_hidden_files_desc),
                    checked = settings.showHidden,
                    onCheckedChange = viewModel::setShowHidden
                )
                Divider()
                SwitchRow(
                    label = stringResource(R.string.settings_hide_empty_folders),
                    description = stringResource(R.string.settings_hide_empty_folders_desc),
                    checked = settings.hideEmptyFolders,
                    onCheckedChange = viewModel::setHideEmptyFolders
                )
                Divider()
                ChoiceRow(
                    label = stringResource(R.string.settings_size_units),
                    value = settings.sizeUnits.displayName,
                    inlinePicker = {
                        UnitsPicker(current = settings.sizeUnits, onSelect = viewModel::setSizeUnits)
                    }
                )
                Divider()
                ChoiceRow(
                    label = stringResource(R.string.settings_default_panel),
                    value = settings.defaultPanelMode.displayName,
                    inlinePicker = {
                        PanelModePicker(
                            current = settings.defaultPanelMode,
                            onSelect = viewModel::setDefaultPanelMode
                        )
                    }
                )
            }

            SectionHeader(stringResource(R.string.settings_section_remote))
            SettingsCard {
                DisclosureRow(
                    icon = Icons.Filled.Storage,
                    label = stringResource(R.string.settings_remote_servers),
                    description = stringResource(R.string.settings_remote_servers_desc),
                    onClick = onOpenRemoteServers
                )
            }

            SectionHeader(stringResource(R.string.settings_section_backup))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = FlInk3
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("filelens-backup.json") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FlAccent)
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_export))
                        }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_import))
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_section_about))
            SettingsCard {
                StaticRow(
                    label = stringResource(R.string.settings_about_app_name),
                    value = stringResource(R.string.settings_about_version)
                )
                Divider()
                VisionBlock(
                    label = stringResource(R.string.settings_about_vision_label),
                    text = stringResource(R.string.settings_about_vision_text)
                )
                Divider()
                DisclosureRow(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(R.string.settings_section_licenses),
                    description = stringResource(R.string.settings_licenses_desc),
                    onClick = {
                        scope.launch { snackbar.showSnackbar(licensesIntro) }
                    }
                )
            }

            BrandFooter()
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NavyHeader(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlNavy)
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = HeaderInk
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = title,
                color = HeaderInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = Eyebrow,
        color = FlInk3,
        modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
            .padding(horizontal = 14.dp)
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
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Spacer(modifier = Modifier.height(3.dp))
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
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (trailing != null) trailing()
        }
        if (inlinePicker != null) {
            Box(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp)) {
                inlinePicker()
            }
        }
    }
}

@Composable
private fun DisclosureRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FlBlue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = FlAccent,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StaticRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = FlInk3)
    }
}

@Composable
private fun VisionBlock(label: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = Eyebrow,
            color = FlAccent
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BrandFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(FlNavy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = HeaderInk,
                modifier = Modifier.size(24.dp)
            )
        }
        Row {
            Text(
                stringResource(R.string.brand_name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "  ·  ",
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3
            )
            Text(
                stringResource(R.string.settings_footer_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3
            )
        }
    }
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
private fun PanelModePicker(current: PanelMode, onSelect: (PanelMode) -> Unit) {
    SegmentRow(
        options = PanelMode.entries.map { it to it.displayName },
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
            .clip(RoundedCornerShape(12.dp))
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
                    .padding(horizontal = 10.dp, vertical = 9.dp),
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
