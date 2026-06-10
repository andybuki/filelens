package com.filelens.commander.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.filelens.commander.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppManagerUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun listApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        val installedApps = try {
            pm.getInstalledApplications(flags)
        } catch (_: Exception) {
            emptyList()
        }

        installedApps.mapNotNull { info ->
            try {
                val pkgInfo = pm.getPackageInfo(info.packageName, 0)
                val apkFile = info.sourceDir?.let { File(it) }
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()
                }
                val installer = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        pm.getInstallSourceInfo(info.packageName).installingPackageName
                    } else {
                        @Suppress("DEPRECATION") pm.getInstallerPackageName(info.packageName)
                    }
                } catch (_: Exception) { null }

                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdated = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    versionName = pkgInfo.versionName,
                    versionCode = versionCode,
                    isSystem = isSystem,
                    isUpdatedSystemApp = isUpdated,
                    sourceApkPath = info.sourceDir ?: "",
                    installerPackageName = installer,
                    firstInstallTime = pkgInfo.firstInstallTime,
                    lastUpdateTime = pkgInfo.lastUpdateTime,
                    apkSize = apkFile?.takeIf { it.exists() }?.length() ?: 0L,
                    targetSdk = info.targetSdkVersion,
                    minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        info.minSdkVersion
                    } else 0
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    /**
     * Copies the APK for [app] into the user's Downloads folder so it can be
     * shared, archived, or inspected outside the FileLens cache. Returns the
     * destination file, or null on failure.
     */
    suspend fun extractApk(app: InstalledApp): File? = withContext(Dispatchers.IO) {
        val source = File(app.sourceApkPath)
        if (!source.exists() || !source.canRead()) return@withContext null
        val downloads = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val targetDir = File(downloads, "FileLens")
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            // Fall back to app-private cache if the public folder isn't writable.
            return@withContext extractToCache(source, app)
        }
        val target = uniqueChild(targetDir, "${sanitize(app.label)}_${app.versionName ?: app.versionCode}.apk")
        copy(source, target) ?: extractToCache(source, app)
    }

    /**
     * Copies the APK into the app's external cache so it can be shared without
     * needing write access to user storage. Used as the fallback path for
     * [extractApk] when Downloads isn't writable.
     */
    suspend fun extractApkForShare(app: InstalledApp): File? = withContext(Dispatchers.IO) {
        val source = File(app.sourceApkPath)
        if (!source.exists() || !source.canRead()) return@withContext null
        extractToCache(source, app)
    }

    private fun extractToCache(source: File, app: InstalledApp): File? {
        val cacheDir = File(context.cacheDir, "apk_extract").apply { if (!exists()) mkdirs() }
        val target = uniqueChild(cacheDir, "${sanitize(app.label)}_${app.versionName ?: app.versionCode}.apk")
        return copy(source, target)
    }

    private fun copy(source: File, target: File): File? {
        return try {
            source.inputStream().use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            target
        } catch (_: Exception) {
            try { if (target.exists()) target.delete() } catch (_: Exception) {}
            null
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60).ifBlank { "app" }

    private fun uniqueChild(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 1
        while (true) {
            val next = File(dir, "$base ($n)$ext")
            if (!next.exists()) return next
            n++
        }
    }
}
