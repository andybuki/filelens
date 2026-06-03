package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.DiffKind
import com.filelens.commander.domain.model.StructuralDiff
import com.filelens.commander.domain.model.StructuralDiffEntry
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@Singleton
class StructuralDiffUseCase @Inject constructor() {

    fun diffJson(left: String, right: String): StructuralDiff? {
        val leftFlat = runCatching { flattenJson(parseJson(left)) }.getOrNull() ?: return null
        val rightFlat = runCatching { flattenJson(parseJson(right)) }.getOrNull() ?: return null
        return buildDiff("json", leftFlat, rightFlat)
    }

    fun diffXml(left: String, right: String): StructuralDiff? {
        val leftFlat = runCatching { flattenXml(left) }.getOrNull() ?: return null
        val rightFlat = runCatching { flattenXml(right) }.getOrNull() ?: return null
        return buildDiff("xml", leftFlat, rightFlat)
    }

    fun diffKeyValue(format: String, left: Map<String, String?>, right: Map<String, String?>): StructuralDiff {
        return buildDiff(format, left, right)
    }

    private fun buildDiff(
        format: String,
        left: Map<String, String?>,
        right: Map<String, String?>
    ): StructuralDiff {
        val keys = (left.keys + right.keys).toSortedSet()
        val entries = mutableListOf<StructuralDiffEntry>()
        var unchanged = 0
        for (key in keys) {
            val l = left[key]
            val r = right[key]
            val kind = when {
                key !in left -> DiffKind.ADDED
                key !in right -> DiffKind.REMOVED
                l != r -> DiffKind.CHANGED
                else -> DiffKind.UNCHANGED
            }
            if (kind == DiffKind.UNCHANGED) {
                unchanged++
            } else {
                entries += StructuralDiffEntry(key, l, r, kind)
            }
        }
        return StructuralDiff(format = format, entries = entries, unchangedCount = unchanged)
    }

    private fun parseJson(text: String): Any {
        val trimmed = text.trim()
        return try {
            JSONObject(trimmed)
        } catch (_: JSONException) {
            JSONArray(trimmed)
        }
    }

    private fun flattenJson(root: Any): Map<String, String?> {
        val out = linkedMapOf<String, String?>()
        flattenJson("", root, out)
        return out
    }

    private fun flattenJson(prefix: String, value: Any?, out: MutableMap<String, String?>) {
        when (value) {
            is JSONObject -> {
                if (value.length() == 0) {
                    out[prefix.ifEmpty { "$" }] = "{}"
                    return
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    val childPath = if (prefix.isEmpty()) key else "$prefix.$key"
                    flattenJson(childPath, child, out)
                }
            }
            is JSONArray -> {
                if (value.length() == 0) {
                    out[prefix.ifEmpty { "$" }] = "[]"
                    return
                }
                for (i in 0 until value.length()) {
                    val child = value.opt(i)
                    val childPath = "$prefix[$i]"
                    flattenJson(childPath, child, out)
                }
            }
            JSONObject.NULL, null -> out[prefix.ifEmpty { "$" }] = null
            else -> out[prefix.ifEmpty { "$" }] = value.toString()
        }
    }

    private fun flattenXml(text: String): Map<String, String?> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(text))

        val out = linkedMapOf<String, String?>()
        val pathStack = mutableListOf<String>()
        val indexStack = mutableListOf<MutableMap<String, Int>>().also { it.add(mutableMapOf()) }
        val textBuf = StringBuilder()
        var lastWasText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    val counters = indexStack.last()
                    val n = counters.getOrDefault(name, 0)
                    counters[name] = n + 1
                    val segment = if (n == 0) name else "$name[$n]"
                    pathStack += segment
                    indexStack += mutableMapOf()
                    val path = pathStack.joinToString("/")
                    for (i in 0 until parser.attributeCount) {
                        val attrName = parser.getAttributeName(i)
                        val attrVal = parser.getAttributeValue(i)
                        out["$path/@$attrName"] = attrVal
                    }
                    textBuf.setLength(0)
                    lastWasText = false
                }
                XmlPullParser.TEXT -> {
                    val txt = parser.text
                    if (txt != null) {
                        textBuf.append(txt)
                        lastWasText = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    val path = pathStack.joinToString("/")
                    val text = textBuf.toString().trim()
                    if (text.isNotEmpty()) out[path] = text
                    pathStack.removeAt(pathStack.lastIndex)
                    indexStack.removeAt(indexStack.lastIndex)
                    textBuf.setLength(0)
                    lastWasText = false
                }
            }
            event = parser.next()
        }
        return out
    }
}
