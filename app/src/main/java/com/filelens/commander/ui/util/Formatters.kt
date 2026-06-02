package com.filelens.commander.ui.util

import androidx.compose.runtime.compositionLocalOf
import com.filelens.commander.domain.model.SizeUnits

val LocalSizeUnits = compositionLocalOf { SizeUnits.BINARY }

fun formatFileSize(bytes: Long, units: SizeUnits = SizeUnits.BINARY): String {
    if (bytes < 0) return "—"
    val base = if (units == SizeUnits.BINARY) 1024.0 else 1000.0
    val suffixes = if (units == SizeUnits.BINARY) {
        listOf("B", "KiB", "MiB", "GiB", "TiB")
    } else {
        listOf("B", "KB", "MB", "GB", "TB")
    }
    if (bytes < base) return "$bytes ${suffixes[0]}"
    var value = bytes.toDouble()
    var unitIdx = 0
    while (value >= base && unitIdx < suffixes.lastIndex) {
        value /= base
        unitIdx++
    }
    return "%.1f %s".format(value, suffixes[unitIdx])
}
