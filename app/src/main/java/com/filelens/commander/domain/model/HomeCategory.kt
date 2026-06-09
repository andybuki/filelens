package com.filelens.commander.domain.model

/**
 * Top-level shortcut shown in the Home tab grid. Each category either
 * targets a standard external-storage folder, or — for type/size-based
 * tiles — triggers a synthetic recursive scan across the home root.
 */
enum class HomeCategory {
    IMAGES,
    VIDEO,
    AUDIO,
    DOCUMENTS,
    APKS,
    ARCHIVES,
    DOWNLOADS,
    LARGE_FILES
}
