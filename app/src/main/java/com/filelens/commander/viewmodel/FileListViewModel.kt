package com.filelens.commander.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.SettingsRepository
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortOrder
import com.filelens.commander.domain.usecase.BrowseFilesUseCase
import kotlinx.coroutines.flow.first
import com.filelens.commander.vfs.LocalFileNode
import com.filelens.commander.vfs.NodeType
import com.filelens.commander.vfs.VirtualFileNode
import com.filelens.commander.vfs.ZipEntryNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileListState(
    val currentPath: String = "",
    val homePath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder(),
    val showHidden: Boolean = false,
    val hideEmptyFolders: Boolean = false,
    val isRoot: Boolean = true,
    val isInsideArchive: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSearching: Boolean = false,
    val isRemote: Boolean = false,
    val remoteServerLabel: String? = null,
    val remoteNotice: String? = null
)

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val browseFilesUseCase: BrowseFilesUseCase,
    private val settingsRepository: SettingsRepository,
    private val recentsRepository: com.filelens.commander.data.RecentsRepository,
    private val remoteConnections: com.filelens.commander.data.RemoteConnectionsRepository,
    private val remoteConnector: com.filelens.commander.vfs.RemoteConnector
) : ViewModel() {

    private val _state = MutableStateFlow(FileListState())
    val state: StateFlow<FileListState> = _state.asStateFlow()

    private val nodeStack = mutableListOf<VirtualFileNode>()
    private var allFiles: List<FileItem> = emptyList()
    private var homePath: String = ""
    private var currentRemoteLabel: String? = null

    private fun isRemoteNode(node: VirtualFileNode): Boolean =
        node is com.filelens.commander.vfs.FtpFileNode ||
            node is com.filelens.commander.vfs.SftpFileNode ||
            node is com.filelens.commander.vfs.SmbFileNode

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settings.first()
            _state.value = _state.value.copy(
                showHidden = initial.showHidden,
                hideEmptyFolders = initial.hideEmptyFolders,
                sortOrder = SortOrder(
                    field = initial.defaultSortField,
                    direction = initial.defaultSortDirection
                )
            )
            val startPath = Environment.getExternalStorageDirectory().absolutePath
            homePath = startPath
            _state.value = _state.value.copy(homePath = startPath)
            val node = LocalFileNode(File(startPath))
            navigateToNode(node, replaceStack = true)

            settingsRepository.settings.collect { s ->
                val refreshNeeded = _state.value.hideEmptyFolders != s.hideEmptyFolders ||
                    _state.value.showHidden != s.showHidden
                _state.value = _state.value.copy(
                    showHidden = s.showHidden,
                    hideEmptyFolders = s.hideEmptyFolders
                )
                if (refreshNeeded) refreshCurrentDirectory()
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            // Confine navigation to the home root; outside paths get clamped back to home.
            val target = if (homePath.isNotEmpty() && !isUnderHome(path)) homePath else path
            val node = LocalFileNode(File(target))
            navigateToNode(node, replaceStack = true)
        }
    }

    /**
     * Drives a Home-tab category tile. Standard-folder categories navigate
     * directly to the typical external-storage location for that content;
     * type- and size-based categories trigger a recursive scan from the
     * home root and present the matches as a flat list under the existing
     * search UI.
     */
    fun browseCategory(category: com.filelens.commander.domain.model.HomeCategory) {
        viewModelScope.launch {
            val ext = Environment.getExternalStorageDirectory()
            when (category) {
                com.filelens.commander.domain.model.HomeCategory.IMAGES ->
                    navigateToFirstExisting(
                        listOf(Environment.DIRECTORY_PICTURES, Environment.DIRECTORY_DCIM),
                        ext
                    )
                com.filelens.commander.domain.model.HomeCategory.VIDEO ->
                    navigateToFirstExisting(
                        listOf(Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_DCIM),
                        ext
                    )
                com.filelens.commander.domain.model.HomeCategory.AUDIO ->
                    navigateToFirstExisting(listOf(Environment.DIRECTORY_MUSIC), ext)
                com.filelens.commander.domain.model.HomeCategory.DOCUMENTS ->
                    navigateToFirstExisting(listOf(Environment.DIRECTORY_DOCUMENTS), ext)
                com.filelens.commander.domain.model.HomeCategory.DOWNLOADS ->
                    navigateToFirstExisting(listOf(Environment.DIRECTORY_DOWNLOADS), ext)
                com.filelens.commander.domain.model.HomeCategory.APKS ->
                    runCategoryScan { it.isFile && it.extension.equals("apk", ignoreCase = true) }
                com.filelens.commander.domain.model.HomeCategory.ARCHIVES ->
                    runCategoryScan { f ->
                        f.isFile && f.extension.lowercase() in ARCHIVE_EXTS ||
                            f.isFile && f.name.lowercase().let { it.endsWith(".tar.gz") || it.endsWith(".tgz") }
                    }
                com.filelens.commander.domain.model.HomeCategory.LARGE_FILES ->
                    runCategoryScan(sortBySizeDesc = true) { it.isFile && it.length() >= LARGE_FILE_BYTES }
            }
        }
    }

    private suspend fun navigateToFirstExisting(subdirs: List<String>, ext: File) {
        val target = subdirs
            .map { File(ext, it) }
            .firstOrNull { it.exists() && it.isDirectory }
            ?: ext
        val node = LocalFileNode(target)
        navigateToNode(node, replaceStack = true)
    }

    private suspend fun runCategoryScan(
        sortBySizeDesc: Boolean = false,
        predicate: (File) -> Boolean
    ) {
        val root = if (homePath.isNotEmpty()) File(homePath) else Environment.getExternalStorageDirectory()
        val rootNode = LocalFileNode(root)
        // Land at the home root first so navigateUp and breadcrumbs still
        // have a sensible parent state once the user dismisses the scan.
        navigateToNode(rootNode, replaceStack = true)
        _state.value = _state.value.copy(
            isSearchActive = true,
            isSearching = true,
            searchQuery = ""
        )
        try {
            val results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val found = mutableListOf<FileItem>()
                scanByPredicate(root, predicate, found, maxDepth = 6)
                if (sortBySizeDesc) found.sortByDescending { it.size ?: 0L }
                found
            }
            _state.value = _state.value.copy(files = results, isSearching = false)
        } catch (_: Exception) {
            _state.value = _state.value.copy(isSearching = false)
        }
    }

    private fun scanByPredicate(
        dir: File,
        predicate: (File) -> Boolean,
        results: MutableList<FileItem>,
        maxDepth: Int,
        depth: Int = 0
    ) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (file in children) {
            if (predicate(file)) {
                val ext = file.extension
                val category = if (file.isDirectory) FileCategory.DIRECTORY
                    else com.filelens.commander.domain.model.classifyByExtension(ext)
                results.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        nodeType = if (file.isDirectory) NodeType.DIRECTORY else NodeType.FILE,
                        size = if (file.isFile) file.length() else null,
                        lastModified = file.lastModified(),
                        isReadable = file.canRead(),
                        isHidden = file.isHidden,
                        fileCategory = category,
                        extension = ext
                    )
                )
            }
            if (file.isDirectory && file.canRead()) {
                scanByPredicate(file, predicate, results, maxDepth, depth + 1)
            }
        }
    }

    private companion object {
        val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "lz4", "jar", "war", "ear")
        const val LARGE_FILE_BYTES = 50L * 1024 * 1024
    }

    private fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            val node = LocalFileNode(File(path))
            navigateToNode(node)
        }
    }

    /**
     * Connects to a saved remote server and navigates to its root. Replaces the
     * current navigation stack — leaving the remote root with [navigateUp] will
     * walk back through ancestors and stop once the stack is exhausted.
     */
    fun openRemoteConnection(id: String) {
        viewModelScope.launch {
            // Clear the current (local) listing while we connect so a failure
            // surfaces the error instead of leaving the previous folder's files
            // on screen — the panel only shows errors when the list is empty.
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                remoteNotice = null,
                files = emptyList()
            )
            val connection = remoteConnections.get(id)
            if (connection == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    files = emptyList(),
                    error = "Server not found"
                )
                return@launch
            }
            try {
                val opened = remoteConnector.open(connection)
                val label = connection.name.ifBlank { "${connection.protocol.displayName} · ${connection.host}" }
                currentRemoteLabel = label
                navigateToNode(opened.rootNode, replaceStack = true)
                val notice = opened.capturedHostKeyFingerprint?.let {
                    "Pinned host key for ${connection.host} ($it). FileLens will warn you if it ever changes."
                }
                if (notice != null) {
                    _state.value = _state.value.copy(remoteNotice = notice)
                }
            } catch (e: com.filelens.commander.vfs.HostKeyMismatchException) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    files = emptyList(),
                    error = "Host key for ${connection.host} does not match the one FileLens has pinned" +
                        (e.fingerprint?.let { " (server now offers $it)" } ?: "") +
                        ". Refusing to connect — verify the server, then edit the saved entry to clear the pin if you trust the new key."
                )
            } catch (e: Exception) {
                val cause = e.cause?.message?.let { " ($it)" } ?: ""
                _state.value = _state.value.copy(
                    isLoading = false,
                    files = emptyList(),
                    error = "Couldn't connect to ${connection.name.ifBlank { connection.host }}: ${e.message ?: e.javaClass.simpleName}$cause"
                )
            }
        }
    }

    fun clearRemoteNotice() {
        if (_state.value.remoteNotice != null) {
            _state.value = _state.value.copy(remoteNotice = null)
        }
    }

    private fun isUnderHome(path: String): Boolean {
        if (homePath.isEmpty()) return true
        return path == homePath || path.startsWith("$homePath/")
    }

    private suspend fun navigateToNode(node: VirtualFileNode, replaceStack: Boolean = false) {
        if (replaceStack && !isRemoteNode(node)) currentRemoteLabel = null
        _state.value = _state.value.copy(isLoading = true, error = null, searchQuery = "", isSearchActive = false)
        try {
            val files = browseFilesUseCase.listFiles(
                node = node,
                sortOrder = _state.value.sortOrder,
                showHidden = _state.value.showHidden,
                hideEmptyFolders = _state.value.hideEmptyFolders
            )
            if (replaceStack) {
                nodeStack.clear()
                nodeStack.add(node)
            } else if (nodeStack.lastOrNull()?.path != node.path) {
                nodeStack.add(node)
            }
            allFiles = files
            val atHome = node !is ZipEntryNode && homePath.isNotEmpty() && node.path == homePath
            _state.value = _state.value.copy(
                currentPath = node.path,
                files = files,
                isLoading = false,
                isRoot = atHome || nodeStack.size <= 1,
                isInsideArchive = node is ZipEntryNode ||
                    node is com.filelens.commander.vfs.TarEntryNode ||
                    node is com.filelens.commander.vfs.SevenZEntryNode ||
                    node is com.filelens.commander.vfs.RarEntryNode,
                isRemote = isRemoteNode(node),
                remoteServerLabel = if (isRemoteNode(node)) currentRemoteLabel ?: node.path else null
            )
            if (node is LocalFileNode) {
                recentsRepository.recordFolderVisit(
                    path = node.path,
                    displayName = node.path.substringAfterLast('/').ifEmpty { "/" }
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to list files"
            )
        }
    }

    fun navigateUp() {
        if (_state.value.isSearchActive) {
            clearSearch()
            return
        }
        // Never escape the home root, even if we somehow got above it.
        val current = nodeStack.lastOrNull()
        if (current is LocalFileNode && homePath.isNotEmpty() && current.path == homePath) {
            return
        }
        if (nodeStack.size > 1) {
            nodeStack.removeAt(nodeStack.lastIndex)
            viewModelScope.launch {
                navigateToNode(nodeStack.removeAt(nodeStack.lastIndex))
            }
        }
    }

    fun onFileClick(item: FileItem): Boolean {
        if (item.fileCategory == FileCategory.DIRECTORY || item.nodeType == NodeType.DIRECTORY) {
            val current = nodeStack.lastOrNull()
            when (current) {
                is ZipEntryNode -> viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
                is com.filelens.commander.vfs.TarEntryNode -> viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
                is com.filelens.commander.vfs.SevenZEntryNode -> viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
                is com.filelens.commander.vfs.RarEntryNode -> viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
                is com.filelens.commander.vfs.FtpFileNode,
                is com.filelens.commander.vfs.SftpFileNode,
                is com.filelens.commander.vfs.SmbFileNode -> viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
                else -> navigateToDirectory(item.path)
            }
            return true
        }
        if (item.fileCategory == FileCategory.ARCHIVE && item.extension.lowercase() in setOf("zip", "jar", "apk", "war", "ear", "aab", "docx", "xlsx", "pptx")) {
            viewModelScope.launch {
                val archive = ZipEntryNode.fromFile(File(item.path), nodeStack.lastOrNull())
                if (archive != null) navigateToNode(archive)
            }
            return true
        }
        val lowerName = item.name.lowercase()
        val isTar = lowerName.endsWith(".tar") ||
            lowerName.endsWith(".tar.gz") ||
            lowerName.endsWith(".tgz")
        if (isTar) {
            viewModelScope.launch {
                val archive = com.filelens.commander.vfs.TarEntryNode.fromFile(File(item.path), nodeStack.lastOrNull())
                if (archive != null) navigateToNode(archive)
            }
            return true
        }
        if (lowerName.endsWith(".7z")) {
            viewModelScope.launch {
                val archive = com.filelens.commander.vfs.SevenZEntryNode.fromFile(File(item.path), nodeStack.lastOrNull())
                if (archive != null) navigateToNode(archive)
            }
            return true
        }
        if (lowerName.endsWith(".rar")) {
            viewModelScope.launch {
                val archive = com.filelens.commander.vfs.RarEntryNode.fromFile(File(item.path), nodeStack.lastOrNull())
                if (archive != null) navigateToNode(archive)
            }
            return true
        }
        return false
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _state.value = _state.value.copy(sortOrder = sortOrder)
        refreshCurrentDirectory()
        viewModelScope.launch {
            settingsRepository.setDefaultSort(sortOrder.field, sortOrder.direction)
        }
    }

    fun toggleHiddenFiles() {
        val next = !_state.value.showHidden
        _state.value = _state.value.copy(showHidden = next)
        refreshCurrentDirectory()
        viewModelScope.launch {
            settingsRepository.setShowHidden(next)
        }
    }

    fun refresh() {
        refreshCurrentDirectory()
    }

    fun setSearchActive(active: Boolean) {
        if (active) {
            _state.value = _state.value.copy(isSearchActive = true, searchQuery = "")
        } else {
            clearSearch()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(files = allFiles)
        } else {
            val filtered = allFiles.filter { it.name.contains(query, ignoreCase = true) }
            _state.value = _state.value.copy(files = filtered)
        }
    }

    fun searchRecursive(query: String) {
        if (query.isBlank()) return
        val currentPath = _state.value.currentPath
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            try {
                val results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val found = mutableListOf<FileItem>()
                    searchDirectory(File(currentPath), query, found, maxDepth = 5)
                    found
                }
                _state.value = _state.value.copy(files = results, isSearching = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSearching = false)
            }
        }
    }

    private fun searchDirectory(dir: File, query: String, results: MutableList<FileItem>, maxDepth: Int, depth: Int = 0) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (file in children) {
            if (file.name.contains(query, ignoreCase = true)) {
                val ext = file.extension
                val category = if (file.isDirectory) FileCategory.DIRECTORY else com.filelens.commander.domain.model.classifyByExtension(ext)
                results.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        nodeType = if (file.isDirectory) NodeType.DIRECTORY else NodeType.FILE,
                        size = if (file.isFile) file.length() else null,
                        lastModified = file.lastModified(),
                        isReadable = file.canRead(),
                        isHidden = file.isHidden,
                        fileCategory = category,
                        extension = ext
                    )
                )
            }
            if (file.isDirectory && file.canRead()) {
                searchDirectory(file, query, results, maxDepth, depth + 1)
            }
        }
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", isSearchActive = false, files = allFiles)
    }

    private fun refreshCurrentDirectory() {
        val current = nodeStack.lastOrNull() ?: return
        viewModelScope.launch {
            if (nodeStack.isNotEmpty()) nodeStack.removeAt(nodeStack.lastIndex)
            navigateToNode(current)
        }
    }
}
