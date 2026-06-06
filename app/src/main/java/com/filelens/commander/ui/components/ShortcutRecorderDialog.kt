package com.filelens.commander.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.filelens.commander.R
import com.filelens.commander.domain.model.ShortcutAction
import com.filelens.commander.domain.model.ShortcutBinding
import com.filelens.commander.ui.theme.FlInk3

@Composable
fun ShortcutRecorderDialog(
    action: ShortcutAction,
    currentBinding: ShortcutBinding,
    onDismiss: () -> Unit,
    onConfirm: (ShortcutBinding) -> Unit
) {
    var captured by remember(action) { mutableStateOf<ShortcutBinding?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(action) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shortcut_bind_title, action.label)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        // Only respond to KeyDown so users don't trigger on
                        // release. Pure modifier presses (just Ctrl, Shift, Alt)
                        // are ignored — we want a real key with optional mods.
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val name = keyName(event.key) ?: return@onPreviewKeyEvent false
                        if (name in MODIFIER_NAMES) return@onPreviewKeyEvent false
                        captured = ShortcutBinding(
                            key = name,
                            ctrl = event.isCtrlPressed,
                            shift = event.isShiftPressed,
                            alt = event.isAltPressed
                        )
                        true
                    }
            ) {
                Text(
                    stringResource(R.string.shortcut_press_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = captured?.displayString() ?: stringResource(R.string.shortcut_current, currentBinding.displayString()),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { captured?.let(onConfirm) },
                enabled = captured != null
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * Compose's [Key] is opaque-ish — its [toString] returns "Key: NAME (code)".
 * We pull just the name out so what we persist is what the user sees in the UI.
 */
private fun keyName(key: Key): String? {
    val s = key.toString()
    val open = s.indexOf(':')
    if (open < 0) return null
    val nameEnd = s.indexOf('(', open).let { if (it < 0) s.length else it }
    val name = s.substring(open + 1, nameEnd).trim()
    return name.takeIf { it.isNotEmpty() }
}

private val MODIFIER_NAMES = setOf(
    "Ctrl Left", "Ctrl Right",
    "Shift Left", "Shift Right",
    "Alt Left", "Alt Right",
    "Meta Left", "Meta Right",
    "Function"
)
