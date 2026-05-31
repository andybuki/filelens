package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import java.io.File

interface FileInspector {
    val supportedCategories: Set<FileCategory>
    suspend fun inspect(file: File): InspectorResult
}
