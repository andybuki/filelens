package com.filelens.commander.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.usecase.TrashUseCase
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.MonoSmall
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.viewmodel.TrashEvent
import com.filelens.commander.viewmodel.TrashViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmEmpty by remember { mutableStateOf(false) }

    val restoredMessage = stringResource(R.string.trash_restored_format)
    val emptiedMessage = stringResource(R.string.trash_emptied)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TrashEvent.Restored -> snackbar.showSnackbar(restoredMessage.format(event.count))
                TrashEvent.Emptied -> snackbar.showSnackbar(emptiedMessage)
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(stringResource(R.string.trash_empty_title)) },
            text = { Text(stringResource(R.string.trash_empty_body, state.items.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmpty = false
                    viewModel.emptyTrash()
                }) {
                    Text(stringResource(R.string.action_delete), color = RiskDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TrashHeader(count = state.items.size, onBack = onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.items.isEmpty() -> EmptyTrashState()
                else -> TrashList(items = state.items)
            }
        }

        if (state.items.isNotEmpty()) {
            ActionBar(
                onRestoreAll = { viewModel.restoreAll() },
                onEmpty = { confirmEmpty = true }
            )
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrashHeader(count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.trash_title),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pluralStringResource(R.plurals.plural_trash_item_count, count, count),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE5E7EB)
            )
        }
    }
}

@Composable
private fun TrashList(items: List<TrashUseCase.TrashedItem>) {
    val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item("section-header") {
            Text(
                stringResource(R.string.trash_section_files).uppercase(),
                style = Eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        items(items, key = { it.trashPath }) { item ->
            TrashRow(item = item, dateFmt = dateFmt)
        }
    }
}

@Composable
private fun TrashRow(item: TrashUseCase.TrashedItem, dateFmt: DateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = compactPath(item.originalPath),
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = stringResource(
                    R.string.trash_deleted_at,
                    dateFmt.format(Date(item.timestamp))
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ActionBar(onRestoreAll: () -> Unit, onEmpty: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onRestoreAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Output, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.trash_action_restore_all))
        }
        OutlinedButton(onClick = onEmpty, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = RiskDanger,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.trash_action_empty), color = RiskDanger)
        }
    }
}

@Composable
private fun EmptyTrashState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.RestoreFromTrash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                stringResource(R.string.trash_empty_headline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.trash_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun compactPath(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
}
