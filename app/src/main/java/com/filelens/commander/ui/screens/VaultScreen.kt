package com.filelens.commander.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.VaultItem
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.MonoSmall
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.ui.vault.VaultAuthAvailability
import com.filelens.commander.ui.vault.launchVaultAuth
import com.filelens.commander.ui.vault.vaultAuthAvailability
import com.filelens.commander.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@Composable
fun VaultScreen(
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingErase by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    // Auto-lock the vault when the user leaves the screen. Mirrors the
    // expected "session-only" behaviour of competitor locked folders.
    DisposableEffect(Unit) {
        onDispose { viewModel.lock() }
    }

    BackHandler {
        if (state.selectedPaths.isNotEmpty()) viewModel.clearSelection() else onBack()
    }

    val unlockTitle = stringResource(R.string.vault_unlock_title)
    val unlockSubtitle = stringResource(R.string.vault_unlock_subtitle)

    val triggerUnlock: () -> Unit = {
        val act = activity
        if (act != null) {
            launchVaultAuth(
                activity = act,
                title = unlockTitle,
                subtitle = unlockSubtitle,
                onSuccess = { viewModel.unlock() },
                onFailure = { msg ->
                    if (msg.isNotBlank()) {
                        scope.launch { snackbar.showSnackbar(msg) }
                    }
                }
            )
        }
    }

    if (pendingErase) {
        AlertDialog(
            onDismissRequest = { pendingErase = false },
            title = { Text(stringResource(R.string.vault_erase_title)) },
            text = { Text(stringResource(R.string.vault_erase_body, state.selectedPaths.size)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingErase = false
                    viewModel.eraseSelected()
                }) {
                    Text(stringResource(R.string.action_delete), color = RiskDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingErase = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VaultHeader(
            unlocked = state.isUnlocked,
            count = state.items.size,
            selection = state.selectedPaths.size,
            onBack = onBack,
            onLock = { viewModel.lock() },
            onClearSelection = { viewModel.clearSelection() }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!state.isUnlocked) {
                LockedState(
                    count = state.items.size,
                    onUnlock = triggerUnlock,
                    availability = remember { vaultAuthAvailability(context) }
                )
            } else if (state.items.isEmpty()) {
                EmptyState()
            } else {
                VaultList(
                    items = state.items,
                    selectedPaths = state.selectedPaths,
                    onToggle = { viewModel.toggleSelection(it) }
                )
            }
        }

        if (state.isUnlocked && state.selectedPaths.isNotEmpty()) {
            SelectionActions(
                onRestore = { viewModel.restoreSelected() },
                onErase = { pendingErase = true },
                onSelectAll = { viewModel.selectAll() }
            )
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VaultHeader(
    unlocked: Boolean,
    count: Int,
    selection: Int,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vault_title),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pluralStringResource(R.plurals.plural_vault_item_count, count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE5E7EB)
                )
            }
            if (unlocked) {
                if (selection > 0) {
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_selection),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    IconButton(onClick = onLock) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.vault_action_lock),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedState(
    count: Int,
    onUnlock: () -> Unit,
    availability: VaultAuthAvailability
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.vault_locked_headline),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.vault_locked_subtitle, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))
            when (availability) {
                VaultAuthAvailability.AVAILABLE -> {
                    Button(
                        onClick = onUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.vault_unlock_button))
                    }
                }
                VaultAuthAvailability.NOT_ENROLLED -> {
                    Text(
                        stringResource(R.string.vault_not_enrolled),
                        style = MaterialTheme.typography.bodySmall,
                        color = RiskDanger
                    )
                }
                VaultAuthAvailability.NO_HARDWARE,
                VaultAuthAvailability.UNAVAILABLE -> {
                    Text(
                        stringResource(R.string.vault_no_hardware),
                        style = MaterialTheme.typography.bodySmall,
                        color = RiskDanger
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                stringResource(R.string.vault_empty_headline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.vault_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultList(
    items: List<VaultItem>,
    selectedPaths: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item("section-header") {
            Text(
                stringResource(R.string.vault_section_files).uppercase(),
                style = Eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        items(items, key = { it.vaultPath }) { item ->
            VaultRow(
                item = item,
                selected = item.vaultPath in selectedPaths,
                onClick = { onToggle(item.vaultPath) }
            )
        }
    }
}

@Composable
private fun VaultRow(item: VaultItem, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(item.size) + " · " + compactPath(item.originalPath),
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SelectionActions(
    onRestore: () -> Unit,
    onErase: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.vault_action_select_all))
        }
        Button(
            onClick = onRestore,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Output, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.vault_action_restore))
        }
        OutlinedButton(onClick = onErase, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = RiskDanger,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.vault_action_erase), color = RiskDanger)
        }
    }
}

private fun compactPath(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
}
