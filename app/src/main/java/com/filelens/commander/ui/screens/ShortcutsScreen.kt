package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.com.filelens.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.model.ShortcutAction
import com.filelens.commander.ui.components.ShortcutRecorderDialog
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.viewmodel.ShortcutsViewModel
import com.filelens.commander.ui.theme.RobotoMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    onBack: () -> Unit,
    viewModel: ShortcutsViewModel = hiltViewModel()
) {
    val bindings by viewModel.shortcuts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<ShortcutAction?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it) }
    }

    editing?.let { action ->
        ShortcutRecorderDialog(
            action = action,
            currentBinding = bindings.getValue(action),
            onDismiss = { editing = null },
            onConfirm = { binding ->
                viewModel.assign(action, binding)
                editing = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.shortcuts_title), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            actions = {
                TextButton(onClick = { viewModel.resetDefaults() }) { Text(stringResource(R.string.shortcuts_action_reset)) }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Text(
            text = stringResource(R.string.shortcuts_hint),
            style = MaterialTheme.typography.bodySmall,
            color = FlInk3,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(ShortcutAction.entries) { action ->
                val binding = bindings.getValue(action)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { editing = action }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.label, style = MaterialTheme.typography.titleSmall)
                        if (binding != action.default) {
                            Text(
                                stringResource(R.string.shortcut_default, action.default.displayString()),
                                style = MaterialTheme.typography.labelSmall,
                                color = FlInk3
                            )
                        }
                    }
                    Text(
                        text = binding.displayString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = RobotoMono),
                        color = FlInk2
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}
