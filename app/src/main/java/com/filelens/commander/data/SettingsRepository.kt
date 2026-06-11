package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.AppSettings
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.StartScreen
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
            showHidden = p[KEY_SHOW_HIDDEN] ?: false,
            hideEmptyFolders = p[KEY_HIDE_EMPTY_FOLDERS] ?: false,
            sizeUnits = p[KEY_SIZE_UNITS]?.let { runCatching { SizeUnits.valueOf(it) }.getOrNull() } ?: SizeUnits.BINARY,
            defaultSortField = p[KEY_SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() } ?: SortField.NAME,
            defaultSortDirection = p[KEY_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() } ?: SortDirection.ASCENDING,
            defaultPanelMode = p[KEY_PANEL_MODE]?.let { runCatching { PanelMode.valueOf(it) }.getOrNull() } ?: PanelMode.INSPECTOR,
            remotePreviewLimitMb = p[KEY_REMOTE_PREVIEW_LIMIT_MB] ?: AppSettings.DEFAULT_REMOTE_PREVIEW_LIMIT_MB,
            remoteArchivePreviewLimitMb = p[KEY_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB] ?: AppSettings.DEFAULT_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB,
            languageTag = p[KEY_LANGUAGE_TAG]?.takeIf { it.isNotBlank() },
            startScreen = p[KEY_START_SCREEN]?.let { runCatching { StartScreen.valueOf(it) }.getOrNull() } ?: StartScreen.HOME,
            askBeforeDelete = p[KEY_ASK_BEFORE_DELETE] ?: true,
            searchRoots = p[KEY_SEARCH_ROOTS] ?: emptySet(),
            telemetryEnabled = p[KEY_TELEMETRY_ENABLED] ?: false,
            warnRootStorage = p[KEY_WARN_ROOT_STORAGE] ?: true
        )
    }

    suspend fun setRemotePreviewLimitMb(mb: Int) {
        val clamped = mb.coerceIn(
            AppSettings.REMOTE_PREVIEW_LIMIT_MIN_MB,
            AppSettings.REMOTE_PREVIEW_LIMIT_MAX_MB
        )
        store.edit { it[KEY_REMOTE_PREVIEW_LIMIT_MB] = clamped }
    }

    suspend fun setRemoteArchivePreviewLimitMb(mb: Int) {
        val clamped = mb.coerceIn(
            AppSettings.REMOTE_ARCHIVE_PREVIEW_LIMIT_MIN_MB,
            AppSettings.REMOTE_ARCHIVE_PREVIEW_LIMIT_MAX_MB
        )
        store.edit { it[KEY_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB] = clamped }
    }

    suspend fun setDefaultPanelMode(mode: PanelMode) {
        store.edit { it[KEY_PANEL_MODE] = mode.name }
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

    suspend fun setLanguageTag(tag: String?) {
        store.edit {
            if (tag.isNullOrBlank()) it.remove(KEY_LANGUAGE_TAG)
            else it[KEY_LANGUAGE_TAG] = tag
        }
    }

    suspend fun setStartScreen(screen: StartScreen) {
        store.edit { it[KEY_START_SCREEN] = screen.name }
    }

    suspend fun setAskBeforeDelete(value: Boolean) {
        store.edit { it[KEY_ASK_BEFORE_DELETE] = value }
    }

    suspend fun setSearchRoots(roots: Set<String>) {
        store.edit { it[KEY_SEARCH_ROOTS] = roots }
    }

    suspend fun addSearchRoot(path: String) {
        store.edit { prefs ->
            val current = prefs[KEY_SEARCH_ROOTS] ?: emptySet()
            prefs[KEY_SEARCH_ROOTS] = current + path
        }
    }

    suspend fun removeSearchRoot(path: String) {
        store.edit { prefs ->
            val current = prefs[KEY_SEARCH_ROOTS] ?: emptySet()
            prefs[KEY_SEARCH_ROOTS] = current - path
        }
    }

    suspend fun setTelemetryEnabled(value: Boolean) {
        store.edit { it[KEY_TELEMETRY_ENABLED] = value }
    }

    suspend fun setWarnRootStorage(value: Boolean) {
        store.edit { it[KEY_WARN_ROOT_STORAGE] = value }
    }

    val hasSeenOnboarding: Flow<Boolean> = store.data.map { p ->
        p[KEY_HAS_SEEN_ONBOARDING] ?: false
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        store.edit { it[KEY_HAS_SEEN_ONBOARDING] = value }
    }

    private companion object {
        // KEY_THEME ("theme_mode") was removed in v1.0 when FileLens went
        // light-only. The stored value is intentionally not migrated; old
        // installs simply ignore it.
        val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val KEY_HIDE_EMPTY_FOLDERS = booleanPreferencesKey("hide_empty_folders")
        val KEY_SIZE_UNITS = stringPreferencesKey("size_units")
        val KEY_SORT_FIELD = stringPreferencesKey("sort_field")
        val KEY_SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val KEY_PANEL_MODE = stringPreferencesKey("panel_mode")
        val KEY_REMOTE_PREVIEW_LIMIT_MB = intPreferencesKey("remote_preview_limit_mb")
        val KEY_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB = intPreferencesKey("remote_archive_preview_limit_mb")
        val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val KEY_LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val KEY_START_SCREEN = stringPreferencesKey("start_screen")
        val KEY_ASK_BEFORE_DELETE = booleanPreferencesKey("ask_before_delete")
        val KEY_SEARCH_ROOTS = stringSetPreferencesKey("search_roots")
        val KEY_TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        val KEY_WARN_ROOT_STORAGE = booleanPreferencesKey("warn_root_storage")
    }
}
