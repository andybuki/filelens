package com.filelens.commander.domain.usecase

import com.filelens.commander.data.BookmarksRepository
import com.filelens.commander.data.RecentsRepository
import com.filelens.commander.data.SettingsRepository
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BackupUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val recentsRepository: RecentsRepository
) {

    suspend fun export(): String {
        val settings = settingsRepository.settings.first()
        val bookmarks = bookmarksRepository.bookmarks.first()
        val recentFiles = recentsRepository.recentFiles.first()
        val frequentFolders = recentsRepository.frequentFolders.first()

        val root = JSONObject().apply {
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("showHidden", settings.showHidden)
                put("hideEmptyFolders", settings.hideEmptyFolders)
                put("sizeUnits", settings.sizeUnits.name)
                put("defaultSortField", settings.defaultSortField.name)
                put("defaultSortDirection", settings.defaultSortDirection.name)
                put("defaultPanelMode", settings.defaultPanelMode.name)
            })
            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { bm ->
                    put(JSONObject().apply {
                        put("path", bm.path)
                        put("displayName", bm.displayName)
                        put("addedAt", bm.addedAt)
                    })
                }
            })
            put("recentFiles", JSONArray().apply {
                recentFiles.forEach { f ->
                    put(JSONObject().apply {
                        put("path", f.path)
                        put("displayName", f.displayName)
                        put("openedAt", f.openedAt)
                    })
                }
            })
            put("frequentFolders", JSONArray().apply {
                frequentFolders.forEach { f ->
                    put(JSONObject().apply {
                        put("path", f.path)
                        put("displayName", f.displayName)
                        put("lastVisitedAt", f.lastVisitedAt)
                        put("visitCount", f.visitCount)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importJson(json: String): ImportResult {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return ImportResult(success = false, message = "Invalid JSON: ${e.message}")
        }

        var imported = 0

        root.optJSONObject("settings")?.let { s ->
            runCatching {
                // themeMode is intentionally ignored: FileLens is light-only as
                // of v1.0. Old backups that include it import cleanly.
                if (s.has("showHidden")) {
                    settingsRepository.setShowHidden(s.getBoolean("showHidden")); imported++
                }
                if (s.has("hideEmptyFolders")) {
                    settingsRepository.setHideEmptyFolders(s.getBoolean("hideEmptyFolders")); imported++
                }
                s.optString("sizeUnits").takeIf { it.isNotEmpty() }?.let {
                    settingsRepository.setSizeUnits(SizeUnits.valueOf(it)); imported++
                }
                val field = s.optString("defaultSortField").takeIf { it.isNotEmpty() }
                val dir = s.optString("defaultSortDirection").takeIf { it.isNotEmpty() }
                if (field != null && dir != null) {
                    settingsRepository.setDefaultSort(SortField.valueOf(field), SortDirection.valueOf(dir))
                    imported++
                }
                s.optString("defaultPanelMode").takeIf { it.isNotEmpty() }?.let {
                    settingsRepository.setDefaultPanelMode(PanelMode.valueOf(it)); imported++
                }
            }
        }

        root.optJSONArray("bookmarks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val bm = arr.optJSONObject(i) ?: continue
                val path = bm.optString("path"); val name = bm.optString("displayName")
                if (path.isNotEmpty()) {
                    runCatching { bookmarksRepository.add(path, name.ifEmpty { path.substringAfterLast('/') }) }
                    imported++
                }
            }
        }

        root.optJSONArray("recentFiles")?.let { arr ->
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                val path = f.optString("path"); val name = f.optString("displayName")
                if (path.isNotEmpty()) {
                    runCatching { recentsRepository.recordFileOpen(path, name.ifEmpty { path.substringAfterLast('/') }) }
                    imported++
                }
            }
        }

        root.optJSONArray("frequentFolders")?.let { arr ->
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                val path = f.optString("path"); val name = f.optString("displayName")
                if (path.isNotEmpty()) {
                    runCatching { recentsRepository.recordFolderVisit(path, name.ifEmpty { path.substringAfterLast('/') }) }
                    imported++
                }
            }
        }

        return ImportResult(success = true, message = "Restored $imported entries")
    }

    data class ImportResult(val success: Boolean, val message: String)

    private companion object {
        const val VERSION = 1
    }
}
