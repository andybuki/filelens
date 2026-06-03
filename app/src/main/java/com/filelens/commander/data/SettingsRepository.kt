package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.AppSettings
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "filelens_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> get() = context.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            showHidden = p[KEY_SHOW_HIDDEN] ?: false,
            hideEmptyFolders = p[KEY_HIDE_EMPTY_FOLDERS] ?: false,
            sizeUnits = p[KEY_SIZE_UNITS]?.let { runCatching { SizeUnits.valueOf(it) }.getOrNull() } ?: SizeUnits.BINARY,
            defaultSortField = p[KEY_SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() } ?: SortField.NAME,
            defaultSortDirection = p[KEY_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() } ?: SortDirection.ASCENDING,
            defaultPanelMode = p[KEY_PANEL_MODE]?.let { runCatching { PanelMode.valueOf(it) }.getOrNull() } ?: PanelMode.INSPECTOR
        )
    }

    suspend fun setDefaultPanelMode(mode: PanelMode) {
        store.edit { it[KEY_PANEL_MODE] = mode.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setShowHidden(value: Boolean) {
        store.edit { it[KEY_SHOW_HIDDEN] = value }
    }

    suspend fun setHideEmptyFolders(value: Boolean) {
        store.edit { it[KEY_HIDE_EMPTY_FOLDERS] = value }
    }

    suspend fun setSizeUnits(units: SizeUnits) {
        store.edit { it[KEY_SIZE_UNITS] = units.name }
    }

    suspend fun setDefaultSort(field: SortField, direction: SortDirection) {
        store.edit {
            it[KEY_SORT_FIELD] = field.name
            it[KEY_SORT_DIRECTION] = direction.name
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val KEY_HIDE_EMPTY_FOLDERS = booleanPreferencesKey("hide_empty_folders")
        val KEY_SIZE_UNITS = stringPreferencesKey("size_units")
        val KEY_SORT_FIELD = stringPreferencesKey("sort_field")
        val KEY_SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val KEY_PANEL_MODE = stringPreferencesKey("panel_mode")
    }
}
