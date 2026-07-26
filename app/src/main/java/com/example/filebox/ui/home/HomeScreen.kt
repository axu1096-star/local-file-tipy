package com.example.filebox.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filebox.R
import com.example.filebox.data.entity.Tag
import com.example.filebox.domain.Category
import com.example.filebox.ui.common.BatchTagDialog
import com.example.filebox.ui.common.formatSize
import com.example.filebox.ui.common.icon
import com.example.filebox.ui.common.labelRes
import com.example.filebox.ui.library.SelectionTopBar
import com.example.filebox.ui.library.TagDialogMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCategory: (Category) -> Unit,
    onOpenFile: (Long) -> Unit,
    onOpenTags: () -> Unit,
    onOpenTag: (Long) -> Unit,
    onOpenUntagged: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importUris(uris)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showDelete by remember { mutableStateOf(false) }
    var tagDialogMode by remember { mutableStateOf<TagDialogMode?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(4.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_home)) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_tags)) },
                    icon = { Icon(Icons.Filled.Label, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenTags()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_tools)) },
                    icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenTools()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_settings)) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        HomeContent(
            state = state,
            selectionMode = selectionMode,
            selectedIds = selectedIds,
            onOpenMenu = { scope.launch { drawerState.open() } },
            onOpenCategory = onOpenCategory,
            onOpenFile = onOpenFile,
            onOpenTags = onOpenTags,
            onOpenTag = onOpenTag,
            onPickFiles = { picker.launch(arrayOf("*/*")) },
            onEnterSelection = viewModel::enterSelection,
            onToggleSelection = viewModel::toggleSelection,
            onExitSelection = viewModel::clearSelection,
            onSelectAll = viewModel::selectAll,
            onAddTag = { tagDialogMode = TagDialogMode.ADD },
            onRemoveTag = { tagDialogMode = TagDialogMode.REMOVE },
            onDelete = { showDelete = true }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.batch_delete_confirm, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteSelected()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    tagDialogMode?.let { mode ->
        BatchTagDialog(
            title = stringResource(
                if (mode == TagDialogMode.ADD) R.string.batch_pick_tag_add
                else R.string.batch_pick_tag_remove
            ),
            tags = allTags,
            onPick = { tagId ->
                if (mode == TagDialogMode.ADD) viewModel.addTagToSelected(tagId)
                else viewModel.removeTagFromSelected(tagId)
                tagDialogMode = null
            },
            onDismiss = { tagDialogMode = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onOpenMenu: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenFile: (Long) -> Unit,
    onOpenTags: () -> Unit,
    onOpenTag: (Long) -> Unit,
    onPickFiles: () -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onExit = onExitSelection,
                    onSelectAll = onSelectAll,
                    onAddTag = onAddTag,
                    onRemoveTag = onRemoveTag,
                    onDelete = onDelete
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_title)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.home_menu_open)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenTags) {
                            Icon(Icons.Filled.Label, contentDescription = stringResource(R.string.tags_title))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onPickFiles,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.home_add_files)) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(
                title = stringResource(R.string.home_tags),
                trailing = null,
                onTrailingClick = onOpenTags
            )
            Spacer(Modifier.height(6.dp))
            TagsRow(
                tags = state.rootTags,
                onOpenTag = onOpenTag,
                onOpenAll = onOpenTags
            )

            Spacer(Modifier.height(12.dp))
            SectionHeader(
                title = stringResource(R.string.home_categories),
                trailing = null
            )
            Spacer(Modifier.height(6.dp))
            CategoriesRow(
                counts = state.counts,
                onOpenCategory = onOpenCategory
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_recent),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "(${state.totalCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            if (state.recent.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.recent, key = { it.file.id }) { fwt ->
                        RecentRow(
                            name = fwt.file.displayName,
                            sizeLabel = formatSize(fwt.file.sizeBytes),
                            category = fwt.file.category,
                            selectionMode = selectionMode,
                            selected = fwt.file.id in selectedIds,
                            onClick = {
                                if (selectionMode) onToggleSelection(fwt.file.id)
                                else onOpenFile(fwt.file.id)
                            },
                            onLongClick = { onEnterSelection(fwt.file.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String?,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun TagsRow(
    tags: List<Tag>,
    onOpenTag: (Long) -> Unit,
    onOpenAll: () -> Unit
) {
    if (tags.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAll),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = stringResource(R.string.home_no_tags),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        return
    }
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.forEach { tag ->
            val color = tag.colorArgb?.let { Color(it) }
            AssistChip(
                onClick = { onOpenTag(tag.id) },
                label = {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (color != null) {
                    {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                    }
                } else null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun CategoriesRow(
    counts: Map<Category, Int>,
    onOpenCategory: (Category) -> Unit
) {
    val cats = remember { Category.values().toList() }
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cats.forEach { cat ->
            CompactCategoryChip(
                category = cat,
                count = counts[cat] ?: 0,
                onClick = { onOpenCategory(cat) }
            )
        }
    }
}

@Composable
private fun CompactCategoryChip(
    category: Category,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(category.labelRes()),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    name: String,
    sizeLabel: String,
    category: Category,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
