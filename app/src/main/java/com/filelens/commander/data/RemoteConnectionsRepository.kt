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
 * Persists saved FTP/SFTP/SMB connections as a delimited string in DataStore.
 * Passwords and pinned SFTP host keys live in [SecretsRepository] (Keystore-
 * backed) and are merged into the returned [RemoteConnection] on demand; the
 * DataStore row only ever holds non-sensitive fields.
 */
@Singleton
class RemoteConnectionsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secrets: SecretsRepository
) {
    private val store: DataStore<Preferences> get() = context.remoteConnectionsDataStore

    /** Lightweight list for the management UI — never includes the password. */
    val connections: Flow<List<RemoteConnection>> = store.data.map { decode(it[KEY]) }

    suspend fun upsert(connection: RemoteConnection): RemoteConnection {
        val withId = if (connection.id.isBlank()) connection.copy(id = UUID.randomUUID().toString())
        else connection
        // Persist secrets separately. If callers passed an empty password they
        // mean "keep what's there" — only overwrite when the field was set.
        if (withId.password.isNotEmpty()) secrets.setPassword(withId.id, withId.password)
        if (withId.expectedHostKey.isNotEmpty()) secrets.setHostKey(withId.id, withId.expectedHostKey)
        val storable = withId.copy(password = "", expectedHostKey = "")
        store.edit { prefs ->
            val current = decode(prefs[KEY]).filterNot { it.id == withId.id }
            prefs[KEY] = encode(current + storable)
        }
        return withId
    }

    suspend fun remove(id: String) {
        secrets.clear(id)
        store.edit { prefs ->
            prefs[KEY] = encode(decode(prefs[KEY]).filterNot { it.id == id })
        }
    }

    /** Returns the connection with its password and pinned host key filled in. */
    suspend fun get(id: String): RemoteConnection? {
        var found: RemoteConnection? = null
        store.edit { prefs -> found = decode(prefs[KEY]).firstOrNull { it.id == id } }
        return found?.copy(
            password = secrets.getPassword(id),
            expectedHostKey = secrets.getHostKey(id)
        )
    }

    /** Persists a freshly-captured host key without touching the rest of the row. */
    fun saveHostKey(connectionId: String, hostKey: String) {
        secrets.setHostKey(connectionId, hostKey)
    }

    /** Forgets the pinned host key — next connect will re-pin on trust. */
    fun clearHostKey(connectionId: String) {
        secrets.setHostKey(connectionId, "")
    }

    private fun encode(list: List<RemoteConnection>): String = list.joinToString(SEP_ROW) { c ->
        listOf(
            c.id,
            c.name,
            c.protocol.name,
            c.host,
            c.port.toString(),
            c.username,
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
            if (parts.size != 11) return@mapNotNull null
            val protocol = runCatching { RemoteProtocol.valueOf(parts[2]) }.getOrNull() ?: return@mapNotNull null
            RemoteConnection(
                id = parts[0],
                name = parts[1],
                protocol = protocol,
                host = parts[3],
                port = parts[4].toIntOrNull() ?: protocol.defaultPort,
                username = parts[5],
                password = "",
                basePath = parts[6],
                shareName = parts[7],
                anonymous = parts[8].toBoolean(),
                ftpPassive = parts[9].toBoolean(),
                createdAt = parts[10].toLongOrNull() ?: System.currentTimeMillis(),
                expectedHostKey = ""
            )
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("remote_connections_v2")
        const val SEP_ROW = ""   // ASCII Record Separator
        const val SEP_FIELD = "" // ASCII Unit Separator
    }
}
