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
import com.filelens.commander.domain.usecase.ProvenanceDetector
import com.filelens.commander.domain.usecase.ReportExportUseCase
import com.filelens.commander.domain.usecase.StructuredSearchUseCase
import com.filelens.commander.domain.usecase.TrustScoreUseCase
import com.filelens.commander.inspector.ApkInspector
import com.filelens.commander.inspector.BinaryInspector
import com.filelens.commander.inspector.ConfigInspector
import com.filelens.commander.inspector.EpubInspector
import com.filelens.commander.inspector.ImageInspector
import com.filelens.commander.inspector.JsonInspector
import com.filelens.commander.inspector.LogInspector
import com.filelens.commander.inspector.FontInspector
import com.filelens.commander.inspector.MediaInspector
import com.filelens.commander.inspector.OfficeInspector
import com.filelens.commander.inspector.PdfInspector
import com.filelens.commander.inspector.SqliteInspector
import com.filelens.commander.inspector.TextInspector
import com.filelens.commander.inspector.XmlInspector
import com.filelens.commander.inspector.ZipInspector
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class InspectorState(
    val selectedFile: FileItem? = null,
    val info: InspectorInfo? = null,
    val result: InspectorResult? = null,
    val detection: DetectionResult? = null,
    val isAnalyzing: Boolean = false,
    val ignoredRiskKeys: Set<String> = emptySet(),
    val detectedCategory: FileCategory? = null,
    val forcedCategory: FileCategory? = null,
    val searchQuery: String = "",
    val searchHits: List<com.filelens.commander.domain.model.SearchHit> = emptyList(),
    val isSearching: Boolean = false
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
    private val configInspector: ConfigInspector,
    private val logInspector: LogInspector,
    private val epubInspector: EpubInspector,
    private val trustScoreUseCase: TrustScoreUseCase,
    private val provenanceDetector: ProvenanceDetector,
    private val structuredSearchUseCase: StructuredSearchUseCase,
    private val reportExportUseCase: ReportExportUseCase,
    private val recentsRepository: com.filelens.commander.data.RecentsRepository,
    private val remoteConnector: com.filelens.commander.vfs.RemoteConnector,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(InspectorState())
    val state: StateFlow<InspectorState> = _state.asStateFlow()

    /** Temp copy of the last remote file we materialised for previewing. */
    private var tempRemoteFile: File? = null

    fun selectFile(item: FileItem, forcedCategory: FileCategory? = null) {
        if (item.fileCategory != FileCategory.DIRECTORY) {
            viewModelScope.launch {
                recentsRepository.recordFileOpen(item.path, item.name)
            }
        }

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
            ignoredRiskKeys = carriedIgnored,
            forcedCategory = forcedCategory
        )

        viewModelScope.launch {
            // Remote files (ftp://, sftp://, smb://) have no valid local path,
            // so download a temp copy and inspect that. Local files are read
            // in place.
            val file = if (isRemotePath(item.path)) {
                materialiseRemote(item) ?: run {
                    _state.value = _state.value.copy(
                        isAnalyzing = false,
                        result = InspectorResult(
                            overview = mapOf("Error" to "Couldn't download this file from the server for preview.")
                        )
                    )
                    return@launch
                }
            } else {
                File(item.path)
            }
            if (!file.exists() || !file.canRead()) {
                _state.value = _state.value.copy(isAnalyzing = false)
                return@launch
            }

            val detection = magicBytesDetector.detect(file)

            val detectedCategory = if (detection.confidence > 0.5f) {
                detection.category
            } else {
                item.fileCategory
            }
            val effectiveCategory = forcedCategory ?: detectedCategory

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
                if (forcedCategory != null && forcedCategory != detectedCategory) {
                    put("Inspected as", forcedCategory.name + " (user override)")
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

            val provenance = provenanceDetector.detect(file.absolutePath)
            val withDetection = result.copy(
                overview = overviewWithDetection,
                risks = risksWithDetection,
                provenance = provenance
            )
            val trustScore = trustScoreUseCase.compute(withDetection, detection)
            val finalResult = withDetection.copy(trustScore = trustScore)

            _state.value = InspectorState(
                selectedFile = item,
                info = updatedInfo,
                result = finalResult,
                detection = detection,
                isAnalyzing = false,
                ignoredRiskKeys = carriedIgnored,
                detectedCategory = detectedCategory,
                forcedCategory = forcedCategory
            )
        }
    }

    fun clearSelection() {
        cleanupTempFile()
        _state.value = InspectorState()
    }

    private fun isRemotePath(path: String): Boolean =
        path.startsWith("ftp://") || path.startsWith("sftp://") || path.startsWith("smb://")

    /**
     * Downloads a remote file to the cache dir so the File-based inspectors and
     * the image loader can read it. Capped so we don't pull huge files just to
     * preview them. Returns null on failure or when over the cap.
     */
    private suspend fun materialiseRemote(item: FileItem): File? = withContext(Dispatchers.IO) {
        cleanupTempFile()
        val size = item.size
        if (size != null && size > MAX_REMOTE_PREVIEW_BYTES) return@withContext null
        try {
            val stream = remoteConnector.openStream(item.path) ?: return@withContext null
            val suffix = item.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
            val tmp = File.createTempFile("filelens_remote_", suffix, appContext.cacheDir)
            stream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            tempRemoteFile = tmp
            tmp
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanupTempFile() {
        tempRemoteFile?.let { runCatching { it.delete() } }
        tempRemoteFile = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupTempFile()
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

    fun openAs(category: FileCategory) {
        val item = _state.value.selectedFile ?: return
        selectFile(item, forcedCategory = category)
    }

    fun reinspect() {
        val item = _state.value.selectedFile ?: return
        selectFile(item, forcedCategory = _state.value.forcedCategory)
    }

    fun searchInside(query: String) {
        val item = _state.value.selectedFile ?: return
        val cat = _state.value.info?.category ?: item.fileCategory
        _state.value = _state.value.copy(
            searchQuery = query,
            isSearching = true,
            searchHits = emptyList()
        )
        viewModelScope.launch {
            // For remote files reuse the temp copy downloaded during preview;
            // the URI path is not a readable local file.
            val target = if (isRemotePath(item.path)) tempRemoteFile else File(item.path)
            if (target == null || !target.exists()) {
                _state.value = _state.value.copy(searchHits = emptyList(), isSearching = false)
                return@launch
            }
            val hits = structuredSearchUseCase.search(target, query, cat)
            _state.value = _state.value.copy(
                searchHits = hits,
                isSearching = false
            )
        }
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", searchHits = emptyList(), isSearching = false)
    }

    fun exportReport(asJson: Boolean, onReady: (java.io.File) -> Unit) {
        val info = _state.value.info ?: return
        val result = _state.value.result ?: return
        val format = if (asJson) ReportExportUseCase.Format.JSON else ReportExportUseCase.Format.MARKDOWN
        viewModelScope.launch {
            val file = reportExportUseCase.export(
                info = info,
                result = result,
                detectedType = _state.value.detection?.detectedType,
                format = format
            )
            onReady(file)
        }
    }

    private suspend fun runInspector(file: File, category: FileCategory, extension: String): InspectorResult {
        return try {
            when (category) {
                FileCategory.IMAGE -> imageInspector.inspect(file)
                FileCategory.ARCHIVE -> zipInspector.inspect(file)
                FileCategory.APK -> apkInspector.inspect(file)
                FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> {
                    if (extension.lowercase() == "epub") {
                        epubInspector.inspect(file)
                    } else if (extension.lowercase() in setOf("docx", "xlsx", "pptx")) {
                        officeInspector.inspect(file)
                    } else {
                        textInspector.inspect(file)
                    }
                }
                FileCategory.TEXT -> {
                    when {
                        extension.lowercase() == "json" -> jsonInspector.inspect(file)
                        extension.lowercase() in setOf("xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist") -> xmlInspector.inspect(file)
                        logInspector.handles(file) -> logInspector.inspect(file)
                        configInspector.handles(file) -> configInspector.inspect(file)
                        else -> textInspector.inspect(file)
                    }
                }
                FileCategory.CODE -> {
                    when {
                        logInspector.handles(file) -> logInspector.inspect(file)
                        configInspector.handles(file) -> configInspector.inspect(file)
                        else -> textInspector.inspect(file)
                    }
                }
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

    private companion object {
        const val MAX_REMOTE_PREVIEW_BYTES = 50L * 1024 * 1024
    }
}
