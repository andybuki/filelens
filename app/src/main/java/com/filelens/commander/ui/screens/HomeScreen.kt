package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.Bookmark
import com.filelens.commander.domain.model.FrequentFolder
import com.filelens.commander.domain.model.HomeCategory
import com.filelens.commander.domain.model.RecentFile
import com.filelens.commander.ui.theme.CategoryArchive
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.CategoryAudio
import com.filelens.commander.ui.theme.CategoryDirectory
import com.filelens.commander.ui.theme.CategoryDocument
import com.filelens.commander.ui.theme.CategoryImage
import com.filelens.commander.ui.theme.CategoryVideo
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.MonoSmall

private data class CategoryTile(
    val icon: ImageVector,
    val labelRes: Int,
    val color: Color,
    val category: HomeCategory? = null,
    val browseAll: Boolean = false
)

private val CategoryTiles = listOf(
    CategoryTile(Icons.Filled.AccountTree, R.string.home_category_browse, FlAccent, browseAll = true),
    CategoryTile(Icons.Filled.Videocam, R.string.home_category_video, CategoryVideo, category = HomeCategory.VIDEO),
    CategoryTile(Icons.Filled.MusicNote, R.string.home_category_audio, CategoryAudio, category = HomeCategory.AUDIO),
    CategoryTile(Icons.Filled.Description, R.string.home_category_docs, CategoryDocument, category = HomeCategory.DOCUMENTS),
    CategoryTile(Icons.Filled.Image, R.string.home_category_images, CategoryImage, category = HomeCategory.IMAGES),
    CategoryTile(Icons.Filled.Archive, R.string.home_category_archives, CategoryArchive, category = HomeCategory.ARCHIVES),
    CategoryTile(Icons.Filled.Download, R.string.home_category_downloads, CategoryDirectory, category = HomeCategory.DOWNLOADS),
    CategoryTile(Icons.Filled.Storage, R.string.home_category_large, FlAccent, category = HomeCategory.LARGE_FILES)
)

@Composable
fun HomeScreen(
    onOpenCategory: (HomeCategory) -> Unit,
    onOpenPath: (String) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVault: () -> Unit = {},
    onOpenGlobalSearch: () -> Unit = {},
    onOpenAppManager: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenStorageUsage: () -> Unit = {},
    onOpenRecentFile: (String) -> Unit = onOpenPath,
    bookmarksViewModel: com.filelens.commander.viewmodel.BookmarksViewModel = hiltViewModel(),
    recentsViewModel: com.filelens.commander.viewmodel.RecentsViewModel = hiltViewModel(),
    vaultViewModel: com.filelens.commander.viewmodel.VaultViewModel = hiltViewModel(),
    trashViewModel: com.filelens.commander.viewmodel.TrashViewModel = hiltViewModel()
) {
    val bookmarks by bookmarksViewModel.bookmarks.collectAsStateWithLifecycle()
    val recentFiles by recentsViewModel.recentFiles.collectAsStateWithLifecycle()
    val frequentFolders by recentsViewModel.frequentFolders.collectAsStateWithLifecycle()
    val vaultCount by vaultViewModel.count.collectAsStateWithLifecycle()
    val trashState by trashViewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeHeader(onOpenSettings = onOpenSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SearchBarCard(onClick = onOpenGlobalSearch)
            }
            item {
                CategoriesSection(
                    onOpenCategory = onOpenCategory,
                    onBrowseAll = onOpenFiles
                )
            }
            item {
                ToolsSection(
                    vaultCount = vaultCount,
                    trashCount = trashState.items.size,
                    onOpenVault = onOpenVault,
                    onOpenAppManager = onOpenAppManager,
                    onOpenTrash = onOpenTrash,
                    onOpenStorageUsage = onOpenStorageUsage
                )
            }
            if (recentFiles.isNotEmpty()) {
                item {
                    RecentSection(
                        recentFiles = recentFiles.take(6),
                        onOpenFile = { file -> onOpenRecentFile(file.path) }
                    )
                }
            }
            if (bookmarks.isNotEmpty()) {
                item {
                    FavoritesSection(
                        bookmarks = bookmarks.sortedByDescending { it.addedAt }.take(6),
                        onOpenBookmark = { onOpenPath(it.path) }
                    )
                }
            }
            if (frequentFolders.isNotEmpty()) {
                item {
                    FrequentSection(
                        folders = frequentFolders.take(6),
                        onOpenFolder = { onOpenPath(it.path) }
                    )
                }
            }
            if (recentFiles.isEmpty() && bookmarks.isEmpty() && frequentFolders.isEmpty()) {
                item {
                    EmptyHomeHint(onOpenFiles = onOpenFiles)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.brand_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = Color(0xFFE5E7EB)
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = Eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun CollapsibleSectionHeader(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = Eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CategoriesSection(
    onOpenCategory: (HomeCategory) -> Unit,
    onBrowseAll: () -> Unit
) {
    Column {
        SectionHeader(stringResource(R.string.home_section_categories))
        CategoryTiles.chunked(4).forEachIndexed { index, row ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { tile ->
                    CategoryTileView(
                        tile = tile,
                        onClick = {
                            when {
                                tile.browseAll -> onBrowseAll()
                                tile.category != null -> onOpenCategory(tile.category)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad the trailing row so tile widths stay aligned with the
                // first row even if the count isn't a multiple of four.
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryTileView(
    tile: CategoryTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tile.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = tile.color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(tile.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentSection(
    recentFiles: List<RecentFile>,
    onOpenFile: (RecentFile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        CollapsibleSectionHeader(
            label = stringResource(R.string.home_section_recent),
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )
        if (expanded) {
            recentFiles.forEach { file ->
                EntryRow(
                    title = file.displayName,
                    subtitle = compactPath(file.path),
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { onOpenFile(file) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    bookmarks: List<Bookmark>,
    onOpenBookmark: (Bookmark) -> Unit
) {
    Column {
        SectionHeader(stringResource(R.string.home_section_favorites))
        bookmarks.forEach { bookmark ->
            EntryRow(
                title = bookmark.displayName,
                subtitle = compactPath(bookmark.path),
                icon = Icons.Filled.Bookmark,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onOpenBookmark(bookmark) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FrequentSection(
    folders: List<FrequentFolder>,
    onOpenFolder: (FrequentFolder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        CollapsibleSectionHeader(
            label = stringResource(R.string.home_section_frequent),
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )
        if (expanded) {
            folders.forEach { folder ->
                val visitsLabel = pluralStringResource(
                    R.plurals.plural_visit_count,
                    folder.visitCount,
                    folder.visitCount
                )
                EntryRow(
                    title = folder.displayName,
                    subtitle = "${compactPath(folder.path)} · $visitsLabel",
                    icon = Icons.Filled.Folder,
                    iconTint = CategoryDirectory,
                    iconBackground = CategoryDirectory.copy(alpha = 0.14f),
                    onClick = { onOpenFolder(folder) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EntryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyHomeHint(onOpenFiles: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenFiles)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_empty_headline),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchBarCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_search_headline),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_search_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ToolsSection(
    vaultCount: Int,
    trashCount: Int,
    onOpenVault: () -> Unit,
    onOpenAppManager: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStorageUsage: () -> Unit
) {
    Column {
        SectionHeader(stringResource(R.string.home_section_tools))
        ToolRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.vault_title),
            subtitle = if (vaultCount == 0) {
                stringResource(R.string.vault_home_empty)
            } else {
                pluralStringResource(R.plurals.plural_vault_item_count, vaultCount, vaultCount)
            },
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenVault
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolRow(
            icon = Icons.Filled.Apps,
            title = stringResource(R.string.app_manager_title),
            subtitle = stringResource(R.string.home_tool_apps_subtitle),
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenAppManager
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolRow(
            icon = Icons.Filled.DonutLarge,
            title = stringResource(R.string.home_tool_storage_title),
            subtitle = stringResource(R.string.home_tool_storage_subtitle),
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenStorageUsage
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolRow(
            icon = Icons.Filled.DeleteOutline,
            title = stringResource(R.string.trash_title),
            subtitle = if (trashCount == 0) {
                stringResource(R.string.trash_home_empty)
            } else {
                pluralStringResource(R.plurals.plural_trash_item_count, trashCount, trashCount)
            },
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenTrash
        )
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun compactPath(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
}
