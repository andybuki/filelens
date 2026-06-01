package com.filelens.commander.domain.model

object Risks {

    fun extensionMismatch(declaredExtension: String, detectedType: String): RiskItem = RiskItem(
        id = "extension-mismatch",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.INTEGRITY,
        title = "File extension mismatch",
        description = "Extension is .$declaredExtension but content is $detectedType.",
        whyItMatters = "A file renamed to hide its true type is a common trick used to bypass filters or trick users into opening unexpected content.",
        recommendedAction = "Confirm the source before opening. Use Open As… to inspect the file as its real type instead of trusting the extension.",
        evidence = mapOf(
            "Declared extension" to ".$declaredExtension",
            "Detected type" to detectedType
        ),
        detector = "MagicBytesDetector",
        learnMoreKey = "extension-mismatch"
    )

    fun exifGps(latitude: Double, longitude: Double): RiskItem = RiskItem(
        id = "exif-gps",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.PRIVACY,
        title = "Photo contains GPS location",
        description = "Coordinates ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)} are embedded in the image.",
        whyItMatters = "GPS coordinates can reveal where the photo was taken — your home, workplace, or daily routine — to anyone you share the file with.",
        recommendedAction = "Use Share cleaned copy to strip location and personal metadata before sharing.",
        evidence = mapOf(
            "Latitude" to "%.5f".format(latitude),
            "Longitude" to "%.5f".format(longitude),
            "Source" to "EXIF GPSInfo"
        ),
        detector = "ImageInspector",
        learnMoreKey = "exif-gps"
    )

    fun exifAuthor(artist: String): RiskItem = RiskItem(
        id = "exif-author",
        severity = RiskSeverity.INFO,
        category = RiskCategory.PRIVACY,
        title = "Photo contains author metadata",
        description = "Artist tag: $artist",
        whyItMatters = "Author and copyright tags can identify who took the photo or which device it came from.",
        recommendedAction = "Use Share cleaned copy to remove identifying metadata before sharing.",
        evidence = mapOf("Artist" to artist),
        detector = "ImageInspector",
        learnMoreKey = "exif-author"
    )

    fun pdfJavaScript(): RiskItem = RiskItem(
        id = "pdf-javascript",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "PDF contains JavaScript",
        description = "This PDF embeds JavaScript code that may run when the document opens.",
        whyItMatters = "JavaScript inside PDFs has been used to exploit reader vulnerabilities, exfiltrate data, or display deceptive content.",
        recommendedAction = "Preview only inside FileLens. Avoid opening in a desktop PDF viewer with scripting enabled.",
        evidence = mapOf("Marker" to "/JavaScript"),
        detector = "PdfInspector",
        learnMoreKey = "pdf-javascript"
    )

    fun pdfLaunchAction(): RiskItem = RiskItem(
        id = "pdf-launch-action",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "PDF contains launch / open actions",
        description = "This PDF defines actions that trigger when it is opened.",
        whyItMatters = "Auto-launch actions can open URLs, run scripts, or attempt to execute attached files without further user confirmation.",
        recommendedAction = "Do not open with a viewer that honours auto-actions. Inspect inside FileLens first.",
        evidence = mapOf("Marker" to "/OpenAction or /AA"),
        detector = "PdfInspector",
        learnMoreKey = "pdf-launch-action"
    )

    fun pdfEmbeddedFile(): RiskItem = RiskItem(
        id = "pdf-embedded-file",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "PDF contains embedded files",
        description = "Files are attached inside this PDF.",
        whyItMatters = "Embedded files can carry executables, scripts, or sensitive documents hidden from a casual reader.",
        recommendedAction = "Treat the PDF as an archive. Inspect attachments individually before extracting.",
        evidence = mapOf("Marker" to "/EmbeddedFile"),
        detector = "PdfInspector",
        learnMoreKey = "pdf-embedded-file"
    )

    fun pdfEncrypted(): RiskItem = RiskItem(
        id = "pdf-encrypted",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.ENCRYPTION,
        title = "PDF is encrypted",
        description = "The PDF requires a password or has restrictions applied.",
        whyItMatters = "Encrypted PDFs cannot be fully inspected for hidden content until decrypted.",
        recommendedAction = "If you trust the sender, open with the password in a viewer you control.",
        detector = "PdfInspector",
        learnMoreKey = "pdf-encrypted"
    )

    fun pdfAuthor(author: String): RiskItem = RiskItem(
        id = "pdf-author",
        severity = RiskSeverity.INFO,
        category = RiskCategory.PRIVACY,
        title = "PDF contains author metadata",
        description = "Author: $author",
        whyItMatters = "Author and producer fields can identify the original creator of a document.",
        recommendedAction = "Use Share cleaned copy to strip author metadata before sharing.",
        evidence = mapOf("Author" to author),
        detector = "PdfInspector",
        learnMoreKey = "pdf-metadata"
    )

    fun officeMacros(): RiskItem = RiskItem(
        id = "office-macros",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "Document contains macros",
        description = "VBA macros are embedded in this Office document.",
        whyItMatters = "Macros are the most common delivery mechanism for Office-based malware. They can run code, download files, or modify your system when the document opens.",
        recommendedAction = "Do not enable macros. Inspect the file in protected view and confirm the source before trusting it.",
        evidence = mapOf("Marker" to "vbaProject.bin"),
        detector = "OfficeInspector",
        learnMoreKey = "office-macros"
    )

    fun officeTrackedChanges(): RiskItem = RiskItem(
        id = "office-tracked-changes",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.PRIVACY,
        title = "Document contains tracked changes",
        description = "Inserted or deleted content from previous edits is still inside the file.",
        whyItMatters = "Tracked changes can reveal deleted text, previous reviewers, or sensitive edits that were never meant to be shared.",
        recommendedAction = "Accept or reject all changes in Word before sharing, or use Share cleaned copy.",
        evidence = mapOf("Marker" to "<w:ins> / <w:del>"),
        detector = "OfficeInspector",
        learnMoreKey = "office-tracked-changes"
    )

    fun officeHiddenSheets(sheets: List<String>): RiskItem = RiskItem(
        id = "office-hidden-sheets",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.PRIVACY,
        title = "Hidden sheets",
        description = "Contains ${sheets.size} hidden sheet(s): ${sheets.joinToString(", ")}",
        whyItMatters = "Hidden sheets often hold raw data, formulas, or notes that the author did not intend to publish but did not delete.",
        recommendedAction = "Unhide and review every hidden sheet before sharing the workbook.",
        evidence = mapOf("Hidden sheets" to sheets.joinToString(", ")),
        detector = "OfficeInspector",
        learnMoreKey = "office-hidden-sheets"
    )

    fun officeExternalLinks(): RiskItem = RiskItem(
        id = "office-external-links",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "Document references external sources",
        description = "This document links to external workbooks, images, or data sources.",
        whyItMatters = "External links can leak your IP address when the document opens, or pull updated content from a server you do not control.",
        recommendedAction = "Open in a viewer that blocks external content, or inspect and remove links before forwarding.",
        detector = "OfficeInspector",
        learnMoreKey = "office-external-links"
    )

    fun officeEmbeddedObjects(): RiskItem = RiskItem(
        id = "office-embedded-objects",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.ACTIVE_CONTENT,
        title = "Embedded objects",
        description = "This document contains embedded OLE objects.",
        whyItMatters = "OLE objects can include scripts, executables, or other documents hidden inside the main file.",
        recommendedAction = "Inspect the structure tab to see what is embedded. Do not double-click embedded items.",
        detector = "OfficeInspector",
        learnMoreKey = "office-embedded-objects"
    )

    fun officeComments(): RiskItem = RiskItem(
        id = "office-comments",
        severity = RiskSeverity.INFO,
        category = RiskCategory.PRIVACY,
        title = "Document contains comments",
        description = "This file has comments authored alongside its content.",
        whyItMatters = "Comments can contain review discussion, internal references, or names of reviewers that may be sensitive.",
        recommendedAction = "Review and remove comments before sharing externally.",
        detector = "OfficeInspector",
        learnMoreKey = "office-comments"
    )

    fun officeSpeakerNotes(): RiskItem = RiskItem(
        id = "office-speaker-notes",
        severity = RiskSeverity.INFO,
        category = RiskCategory.PRIVACY,
        title = "Presentation contains speaker notes",
        description = "Speaker notes are attached to one or more slides.",
        whyItMatters = "Speaker notes are often invisible during presenting but ship with the file and may include private context.",
        recommendedAction = "Remove speaker notes before sending the deck to external recipients.",
        detector = "OfficeInspector",
        learnMoreKey = "office-speaker-notes"
    )

    fun zipBomb(ratio: Long): RiskItem = RiskItem(
        id = "zip-bomb",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.SECURITY,
        title = "Possible zip bomb",
        description = "Compressed contents would expand to roughly ${ratio}× the archive size.",
        whyItMatters = "Extreme compression ratios are characteristic of zip bombs designed to exhaust disk space or memory when extracted.",
        recommendedAction = "Do not extract. Browse contents inside FileLens to inspect without unpacking.",
        evidence = mapOf("Expansion ratio" to "${ratio}x"),
        detector = "ZipInspector",
        learnMoreKey = "zip-bomb"
    )

    fun zipExecutables(count: Int): RiskItem = RiskItem(
        id = "zip-executables",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.SECURITY,
        title = "Archive contains executables",
        description = "$count executable file(s) detected inside the archive.",
        whyItMatters = "Archives are often used to ship executables past email filters or to obscure them from a casual file listing.",
        recommendedAction = "Inspect each executable individually before extracting. Confirm the source.",
        evidence = mapOf("Executable count" to count.toString()),
        detector = "ZipInspector",
        learnMoreKey = "zip-executables"
    )

    fun zipNested(): RiskItem = RiskItem(
        id = "zip-nested",
        severity = RiskSeverity.INFO,
        category = RiskCategory.SECURITY,
        title = "Nested archives",
        description = "This archive contains other archives.",
        whyItMatters = "Nested archives can hide content from scanners and increase the chance of unexpected extraction depth.",
        recommendedAction = "Browse inner archives in place rather than extracting.",
        detector = "ZipInspector",
        learnMoreKey = "zip-nested"
    )

    fun zipEncrypted(): RiskItem = RiskItem(
        id = "zip-encrypted",
        severity = RiskSeverity.INFO,
        category = RiskCategory.ENCRYPTION,
        title = "Archive is encrypted",
        description = "One or more entries in the archive are password protected.",
        whyItMatters = "Encrypted archives cannot be inspected until decrypted. Attackers sometimes use this to hide malicious content from scanners.",
        recommendedAction = "Only enter the password if you trust the sender.",
        detector = "ZipInspector",
        learnMoreKey = "zip-encrypted"
    )

    fun apkDebugBuild(): RiskItem = RiskItem(
        id = "apk-debug-build",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.SECURITY,
        title = "APK is a debug build",
        description = "The application is marked debuggable.",
        whyItMatters = "Debug builds disable security checks and let any process on the device attach a debugger to extract data or modify behaviour.",
        recommendedAction = "Avoid installing debug builds from untrusted sources.",
        evidence = mapOf("Flag" to "FLAG_DEBUGGABLE"),
        detector = "ApkInspector",
        learnMoreKey = "apk-debug"
    )

    fun apkBackupFlag(): RiskItem = RiskItem(
        id = "apk-backup-flag",
        severity = RiskSeverity.INFO,
        category = RiskCategory.PRIVACY,
        title = "App allows backup",
        description = "The app allows its data to be backed up by adb or cloud backup.",
        whyItMatters = "Apps with backup enabled can have their private data extracted via adb, even on non-rooted devices.",
        recommendedAction = "Acceptable for many apps. Worth noting if the app holds sensitive credentials.",
        evidence = mapOf("Flag" to "FLAG_ALLOW_BACKUP"),
        detector = "ApkInspector",
        learnMoreKey = "apk-backup"
    )

    fun apkDangerousPermissions(count: Int, names: List<String>): RiskItem = RiskItem(
        id = "apk-dangerous-permissions",
        severity = RiskSeverity.WARNING,
        category = RiskCategory.PRIVACY,
        title = "Requests $count sensitive permission${if (count > 1) "s" else ""}",
        description = names.joinToString(", "),
        whyItMatters = "Permissions like CAMERA, LOCATION, SMS, or CONTACTS grant access to personal data. Apps that request more than they need are a privacy red flag.",
        recommendedAction = "Compare the requested permissions to what the app actually does. Decline if the scope feels wrong.",
        evidence = mapOf("Permissions" to names.joinToString(", ")),
        detector = "ApkInspector",
        learnMoreKey = "apk-permissions"
    )

    fun apkMultidex(): RiskItem = RiskItem(
        id = "apk-multidex",
        severity = RiskSeverity.INFO,
        category = RiskCategory.INTEGRITY,
        title = "Multidex required",
        description = "App exceeds the 65,536 method limit and ships multiple DEX files.",
        whyItMatters = "Multidex itself is normal for large apps but can also hide additional code in secondary DEX files that scanners overlook.",
        recommendedAction = "Inspect each DEX file if you are doing a deep review.",
        detector = "ApkInspector"
    )

    fun secretsFound(count: Int, kinds: List<String>): RiskItem = RiskItem(
        id = "secrets-found",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.SECURITY,
        title = "Possible secrets in file",
        description = "$count potential secret${if (count > 1) "s" else ""} matched: ${kinds.joinToString(", ")}",
        whyItMatters = "API keys, tokens, and passwords committed into shared files often lead to account compromise within hours of disclosure.",
        recommendedAction = "Rotate any leaked credential immediately. Remove the secret from the file before sharing.",
        evidence = mapOf("Matches" to count.toString(), "Kinds" to kinds.joinToString(", ")),
        detector = "SecretScanner",
        learnMoreKey = "secrets"
    )

    fun xxeRisk(): RiskItem = RiskItem(
        id = "xml-xxe",
        severity = RiskSeverity.DANGER,
        category = RiskCategory.SECURITY,
        title = "Possible XML external entity (XXE)",
        description = "The XML declares a DOCTYPE with entity definitions.",
        whyItMatters = "External entities in XML can be abused to read local files or trigger network requests when parsed by a permissive parser.",
        recommendedAction = "Do not parse with default XML libraries. Inspect the entities inside FileLens.",
        evidence = mapOf("Marker" to "<!DOCTYPE … <!ENTITY"),
        detector = "XmlInspector",
        learnMoreKey = "xxe"
    )

    fun sqliteWal(): RiskItem = RiskItem(
        id = "sqlite-wal",
        severity = RiskSeverity.INFO,
        category = RiskCategory.INTEGRITY,
        title = "SQLite WAL file present",
        description = "A write-ahead log sidecar exists for this database.",
        whyItMatters = "Without the WAL file, the database view may be stale. Copying only the .db file can lose recent writes.",
        recommendedAction = "Copy the -wal and -shm files alongside the database when transferring it.",
        detector = "SqliteInspector",
        learnMoreKey = "sqlite-wal"
    )

    fun binaryHighEntropy(entropy: Double): RiskItem = RiskItem(
        id = "binary-high-entropy",
        severity = RiskSeverity.INFO,
        category = RiskCategory.SECURITY,
        title = "High-entropy binary",
        description = "Shannon entropy %.2f bits/byte suggests encrypted or compressed content.".format(entropy),
        whyItMatters = "Packed or encrypted payloads are common in obfuscated malware, but also in legitimate compressed assets.",
        recommendedAction = "Cross-reference with the file type. Treat as suspicious only if the type does not normally compress its contents.",
        evidence = mapOf("Entropy" to "%.2f bits/byte".format(entropy)),
        detector = "BinaryInspector"
    )
}

object RiskLearnMore {
    private val entries = mapOf(
        "extension-mismatch" to "File types are recognised by their magic bytes (the first few bytes of the file), not just by their extension. When the two disagree, the extension may have been changed to disguise the file's real nature — for example a .jpg that is actually a Windows executable.",
        "exif-gps" to "Phone cameras embed GPS coordinates inside the JPEG/HEIC file by default. The location stays inside the file as it is forwarded, uploaded, or backed up — even after the photo is renamed or cropped.",
        "exif-author" to "EXIF tags such as Artist, Copyright, and Software identify the creator and the tool used. Removing them before sharing avoids leaking personal or device-identifying information.",
        "pdf-javascript" to "PDF supports embedded JavaScript that runs inside the reader. Historically this has been abused for phishing dialogs, data exfiltration, and reader exploits.",
        "pdf-launch-action" to "PDFs can declare actions that fire automatically when the document opens, including opening URLs, launching attachments, or running JavaScript.",
        "pdf-embedded-file" to "PDFs can carry arbitrary files as attachments. The host PDF acts as a transport for content that would otherwise be blocked by email filters.",
        "pdf-encrypted" to "PDF encryption uses RC4 or AES, depending on the version. Inspection of encrypted content is limited until the file is unlocked.",
        "pdf-metadata" to "PDF metadata stores Author, Producer, Creator, and timestamps that often identify the originating document, machine, or user.",
        "office-macros" to "Microsoft Office VBA macros are full-featured scripts that run inside Word, Excel, and PowerPoint. They are the single most common delivery vehicle for Office malware.",
        "office-tracked-changes" to "Tracked changes record every inserted and deleted edit, including the reviewer's name. Many leaked documents have exposed redacted information through unaccepted changes.",
        "office-hidden-sheets" to "Excel sheets can be hidden (un-hideable from the UI) or very hidden (only un-hideable through VBA). Both states preserve the underlying data inside the file.",
        "office-external-links" to "Office documents can reference external workbooks, images, and data feeds. Opening the document can trigger network requests that reveal your IP address.",
        "office-embedded-objects" to "OLE objects let documents embed other documents, spreadsheets, or executables as packages that can be extracted by double-clicking.",
        "office-comments" to "Document comments preserve review discussion and the names of reviewers, even after the surrounding text changes.",
        "office-speaker-notes" to "PowerPoint speaker notes are stored alongside the slides and travel with the file even when the presenter view is not used.",
        "zip-bomb" to "A zip bomb is a small archive whose decompressed size is many gigabytes or terabytes. Naive extraction can exhaust memory or disk space.",
        "zip-executables" to "Archives are commonly used to bypass executable-file restrictions in email and chat applications.",
        "zip-nested" to "Nested archives obscure their contents from scanners and increase the chance of unexpected extraction depth.",
        "zip-encrypted" to "Encrypted archive entries hide their content until the password is supplied. Attackers sometimes use encryption purely to defeat scanning.",
        "apk-debug" to "Debug builds set the FLAG_DEBUGGABLE flag in the manifest. Any local process can attach a debugger and inspect the app's memory, which is unsafe for production data.",
        "apk-backup" to "Apps with android:allowBackup=\"true\" let their data be exported by adb backup. The default in modern Android is false, so this flag is worth noting.",
        "apk-permissions" to "Android permissions are declared in the manifest and shown to the user at install or first use. Apps requesting permissions outside their stated purpose are a privacy red flag.",
        "secrets" to "Secrets such as API keys, OAuth tokens, and database URLs are routinely found in committed source, log files, and configuration backups. Once leaked, automated bots scrape them within minutes.",
        "xxe" to "XML External Entity attacks abuse DOCTYPE entity definitions to make XML parsers fetch local files or external URLs at parse time.",
        "sqlite-wal" to "SQLite's write-ahead log (.wal) and shared memory (.shm) files hold pending writes. Without them the .db file may not reflect recent transactions."
    )

    operator fun get(key: String?): String? = key?.let { entries[it] }
}
