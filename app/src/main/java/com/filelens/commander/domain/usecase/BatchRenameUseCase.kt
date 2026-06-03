package com.filelens.commander.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchRenameUseCase @Inject constructor() {

    /**
     * Pattern tokens:
     *  {n}        — 1-based sequence
     *  {n:WIDTH}  — zero-padded sequence (e.g. {n:03})
     *  {name}     — original file stem (no extension)
     *  {ext}      — original extension with leading dot ("" if none)
     *  {EXT}      — uppercase {ext}
     */
    data class Rule(
        val pattern: String,
        val find: String = "",
        val replace: String = "",
        val startIndex: Int = 1,
        val useRegex: Boolean = false
    )

    data class Plan(
        val items: List<PlanItem>,
        val collisions: Set<String>
    )

    data class PlanItem(
        val sourcePath: String,
        val originalName: String,
        val newName: String,
        val isValid: Boolean,
        val reason: String? = null
    )

    fun plan(sourcePaths: List<String>, rule: Rule): Plan {
        val items = sourcePaths.mapIndexed { idx, path ->
            val file = File(path)
            val original = file.name
            val newName = render(original, rule, idx + rule.startIndex)
            val (valid, reason) = validate(newName, original)
            PlanItem(
                sourcePath = path,
                originalName = original,
                newName = newName,
                isValid = valid,
                reason = reason
            )
        }
        // Collisions: same target name in the same directory (or against existing
        // files not in the rename set).
        val byDir = items.groupBy { File(it.sourcePath).parent ?: "" }
        val collisions = mutableSetOf<String>()
        for ((dir, group) in byDir) {
            val targetCounts = group.groupingBy { it.newName }.eachCount()
            for ((name, count) in targetCounts) {
                if (count > 1) collisions += "$dir/$name"
            }
            val renamedPaths = group.map { it.sourcePath }.toSet()
            val existing = File(dir).listFiles()?.map { it.name }?.toSet() ?: emptySet()
            for (item in group) {
                if (item.newName != item.originalName &&
                    item.newName in existing &&
                    File(dir, item.newName).absolutePath !in renamedPaths
                ) {
                    collisions += "$dir/${item.newName}"
                }
            }
        }
        return Plan(items, collisions)
    }

    suspend fun apply(plan: Plan): ApplyResult = withContext(Dispatchers.IO) {
        var renamed = 0
        var skipped = 0
        val errors = mutableListOf<String>()
        for (item in plan.items) {
            if (!item.isValid || "${File(item.sourcePath).parent}/${item.newName}" in plan.collisions) {
                skipped++
                continue
            }
            if (item.newName == item.originalName) {
                skipped++
                continue
            }
            val source = File(item.sourcePath)
            val target = File(source.parentFile, item.newName)
            try {
                if (source.renameTo(target)) renamed++ else errors += "Failed: ${item.originalName}"
            } catch (e: Exception) {
                errors += "${item.originalName}: ${e.message}"
            }
        }
        ApplyResult(renamed = renamed, skipped = skipped, errors = errors)
    }

    data class ApplyResult(val renamed: Int, val skipped: Int, val errors: List<String>)

    private fun render(originalName: String, rule: Rule, index: Int): String {
        val dot = originalName.lastIndexOf('.')
        val stem = if (dot > 0) originalName.substring(0, dot) else originalName
        val ext = if (dot > 0) originalName.substring(dot) else ""

        // Optional find/replace first — operates on the original name before
        // the pattern slots it in via {name}.
        val transformedStem = if (rule.find.isNotEmpty()) {
            if (rule.useRegex) {
                runCatching { Regex(rule.find).replace(stem, rule.replace) }.getOrElse { stem }
            } else {
                stem.replace(rule.find, rule.replace)
            }
        } else stem

        var out = rule.pattern
            .replace("{name}", transformedStem)
            .replace("{ext}", ext)
            .replace("{EXT}", ext.uppercase())

        out = SEQ_REGEX.replace(out) { match ->
            val width = match.groupValues[1].toIntOrNull() ?: 0
            if (width > 0) index.toString().padStart(width, '0') else index.toString()
        }
        return out
    }

    private fun validate(newName: String, @Suppress("UNUSED_PARAMETER") original: String): Pair<Boolean, String?> {
        if (newName.isBlank()) return false to "Empty name"
        if (newName.contains('/') || newName.any { it.code < 32 }) {
            return false to "Illegal characters"
        }
        if (newName == "." || newName == "..") return false to "Reserved name"
        return true to null
    }

    private companion object {
        val SEQ_REGEX = Regex("""\{n(?::(\d+))?}""")
    }
}
