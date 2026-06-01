package com.filelens.commander.inspector

import android.database.sqlite.SQLiteDatabase
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqliteInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.DATABASE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()
        var structure: StructureNode? = null
        var previewContent: PreviewContent? = null

        try {
            overview["File size"] = formatSize(file.length())

            val db = SQLiteDatabase.openDatabase(
                file.absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )

            db.use { database ->
                val version = queryScalar(database, "PRAGMA user_version")
                overview["User version"] = version ?: "0"

                val sqliteVersion = queryScalar(database, "SELECT sqlite_version()")
                sqliteVersion?.let { overview["SQLite version"] = it }

                val journalMode = queryScalar(database, "PRAGMA journal_mode")
                journalMode?.let { overview["Journal mode"] = it }

                if (journalMode.equals("wal", ignoreCase = true)) {
                    risks.add(
                        RiskItem(
                            severity = RiskSeverity.WARNING,
                            title = "WAL journal mode detected",
                            description = "This database uses WAL mode. There may be uncommitted data in the WAL file that is not visible in this snapshot."
                        )
                    )
                }

                val tables = queryList(database, "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
                overview["Tables"] = tables.size.toString()

                val indices = queryList(database, "SELECT name FROM sqlite_master WHERE type='index' ORDER BY name")
                overview["Indices"] = indices.size.toString()

                val tableNodes = mutableListOf<StructureNode>()
                for (table in tables) {
                    val rowCount = queryScalar(database, "SELECT COUNT(*) FROM \"${table.replace("\"", "\"\"")}\"") ?: "?"
                    val columns = mutableListOf<StructureNode>()

                    database.rawQuery("PRAGMA table_info(\"${table.replace("\"", "\"\"")}\")", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val colName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            val colType = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                            val notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1
                            val pk = cursor.getInt(cursor.getColumnIndexOrThrow("pk")) > 0

                            val attrs = buildList {
                                add(colType.ifEmpty { "ANY" })
                                if (pk) add("PK")
                                if (notNull) add("NOT NULL")
                            }.joinToString(", ")

                            columns.add(StructureNode.Leaf(name = colName, value = attrs))
                        }
                    }

                    tableNodes.add(
                        StructureNode.Branch(
                            name = table,
                            children = columns,
                            details = "$rowCount rows"
                        )
                    )
                }

                structure = StructureNode.Branch(
                    name = file.name,
                    children = tableNodes,
                    size = file.length()
                )

                val schemaBuilder = StringBuilder()
                database.rawQuery(
                    "SELECT type, name, sql FROM sqlite_master WHERE sql IS NOT NULL ORDER BY type, name",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val sql = cursor.getString(cursor.getColumnIndexOrThrow("sql"))
                        schemaBuilder.appendLine(sql).appendLine(";").appendLine()
                    }
                }

                val schemaText = schemaBuilder.toString().trimEnd()
                if (schemaText.isNotEmpty()) {
                    previewContent = PreviewContent.TextContent(
                        content = schemaText,
                        language = "SQL",
                        lineCount = schemaText.lines().size
                    )
                }
            }
        } catch (e: Exception) {
            overview["Error"] = e.message ?: "Failed to open database"
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            structure = structure,
            previewContent = previewContent
        )
    }

    private fun queryScalar(db: SQLiteDatabase, sql: String): String? {
        return try {
            db.rawQuery(sql, null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryList(db: SQLiteDatabase, sql: String): List<String> {
        val result = mutableListOf<String>()
        try {
            db.rawQuery(sql, null).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(0))
                }
            }
        } catch (_: Exception) {
            // ignore
        }
        return result
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}
