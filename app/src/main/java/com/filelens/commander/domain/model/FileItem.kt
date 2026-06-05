package com.filelens.commander.domain.model

import com.filelens.commander.vfs.NodeType

data class FileItem(
    val name: String,
    val path: String,
    val nodeType: NodeType,
    val size: Long?,
    val lastModified: Long?,
    val isReadable: Boolean,
    val isHidden: Boolean = false,
    val fileCategory: FileCategory = FileCategory.UNKNOWN,
    val extension: String = "",
    val childCount: Int? = null
)

enum class FileCategory {
    DIRECTORY,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    ARCHIVE,
    CODE,
    TEXT,
    PDF,
    APK,
    DATABASE,
    FONT,
    EXECUTABLE,
    UNKNOWN
}

fun classifyByExtension(extension: String): FileCategory {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif", "heic", "heif", "avif" -> FileCategory.IMAGE
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp" -> FileCategory.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus" -> FileCategory.AUDIO
        "doc", "docx", "odt", "rtf", "tex", "wpd", "epub" -> FileCategory.DOCUMENT
        "xls", "xlsx", "ods", "csv", "tsv" -> FileCategory.SPREADSHEET
        "ppt", "pptx", "odp" -> FileCategory.PRESENTATION
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "lz4", "jar", "war", "ear" -> FileCategory.ARCHIVE
        "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp",
        "rs", "go", "rb", "php", "swift", "m", "scala", "groovy", "dart",
        "sh", "bash", "zsh", "fish", "ps1", "bat", "cmd",
        "html", "css", "scss", "less", "sass",
        "sql", "graphql", "proto" -> FileCategory.CODE
        "txt", "md", "markdown", "log", "ini", "cfg", "conf", "config",
        "json", "xml", "yaml", "yml", "toml", "properties", "env" -> FileCategory.TEXT
        "pdf" -> FileCategory.PDF
        "apk", "aab" -> FileCategory.APK
        "db", "sqlite", "sqlite3", "mdb" -> FileCategory.DATABASE
        "ttf", "otf", "woff", "woff2" -> FileCategory.FONT
        "exe", "msi", "dll", "so", "dylib", "deb", "rpm" -> FileCategory.EXECUTABLE
        else -> FileCategory.UNKNOWN
    }
}
