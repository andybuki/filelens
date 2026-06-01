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
import com.filelens.commander.domain.usecase.TrustScoreUseCase
import com.filelens.commander.inspector.ApkInspector
import com.filelens.commander.inspector.BinaryInspector
import com.filelens.commander.inspector.ImageInspector
import com.filelens.commander.inspector.JsonInspector
import com.filelens.commander.inspector.FontInspector
import com.filelens.commander.inspector.MediaInspector
import com.filelens.commander.inspector.OfficeInspector
import com.filelens.commander.inspector.PdfInspector
import com.filelens.commander.inspector.SqliteInspector
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
    val isAnalyzing: Boolean = false,
    val ignoredRiskKeys: Set<String> = emptySet()
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
    private val binaryInspector: BinaryInspector,
    private val pdfInspector: PdfInspector,
    private val mediaInspector: MediaInspector,
    private val sqliteInspector: SqliteInspector,
    private val fontInspector: FontInspector,
    private val trustScoreUseCase: TrustScoreUseCase
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

        val carriedIgnored = _state.value.ignoredRiskKeys
        _state.value = InspectorState(
            selectedFile = item,
            info = info,
            isAnalyzing = true,
            ignoredRiskKeys = carriedIgnored
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
                    add(com.filelens.commander.domain.model.Risks.extensionMismatch(
                        detection.declaredExtension,
                        detection.detectedType
                    ))
                }
                addAll(result.risks)
            }

            val withDetection = result.copy(
                overview = overviewWithDetection,
                risks = risksWithDetection
            )
            val trustScore = trustScoreUseCase.compute(withDetection, detection)
            val finalResult = withDetection.copy(trustScore = trustScore)

            _state.value = InspectorState(
                selectedFile = item,
                info = updatedInfo,
                result = finalResult,
                detection = detection,
                isAnalyzing = false,
                ignoredRiskKeys = carriedIgnored
            )
        }
    }

    fun clearSelection() {
        _state.value = InspectorState()
    }

    fun ignoreRisk(filePath: String, riskId: String) {
        if (riskId.isEmpty()) return
        val key = "$filePath::$riskId"
        _state.value = _state.value.copy(
            ignoredRiskKeys = _state.value.ignoredRiskKeys + key
        )
    }

    fun restoreIgnoredRisks() {
        _state.value = _state.value.copy(ignoredRiskKeys = emptySet())
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
                FileCategory.PDF -> pdfInspector.inspect(file)
                FileCategory.AUDIO, FileCategory.VIDEO -> mediaInspector.inspect(file)
                FileCategory.DATABASE -> sqliteInspector.inspect(file)
                FileCategory.FONT -> fontInspector.inspect(file)
                else -> binaryInspector.inspect(file)
            }
        } catch (e: Exception) {
            InspectorResult(
                overview = mapOf("Error" to (e.message ?: "Analysis failed"))
            )
        }
    }
}
