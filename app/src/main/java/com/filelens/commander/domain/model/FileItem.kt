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
    CERTIFICATE,
    UNKNOWN
}

fun classifyByExtension(extension: String): FileCategory {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "jpe", "jfif", "png", "apng", "gif", "bmp", "webp", "svg", "ico",
        "tiff", "tif", "heic", "heif", "avif", "jxl", "jp2", "jpx",
        "dng", "cr2", "cr3", "nef", "arw", "raf", "orf", "rw2",
        "psd", "ai", "xcf" -> FileCategory.IMAGE
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp",
        "ts", "m2ts", "mts", "vob", "ogv", "mxf", "f4v" -> FileCategory.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "oga", "wma", "m4a", "opus",
        "amr", "awb", "mid", "midi", "aiff", "aif", "ac3", "dts", "mka" -> FileCategory.AUDIO
        "doc", "docx", "docm", "odt", "rtf", "tex", "wpd", "epub",
        "mobi", "azw", "azw3", "fb2", "djvu", "xps", "oxps", "pages" -> FileCategory.DOCUMENT
        "xls", "xlsx", "xlsm", "xlsb", "ods", "csv", "tsv", "numbers" -> FileCategory.SPREADSHEET
        "ppt", "pptx", "pptm", "odp" -> FileCategory.PRESENTATION
        "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz", "tbz2", "xz", "txz",
        "zst", "lz4", "jar", "war", "ear",
        "zipx", "xpi", "crx", "cab", "lzh", "lha", "arj",
        "iso", "dmg", "img", "vhd", "vmdk", "qcow2" -> FileCategory.ARCHIVE
        "kt", "kts", "java", "py", "js", "mjs", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp",
        "rs", "go", "rb", "php", "swift", "m", "scala", "groovy", "dart",
        "sh", "bash", "zsh", "fish", "ps1", "bat", "cmd",
        "html", "htm", "css", "scss", "less", "sass",
        "sql", "graphql", "proto",
        "vue", "svelte", "astro", "mdx", "xaml",
        "hs", "elm", "clj", "cljs", "ex", "exs", "erl", "lisp", "scm", "rkt",
        "jl", "zig", "nim", "cr", "ml", "mli",
        "vb", "vbs", "fs", "fsx", "asm", "s", "r", "lua", "pl",
        "tf", "tfvars", "hcl", "nix", "cmake", "gradle", "bzl" -> FileCategory.CODE
        "txt", "md", "markdown", "rst", "adoc", "log", "ini", "cfg", "conf", "config",
        "json", "jsonc", "json5", "ndjson", "xml", "yaml", "yml", "toml",
        "properties", "env", "editorconfig", "gitignore", "gitattributes",
        "srt", "vtt", "ass", "ssa" -> FileCategory.TEXT
        "pdf" -> FileCategory.PDF
        "apk", "aab", "ipa", "xapk" -> FileCategory.APK
        "db", "sqlite", "sqlite3", "mdb", "accdb", "realm", "parquet" -> FileCategory.DATABASE
        "ttf", "otf", "ttc", "woff", "woff2", "eot", "pfa", "pfb" -> FileCategory.FONT
        "exe", "msi", "dll", "so", "dylib", "deb", "rpm", "dex", "wasm" -> FileCategory.EXECUTABLE
        "pem", "crt", "cer", "der", "key", "p12", "pfx", "jks", "pub", "ppk",
        "ovpn", "wg", "mobileconfig" -> FileCategory.CERTIFICATE
        else -> FileCategory.UNKNOWN
    }
}
