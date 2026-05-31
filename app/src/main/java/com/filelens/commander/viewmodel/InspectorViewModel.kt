package com.filelens.commander.viewmodel

import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.detector.MagicBytesDetector
import com.filelens.commander.detector.DetectionResult
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.InspectorInfo
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.inspector.ApkInspector
import com.filelens.commander.inspector.BinaryInspector
import com.filelens.commander.inspector.ImageInspector
import com.filelens.commander.inspector.JsonInspector
import com.filelens.commander.inspector.OfficeInspector
import com.filelens.commander.inspector.TextInspector
import com.filelens.commander.inspector.XmlInspector
import com.filelens.commander.inspector.ZipInspector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class InspectorState(
    val selectedFile: FileItem? = null,
    val info: InspectorInfo? = null,
    val result: InspectorResult? = null,
    val detection: DetectionResult? = null,
    val isAnalyzing: Boolean = false
)

@HiltViewModel
class InspectorViewModel @Inject constructor(
    private val magicBytesDetector: MagicBytesDetector,
    private val imageInspector: ImageInspector,
    private val textInspector: TextInspector,
    private val jsonInspector: JsonInspector,
    private val xmlInspector: XmlInspector,
    private val zipInspector: ZipInspector,
    private val apkInspector: ApkInspector,
    private val officeInspector: OfficeInspector,
    private val binaryInspector: BinaryInspector
) : ViewModel() {

    private val _state = MutableStateFlow(InspectorState())
    val state: StateFlow<InspectorState> = _state.asStateFlow()

    fun selectFile(item: FileItem) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(item.extension)

        val info = InspectorInfo(
            name = item.name,
            path = item.path,
            category = item.fileCategory,
            extension = item.extension,
            size = item.size,
            lastModified = item.lastModified,
            isReadable = item.isReadable,
            mimeType = mimeType
        )

        _state.value = InspectorState(
            selectedFile = item,
            info = info,
            isAnalyzing = true
        )

        viewModelScope.launch {
            val file = File(item.path)
            if (!file.exists() || !file.canRead()) {
                _state.value = _state.value.copy(isAnalyzing = false)
                return@launch
            }

            val detection = magicBytesDetector.detect(file)

            val effectiveCategory = if (detection.confidence > 0.5f) {
                detection.category
            } else {
                item.fileCategory
            }

            val updatedInfo = info.copy(
                category = effectiveCategory,
                mimeType = detection.mimeType ?: mimeType
            )

            val result = runInspector(file, effectiveCategory, item.extension)

            val overviewWithDetection = buildMap {
                put("Detected type", detection.detectedType)
                if (detection.extensionMismatch) {
                    put("Extension mismatch", "Extension .${detection.declaredExtension} doesn't match detected type ${detection.detectedType}")
                }
                putAll(result.overview)
            }

            val risksWithDetection = buildList {
                if (detection.extensionMismatch) {
                    add(com.filelens.commander.domain.model.RiskItem(
                        severity = com.filelens.commander.domain.model.RiskSeverity.WARNING,
                        title = "File extension mismatch",
                        description = "Extension is .${detection.declaredExtension} but content is ${detection.detectedType}. The file may have been renamed to hide its true type."
                    ))
                }
                addAll(result.risks)
            }

            val finalResult = result.copy(
                overview = overviewWithDetection,
                risks = risksWithDetection
            )

            _state.value = InspectorState(
                selectedFile = item,
                info = updatedInfo,
                result = finalResult,
                detection = detection,
                isAnalyzing = false
            )
        }
    }

    fun clearSelection() {
        _state.value = InspectorState()
    }

    private suspend fun runInspector(file: File, category: FileCategory, extension: String): InspectorResult {
        return try {
            when (category) {
                FileCategory.IMAGE -> imageInspector.inspect(file)
                FileCategory.ARCHIVE -> zipInspector.inspect(file)
                FileCategory.APK -> apkInspector.inspect(file)
                FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> {
                    if (extension.lowercase() in setOf("docx", "xlsx", "pptx")) {
                        officeInspector.inspect(file)
                    } else {
                        textInspector.inspect(file)
                    }
                }
                FileCategory.TEXT -> {
                    when (extension.lowercase()) {
                        "json" -> jsonInspector.inspect(file)
                        "xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist" -> xmlInspector.inspect(file)
                        else -> textInspector.inspect(file)
                    }
                }
                FileCategory.CODE -> textInspector.inspect(file)
                FileCategory.PDF -> InspectorResult(
                    overview = mapOf("Type" to "PDF document"),
                    metadata = mapOf("Note" to "Full PDF inspection coming soon")
                )
                FileCategory.AUDIO, FileCategory.VIDEO -> InspectorResult(
                    overview = mapOf("Type" to category.name.lowercase().replaceFirstChar { it.uppercase() }),
                    metadata = mapOf("Note" to "Media inspection coming soon")
                )
                else -> binaryInspector.inspect(file)
            }
        } catch (e: Exception) {
            InspectorResult(
                overview = mapOf("Error" to (e.message ?: "Analysis failed"))
            )
        }
    }
}
