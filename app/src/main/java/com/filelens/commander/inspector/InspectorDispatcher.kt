package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectorDispatcher @Inject constructor(
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
    private val epubInspector: EpubInspector
) {

    suspend fun inspect(file: File, category: FileCategory, extension: String): InspectorResult {
        return try {
            when (category) {
                FileCategory.IMAGE -> imageInspector.inspect(file)
                FileCategory.ARCHIVE -> zipInspector.inspect(file)
                FileCategory.APK -> apkInspector.inspect(file)
                FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> {
                    when (extension.lowercase()) {
                        "epub" -> epubInspector.inspect(file)
                        "docx", "xlsx", "pptx" -> officeInspector.inspect(file)
                        else -> textInspector.inspect(file)
                    }
                }
                FileCategory.TEXT -> when {
                    extension.lowercase() == "json" -> jsonInspector.inspect(file)
                    extension.lowercase() in setOf("xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist") -> xmlInspector.inspect(file)
                    logInspector.handles(file) -> logInspector.inspect(file)
                    configInspector.handles(file) -> configInspector.inspect(file)
                    else -> textInspector.inspect(file)
                }
                FileCategory.CODE -> when {
                    logInspector.handles(file) -> logInspector.inspect(file)
                    configInspector.handles(file) -> configInspector.inspect(file)
                    else -> textInspector.inspect(file)
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
}
