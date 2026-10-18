package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.FrequentFolder
import com.filelens.commander.domain.model.RecentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.recentsDataStore: DataStore<Preferences> by preferencesDataStore(name = "filelens_recents")

/**
 * Tracks recently inspected files and folder visit counts so users can
 * jump back to what they were looking at. Each list is stored as a
 * delimited string (record separator 0x1E, unit separator 0x1F) and
 * capped at a fixed size to keep DataStore small.
 */
@Singleton
class RecentsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> get() = context.recentsDataStore

    val recentFiles: Flow<List<RecentFile>> = store.data.map { decodeFiles(it[KEY_FILES]) }
    val frequentFolders: Flow<List<FrequentFolder>> = store.data.map { decodeFolders(it[KEY_FOLDERS]) }

    @Suppress("unused")
    val combined: Flow<Pair<List<RecentFile>, List<FrequentFolder>>> =
        combine(recentFiles, frequentFolders) { a, b -> a to b }

    // Files shared in from other apps are temporary cache copies.
    private fun isTemporary(path: String) = path.startsWith(context.cacheDir.absolutePath)

    suspend fun recordFileOpen(path: String, displayName: String) {
        if (isTemporary(path)) return
        store.edit { prefs ->
            val current = decodeFiles(prefs[KEY_FILES]).filterNot { it.path == path }
            val next = (listOf(RecentFile(path, displayName, System.currentTimeMillis())) + current)
                .take(MAX_FILES)
            prefs[KEY_FILES] = encodeFiles(next)
        }
    }

    suspend fun recordFolderVisit(path: String, displayName: String) {
        if (path.isBlank() || isTemporary(path)) return
        store.edit { prefs ->
            val current = decodeFolders(prefs[KEY_FOLDERS])
            val existing = current.firstOrNull { it.path == path }
            val updated = if (existing == null) {
                FrequentFolder(path, displayName, System.currentTimeMillis(), 1)
            } else {
                existing.copy(
                    lastVisitedAt = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1,
                    displayName = displayName.ifEmpty { existing.displayName }
                )
            }
            val next = (listOf(updated) + current.filterNot { it.path == path })
                .take(MAX_FOLDERS)
            prefs[KEY_FOLDERS] = encodeFolders(next)
        }
    }

    suspend fun clear() {
        store.edit {
            it.remove(KEY_FILES)
            it.remove(KEY_FOLDERS)
        }
    }

    private fun encodeFiles(list: List<RecentFile>): String = list.joinToString(SEP_ROW) {
        "${it.openedAt}$SEP_FIELD${it.displayName}$SEP_FIELD${it.path}"
    }

    private fun decodeFiles(raw: String?): List<RecentFile> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(SEP_ROW).mapNotNull { row ->
            val parts = row.split(SEP_FIELD)
            if (parts.size != 3) return@mapNotNull null
            val openedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            RecentFile(path = parts[2], displayName = parts[1], openedAt = openedAt)
        }
    }

    private fun encodeFolders(list: List<FrequentFolder>): String = list.joinToString(SEP_ROW) {
        "${it.lastVisitedAt}$SEP_FIELD${it.visitCount}$SEP_FIELD${it.displayName}$SEP_FIELD${it.path}"
    }

    private fun decodeFolders(raw: String?): List<FrequentFolder> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(SEP_ROW).mapNotNull { row ->
            val parts = row.split(SEP_FIELD)
            if (parts.size != 4) return@mapNotNull null
            val lastVisitedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            val visitCount = parts[1].toIntOrNull() ?: return@mapNotNull null
            FrequentFolder(
                path = parts[3],
                displayName = parts[2],
                lastVisitedAt = lastVisitedAt,
                visitCount = visitCount
            )
        }
    }

    private companion object {
        val KEY_FILES = stringPreferencesKey("recent_files_v1")
        val KEY_FOLDERS = stringPreferencesKey("frequent_folders_v1")
        const val SEP_ROW = ""
        const val SEP_FIELD = ""
        const val MAX_FILES = 50
        const val MAX_FOLDERS = 50
    }
}
