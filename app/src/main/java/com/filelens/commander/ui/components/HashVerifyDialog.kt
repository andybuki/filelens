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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.com.filelens.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskDangerTint
import com.filelens.commander.ui.theme.RiskSafe
import com.filelens.commander.ui.theme.RiskSafeTint
import com.filelens.commander.viewmodel.HashVerifyViewModel
import com.filelens.commander.viewmodel.Match
import com.filelens.commander.ui.theme.RobotoMono

@Composable
fun HashVerifyDialog(
    filePath: String,
    fileName: String,
    onDismiss: () -> Unit,
    viewModel: HashVerifyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard: ClipboardManager = LocalClipboardManager.current

    LaunchedEffect(filePath) { viewModel.load(filePath, fileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { viewModel.reset(); onDismiss() }) { Text(stringResource(R.string.action_close)) }
        },
        title = {
            Column {
                Text(stringResource(R.string.hash_verify_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3,
                    maxLines = 1
                )
            }
        },
        text = {
            Column {
                if (state.isComputing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.hash_verify_hashing), style = MaterialTheme.typography.bodyMedium, color = FlInk2)
                    }
                } else {
                    HashRow(
                        label = "SHA-256",
                        value = state.checksum?.sha256 ?: "—",
                        onCopy = { state.checksum?.sha256?.let { clipboard.setText(AnnotatedString(it)) } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HashRow(
                        label = "MD5",
                        value = state.checksum?.md5 ?: "—",
                        onCopy = { state.checksum?.md5?.let { clipboard.setText(AnnotatedString(it)) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.hash_verify_compare_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlInk3
                )
                OutlinedTextField(
                    value = state.expected,
                    onValueChange = viewModel::setExpected,
                    placeholder = { Text(stringResource(R.string.hash_verify_paste_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                MatchBadge(state.matches, hasInput = state.expected.isNotBlank())
            }
        }
    )
}

@Composable
private fun HashRow(label: String, value: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = FlInk3)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = RobotoMono,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy), tint = FlInk2)
        }
    }
}

@Composable
private fun MatchBadge(match: Match, hasInput: Boolean) {
    if (!hasInput) return
    val (bg, fg, label, icon) = when (match) {
        Match.MATCH -> Quad(RiskSafeTint, RiskSafe, stringResource(R.string.hash_verify_match), Icons.Filled.Check)
        Match.MISMATCH -> Quad(RiskDangerTint, RiskDanger, stringResource(R.string.hash_verify_no_match), Icons.Filled.Close)
        Match.UNKNOWN -> Quad(MaterialTheme.colorScheme.surfaceVariant, FlInk2, stringResource(R.string.hash_verify_unknown), Icons.Filled.Check)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
