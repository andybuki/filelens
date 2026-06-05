package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.SortOrder
import com.filelens.commander.domain.model.classifyByExtension
import com.filelens.commander.vfs.LocalFileNode
import com.filelens.commander.vfs.NodeType
import com.filelens.commander.vfs.VirtualFileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowseFilesUseCase @Inject constructor() {

    suspend fun listFiles(
        node: VirtualFileNode,
        sortOrder: SortOrder = SortOrder(),
        showHidden: Boolean = false,
        hideEmptyFolders: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val children = node.children()

        children
            .filter { child ->
                if (!hideEmptyFolders || child.type != NodeType.DIRECTORY) return@filter true
                hasVisibleChild(child, showHidden)
            }
            .map { child -> toFileItem(child) }
            .filter { showHidden || !it.isHidden }
            .let { items -> sortItems(items, sortOrder) }
    }

    private suspend fun hasVisibleChild(node: VirtualFileNode, showHidden: Boolean): Boolean {
        return try {
            node.children().any { child ->
                val hidden = (child as? LocalFileNode)?.isHidden ?: false
                showHidden || !hidden
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun toFileItem(node: VirtualFileNode): FileItem {
        val extension = node.name.substringAfterLast('.', "").takeIf { it != node.name } ?: ""
        val isHidden = (node as? LocalFileNode)?.isHidden ?: false
        val category = if (node.type == NodeType.DIRECTORY) {
            FileCategory.DIRECTORY
        } else {
            classifyByExtension(extension)
        }
        // Cheap shallow probe so the row can tell empty vs populated folders
        // without listing recursively. Remote / archive nodes leave it null.
        val childCount = if (node.type == NodeType.DIRECTORY && node is LocalFileNode) {
            runCatching { node.absoluteFile.list()?.size }.getOrNull()
        } else null

        return FileItem(
            name = node.name,
            path = node.path,
            nodeType = node.type,
            size = node.size,
            lastModified = node.lastModified,
            isReadable = node.isReadable,
            isHidden = isHidden,
            fileCategory = category,
            extension = extension,
            childCount = childCount
        )
    }

    private fun sortItems(items: List<FileItem>, sortOrder: SortOrder): List<FileItem> {
        val comparator = buildComparator(sortOrder)
        return items.sortedWith(comparator)
    }

    private fun buildComparator(sortOrder: SortOrder): Comparator<FileItem> {
        var comparator: Comparator<FileItem> = when (sortOrder.field) {
            SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortField.SIZE -> compareBy { it.size ?: 0L }
            SortField.DATE -> compareBy { it.lastModified ?: 0L }
            SortField.TYPE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.extension }
        }

        if (sortOrder.direction == SortDirection.DESCENDING) {
            comparator = comparator.reversed()
        }

        if (sortOrder.directoriesFirst) {
            comparator = compareBy<FileItem> { it.nodeType != NodeType.DIRECTORY }.then(comparator)
        }

        return comparator
    }
}
