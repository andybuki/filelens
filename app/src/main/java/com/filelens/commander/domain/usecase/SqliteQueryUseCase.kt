package com.filelens.commander.domain.usecase

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqliteQueryUseCase @Inject constructor() {

    data class QueryResult(
        val columns: List<String>,
        val rows: List<List<String?>>,
        val rowCount: Int,
        val truncated: Boolean
    )

    data class QueryError(val message: String)

    sealed class Result {
        data class Ok(val result: QueryResult) : Result()
        data class Error(val error: QueryError) : Result()
    }

    suspend fun listTables(path: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            SQLiteDatabase.openDatabase(
                path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { db ->
                val out = mutableListOf<String>()
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) out += cursor.getString(0)
                }
                out
            }
        }.getOrDefault(emptyList())
    }

    suspend fun query(
        path: String,
        sql: String,
        maxRows: Int = MAX_ROWS
    ): Result = withContext(Dispatchers.IO) {
        val trimmed = sql.trim().removeSuffix(";")
        // Read-only by design: refuse anything other than SELECT/PRAGMA.
        val firstWord = trimmed.takeWhile { !it.isWhitespace() }.uppercase()
        if (firstWord !in ALLOWED_VERBS) {
            return@withContext Result.Error(QueryError("Only SELECT / PRAGMA queries are allowed."))
        }

        try {
            SQLiteDatabase.openDatabase(
                path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { db ->
                db.rawQuery(trimmed, null).use { cursor ->
                    val columns = (0 until cursor.columnCount).map { cursor.getColumnName(it) }
                    val rows = mutableListOf<List<String?>>()
                    var truncated = false
                    while (cursor.moveToNext()) {
                        if (rows.size >= maxRows) {
                            truncated = true
                            break
                        }
                        rows += (0 until cursor.columnCount).map { idx ->
                            try { cursor.getString(idx) } catch (_: Exception) { "<blob>" }
                        }
                    }
                    Result.Ok(
                        QueryResult(
                            columns = columns,
                            rows = rows,
                            rowCount = rows.size,
                            truncated = truncated
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.Error(QueryError(e.message ?: "Query failed"))
        }
    }

    suspend fun previewTable(path: String, table: String, limit: Int = 100): Result {
        val safe = "\"${table.replace("\"", "\"\"")}\""
        return query(path, "SELECT * FROM $safe LIMIT $limit")
    }

    private companion object {
        const val MAX_ROWS = 500
        val ALLOWED_VERBS = setOf("SELECT", "PRAGMA", "WITH", "EXPLAIN")
    }
}
