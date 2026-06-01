package com.filelens.commander.domain.usecase

import com.filelens.commander.detector.MagicBytesDetector
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.inspector.ApkInspector
import com.filelens.commander.inspector.ImageInspector
import com.filelens.commander.inspector.OfficeInspector
import com.filelens.commander.inspector.PdfInspector
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
    private val pdfInspector: PdfInspector
) {

    data class ScanResult(
        val path: String,
        val name: String,
        val risks: List<RiskItem>
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

            val risks = try {
                inspectFile(file)
            } catch (_: Exception) {
                emptyList()
            }

            if (risks.isNotEmpty()) {
                results.add(ScanResult(path = file.absolutePath, name = file.name, risks = risks))
            }
        }

        emit(ScanState.Complete(results))
    }.flowOn(Dispatchers.IO)

    private suspend fun inspectFile(file: File): List<RiskItem> {
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "heic", "heif" -> imageInspector.inspect(file).risks
            "apk", "aab" -> apkInspector.inspect(file).risks
            "pdf" -> pdfInspector.inspect(file).risks
            "docx", "xlsx", "pptx" -> officeInspector.inspect(file).risks
            else -> emptyList()
        }
    }
}

sealed class ScanState {
    data class Progress(val current: Int, val total: Int, val currentFile: String) : ScanState()
    data class Complete(val results: List<BatchScanUseCase.ScanResult>) : ScanState()
    data class Error(val message: String) : ScanState()
}
