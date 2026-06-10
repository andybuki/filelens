package com.filelens.commander.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.InstalledApp
import com.filelens.commander.ui.theme.CategoryApk
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlBg
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.FlSurface
import com.filelens.commander.ui.theme.FlText
import com.filelens.commander.ui.theme.MonoSmall
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.viewmodel.AppFilter
import com.filelens.commander.viewmodel.AppManagerEvent
import com.filelens.commander.viewmodel.AppManagerViewModel
import java.io.File

@Composable
fun AppManagerScreen(
    onBack: () -> Unit,
    onInspectApk: (String) -> Unit,
    viewModel: AppManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val visible = remember(state.apps, state.filter, state.query) { viewModel.visibleApps() }

    val extractedMessage = stringResource(R.string.app_manager_extracted)
    val extractFailedMessage = stringResource(R.string.app_manager_extract_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppManagerEvent.ApkExtracted -> {
                    snackbar.showSnackbar("$extractedMessage: ${event.targetPath}")
                }
                is AppManagerEvent.ShareApk -> shareApk(context, event.app, event.targetPath)
                is AppManagerEvent.OpenInspector -> onInspectApk(event.apkPath)
                is AppManagerEvent.ExtractFailed -> snackbar.showSnackbar(extractFailedMessage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(FlBg)) {
        AppManagerHeader(
            count = visible.size,
            query = state.query,
            onBack = onBack,
            onQueryChanged = viewModel::setQuery
        )

        FilterRow(
            selected = state.filter,
            onSelect = viewModel::setFilter
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlAccent)
                }
                visible.isEmpty() -> EmptyAppsState()
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            onExtract = { viewModel.extractApk(app) },
                            onShare = { viewModel.shareApk(app) },
                            onInspect = { viewModel.openInspector(app) }
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AppManagerHeader(
    count: Int,
    query: String,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlNavy)
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = FlOnAccent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_manager_title),
                    color = FlOnAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.app_manager_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE5E7EB)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            placeholder = { Text(stringResource(R.string.app_manager_search_hint), color = Color(0xFFCBD0D8)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = FlOnAccent) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear),
                            tint = FlOnAccent
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x33000000),
                unfocusedContainerColor = Color(0x33000000),
                focusedTextColor = FlOnAccent,
                unfocusedTextColor = FlOnAccent,
                focusedIndicatorColor = FlOnAccent,
                unfocusedIndicatorColor = Color(0x44FFFFFF),
                cursorColor = FlOnAccent
            )
        )
    }
}

@Composable
private fun FilterRow(selected: AppFilter, onSelect: (AppFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(FlSurface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf(
            AppFilter.USER to R.string.app_manager_filter_user,
            AppFilter.SYSTEM to R.string.app_manager_filter_system,
            AppFilter.ALL to R.string.app_manager_filter_all
        )
        items(filters) { (filter, labelRes) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FlAccent,
                    selectedLabelColor = FlOnAccent
                )
            )
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onInspect: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FlSurface)
            .clickable(onClick = onInspect)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(CategoryApk.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Android,
                contentDescription = null,
                tint = CategoryApk,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = FlInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isSystem) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.app_manager_badge_system),
                        style = Eyebrow,
                        color = FlInk3
                    )
                }
            }
            Text(
                text = app.packageName,
                style = MonoSmall,
                color = FlText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            val sizeLabel = if (app.apkSize > 0) formatFileSize(app.apkSize) else "—"
            val versionLabel = app.versionName ?: app.versionCode.toString()
            Text(
                text = "v$versionLabel · $sizeLabel",
                style = MaterialTheme.typography.labelSmall,
                color = FlInk3,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = FlInk3
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_manager_action_inspect)) },
                onClick = { menuOpen = false; onInspect() },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_manager_action_extract)) },
                onClick = { menuOpen = false; onExtract() },
                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_manager_action_share)) },
                onClick = { menuOpen = false; onShare() },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun EmptyAppsState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FlAccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Android,
                    contentDescription = null,
                    tint = FlAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_manager_empty),
                style = MaterialTheme.typography.titleMedium,
                color = FlInk
            )
        }
    }
}

private fun shareApk(context: android.content.Context, app: InstalledApp, path: String) {
    try {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${app.label} ${app.versionName ?: ""}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.app_manager_chooser_share))
        )
    } catch (_: Exception) {
    }
}
