package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.Provenance
import com.filelens.commander.domain.model.ProvenanceConfidence
import com.filelens.commander.domain.model.ProvenanceSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvenanceDetector @Inject constructor() {

    fun detect(path: String): Provenance? {
        val normalized = path.replace('\\', '/')
        val lower = normalized.lowercase()

        for (rule in rules) {
            if (rule.matches(lower)) {
                return Provenance(
                    source = rule.source,
                    displayName = rule.displayName,
                    description = rule.description,
                    confidence = rule.confidence
                )
            }
        }
        return null
    }

    private data class Rule(
        val source: ProvenanceSource,
        val displayName: String,
        val description: String,
        val confidence: ProvenanceConfidence,
        val patterns: List<String>
    ) {
        fun matches(lowerPath: String): Boolean = patterns.any { lowerPath.contains(it) }
    }

    private val rules = listOf(
        Rule(
            ProvenanceSource.SCREENSHOTS,
            "Screenshot",
            "File lives in a Screenshots folder. Likely captured by the device itself.",
            ProvenanceConfidence.HIGH,
            listOf("/pictures/screenshots", "/dcim/screenshots", "/screenshots/")
        ),
        Rule(
            ProvenanceSource.CAMERA,
            "Camera",
            "File lives in DCIM/Camera. Likely captured by the device camera.",
            ProvenanceConfidence.HIGH,
            listOf("/dcim/camera", "/dcim/100andro", "/dcim/100media")
        ),
        Rule(
            ProvenanceSource.WHATSAPP,
            "WhatsApp",
            "File arrived via WhatsApp. Treat metadata and content as supplied by the sender.",
            ProvenanceConfidence.HIGH,
            listOf("/whatsapp/", "/com.whatsapp/", "whatsapp/media/", "whatsapp images", "whatsapp documents", "whatsapp audio", "whatsapp video")
        ),
        Rule(
            ProvenanceSource.TELEGRAM,
            "Telegram",
            "File arrived via Telegram.",
            ProvenanceConfidence.HIGH,
            listOf("/telegram/", "/org.telegram.messenger/", "telegram documents", "telegram images", "telegram audio", "telegram video")
        ),
        Rule(
            ProvenanceSource.SIGNAL,
            "Signal",
            "File arrived via Signal.",
            ProvenanceConfidence.HIGH,
            listOf("/signal/", "/org.thoughtcrime.securesms/")
        ),
        Rule(
            ProvenanceSource.BROWSER,
            "Browser download",
            "File was downloaded by a browser.",
            ProvenanceConfidence.MEDIUM,
            listOf("/chrome/", "/firefox/", "/com.android.chrome/", "/org.mozilla", "/com.brave.browser", "/com.duckduckgo", "/com.opera")
        ),
        Rule(
            ProvenanceSource.BLUETOOTH,
            "Bluetooth",
            "File was received via Bluetooth.",
            ProvenanceConfidence.HIGH,
            listOf("/bluetooth/", "/btopp/")
        ),
        Rule(
            ProvenanceSource.EMAIL,
            "Email attachment",
            "File came from an email client.",
            ProvenanceConfidence.MEDIUM,
            listOf("/gmail/", "/com.google.android.gm/", "/com.microsoft.office.outlook/", "/k9mail/", "/fairemail/", "/protonmail/")
        ),
        Rule(
            ProvenanceSource.CLOUD,
            "Cloud storage",
            "File originates from a cloud storage provider on this device.",
            ProvenanceConfidence.MEDIUM,
            listOf("/google drive", "/com.google.android.apps.docs", "/dropbox", "/com.dropbox", "/onedrive", "/com.microsoft.skydrive", "/megasync", "/nextcloud", "/owncloud")
        ),
        Rule(
            ProvenanceSource.DOWNLOADS,
            "Downloads",
            "File lives in the device Downloads folder.",
            ProvenanceConfidence.MEDIUM,
            listOf("/download/", "/downloads/")
        ),
        Rule(
            ProvenanceSource.APP_PRIVATE,
            "App private storage",
            "File lives inside an app's private data folder.",
            ProvenanceConfidence.MEDIUM,
            listOf("/android/data/", "/android/media/", "/data/data/")
        ),
        Rule(
            ProvenanceSource.SYSTEM,
            "System",
            "File lives in a system directory.",
            ProvenanceConfidence.LOW,
            listOf("/system/", "/vendor/", "/product/")
        ),
        Rule(
            ProvenanceSource.SDCARD,
            "External SD card",
            "File lives on a secondary storage volume.",
            ProvenanceConfidence.LOW,
            listOf("/storage/sdcard1", "/mnt/media_rw/", "/storage/extsdcard")
        )
    )
}
