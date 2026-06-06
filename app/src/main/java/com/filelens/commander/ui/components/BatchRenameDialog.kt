package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.com.filelens.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.usecase.BatchRenameUseCase
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskDangerTint
import com.filelens.commander.ui.theme.RiskSafe
import com.filelens.commander.viewmodel.BatchRenameViewModel
import com.filelens.commander.ui.theme.RobotoMono

@Composable
fun BatchRenameDialog(
    sourcePaths: List<String>,
    onDismiss: () -> Unit,
    onApplied: () -> Unit,
    viewModel: BatchRenameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(sourcePaths) { viewModel.start(sourcePaths) }

    val hasCollisions = state.plan.collisions.isNotEmpty()
    val hasInvalid = state.plan.items.any { !it.isValid }

    AlertDialog(
        onDismissRequest = { viewModel.reset(); onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { viewModel.commit { viewModel.reset(); onApplied() } },
                enabled = !state.applying && !hasCollisions && !hasInvalid && state.plan.items.isNotEmpty()
            ) {
                Text(if (state.applying) stringResource(R.string.batch_rename_applying) else pluralStringResource(R.plurals.plural_rename_action, state.plan.items.size, state.plan.items.size))
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.reset(); onDismiss() }) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.batch_rename_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.batch_rename_tokens_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = FlInk3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.rule.pattern,
                    onValueChange = { viewModel.updateRule(state.rule.copy(pattern = it)) },
                    label = { Text(stringResource(R.string.batch_rename_label_pattern)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.rule.find,
                        onValueChange = { viewModel.updateRule(state.rule.copy(find = it)) },
                        label = { Text(stringResource(R.string.batch_rename_label_find)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = state.rule.replace,
                        onValueChange = { viewModel.updateRule(state.rule.copy(replace = it)) },
                        label = { Text(stringResource(R.string.batch_rename_label_replace)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = state.rule.useRegex,
                        onCheckedChange = { viewModel.updateRule(state.rule.copy(useRegex = it)) }
                    )
                    Text(stringResource(R.string.batch_rename_use_regex), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.batch_rename_start_index, state.rule.startIndex),
                        style = MaterialTheme.typography.labelMedium,
                        color = FlInk3
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (hasCollisions) {
                    Banner(text = pluralStringResource(R.plurals.plural_collision_count, state.plan.collisions.size, state.plan.collisions.size), isError = true)
                } else if (hasInvalid) {
                    Banner(text = stringResource(R.string.batch_rename_banner_invalid), isError = true)
                } else if (state.plan.items.isNotEmpty()) {
                    Banner(text = stringResource(R.string.batch_rename_banner_ready), isError = false)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 240.dp)
                ) {
                    items(state.plan.items, key = { it.sourcePath }) { item ->
                        val changed = item.newName != item.originalName
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = item.originalName,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = RobotoMono,
                                color = FlInk3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.batch_rename_arrow, item.newName),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = RobotoMono,
                                color = when {
                                    !item.isValid -> RiskDanger
                                    changed -> MaterialTheme.colorScheme.onSurface
                                    else -> FlInk3
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.reason != null) {
                                Text(
                                    text = item.reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RiskDanger
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun Banner(text: String, isError: Boolean) {
    val bg = if (isError) RiskDangerTint else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isError) RiskDanger else RiskSafe
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
    Spacer(modifier = Modifier.height(6.dp))
}
