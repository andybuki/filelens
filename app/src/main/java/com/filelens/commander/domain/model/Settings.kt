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

data class AppSettings(
    val showHidden: Boolean = false,
    val hideEmptyFolders: Boolean = false,
    val sizeUnits: SizeUnits = SizeUnits.BINARY,
    val defaultSortField: SortField = SortField.NAME,
    val defaultSortDirection: SortDirection = SortDirection.ASCENDING,
    val defaultPanelMode: PanelMode = PanelMode.INSPECTOR
)
