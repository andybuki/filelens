package com.filelens.commander.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    val displayName: String
        get() = when (this) {
            SYSTEM -> "Match system"
            LIGHT -> "Light"
            DARK -> "Dark"
        }
}

enum class SizeUnits {
    BINARY,  // 1 KiB = 1024 B
    DECIMAL; // 1 KB = 1000 B

    val displayName: String
        get() = when (this) {
            BINARY -> "Binary (KiB, MiB)"
            DECIMAL -> "Decimal (KB, MB)"
        }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showHidden: Boolean = false,
    val sizeUnits: SizeUnits = SizeUnits.BINARY,
    val defaultSortField: SortField = SortField.NAME,
    val defaultSortDirection: SortDirection = SortDirection.ASCENDING
)
