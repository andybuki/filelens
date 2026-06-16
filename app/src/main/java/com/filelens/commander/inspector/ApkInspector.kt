package com.filelens.commander.inspector

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.Risks
import com.filelens.commander.domain.model.StructureNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
        when (file.extension.lowercase()) {
            "ipa" -> return@withContext inspectIpa(file)
            "xapk" -> return@withContext inspectXapk(file)
        }

        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()
        var iconPreview: PreviewContent.ImageContent? = null

        val pm = context.packageManager
        val baseFlags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA
        val signingFlag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        val packageInfo = try {
            pm.getPackageArchiveInfo(file.absolutePath, baseFlags or signingFlag)
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
                try {
                    appInfo.sourceDir = file.absolutePath
                    appInfo.publicSourceDir = file.absolutePath
                    val drawable = pm.getApplicationIcon(appInfo)
                    val intrinsicW = drawable.intrinsicWidth
                    val intrinsicH = drawable.intrinsicHeight
                    val width = if (intrinsicW > 0) intrinsicW else 192
                    val height = if (intrinsicH > 0) intrinsicH else 192
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    val pkgName = packageInfo.packageName ?: "unknown"
                    val iconFile = File(context.cacheDir, "apk_icon_$pkgName.png")
                    FileOutputStream(iconFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    bitmap.recycle()
                    iconPreview = PreviewContent.ImageContent(
                        path = iconFile.absolutePath,
                        width = width,
                        height = height
                    )
                } catch (e: Exception) {
                    overview["Icon error"] = e.message?.take(100) ?: "Could not extract icon"
                }

                appInfo.minSdkVersion.let { overview["Min SDK"] = "$it (Android ${sdkToAndroid(it)})" }
                overview["Target SDK"] = "${appInfo.targetSdkVersion} (Android ${sdkToAndroid(appInfo.targetSdkVersion)})"

                val isDebuggable = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
                overview["Debuggable"] = if (isDebuggable) "Yes" else "No"
                if (isDebuggable) {
                    risks.add(Risks.apkDebugBuild())
                }

                val allowBackup = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0
                overview["Allow backup"] = if (allowBackup) "Yes" else "No"
                if (allowBackup) {
                    risks.add(Risks.apkBackupFlag())
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val cleartext = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0
                    if (cleartext) {
                        overview["Cleartext traffic"] = "Allowed"
                        risks.add(RiskItem(
                            id = "apk-cleartext-traffic",
                            severity = RiskSeverity.WARNING,
                            category = com.filelens.commander.domain.model.RiskCategory.SECURITY,
                            title = "App allows cleartext traffic",
                            description = "Manifest permits HTTP (not HTTPS) connections.",
                            whyItMatters = "Cleartext traffic exposes user data to network observers and is rejected by default on modern Android. Apps that opt in often communicate with legacy or attacker-controlled servers.",
                            recommendedAction = "Treat the app's network activity as untrusted on shared networks.",
                            evidence = mapOf("Flag" to "FLAG_USES_CLEARTEXT_TRAFFIC"),
                            detector = "ApkInspector"
                        ))
                    }
                }
            }

            // Signing certificates
            val certFingerprints = mutableListOf<String>()
            try {
                @Suppress("DEPRECATION")
                val signatures: Array<android.content.pm.Signature>? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.let { si ->
                        if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
                    }
                } else {
                    packageInfo.signatures
                }
                signatures?.forEach { sig ->
                    val sha256 = java.security.MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                    val hex = sha256.joinToString("") { "%02X".format(it) }
                    certFingerprints += hex.chunked(2).joinToString(":")
                }
            } catch (_: Exception) {}
            if (certFingerprints.isNotEmpty()) {
                overview["Signers"] = certFingerprints.size.toString()
                metadata["Signing SHA-256"] = certFingerprints.joinToString("\n") { it }
            }

            val permissions = packageInfo.requestedPermissions ?: emptyArray()
            overview["Permissions"] = permissions.size.toString()

            val dangerous = permissions.filter { it in dangerousPermissions }
            if (dangerous.isNotEmpty()) {
                overview["Dangerous permissions"] = dangerous.size.toString()
                risks.add(Risks.apkDangerousPermissions(
                    count = dangerous.size,
                    names = dangerous.map { it.removePrefix("android.permission.") }
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

            // Parse DEX headers for method/class counts
            var totalMethods = 0L
            var totalClasses = 0L
            for (dexEntry in dexFiles) {
                try {
                    val dexHeader = zipFile.getInputStream(dexEntry).use { stream ->
                        val buf = ByteArray(92)
                        var offset = 0
                        while (offset < buf.size) {
                            val read = stream.read(buf, offset, buf.size - offset)
                            if (read <= 0) break
                            offset += read
                        }
                        if (offset >= 92) buf else null
                    }
                    if (dexHeader != null) {
                        val stringCount = readLittleEndianUInt32(dexHeader, 56)
                        val typeCount = readLittleEndianUInt32(dexHeader, 64)
                        val methodCount = readLittleEndianUInt32(dexHeader, 80)
                        totalMethods += methodCount
                        totalClasses += typeCount
                    }
                } catch (_: Exception) { /* skip unparseable DEX */ }
            }
            if (dexFiles.isNotEmpty()) {
                overview["Total methods"] = "%,d".format(totalMethods)
                overview["Total classes"] = "%,d".format(totalClasses)
                if (totalMethods > 65536) {
                    risks.add(Risks.apkMultidex())
                }
            }

            val nativeLibs = entries.filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
            if (nativeLibs.isNotEmpty()) {
                val abiCounts = nativeLibs
                    .groupingBy { it.name.split("/").getOrElse(1) { "unknown" } }
                    .eachCount()
                overview["Native libraries"] = nativeLibs.size.toString()
                metadata["Native libs by ABI"] = abiCounts.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            }

            // Tracker / library hints from asset and lib filenames (local rules only)
            val trackerHits = mutableSetOf<String>()
            val trackerRules = mapOf(
                "Firebase" to listOf("com/google/firebase/", "google-services"),
                "Google Analytics" to listOf("com/google/analytics/", "google_app_measurement"),
                "Crashlytics" to listOf("com/crashlytics/", "crashlytics-build.properties"),
                "Facebook SDK" to listOf("com/facebook/", "facebook_app_id"),
                "AppsFlyer" to listOf("com/appsflyer/"),
                "Adjust" to listOf("com/adjust/sdk/"),
                "Branch.io" to listOf("io/branch/"),
                "Mixpanel" to listOf("com/mixpanel/"),
                "Segment" to listOf("com/segment/analytics/"),
                "OneSignal" to listOf("com/onesignal/"),
                "Amplitude" to listOf("com/amplitude/"),
                "Bugsnag" to listOf("com/bugsnag/"),
                "Sentry" to listOf("io/sentry/"),
                "Flurry" to listOf("com/flurry/")
            )
            for (entry in entries) {
                if (entry.name.startsWith("META-INF/")) continue
                val nameLower = entry.name.lowercase()
                for ((tracker, markers) in trackerRules) {
                    if (markers.any { nameLower.contains(it.lowercase()) }) {
                        trackerHits += tracker
                    }
                }
            }
            if (trackerHits.isNotEmpty()) {
                overview["Detected SDKs"] = trackerHits.size.toString()
                metadata["SDK / tracker hints"] = trackerHits.joinToString(", ")
                risks.add(RiskItem(
                    id = "apk-trackers",
                    severity = RiskSeverity.INFO,
                    category = com.filelens.commander.domain.model.RiskCategory.PRIVACY,
                    title = "Likely analytics / tracker SDKs",
                    description = trackerHits.joinToString(", "),
                    whyItMatters = "Analytics, crash reporting, and ad SDKs typically send event data off-device. Volume and identity of trackers affect the app's privacy profile.",
                    recommendedAction = "Cross-reference with the app's privacy policy. Heuristic — based on package names only, not behaviour.",
                    evidence = mapOf("Trackers" to trackerHits.joinToString(", ")),
                    detector = "ApkInspector"
                ))
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
                structure = tree,
                previewContent = iconPreview
            )
        } catch (e: Exception) {
            overview["ZIP Error"] = e.message?.take(100) ?: "Failed to read APK"
            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                previewContent = iconPreview
            )
        }
    }

    // IPA = iOS app bundle, a ZIP containing Payload/<AppName>.app/Info.plist.
    // We don't parse binary plists — most modern IPAs ship Info.plist as XML.
    private fun inspectIpa(file: File): InspectorResult {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["Format"] = "iOS app (IPA)"

        try {
            ZipFile(file).use { zipFile ->
                val entries = zipFile.entries().toList()
                val infoEntry = entries.firstOrNull {
                    it.name.startsWith("Payload/") &&
                        it.name.endsWith(".app/Info.plist") &&
                        it.name.count { ch -> ch == '/' } == 2
                }
                if (infoEntry != null) {
                    overview["Bundle"] = infoEntry.name.removePrefix("Payload/").removeSuffix("/Info.plist")
                    val plist = zipFile.getInputStream(infoEntry).bufferedReader().readText()
                    if (plist.startsWith("bplist", ignoreCase = false) || plist.startsWith(" ")) {
                        overview["Info.plist"] = "Binary plist (not parsed)"
                    } else {
                        readPlistString(plist, "CFBundleIdentifier")?.let { overview["Bundle identifier"] = it }
                        readPlistString(plist, "CFBundleName")?.let { overview["Name"] = it }
                        readPlistString(plist, "CFBundleDisplayName")?.let { overview["Display name"] = it }
                        readPlistString(plist, "CFBundleShortVersionString")?.let { overview["Version"] = it }
                        readPlistString(plist, "CFBundleVersion")?.let { overview["Build"] = it }
                        readPlistString(plist, "CFBundleExecutable")?.let { overview["Executable"] = it }
                        readPlistString(plist, "MinimumOSVersion")?.let { overview["Minimum iOS"] = it }
                        readPlistString(plist, "DTPlatformName")?.let { metadata["Platform"] = it }
                        readPlistString(plist, "DTSDKName")?.let { metadata["SDK"] = it }
                    }
                } else {
                    overview["Note"] = "No Info.plist found under Payload/"
                }

                val frameworks = entries.count {
                    it.name.startsWith("Payload/") && it.name.contains("/Frameworks/") && it.name.endsWith(".framework/")
                }
                if (frameworks > 0) overview["Frameworks"] = frameworks.toString()

                val totalFiles = entries.count { !it.isDirectory }
                val totalSize = entries.sumOf { it.size.coerceAtLeast(0) }
                overview["Total files"] = "%,d".format(totalFiles)
                overview["Uncompressed size"] = formatSize(totalSize)

                val tree = buildIpaTree(entries)
                return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
            }
        } catch (e: Exception) {
            overview["Error"] = "Failed to read IPA: ${e.message?.take(100)}"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    // XAPK = ZIP containing manifest.json plus one base APK and split APKs.
    private fun inspectXapk(file: File): InspectorResult {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["Format"] = "Split APK bundle (XAPK)"

        try {
            ZipFile(file).use { zipFile ->
                val entries = zipFile.entries().toList()
                val manifest = zipFile.getEntry("manifest.json")?.let { entry ->
                    zipFile.getInputStream(entry).bufferedReader().readText()
                }
                if (manifest != null) {
                    Regex(""""package_name"\s*:\s*"([^"]+)"""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Package"] = it }
                    Regex(""""name"\s*:\s*"([^"]+)"""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Name"] = it }
                    Regex(""""version_name"\s*:\s*"([^"]+)"""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Version"] = it }
                    Regex(""""version_code"\s*:\s*"?([^",}]+)"?""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Version code"] = it.trim() }
                    Regex(""""min_sdk_version"\s*:\s*"?([^",}]+)"?""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Min SDK"] = it.trim() }
                    Regex(""""target_sdk_version"\s*:\s*"?([^",}]+)"?""").find(manifest)?.groupValues?.get(1)
                        ?.let { overview["Target SDK"] = it.trim() }
                }

                val apks = entries.filter { it.name.endsWith(".apk", ignoreCase = true) }
                overview["APKs inside"] = apks.size.toString()
                val obbs = entries.filter { it.name.endsWith(".obb", ignoreCase = true) }
                if (obbs.isNotEmpty()) overview["OBB files"] = obbs.size.toString()

                val totalFiles = entries.count { !it.isDirectory }
                val totalSize = entries.sumOf { it.size.coerceAtLeast(0) }
                overview["Total files"] = "%,d".format(totalFiles)
                overview["Uncompressed size"] = formatSize(totalSize)

                if (apks.isNotEmpty()) {
                    metadata["APK list"] = apks.take(10).joinToString("\n") {
                        "${it.name} (${formatSize(it.size.coerceAtLeast(0))})"
                    } + if (apks.size > 10) "\n… ${apks.size - 10} more" else ""
                }

                val tree = buildIpaTree(entries)
                return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
            }
        } catch (e: Exception) {
            overview["Error"] = "Failed to read XAPK: ${e.message?.take(100)}"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun readPlistString(xml: String, key: String): String? {
        // Matches <key>NAME</key><string>VALUE</string>; whitespace tolerant.
        val re = Regex("""<key>\Q$key\E</key>\s*<string>([^<]*)</string>""")
        return re.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    // Light-touch tree for IPA / XAPK contents — flat listing grouped under root.
    private fun buildIpaTree(entries: List<java.util.zip.ZipEntry>): StructureNode {
        val files = entries.filter { !it.isDirectory }.take(50)
        val leaves = files.map { entry ->
            StructureNode.Leaf(
                name = entry.name,
                value = formatSize(entry.size.coerceAtLeast(0)),
                size = entry.size
            )
        }
        val children = if (entries.size > 50) {
            leaves + StructureNode.Leaf(name = "… ${entries.size - 50} more")
        } else leaves
        return StructureNode.Branch(name = "Contents", children = children)
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

    private fun readLittleEndianUInt32(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFF) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
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
