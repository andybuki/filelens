package com.filelens.commander.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class CommanderAction(
    val label: String,
    val shortcut: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
fun CommanderToolbar(
    hasSelection: Boolean,
    onView: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onNewFolder: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        CommanderAction("View", "F3", Icons.Filled.Info, onView, hasSelection),
        CommanderAction("Copy", "F5", Icons.Filled.ContentCopy, onCopy, hasSelection),
        CommanderAction("Move", "F6", Icons.AutoMirrored.Filled.DriveFileMove, onMove, hasSelection),
        CommanderAction("Mkdir", "F7", Icons.Filled.CreateNewFolder, onNewFolder, true),
        CommanderAction("Delete", "F8", Icons.Filled.Delete, onDelete, hasSelection),
        CommanderAction("Rename", "F2", Icons.Filled.Edit, onRename, hasSelection)
    )

    BottomAppBar(
        modifier = modifier.height(56.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                FilledTonalButton(
                    onClick = action.onClick,
                    enabled = action.enabled,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
