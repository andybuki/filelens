package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.Bookmark
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookmarksDataStore: DataStore<Preferences> by preferencesDataStore(name = "filelens_bookmarks")

/**
 * Stores user folder bookmarks as a delimited string in DataStore. Rows are
 * separated by the ASCII Record Separator (0x1E); fields by the Unit
 * Separator (0x1F). Both are non-printable so they cannot collide with any
 * realistic path or display name.
 */
@Singleton
class BookmarksRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> get() = context.bookmarksDataStore

    val bookmarks: Flow<List<Bookmark>> = store.data.map { decode(it[KEY]) }

    suspend fun add(path: String, displayName: String) {
        store.edit { prefs ->
            val current = decode(prefs[KEY]).filterNot { it.path == path }
            val next = current + Bookmark(path, displayName, System.currentTimeMillis())
            prefs[KEY] = encode(next)
        }
    }

    suspend fun remove(path: String) {
        store.edit { prefs ->
            prefs[KEY] = encode(decode(prefs[KEY]).filterNot { it.path == path })
        }
    }

    private fun encode(list: List<Bookmark>): String = list.joinToString(SEP_ROW) {
        "${it.addedAt}$SEP_FIELD${it.displayName}$SEP_FIELD${it.path}"
    }

    private fun decode(raw: String?): List<Bookmark> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(SEP_ROW).mapNotNull { row ->
            val parts = row.split(SEP_FIELD)
            if (parts.size != 3) return@mapNotNull null
            val addedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            Bookmark(path = parts[2], displayName = parts[1], addedAt = addedAt)
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("bookmarks_v1")
        const val SEP_ROW = ""   // ASCII Record Separator
        const val SEP_FIELD = "" // ASCII Unit Separator
    }
}
