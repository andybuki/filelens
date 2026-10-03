package com.filelens.commander.domain.model

enum class SizeUnits {
    BINARY,  // 1 KiB = 1024 B
    DECIMAL; // 1 KB = 1000 B

    val displayName: String
        get() = when (this) {
            BINARY -> "Binary (KiB, MiB)"
            DECIMAL -> "Decimal (KB, MB)"
        }
}

enum class PanelMode {
    INSPECTOR,
    DUAL_FILES;

    val displayName: String
        get() = when (this) {
            INSPECTOR -> "Inspector"
            DUAL_FILES -> "Dual files"
        }
}

/** Where the app lands after launch. */
enum class StartScreen {
    HOME,
    FILES
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    val displayName: String
        get() = when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System"
        }
}

data class AppSettings(
    val showHidden: Boolean = false,
    val hideEmptyFolders: Boolean = false,
    val sizeUnits: SizeUnits = SizeUnits.BINARY,
    val defaultSortField: SortField = SortField.NAME,
    val defaultSortDirection: SortDirection = SortDirection.ASCENDING,
    val defaultPanelMode: PanelMode = PanelMode.INSPECTOR,
    /**
     * Max size, in MB, of a remote file FileLens will download to preview.
     * Remote previews fetch the whole file first, so this caps the wait.
     */
    val remotePreviewLimitMb: Int = DEFAULT_REMOTE_PREVIEW_LIMIT_MB,
    /**
     * Separate, usually larger cap for archives (zip, apk, …). Peeking inside
     * an archive needs the whole file, but the listing is often worth a longer
     * download than a plain media file would be.
     */
    val remoteArchivePreviewLimitMb: Int = DEFAULT_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB,
    /**
     * BCP-47 language tag the user picked as an override, or null to follow
     * the system language. Persisted across launches and applied via
     * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales].
     */
    val languageTag: String? = null,
    /** Whether the app opens on the Home tab or jumps straight to Files. */
    val startScreen: StartScreen = StartScreen.HOME,
    /** Show the confirm sheet before deleting. Off skips straight to trash. */
    val askBeforeDelete: Boolean = true,
    /**
     * Extra search roots (absolute paths). Empty means "scan the whole
     * external storage", which is the default for new installs.
     */
    val searchRoots: Set<String> = emptySet(),
    /**
     * Reserved for opt-in usage reporting. FileLens ships with no telemetry
     * in v1.0; this flag is wired so we can never enable a future ping by
     * surprise — opt-in only.
     */
    val telemetryEnabled: Boolean = false,
    /** Warn when entering /system, /data, /proc, /sys, /vendor, /apex. */
    val warnRootStorage: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.LIGHT
) {
    companion object {
        const val DEFAULT_REMOTE_PREVIEW_LIMIT_MB = 50
        const val DEFAULT_REMOTE_ARCHIVE_PREVIEW_LIMIT_MB = 256

        const val REMOTE_PREVIEW_LIMIT_MIN_MB = 5
        const val REMOTE_PREVIEW_LIMIT_MAX_MB = 200
        const val REMOTE_PREVIEW_LIMIT_STEP_MB = 5

        const val REMOTE_ARCHIVE_PREVIEW_LIMIT_MIN_MB = 32
        const val REMOTE_ARCHIVE_PREVIEW_LIMIT_MAX_MB = 1024
        const val REMOTE_ARCHIVE_PREVIEW_LIMIT_STEP_MB = 32
    }
}
