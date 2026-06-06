package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.usecase.FindDuplicatesUseCase
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.util.LocalSizeUnits
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.viewmodel.DuplicateFinderViewModel
import com.filelens.commander.ui.theme.RobotoMono
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    rootPath: String,
    onBack: () -> Unit,
    viewModel: DuplicateFinderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(rootPath) { viewModel.start(rootPath) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.duplicates_title), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Text(
            text = rootPath,
            style = MaterialTheme.typography.bodySmall,
            color = FlInk3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        when {
            state.isScanning -> ScanningView(state)
            state.groups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.duplicates_none_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlInk3
                )
            }
            else -> ResultsView(
                state = state,
                onToggleSelected = viewModel::toggleSelected,
                onSelectOldExtras = viewModel::selectOldExtras,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = viewModel::deleteSelected
            )
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ScanningView(state: com.filelens.commander.viewmodel.DuplicateFinderState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = FlAccent, strokeWidth = 2.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            if (state.hashTotal > 0) {
                Text(
                    stringResource(R.string.duplicates_hashing, state.hashCurrent, state.hashTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlInk2
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.hashCurrent.toFloat() / state.hashTotal.coerceAtLeast(1).toFloat() },
                    color = FlAccent,
                    modifier = Modifier.width(220.dp)
                )
            } else {
                Text(
                    stringResource(R.string.duplicates_scanning, state.filesSeen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlInk2
                )
            }
        }
    }
}

@Composable
private fun ResultsView(
    state: com.filelens.commander.viewmodel.DuplicateFinderState,
    onToggleSelected: (String) -> Unit,
    onSelectOldExtras: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val units = LocalSizeUnits.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.duplicates_group_count, state.groups.size),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.duplicates_wasted_summary, formatFileSize(state.totalWastedBytes, units), state.totalFiles),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlInk3
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onSelectOldExtras) { Text(stringResource(R.string.duplicates_action_select_old)) }
            TextButton(
                onClick = onClearSelection,
                enabled = state.selectedForDeletion.isNotEmpty()
            ) { Text(stringResource(R.string.action_clear)) }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onDeleteSelected,
                enabled = state.selectedForDeletion.isNotEmpty()
            ) { Text(pluralStringResource(R.plurals.plural_delete_action, state.selectedForDeletion.size, state.selectedForDeletion.size)) }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
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
        Row {
            Text(
                stringResource(R.string.duplicates_copies_each, group.files.size, formatFileSize(group.size, units)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.duplicates_wasted, formatFileSize(group.wastedBytes, units)),
                style = MaterialTheme.typography.labelMedium,
                color = FlInk3
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
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
                        color = FlInk3
                    )
                }
            }
        }
    }
}
