package com.filelens.commander.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filelens.commander.domain.model.FileCategory

// Solid cartoon-style folder and file tiles used in the browser list. The
// shapes are drawn with Canvas so they stay crisp at any size and we can
// vary fill / outline based on emptiness and category.

private val FolderYellow = Color(0xFFFFC940)
private val FolderYellowDark = Color(0xFFFFA21A)
private val FileStroke = Color(0xFF1F2937)
private val EmptyOutline = Color(0xFF1F2937)
private val EmptyFill = Color.White

@Composable
fun FolderTile(
    hasContents: Boolean,
    modifier: Modifier = Modifier
) {
    val body = if (hasContents) FolderYellow else EmptyFill
    val tabFill = if (hasContents) FolderYellowDark else EmptyFill
    val outline = EmptyOutline
    Canvas(modifier = modifier) {
        val sw = (size.minDimension * 0.05f).coerceAtLeast(2f)
        val r = size.minDimension * 0.10f
        val w = size.width
        val h = size.height
        val tabWidth = w * 0.55f
        val bodyTop = h * 0.24f

        // Back tab (drawn first so the front body covers all but the visible peek).
        drawRoundRect(
            color = tabFill,
            topLeft = Offset(sw, sw),
            size = Size(tabWidth, h - 2 * sw),
            cornerRadius = CornerRadius(r, r),
            style = Fill
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(sw, sw),
            size = Size(tabWidth, h - 2 * sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = sw)
        )

        // Front body — overlaps the lower portion of the tab.
        drawRoundRect(
            color = body,
            topLeft = Offset(sw, bodyTop),
            size = Size(w - 2 * sw, h - bodyTop - sw),
            cornerRadius = CornerRadius(r, r),
            style = Fill
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(sw, bodyTop),
            size = Size(w - 2 * sw, h - bodyTop - sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = sw)
        )
    }
}

@Composable
fun FileExtensionTile(
    extension: String,
    bodyColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val foldColor = bodyColor.lighten(0.18f)
    val label = extension.uppercase().take(4).ifBlank { "FILE" }

    Canvas(modifier = modifier) {
        val sw = (size.minDimension * 0.04f).coerceAtLeast(1.4f)
        val r = size.minDimension * 0.10f
        val w = size.width
        val h = size.height
        val fold = w * 0.34f

        // Outer file shape with the top-right corner clipped diagonally.
        val body = Path().apply {
            moveTo(r, sw)
            lineTo(w - fold, sw)
            lineTo(w - sw, fold)
            lineTo(w - sw, h - r)
            quadraticBezierTo(w - sw, h - sw, w - r, h - sw)
            lineTo(r, h - sw)
            quadraticBezierTo(sw, h - sw, sw, h - r)
            lineTo(sw, r)
            quadraticBezierTo(sw, sw, r, sw)
            close()
        }
        drawPath(body, color = bodyColor, style = Fill)

        // Folded-corner triangle in a lighter shade of the body color.
        val foldTri = Path().apply {
            moveTo(w - fold, sw)
            lineTo(w - fold, fold)
            lineTo(w - sw, fold)
            close()
        }
        drawPath(foldTri, color = foldColor, style = Fill)

        // Extension label, sized to fit the body width.
        val targetWidth = (w - sw * 2) * 0.72f
        var fontSizeSp = (size.minDimension / density / 4f).coerceAtLeast(8f)
        var style = TextStyle(
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = fontSizeSp.sp
        )
        var measured = textMeasurer.measure(label, style)
        var guard = 0
        while (measured.size.width > targetWidth && fontSizeSp > 6f && guard < 6) {
            fontSizeSp -= 1.5f
            style = style.copy(fontSize = fontSizeSp.sp)
            measured = textMeasurer.measure(label, style)
            guard++
        }
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(
                x = (w - measured.size.width) / 2f,
                y = h * 0.58f - measured.size.height / 2f
            ),
            style = style
        )
    }
}

/**
 * Lighten a color by mixing it toward white. Used to give the file-fold
 * triangle a softer highlight without picking a second palette entry.
 */
private fun Color.lighten(amount: Float): Color {
    val k = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * k,
        green = green + (1f - green) * k,
        blue = blue + (1f - blue) * k,
        alpha = alpha
    )
}

/**
 * Default body color for a file based on its category. Mirrors the
 * familiar OS conventions — yellow for code, blue for documents, orange
 * for databases, red for media that's risky to share.
 */
fun fileTileColor(category: FileCategory, extension: String): Color {
    // A few well-known extensions read better with hand-picked colors than the
    // broader category mapping (e.g. JS is famously yellow even though the
    // category is "code" which we colour blue elsewhere).
    extensionOverride(extension)?.let { return it }
    return when (category) {
        FileCategory.IMAGE -> Color(0xFF14B8A6)
        FileCategory.VIDEO -> Color(0xFFEF4444)
        FileCategory.AUDIO -> Color(0xFFF97316)
        FileCategory.DOCUMENT -> Color(0xFF2563EB)
        FileCategory.SPREADSHEET -> Color(0xFF16A34A)
        FileCategory.PRESENTATION -> Color(0xFFEA580C)
        FileCategory.ARCHIVE -> Color(0xFF7C3AED)
        FileCategory.CODE -> Color(0xFFEAB308)
        FileCategory.TEXT -> Color(0xFF64748B)
        FileCategory.PDF -> Color(0xFFDC2626)
        FileCategory.APK -> Color(0xFF16A34A)
        FileCategory.DATABASE -> Color(0xFFF97316)
        FileCategory.FONT -> Color(0xFFDB2777)
        FileCategory.EXECUTABLE -> Color(0xFF6B7280)
        FileCategory.UNKNOWN -> Color(0xFF94A3B8)
        FileCategory.DIRECTORY -> FolderYellow
    }
}

private fun extensionOverride(extension: String): Color? = when (extension.lowercase()) {
    "js", "jsx", "mjs" -> Color(0xFFEAB308)
    "ts", "tsx" -> Color(0xFF2563EB)
    "py" -> Color(0xFF3776AB)
    "kt", "kts" -> Color(0xFF7C3AED)
    "java" -> Color(0xFFB07219)
    "go" -> Color(0xFF00ADD8)
    "rs" -> Color(0xFFCE422B)
    "sql", "db", "sqlite", "sqlite3" -> Color(0xFFF97316)
    "json" -> Color(0xFF64748B)
    "html", "htm" -> Color(0xFFE34F26)
    "css", "scss" -> Color(0xFF2563EB)
    "md", "markdown" -> Color(0xFF475569)
    "pdf" -> Color(0xFFDC2626)
    else -> null
}

/**
 * Convenience wrapper that picks the right tile (folder vs file) and a
 * sensible category color for callers that just have a FileItem-shaped
 * input.
 */
@Composable
fun CategoryTile(
    category: FileCategory,
    extension: String,
    childCount: Int?,
    modifier: Modifier = Modifier,
    tileSize: Dp = 44.dp
) {
    val sized = modifier.size(tileSize)
    if (category == FileCategory.DIRECTORY) {
        FolderTile(hasContents = (childCount ?: 1) > 0, modifier = sized)
    } else {
        FileExtensionTile(
            extension = extension,
            bodyColor = fileTileColor(category, extension),
            modifier = sized
        )
    }
}
