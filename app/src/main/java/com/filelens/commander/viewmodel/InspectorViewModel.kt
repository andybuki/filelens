package com.filelens.commander.viewmodel

import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.InspectorInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

data class InspectorState(
    val selectedFile: FileItem? = null,
    val info: InspectorInfo? = null
)

@HiltViewModel
class InspectorViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(InspectorState())
    val state: StateFlow<InspectorState> = _state.asStateFlow()

    fun selectFile(item: FileItem) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(item.extension)

        val details = buildDetails(item)

        val info = InspectorInfo(
            name = item.name,
            path = item.path,
            category = item.fileCategory,
            extension = item.extension,
            size = item.size,
            lastModified = item.lastModified,
            isReadable = item.isReadable,
            mimeType = mimeType,
            details = details
        )

        _state.value = InspectorState(selectedFile = item, info = info)
    }

    fun clearSelection() {
        _state.value = InspectorState()
    }

    private fun buildDetails(item: FileItem): Map<String, String> {
        val details = mutableMapOf<String, String>()
        val file = File(item.path)

        if (file.exists()) {
            details["Writable"] = if (file.canWrite()) "Yes" else "No"
            details["Executable"] = if (file.canExecute()) "Yes" else "No"

            if (file.isFile) {
                val parent = file.parentFile
                if (parent != null) {
                    details["Parent folder"] = parent.name
                }
            }
        }

        return details
    }
}
