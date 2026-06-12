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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.filelens.commander.domain.model.AppSettings
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.StartScreen
import kotlin.math.roundToInt
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlBlue50
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.viewmodel.SettingsViewModel

private val HeaderInk = FlOnAccent

/**
 * Languages we ship translations for. Display names stay in the target
 * language so a user can find their own language even when the current
 * UI is in something else.
 */
private data class LanguageOption(val tag: String, val nativeName: String)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English"),
    LanguageOption("es", "Español"),
    LanguageOption("pt-BR", "Português (Brasil)"),
    LanguageOption("fr", "Français"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("it", "Italiano"),
    LanguageOption("ru", "Русский"),
    LanguageOption("uk", "Українська"),
    LanguageOption("tr", "Türkçe"),
    LanguageOption("pl", "Polski"),
    LanguageOption("ja", "日本語"),
    LanguageOption("ko", "한국어"),
    LanguageOption("zh-CN", "中文 (简体)"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("ar", "العربية")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenRemoteServers: () -> Unit = {},
    onOpenVault: () -> Unit = {},
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

    val pickSearchRoot = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Resolve the SAF tree URI back to an absolute path under the
            // primary emulated storage. Off-volume trees (SD cards, etc.)
            // don't have a usable filesystem path; we fall back to the URI
            // string so the row remains identifiable, even if global search
            // can't walk it directly.
            val docId = uri.lastPathSegment.orEmpty()
            val rel = docId.substringAfter(':', "")
            val path = when {
                docId.startsWith("primary:") -> "/storage/emulated/0" + if (rel.isBlank()) "" else "/$rel"
                else -> uri.toString()
            }
            viewModel.addSearchRoot(path)
        }
    }

    var languageDialog by remember { mutableStateOf(false) }

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
            // ────────── General ──────────
            SectionHeader(stringResource(R.string.settings_section_general))
            SettingsCard {
                DisclosureRow(
                    icon = Icons.Filled.Language,
                    label = stringResource(R.string.settings_language),
                    description = languageDescription(settings.languageTag, context),
                    onClick = { languageDialog = true }
                )
                Divider()
                ChoiceRow(
                    label = stringResource(R.string.settings_start_screen),
                    value = startScreenLabel(settings.startScreen),
                    inlinePicker = {
                        StartScreenPicker(
                            current = settings.startScreen,
                            onSelect = viewModel::setStartScreen
                        )
                    }
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
                DisclosureRow(
                    icon = Icons.Filled.Storage,
                    label = stringResource(R.string.settings_remote_servers),
                    description = stringResource(R.string.settings_remote_servers_desc),
                    onClick = onOpenRemoteServers
                )
            }

            // ────────── Files ──────────
            SectionHeader(stringResource(R.string.settings_section_files))
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
                SwitchRow(
                    label = stringResource(R.string.settings_ask_before_delete),
                    description = stringResource(R.string.settings_ask_before_delete_desc),
                    checked = settings.askBeforeDelete,
                    onCheckedChange = viewModel::setAskBeforeDelete
                )
            }

            // ────────── Inspector & preview ──────────
            SectionHeader(stringResource(R.string.settings_section_inspector))
            SettingsCard {
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
                Divider()
                Text(
                    stringResource(R.string.settings_preview_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                )
                Divider()
                SliderRow(
                    label = stringResource(R.string.settings_preview_limit),
                    description = stringResource(R.string.settings_preview_limit_desc),
                    valueMb = settings.remotePreviewLimitMb,
                    min = AppSettings.REMOTE_PREVIEW_LIMIT_MIN_MB,
                    max = AppSettings.REMOTE_PREVIEW_LIMIT_MAX_MB,
                    step = AppSettings.REMOTE_PREVIEW_LIMIT_STEP_MB,
                    onCommit = viewModel::setRemotePreviewLimitMb
                )
                Divider()
                SliderRow(
                    label = stringResource(R.string.settings_preview_archive_limit),
                    description = stringResource(R.string.settings_preview_archive_limit_desc),
                    valueMb = settings.remoteArchivePreviewLimitMb,
                    min = AppSettings.REMOTE_ARCHIVE_PREVIEW_LIMIT_MIN_MB,
                    max = AppSettings.REMOTE_ARCHIVE_PREVIEW_LIMIT_MAX_MB,
                    step = AppSettings.REMOTE_ARCHIVE_PREVIEW_LIMIT_STEP_MB,
                    onCommit = viewModel::setRemoteArchivePreviewLimitMb
                )
            }

            // ────────── Safety & privacy ──────────
            SectionHeader(stringResource(R.string.settings_section_safety))
            SettingsCard {
                DisclosureRow(
                    icon = Icons.Filled.Lock,
                    label = stringResource(R.string.settings_vault),
                    description = stringResource(R.string.settings_vault_desc),
                    onClick = onOpenVault
                )
                Divider()
                SearchRootsRow(
                    roots = settings.searchRoots,
                    onAdd = {
                        try {
                            pickSearchRoot.launch(null)
                        } catch (_: Exception) {
                            scope.launch {
                                snackbar.showSnackbar(
                                    context.getString(R.string.settings_export_failed, "no folder picker")
                                )
                            }
                        }
                    },
                    onRemove = viewModel::removeSearchRoot
                )
                Divider()
                SwitchRow(
                    label = stringResource(R.string.settings_warn_root_storage),
                    description = stringResource(R.string.settings_warn_root_storage_desc),
                    checked = settings.warnRootStorage,
                    onCheckedChange = viewModel::setWarnRootStorage
                )
                Divider()
                SwitchRow(
                    label = stringResource(R.string.settings_telemetry),
                    description = stringResource(R.string.settings_telemetry_desc),
                    checked = settings.telemetryEnabled,
                    onCheckedChange = viewModel::setTelemetryEnabled
                )
            }

            // ────────── Backup & restore ──────────
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

            // ────────── About ──────────
            SectionHeader(stringResource(R.string.settings_section_about))
            SettingsCard {
                StaticRow(
                    label = stringResource(R.string.settings_about_app_name),
                    value = stringResource(R.string.settings_about_version)
                )
                Divider()
                PrivacyPromiseBlock()
                Divider()
                VisionBlock(
                    label = stringResource(R.string.settings_about_vision_label),
                    text = stringResource(R.string.settings_about_vision_text)
                )
            }

            BrandFooter()
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }

    if (languageDialog) {
        LanguageDialog(
            current = settings.languageTag,
            onSelect = { tag ->
                viewModel.setLanguageTag(tag)
                languageDialog = false
            },
            onDismiss = { languageDialog = false }
        )
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
                    maxLines = 3,
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
private fun formatMb(mb: Int): String {
    return if (mb >= 1024) {
        val gb = mb / 1024.0
        val text = if (gb == gb.toLong().toDouble()) gb.toLong().toString() else String.format("%.1f", gb)
        stringResource(R.string.settings_preview_limit_value_gb, text)
    } else {
        stringResource(R.string.settings_preview_limit_value, mb)
    }
}

@Composable
private fun SliderRow(
    label: String,
    description: String,
    valueMb: Int,
    min: Int,
    max: Int,
    step: Int,
    onCommit: (Int) -> Unit
) {
    // Track the drag locally so the thumb feels responsive; only persist on
    // release to avoid writing to DataStore on every pixel of movement.
    var sliderValue by remember(valueMb) { mutableFloatStateOf(valueMb.toFloat()) }
    val steps = ((max - min) / step - 1).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                formatMb(sliderValue.roundToInt()),
                style = MaterialTheme.typography.titleSmall,
                color = FlAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onCommit(sliderValue.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = FlAccent,
                activeTrackColor = FlAccent
            )
        )
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
private fun PrivacyPromiseBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_about_privacy_label).uppercase(),
            style = Eyebrow,
            color = FlAccent
        )
        Spacer(modifier = Modifier.height(9.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrivacyChip(stringResource(R.string.settings_about_privacy_no_ads))
            PrivacyChip(stringResource(R.string.settings_about_privacy_no_accounts))
            PrivacyChip(stringResource(R.string.settings_about_privacy_no_tracking))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_about_privacy_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PrivacyChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(FlBlue50)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = FlAccent,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = FlAccent,
            fontWeight = FontWeight.SemiBold
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
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
        )
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
private fun StartScreenPicker(current: StartScreen, onSelect: (StartScreen) -> Unit) {
    val homeLabel = stringResource(R.string.settings_start_screen_home)
    val filesLabel = stringResource(R.string.settings_start_screen_files)
    SegmentRow(
        options = listOf(
            StartScreen.HOME to homeLabel,
            StartScreen.FILES to filesLabel
        ),
        current = current,
        onSelect = onSelect
    )
}

@Composable
private fun startScreenLabel(screen: StartScreen): String = when (screen) {
    StartScreen.HOME -> stringResource(R.string.settings_start_screen_home)
    StartScreen.FILES -> stringResource(R.string.settings_start_screen_files)
}

private fun languageDescription(tag: String?, context: android.content.Context): String {
    val explicit = SUPPORTED_LANGUAGES.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    return explicit?.nativeName
        ?: context.getString(R.string.settings_language_system)
}

@Composable
private fun LanguageDialog(
    current: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    LanguageRow(
                        label = stringResource(R.string.settings_language_system),
                        selected = current.isNullOrBlank(),
                        onClick = { onSelect(null) }
                    )
                }
                items(SUPPORTED_LANGUAGES) { option ->
                    LanguageRow(
                        label = option.nativeName,
                        selected = current.equals(option.tag, ignoreCase = true),
                        onClick = { onSelect(option.tag) }
                    )
                }
            }
        }
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Language,
            contentDescription = null,
            tint = if (selected) FlAccent else FlInk3,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) FlAccent else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SearchRootsRow(
    roots: Set<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_search_roots),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    stringResource(R.string.settings_search_roots_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (roots.isEmpty()) {
            Text(
                stringResource(R.string.settings_search_roots_default),
                style = MaterialTheme.typography.bodySmall,
                color = FlAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            for (root in roots.sorted()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = FlAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        root,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { onRemove(root) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.settings_search_roots_remove),
                            tint = FlInk3,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_search_roots_add))
        }
    }
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
