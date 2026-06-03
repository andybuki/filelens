package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.viewmodel.SqliteBrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqliteBrowserScreen(
    dbPath: String,
    onBack: () -> Unit,
    viewModel: SqliteBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(dbPath) { viewModel.load(dbPath) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("SQLite browser", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Text(
            text = dbPath,
            style = MaterialTheme.typography.bodySmall,
            color = FlInk3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (state.tables.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.tables) { table ->
                    FilterChip(
                        selected = state.selectedTable == table,
                        onClick = { viewModel.selectTable(table) },
                        label = { Text(table) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlAccentTint,
                            selectedLabelColor = FlAccent
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("SELECT … FROM …") },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                singleLine = false,
                maxLines = 4
            )
            IconButton(
                onClick = viewModel::runQuery,
                enabled = !state.isRunning && state.query.isNotBlank()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Run", tint = FlAccent)
            }
        }

        when {
            state.isRunning -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlAccent, strokeWidth = 2.5.dp)
            }
            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(state.error ?: "", color = RiskDanger, style = MaterialTheme.typography.bodyMedium)
            }
            state.result != null -> ResultGrid(state.result!!)
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Pick a table or run a query",
                    color = FlInk3,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ResultGrid(result: com.filelens.commander.domain.usecase.SqliteQueryUseCase.QueryResult) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = buildString {
                append("${result.rowCount} rows")
                if (result.truncated) append(" · truncated")
            },
            style = MaterialTheme.typography.labelMedium,
            color = FlInk3,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // The grid scrolls horizontally as one unit; the lazy column scrolls
        // vertically over the row data. Headers share the scroll state so
        // they stay aligned with the rows underneath.
        val scroll = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize().horizontalScroll(scroll)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                result.columns.forEach { col ->
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(140.dp).padding(horizontal = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(result.rows) { row ->
                    Row(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell ?: "<null>",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (cell == null) FlInk3 else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(140.dp).padding(horizontal = 6.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
