package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.ShortcutAction
import com.filelens.commander.domain.model.ShortcutBinding
import com.filelens.commander.domain.model.ShortcutMap
import com.filelens.commander.domain.model.defaultShortcuts
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shortcutsDataStore: DataStore<Preferences> by preferencesDataStore(name = "filelens_shortcuts")

/**
 * Each binding lives under its own key as a compact `"K|c|s|a"` string
 * (key, ctrl, shift, alt). One field per action keeps reads cheap and
 * lets us fall back to the default if a single entry is missing or
 * corrupted, without losing every customization.
 */
@Singleton
class ShortcutsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> get() = context.shortcutsDataStore

    val shortcuts: Flow<ShortcutMap> = store.data.map { prefs ->
        ShortcutAction.entries.associateWith { action ->
            decode(prefs[keyFor(action)]) ?: action.default
        }
    }

    suspend fun set(action: ShortcutAction, binding: ShortcutBinding) {
        store.edit { it[keyFor(action)] = encode(binding) }
    }

    suspend fun resetToDefaults() {
        store.edit { prefs ->
            for (action in ShortcutAction.entries) prefs.remove(keyFor(action))
        }
    }

    private fun keyFor(action: ShortcutAction) = stringPreferencesKey("shortcut_${action.name}")

    private fun encode(binding: ShortcutBinding): String =
        "${binding.key}|${binding.ctrl}|${binding.shift}|${binding.alt}"

    private fun decode(raw: String?): ShortcutBinding? {
        if (raw.isNullOrEmpty()) return null
        val parts = raw.split('|')
        if (parts.size != 4) return null
        return runCatching {
            ShortcutBinding(
                key = parts[0],
                ctrl = parts[1].toBoolean(),
                shift = parts[2].toBoolean(),
                alt = parts[3].toBoolean()
            )
        }.getOrNull()
    }
}
