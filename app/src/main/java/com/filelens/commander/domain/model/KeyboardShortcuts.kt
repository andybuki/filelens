package com.filelens.commander.domain.model

enum class ShortcutAction(val label: String, val default: ShortcutBinding) {
    VIEW("View / inspect", ShortcutBinding(key = "F3")),
    RENAME("Rename", ShortcutBinding(key = "F2")),
    COPY("Copy", ShortcutBinding(key = "F5")),
    MOVE("Move", ShortcutBinding(key = "F6")),
    NEW_FOLDER("New folder", ShortcutBinding(key = "F7")),
    DELETE("Delete", ShortcutBinding(key = "F8")),
    REFRESH("Refresh", ShortcutBinding(key = "R", ctrl = true)),
    SELECT_ALL("Select all", ShortcutBinding(key = "A", ctrl = true)),
    UNDO("Undo delete", ShortcutBinding(key = "Z", ctrl = true)),
    NAVIGATE_UP("Navigate up", ShortcutBinding(key = "Backspace"))
}

/**
 * Single binding: a base key plus optional modifiers. The [key] string is
 * the canonical name from `androidx.compose.ui.input.key.Key.toString()`'s
 * payload (e.g. "F3", "A", "Backspace"). Storing the name keeps the format
 * stable across Compose versions even if the underlying keycode changes.
 */
data class ShortcutBinding(
    val key: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false
) {
    fun displayString(): String = buildString {
        if (ctrl) append("Ctrl+")
        if (shift) append("Shift+")
        if (alt) append("Alt+")
        append(key)
    }

    fun matches(other: ShortcutBinding): Boolean =
        key.equals(other.key, ignoreCase = true) &&
            ctrl == other.ctrl && shift == other.shift && alt == other.alt
}

typealias ShortcutMap = Map<ShortcutAction, ShortcutBinding>

fun defaultShortcuts(): ShortcutMap =
    ShortcutAction.entries.associateWith { it.default }
