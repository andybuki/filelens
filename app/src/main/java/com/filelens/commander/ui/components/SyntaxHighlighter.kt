package com.filelens.commander.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val KeywordColor = Color(0xFFCF222E)
private val StringColor = Color(0xFF0A3069)
private val CommentColor = Color(0xFF6E7781)
private val NumberColor = Color(0xFF0550AE)
private val TypeColor = Color(0xFF953800)
private val FunctionColor = Color(0xFF6F42C1)

private val KOTLIN_KEYWORDS = setOf(
    "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum",
    "abstract", "open", "override", "private", "public", "protected", "internal",
    "if", "else", "when", "for", "while", "do", "return", "break", "continue",
    "try", "catch", "finally", "throw", "import", "package", "as", "is", "in",
    "null", "true", "false", "this", "super", "suspend", "companion", "init",
    "lateinit", "const", "by", "where", "out", "in", "vararg", "operator", "infix",
    "inline", "noinline", "crossinline", "typealias", "external", "annotation"
)

private val JAVA_KEYWORDS = setOf(
    "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
    "const", "continue", "default", "do", "double", "else", "enum", "extends",
    "final", "finally", "float", "for", "goto", "if", "implements", "import",
    "instanceof", "int", "interface", "long", "native", "new", "package",
    "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient",
    "try", "void", "volatile", "while", "true", "false", "null", "var"
)

private val PYTHON_KEYWORDS = setOf(
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
    "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
    "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
    "raise", "return", "True", "try", "while", "with", "yield"
)

private val JS_KEYWORDS = setOf(
    "var", "let", "const", "function", "class", "extends", "return", "if", "else",
    "for", "while", "do", "switch", "case", "default", "break", "continue", "new",
    "this", "super", "import", "export", "from", "as", "async", "await", "yield",
    "try", "catch", "finally", "throw", "typeof", "instanceof", "in", "of", "void",
    "delete", "true", "false", "null", "undefined", "interface", "type", "enum"
)

fun highlightSyntax(content: String, language: String?): AnnotatedString {
    val lang = language?.lowercase() ?: return AnnotatedString(content)

    val keywords = when (lang) {
        "kotlin" -> KOTLIN_KEYWORDS
        "java" -> JAVA_KEYWORDS
        "python" -> PYTHON_KEYWORDS
        "javascript", "typescript", "jsx", "tsx" -> JS_KEYWORDS
        else -> return highlightGeneric(content, lang)
    }

    return buildAnnotatedString {
        val tokens = tokenize(content, keywords, lang)
        for (token in tokens) {
            when (token.type) {
                TokenType.KEYWORD -> withStyle(SpanStyle(color = KeywordColor)) { append(token.text) }
                TokenType.STRING -> withStyle(SpanStyle(color = StringColor)) { append(token.text) }
                TokenType.COMMENT -> withStyle(SpanStyle(color = CommentColor)) { append(token.text) }
                TokenType.NUMBER -> withStyle(SpanStyle(color = NumberColor)) { append(token.text) }
                TokenType.TYPE -> withStyle(SpanStyle(color = TypeColor)) { append(token.text) }
                TokenType.FUNCTION -> withStyle(SpanStyle(color = FunctionColor)) { append(token.text) }
                TokenType.PLAIN -> append(token.text)
            }
        }
    }
}

private fun highlightGeneric(content: String, lang: String): AnnotatedString {
    return when (lang) {
        "json" -> highlightJson(content)
        "xml" -> highlightXml(content)
        "yaml" -> highlightYaml(content)
        "markdown" -> highlightMarkdown(content)
        else -> AnnotatedString(content)
    }
}

private fun highlightJson(content: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < content.length) {
        val c = content[i]
        when {
            c == '"' -> {
                val end = findStringEnd(content, i)
                val isKey = lookForKeyColon(content, end + 1)
                withStyle(SpanStyle(color = if (isKey) KeywordColor else StringColor)) {
                    append(content.substring(i, minOf(end + 1, content.length)))
                }
                i = end + 1
            }
            c.isDigit() || (c == '-' && i + 1 < content.length && content[i + 1].isDigit()) -> {
                val end = findNumberEnd(content, i)
                withStyle(SpanStyle(color = NumberColor)) { append(content.substring(i, end)) }
                i = end
            }
            content.regionMatches(i, "true", 0, 4) || content.regionMatches(i, "false", 0, 5) || content.regionMatches(i, "null", 0, 4) -> {
                val word = when {
                    content.regionMatches(i, "true", 0, 4) -> "true"
                    content.regionMatches(i, "false", 0, 5) -> "false"
                    else -> "null"
                }
                withStyle(SpanStyle(color = KeywordColor)) { append(word) }
                i += word.length
            }
            else -> { append(c); i++ }
        }
    }
}

private fun highlightXml(content: String): AnnotatedString = buildAnnotatedString {
    val tagRegex = Regex("""<!--[\s\S]*?-->|</?[\w:]+[^>]*>""")
    var last = 0
    for (match in tagRegex.findAll(content)) {
        if (match.range.first > last) append(content.substring(last, match.range.first))
        val tag = match.value
        if (tag.startsWith("<!--")) {
            withStyle(SpanStyle(color = CommentColor)) { append(tag) }
        } else {
            withStyle(SpanStyle(color = KeywordColor)) { append(tag) }
        }
        last = match.range.last + 1
    }
    if (last < content.length) append(content.substring(last))
}

private fun highlightYaml(content: String): AnnotatedString = buildAnnotatedString {
    for (line in content.lines()) {
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("#") -> withStyle(SpanStyle(color = CommentColor)) { append(line) }
            trimmed.contains(":") -> {
                val colonIdx = line.indexOf(':')
                withStyle(SpanStyle(color = KeywordColor)) { append(line.substring(0, colonIdx)) }
                append(line.substring(colonIdx))
            }
            else -> append(line)
        }
        append("\n")
    }
}

private fun highlightMarkdown(content: String): AnnotatedString = buildAnnotatedString {
    for (line in content.lines()) {
        when {
            line.startsWith("#") -> withStyle(SpanStyle(color = KeywordColor)) { append(line) }
            line.startsWith("```") -> withStyle(SpanStyle(color = CommentColor)) { append(line) }
            line.startsWith("- ") || line.startsWith("* ") -> {
                withStyle(SpanStyle(color = TypeColor)) { append(line.take(2)) }
                append(line.drop(2))
            }
            else -> append(line)
        }
        append("\n")
    }
}

private enum class TokenType { KEYWORD, STRING, COMMENT, NUMBER, TYPE, FUNCTION, PLAIN }
private data class Token(val text: String, val type: TokenType)

private fun tokenize(content: String, keywords: Set<String>, lang: String): List<Token> {
    val result = mutableListOf<Token>()
    var i = 0
    val len = content.length

    while (i < len) {
        val c = content[i]

        if (c == '/' && i + 1 < len && content[i + 1] == '/') {
            val end = content.indexOf('\n', i).let { if (it == -1) len else it }
            result.add(Token(content.substring(i, end), TokenType.COMMENT))
            i = end
            continue
        }
        if (c == '/' && i + 1 < len && content[i + 1] == '*') {
            val end = content.indexOf("*/", i + 2).let { if (it == -1) len else it + 2 }
            result.add(Token(content.substring(i, end), TokenType.COMMENT))
            i = end
            continue
        }
        if (c == '#' && lang == "python") {
            val end = content.indexOf('\n', i).let { if (it == -1) len else it }
            result.add(Token(content.substring(i, end), TokenType.COMMENT))
            i = end
            continue
        }
        if (c == '"' || c == '\'') {
            val end = findStringEnd(content, i)
            result.add(Token(content.substring(i, minOf(end + 1, len)), TokenType.STRING))
            i = end + 1
            continue
        }
        if (c.isDigit()) {
            val end = findNumberEnd(content, i)
            result.add(Token(content.substring(i, end), TokenType.NUMBER))
            i = end
            continue
        }
        if (c.isLetter() || c == '_') {
            var j = i
            while (j < len && (content[j].isLetterOrDigit() || content[j] == '_')) j++
            val word = content.substring(i, j)
            val type = when {
                word in keywords -> TokenType.KEYWORD
                word.first().isUpperCase() -> TokenType.TYPE
                j < len && content[j] == '(' -> TokenType.FUNCTION
                else -> TokenType.PLAIN
            }
            result.add(Token(word, type))
            i = j
            continue
        }
        result.add(Token(c.toString(), TokenType.PLAIN))
        i++
    }
    return result
}

private fun findStringEnd(content: String, start: Int): Int {
    val quote = content[start]
    var i = start + 1
    while (i < content.length) {
        val c = content[i]
        if (c == '\\') { i += 2; continue }
        if (c == quote) return i
        i++
    }
    return content.length - 1
}

private fun findNumberEnd(content: String, start: Int): Int {
    var i = start
    if (content[i] == '-') i++
    while (i < content.length && (content[i].isDigit() || content[i] == '.' || content[i] == 'e' || content[i] == 'E' || content[i] == '+' || content[i] == '-')) i++
    return i
}

private fun lookForKeyColon(content: String, start: Int): Boolean {
    var i = start
    while (i < content.length && content[i].isWhitespace()) i++
    return i < content.length && content[i] == ':'
}
