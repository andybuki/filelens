package com.filelens.commander.domain.usecase

import com.filelens.commander.detector.MagicBytesDetector
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.inspector.ApkInspector
import com.filelens.commander.inspector.ConfigInspector
import com.filelens.commander.inspector.ImageInspector
import com.filelens.commander.inspector.JsonInspector
import com.filelens.commander.inspector.LogInspector
import com.filelens.commander.inspector.OfficeInspector
import com.filelens.commander.inspector.PdfInspector
import com.filelens.commander.inspector.TextInspector
import com.filelens.commander.inspector.XmlInspector
import com.filelens.commander.inspector.ZipInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchScanUseCase @Inject constructor(
    private val magicBytesDetector: MagicBytesDetector,
    private val imageInspector: ImageInspector,
    private val officeInspector: OfficeInspector,
    private val apkInspector: ApkInspector,
    private val pdfInspector: PdfInspector,
    private val configInspector: ConfigInspector,
    private val logInspector: LogInspector,
    private val textInspector: TextInspector,
    private val jsonInspector: JsonInspector,
    private val xmlInspector: XmlInspector,
    private val zipInspector: ZipInspector
) {

    enum class Bucket { RISKY, SAFE, UNSUPPORTED }

    data class ScanResult(
        val path: String,
        val name: String,
        val risks: List<RiskItem>,
        val bucket: Bucket = if (risks.isNotEmpty()) Bucket.RISKY else Bucket.SAFE
    )

    data class ScanProgress(
        val current: Int,
        val total: Int,
        val currentFile: String
    )

    fun scanFolder(folderPath: String): Flow<ScanState> = flow {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            emit(ScanState.Error("Folder not found or not a directory: $folderPath"))
            return@flow
        }

        val files = try {
            folder.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            emit(ScanState.Error(e.message ?: "Failed to list folder"))
            return@flow
        }

        val total = files.size
        val results = mutableListOf<ScanResult>()

        files.forEachIndexed { index, file ->
            emit(ScanState.Progress(current = index, total = total, currentFile = file.name))

            val outcome = try {
                inspectFile(file)
            } catch (_: Exception) {
                Outcome.Unsupported
            }
            val bucket = when (outcome) {
                is Outcome.Risks -> if (outcome.risks.isNotEmpty()) Bucket.RISKY else Bucket.SAFE
                Outcome.Unsupported -> Bucket.UNSUPPORTED
            }
            val risks = (outcome as? Outcome.Risks)?.risks ?: emptyList()
            results.add(ScanResult(
                path = file.absolutePath,
                name = file.name,
                risks = risks,
                bucket = bucket
            ))
        }

        emit(ScanState.Complete(results))
    }.flowOn(Dispatchers.IO)

    private sealed class Outcome {
        data class Risks(val risks: List<RiskItem>) : Outcome()
        data object Unsupported : Outcome()
    }

    private suspend fun inspectFile(file: File): Outcome {
        val ext = file.extension.lowercase()
        return when {
            ext in setOf("jpg", "jpeg", "png", "heic", "heif") ->
                Outcome.Risks(imageInspector.inspect(file).risks)
            ext in setOf("apk", "aab") ->
                Outcome.Risks(apkInspector.inspect(file).risks)
            ext == "pdf" ->
                Outcome.Risks(pdfInspector.inspect(file).risks)
            ext in setOf("docx", "xlsx", "pptx") ->
                Outcome.Risks(officeInspector.inspect(file).risks)
            ext in setOf("zip", "jar", "war", "ear") ->
                Outcome.Risks(zipInspector.inspect(file).risks)
            ext == "json" ->
                Outcome.Risks(jsonInspector.inspect(file).risks)
            ext in setOf("xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist") ->
                Outcome.Risks(xmlInspector.inspect(file).risks)
            logInspector.handles(file) ->
                Outcome.Risks(logInspector.inspect(file).risks)
            configInspector.handles(file) ->
                Outcome.Risks(configInspector.inspect(file).risks)
            ext in setOf("txt", "md", "kt", "java", "py", "js", "ts", "rs", "go", "c", "cpp", "h", "rb", "php", "sql", "sh", "html", "css") ->
                Outcome.Risks(textInspector.inspect(file).risks)
            else -> Outcome.Unsupported
        }
    }
}

sealed class ScanState {
    data class Progress(val current: Int, val total: Int, val currentFile: String) : ScanState()
    data class Complete(val results: List<BatchScanUseCase.ScanResult>) : ScanState()
    data class Error(val message: String) : ScanState()
}
