package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.usecase.FindDuplicatesUseCase
import com.filelens.commander.ui.theme.RobotoMono
import com.filelens.commander.ui.util.LocalSizeUnits
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.viewmodel.DuplicateFinderViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DuplicatesDialog(
    rootPath: String,
    onDismiss: () -> Unit,
    viewModel: DuplicateFinderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(rootPath) { viewModel.start(rootPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FileCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.duplicates_title))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    state.isScanning -> Scanning(state)
                    state.groups.isEmpty() -> Text(
                        stringResource(R.string.duplicates_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Results(
                        state = state,
                        onToggleSelected = viewModel::toggleSelected,
                        onSelectOldExtras = viewModel::selectOldExtras
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!state.isScanning && state.selectedForDeletion.isNotEmpty()) {
                    TextButton(onClick = { viewModel.deleteSelected() }) {
                        Text(pluralStringResource(R.plurals.plural_delete_action, state.selectedForDeletion.size, state.selectedForDeletion.size))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    )
}

@Composable
private fun Scanning(state: com.filelens.commander.viewmodel.DuplicateFinderState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(10.dp))
        if (state.hashTotal > 0) {
            Text(
                stringResource(R.string.duplicates_hashing, state.hashCurrent, state.hashTotal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.hashCurrent.toFloat() / state.hashTotal.coerceAtLeast(1).toFloat() },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                stringResource(R.string.duplicates_scanning, state.filesSeen),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Results(
    state: com.filelens.commander.viewmodel.DuplicateFinderState,
    onToggleSelected: (String) -> Unit,
    onSelectOldExtras: () -> Unit
) {
    val units = LocalSizeUnits.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(10.dp)
        ) {
            Text(
                stringResource(R.string.duplicates_group_count, state.groups.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.duplicates_wasted_summary, formatFileSize(state.totalWastedBytes, units), state.totalFiles),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onSelectOldExtras) {
            Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.duplicates_action_select_old))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
            items(state.groups, key = { it.checksum }) { group ->
                DuplicateGroupCard(
                    group = group,
                    selected = state.selectedForDeletion,
                    onToggleSelected = onToggleSelected
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: FindDuplicatesUseCase.DuplicateGroup,
    selected: Set<String>,
    onToggleSelected: (String) -> Unit
) {
    val units = LocalSizeUnits.current
    val df = remember { DateFormat.getDateInstance(DateFormat.SHORT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.duplicates_copies_each, group.files.size, formatFileSize(group.size, units)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.duplicates_wasted, formatFileSize(group.wastedBytes, units)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        group.files.forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSelected(f.path) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = f.path in selected,
                    onCheckedChange = { onToggleSelected(f.path) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = f.path,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = RobotoMono,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.duplicates_modified, df.format(Date(f.lastModified))),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
