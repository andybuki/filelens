package com.filelens.commander.domain.model

data class InspectorInfo(
    val name: String,
    val path: String,
    val category: FileCategory,
    val extension: String,
    val size: Long?,
    val lastModified: Long?,
    val isReadable: Boolean,
    val mimeType: String? = null,
    val details: Map<String, String> = emptyMap()
)
