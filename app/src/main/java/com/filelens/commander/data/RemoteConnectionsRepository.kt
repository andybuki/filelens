package com.filelens.commander.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filelens.commander.domain.model.RemoteConnection
import com.filelens.commander.domain.model.RemoteProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.remoteConnectionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "filelens_remote_connections")

/**
 * Persists saved FTP/SFTP/SMB connections. Stores them as a delimited string —
 * same convention as [BookmarksRepository] — so we avoid pulling in a JSON
 * dependency for what is effectively a small flat list.
 *
 * WARNING: passwords are stored in plain text. This is a known gap; a follow-up
 * should move secrets behind the Android Keystore.
 */
@Singleton
class RemoteConnectionsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> get() = context.remoteConnectionsDataStore

    val connections: Flow<List<RemoteConnection>> = store.data.map { decode(it[KEY]) }

    suspend fun upsert(connection: RemoteConnection): RemoteConnection {
        val withId = if (connection.id.isBlank()) connection.copy(id = UUID.randomUUID().toString())
        else connection
        store.edit { prefs ->
            val current = decode(prefs[KEY]).filterNot { it.id == withId.id }
            prefs[KEY] = encode(current + withId)
        }
        return withId
    }

    suspend fun remove(id: String) {
        store.edit { prefs ->
            prefs[KEY] = encode(decode(prefs[KEY]).filterNot { it.id == id })
        }
    }

    suspend fun get(id: String): RemoteConnection? {
        var found: RemoteConnection? = null
        store.edit { prefs -> found = decode(prefs[KEY]).firstOrNull { it.id == id } }
        return found
    }

    private fun encode(list: List<RemoteConnection>): String = list.joinToString(SEP_ROW) { c ->
        listOf(
            c.id,
            c.name,
            c.protocol.name,
            c.host,
            c.port.toString(),
            c.username,
            c.password,
            c.basePath,
            c.shareName,
            c.anonymous.toString(),
            c.ftpPassive.toString(),
            c.createdAt.toString()
        ).joinToString(SEP_FIELD)
    }

    private fun decode(raw: String?): List<RemoteConnection> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(SEP_ROW).mapNotNull { row ->
            val parts = row.split(SEP_FIELD)
            if (parts.size != 12) return@mapNotNull null
            val protocol = runCatching { RemoteProtocol.valueOf(parts[2]) }.getOrNull() ?: return@mapNotNull null
            RemoteConnection(
                id = parts[0],
                name = parts[1],
                protocol = protocol,
                host = parts[3],
                port = parts[4].toIntOrNull() ?: protocol.defaultPort,
                username = parts[5],
                password = parts[6],
                basePath = parts[7],
                shareName = parts[8],
                anonymous = parts[9].toBoolean(),
                ftpPassive = parts[10].toBoolean(),
                createdAt = parts[11].toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("remote_connections_v1")
        const val SEP_ROW = ""   // ASCII Record Separator
        const val SEP_FIELD = "" // ASCII Unit Separator
    }
}
