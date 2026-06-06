package com.filelens.commander.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.filelens.commander.R
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk2

@Composable
fun SelectionBar(
    selectedCount: Int,
    canCompare: Boolean,
    onClear: () -> Unit,
    onCompare: () -> Unit,
    canBatchRename: Boolean = false,
    onBatchRename: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Slide-in pill: gives the user a visible action surface the moment they
    // long-press an item. Without it the bottom commander toolbar is the only
    // touch path to multi-select actions and it's easy to miss.
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FlAccentTint)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_selection), tint = FlAccent)
            }
            Text(
                text = pluralStringResource(R.plurals.plural_selected_count, selectedCount, selectedCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (canBatchRename) {
                TextButton(onClick = onBatchRename) {
                    Icon(
                        Icons.Filled.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = FlAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_rename), color = FlAccent)
                }
            }
            if (canCompare) {
                TextButton(onClick = onCompare) {
                    Icon(
                        Icons.Filled.Compare,
                        contentDescription = null,
                        tint = FlAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_compare), color = FlAccent)
                }
            } else if (!canBatchRename) {
                Text(
                    text = stringResource(R.string.selection_hint_select_two),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlInk2
                )
            }
        }
    }
}
