package com.filelens.commander.inspector

import android.content.Context
import android.content.pm.PackageManager
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkInspector @Inject constructor(
    @ApplicationContext private val context: Context
) : FileInspector {

    override val supportedCategories = setOf(FileCategory.APK)

    private val dangerousPermissions = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.BODY_SENSORS",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR"
    )

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val pm = context.packageManager
        val packageInfo = try {
            pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        } catch (_: Exception) { null }

        if (packageInfo != null) {
            overview["Package"] = packageInfo.packageName ?: "unknown"
            overview["Version name"] = packageInfo.versionName ?: "unknown"
            overview["Version code"] = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            val appInfo = packageInfo.applicationInfo
            if (appInfo != null) {
                appInfo.minSdkVersion.let { overview["Min SDK"] = "$it (Android ${sdkToAndroid(it)})" }
                overview["Target SDK"] = "${appInfo.targetSdkVersion} (Android ${sdkToAndroid(appInfo.targetSdkVersion)})"

                val isDebuggable = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
                overview["Debuggable"] = if (isDebuggable) "Yes" else "No"
                if (isDebuggable) {
                    risks.add(RiskItem(
                        severity = RiskSeverity.WARNING,
                        title = "Debug build",
                        description = "This APK has the debuggable flag enabled, which should not be used in production."
                    ))
                }

                val allowBackup = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0
                overview["Allow backup"] = if (allowBackup) "Yes" else "No"
            }

            val permissions = packageInfo.requestedPermissions ?: emptyArray()
            overview["Permissions"] = permissions.size.toString()

            val dangerous = permissions.filter { it in dangerousPermissions }
            if (dangerous.isNotEmpty()) {
                overview["Dangerous permissions"] = dangerous.size.toString()
                risks.add(RiskItem(
                    severity = RiskSeverity.WARNING,
                    title = "Requests ${dangerous.size} dangerous permission(s)",
                    description = dangerous.joinToString("\n") { "• ${it.removePrefix("android.permission.")}" }
                ))
            }

            val activities = packageInfo.activities?.size ?: 0
            val services = packageInfo.services?.size ?: 0
            val receivers = packageInfo.receivers?.size ?: 0
            val providers = packageInfo.providers?.size ?: 0

            overview["Activities"] = activities.toString()
            overview["Services"] = services.toString()
            overview["Broadcast receivers"] = receivers.toString()
            overview["Content providers"] = providers.toString()

            metadata["All permissions"] = permissions.joinToString("\n") { it.removePrefix("android.permission.") }
        } else {
            overview["Note"] = "Could not parse APK info via PackageManager"
        }

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            val dexFiles = entries.filter { it.name.endsWith(".dex") }
            overview["DEX files"] = dexFiles.size.toString()

            val nativeLibs = entries.filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
            if (nativeLibs.isNotEmpty()) {
                val abis = nativeLibs.map { it.name.split("/").getOrElse(1) { "unknown" } }.distinct()
                overview["Native libraries"] = "${nativeLibs.size} (${abis.joinToString(", ")})"
            }

            val totalFiles = entries.count { !it.isDirectory }
            val totalSize = entries.sumOf { it.size.coerceAtLeast(0) }
            overview["Total files"] = "%,d".format(totalFiles)
            overview["Uncompressed size"] = formatSize(totalSize)

            val tree = buildApkTree(entries)

            zipFile.close()

            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                structure = tree
            )
        } catch (e: Exception) {
            overview["ZIP Error"] = e.message?.take(100) ?: "Failed to read APK"
            InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun buildApkTree(entries: List<java.util.zip.ZipEntry>): StructureNode {
        val categories = mapOf(
            "Code" to entries.filter { it.name.endsWith(".dex") },
            "Resources" to entries.filter { it.name.startsWith("res/") },
            "Assets" to entries.filter { it.name.startsWith("assets/") },
            "Native libs" to entries.filter { it.name.startsWith("lib/") },
            "META-INF" to entries.filter { it.name.startsWith("META-INF/") },
            "Other" to entries.filter { entry ->
                !entry.isDirectory &&
                !entry.name.endsWith(".dex") &&
                !entry.name.startsWith("res/") &&
                !entry.name.startsWith("assets/") &&
                !entry.name.startsWith("lib/") &&
                !entry.name.startsWith("META-INF/") &&
                entry.name != "AndroidManifest.xml" &&
                entry.name != "resources.arsc"
            }
        )

        val children = mutableListOf<StructureNode>()
        children.add(StructureNode.Leaf(name = "AndroidManifest.xml"))
        entries.find { it.name == "resources.arsc" }?.let {
            children.add(StructureNode.Leaf(name = "resources.arsc", value = formatSize(it.size)))
        }

        for ((category, catEntries) in categories) {
            if (catEntries.isEmpty()) continue
            val catChildren = catEntries.take(30).map { entry ->
                StructureNode.Leaf(
                    name = entry.name,
                    value = formatSize(entry.size.coerceAtLeast(0)),
                    size = entry.size
                )
            }
            val display = if (catEntries.size > 30) {
                catChildren + StructureNode.Leaf(name = "... ${catEntries.size - 30} more")
            } else catChildren

            children.add(StructureNode.Branch(
                name = "$category (${catEntries.size})",
                children = display
            ))
        }

        return StructureNode.Branch(name = "APK contents", children = children)
    }

    private fun sdkToAndroid(sdk: Int): String = when (sdk) {
        35 -> "15"
        34 -> "14"
        33 -> "13"
        32 -> "12L"
        31 -> "12"
        30 -> "11"
        29 -> "10"
        28 -> "9"
        27 -> "8.1"
        26 -> "8.0"
        25 -> "7.1"
        24 -> "7.0"
        23 -> "6.0"
        22 -> "5.1"
        21 -> "5.0"
        else -> if (sdk > 35) "$sdk" else "< 5.0"
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 0) return "?"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}
