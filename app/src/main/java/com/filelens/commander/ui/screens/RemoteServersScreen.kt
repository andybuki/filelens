package com.filelens.commander.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.RemoteConnection
import com.filelens.commander.domain.model.RemoteProtocol
import com.filelens.commander.viewmodel.RemoteServersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteServersScreen(
    onBack: () -> Unit,
    onConnect: (connectionId: String) -> Unit = { _ -> onBack() },
    viewModel: RemoteServersViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RemoteConnection?>(null) }
    var pendingDelete by remember { mutableStateOf<RemoteConnection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remote_servers_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = viewModel.blankDraft() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_server))
            }
        }
    ) { padding ->
        if (connections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.remote_servers_empty_headline),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.remote_servers_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(connections, key = { it.id }) { connection ->
                    ServerRow(
                        connection = connection,
                        onConnect = {
                            onConnect(connection.id)
                        },
                        onEdit = { editing = connection },
                        onDelete = { pendingDelete = connection }
                    )
                }
            }
        }
    }

    editing?.let { draft ->
        ServerEditorDialog(
            initial = draft,
            onDismiss = { editing = null },
            onSave = { saved, clearHostKey ->
                viewModel.save(saved, clearHostKey)
                editing = null
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.remote_dialog_delete_title)) },
            text = { Text(stringResource(R.string.remote_dialog_delete_message, target.name.ifBlank { target.host })) },
            confirmButton = {
                Button(onClick = {
                    viewModel.remove(target.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun ServerRow(
    connection: RemoteConnection,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onConnect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                connection.name.ifBlank { connection.host },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${connection.protocol.displayName}  ·  ${connection.host}:${connection.effectivePort}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onConnect) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.cd_connect))
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerEditorDialog(
    initial: RemoteConnection,
    onDismiss: () -> Unit,
    onSave: (RemoteConnection, Boolean) -> Unit
) {
    val isEdit = initial.id.isNotBlank()
    var protocol by remember { mutableStateOf(initial.protocol) }
    var name by remember { mutableStateOf(initial.name) }
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var username by remember { mutableStateOf(initial.username) }
    var password by remember { mutableStateOf("") }
    var basePath by remember { mutableStateOf(initial.basePath) }
    var shareName by remember { mutableStateOf(initial.shareName) }
    var anonymous by remember { mutableStateOf(initial.anonymous) }
    var ftpPassive by remember { mutableStateOf(initial.ftpPassive) }
    var clearPinnedHostKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id.isBlank()) R.string.remote_dialog_add_title else R.string.remote_dialog_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteProtocol.entries.forEach { p ->
                        FilterChip(
                            selected = protocol == p,
                            onClick = {
                                protocol = p
                                val current = port.toIntOrNull() ?: 0
                                if (current == 0 || RemoteProtocol.entries.any { it.defaultPort == current }) {
                                    port = p.defaultPort.toString()
                                }
                            },
                            label = { Text(p.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.remote_field_display_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.remote_field_host)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.remote_field_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (protocol == RemoteProtocol.FTP) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = anonymous, onCheckedChange = { anonymous = it })
                        Text(stringResource(R.string.remote_option_anonymous))
                        Spacer(Modifier.width(16.dp))
                        Checkbox(checked = ftpPassive, onCheckedChange = { ftpPassive = it })
                        Text(stringResource(R.string.remote_option_passive_mode))
                    }
                }
                if (!anonymous || protocol != RemoteProtocol.FTP) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.remote_field_username)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(if (isEdit) R.string.remote_field_password_edit else R.string.remote_field_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isEdit && protocol == RemoteProtocol.SFTP) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearPinnedHostKey,
                            onCheckedChange = { clearPinnedHostKey = it }
                        )
                        Text(
                            stringResource(R.string.remote_option_clear_host_key),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (protocol == RemoteProtocol.SMB) {
                    OutlinedTextField(
                        value = shareName,
                        onValueChange = { shareName = it },
                        label = { Text(stringResource(R.string.remote_field_share_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = basePath,
                    onValueChange = { basePath = it },
                    label = { Text(stringResource(R.string.remote_field_base_path)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = host.isNotBlank() && (protocol != RemoteProtocol.SMB || shareName.isNotBlank()),
                onClick = {
                    val portNum = port.toIntOrNull()?.takeIf { it > 0 } ?: protocol.defaultPort
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            protocol = protocol,
                            host = host.trim(),
                            port = portNum,
                            username = username.trim(),
                            password = password,
                            basePath = basePath.trim(),
                            shareName = shareName.trim(),
                            anonymous = anonymous,
                            ftpPassive = ftpPassive
                        ),
                        clearPinnedHostKey
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
