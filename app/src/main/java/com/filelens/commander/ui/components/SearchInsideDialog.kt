package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.filelens.commander.R
import com.filelens.commander.domain.model.SearchHit
import com.filelens.commander.domain.model.SearchHitKind
import com.filelens.commander.ui.theme.RobotoMono

@Composable
fun SearchInsideDialog(
    initialQuery: String,
    hits: List<SearchHit>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_inside_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_inside_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { if (query.isNotBlank()) onSearch(query) }) {
                        Text(stringResource(R.string.action_find))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.search_inside_searching), style = MaterialTheme.typography.bodySmall)
                    }
                } else if (hits.isEmpty() && initialQuery.isNotBlank()) {
                    Text(
                        stringResource(R.string.search_inside_no_matches),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hits.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(R.plurals.plural_match_count, hits.size, hits.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val grouped = hits.groupBy { it.kind }
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        grouped.forEach { (kind, group) ->
                            Text(
                                text = "${kind.displayName} (${group.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                            group.take(50).forEach { hit ->
                                HitRow(hit)
                            }
                            if (group.size > 50) {
                                Text(
                                    stringResource(R.string.search_inside_more, group.size - 50),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun HitRow(hit: SearchHit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = hit.location,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = hit.snippet,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMono),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
