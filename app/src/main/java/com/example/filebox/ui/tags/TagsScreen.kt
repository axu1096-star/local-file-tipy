package com.example.filebox.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filebox.R
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.data.entity.Tag
import com.example.filebox.domain.PreviewType
import com.example.filebox.ui.common.formatSize
import com.example.filebox.ui.common.formatTime
import com.example.filebox.ui.common.icon
import com.example.filebox.ui.common.labelRes
import com.example.filebox.ui.detail.ExternalOpen
import com.example.filebox.ui.detail.preview.AudioPreview
import com.example.filebox.ui.detail.preview.ImagePreview
import com.example.filebox.ui.detail.preview.TextPreview
import com.example.filebox.ui.detail.preview.VideoPreview
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    onOpenTag: (Long) -> Unit,
    onOpenFile: (Long) -> Unit,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showCreateRoot by remember { mutableStateOf(false) }
    var creatingChildOf by remember { mutableStateOf<Tag?>(null) }
    var editing by remember { mutableStateOf<Tag?>(null) }
    var deleting by remember { mutableStateOf<Tag?>(null) }
    var moving by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.viewMode == TagsViewMode.TREE) {
                        IconButton(onClick = { viewModel.expandAll() }) {
                            Icon(
                                Icons.Filled.UnfoldMore,
                                contentDescription = stringResource(R.string.tags_expand_all)
                            )
                        }
                        IconButton(onClick = { viewModel.collapseAll() }) {
                            Icon(
                                Icons.Filled.UnfoldLess,
                                contentDescription = stringResource(R.string.tags_collapse_all)
                            )
                        }
                    }
                    val nextMode = if (state.viewMode == TagsViewMode.TREE) {
                        TagsViewMode.LIST
                    } else {
                        TagsViewMode.TREE
                    }
                    IconButton(onClick = { viewModel.setViewMode(nextMode) }) {
                        Icon(
                            imageVector = if (state.viewMode == TagsViewMode.TREE) {
                                Icons.AutoMirrored.Filled.List
                            } else {
                                Icons.Filled.AccountTree
                            },
                            contentDescription = stringResource(
                                if (state.viewMode == TagsViewMode.TREE) {
                                    R.string.tags_view_list
                                } else {
                                    R.string.tags_view_tree
                                }
                            )
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateRoot = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.tags_new)) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.viewMode) {
                TagsViewMode.TREE -> TreePane(
                    state = state,
                    onToggleExpand = viewModel::toggleExpanded,
                    onClickTag = onOpenTag,
                    onAddChild = { creatingChildOf = it },
                    onRename = { editing = it },
                    onMove = { moving = it },
                    onDelete = { deleting = it },
                    onSelectFile = viewModel::selectFile,
                    onOpenFile = onOpenFile,
                    resolveFile = viewModel::resolveFile
                )
                TagsViewMode.LIST -> FlatList(
                    state = state,
                    onClickTag = onOpenTag,
                    onAddChild = { creatingChildOf = it },
                    onRename = { editing = it },
                    onMove = { moving = it },
                    onDelete = { deleting = it }
                )
            }
        }
    }

    if (showCreateRoot) {
        TagEditDialog(
            initial = "",
            title = stringResource(R.string.tags_new),
            onDismiss = { showCreateRoot = false },
            onConfirm = {
                viewModel.create(it, parentId = null)
                showCreateRoot = false
            }
        )
    }

    creatingChildOf?.let { parent ->
        TagEditDialog(
            initial = "",
            title = stringResource(R.string.tags_new_child_of, parent.name),
            onDismiss = { creatingChildOf = null },
            onConfirm = {
                viewModel.create(it, parentId = parent.id)
                creatingChildOf = null
            }
        )
    }

    editing?.let { t ->
        TagEditDialog(
            initial = t.name,
            title = stringResource(R.string.tags_rename),
            onDismiss = { editing = null },
            onConfirm = {
                viewModel.rename(t.id, it)
                editing = null
            }
        )
    }

    moving?.let { t ->
        MoveTagDialog(
            tag = t,
            allTags = state.allTags,
            onDismiss = { moving = null },
            onConfirm = { newParent ->
                viewModel.reparent(t.id, newParent)
                moving = null
            }
        )
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.tags_delete_confirm, t.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(t.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TreePane(
    state: TagsUiState,
    onToggleExpand: (Long) -> Unit,
    onClickTag: (Long) -> Unit,
    onAddChild: (Tag) -> Unit,
    onRename: (Tag) -> Unit,
    onMove: (Tag) -> Unit,
    onDelete: (Tag) -> Unit,
    onSelectFile: (Long?) -> Unit,
    onOpenFile: (Long) -> Unit,
    resolveFile: (ManagedFile) -> File
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        ) {
            if (state.rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tags_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state.rows, key = { row -> rowKey(row) }) { row ->
                        when (row) {
                            is TagsRow.TagRow -> TreeTagRow(
                                row = row,
                                onToggleExpand = { onToggleExpand(row.tag.id) },
                                onClickTag = { onClickTag(row.tag.id) },
                                onAddChild = { onAddChild(row.tag) },
                                onRename = { onRename(row.tag) },
                                onMove = { onMove(row.tag) },
                                onDelete = { onDelete(row.tag) }
                            )
                            is TagsRow.FileRow -> TreeFileRow(
                                file = row.file,
                                depth = row.depth,
                                selected = state.selectedFile?.id == row.file.id,
                                onSelect = { onSelectFile(row.file.id) },
                                onOpen = { onOpenFile(row.file.id) }
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        ) {
            PreviewPane(
                file = state.selectedFile,
                onOpenFile = onOpenFile,
                resolveFile = resolveFile
            )
        }
    }
}

private fun rowKey(row: TagsRow): String = when (row) {
    is TagsRow.TagRow -> "t:${row.tag.id}"
    is TagsRow.FileRow -> "f:${row.file.id}"
}

@Composable
private fun TreeTagRow(
    row: TagsRow.TagRow,
    onToggleExpand: () -> Unit,
    onClickTag: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val indent = (row.depth * 16).dp
    Surface(
        onClick = onClickTag,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indent + 4.dp, end = 2.dp)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (row.hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (row.expanded) {
                            Icons.Filled.ExpandMore
                        } else {
                            Icons.Filled.ChevronRight
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }
            Text(
                text = row.tag.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            if (row.fileCount > 0) {
                Text(
                    text = row.fileCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            IconButton(
                onClick = onAddChild,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.tags_add_child),
                    modifier = Modifier.size(16.dp)
                )
            }
            TagRowMenu(
                onAddChild = onAddChild,
                onRename = onRename,
                onMove = onMove,
                onDelete = onDelete,
                compact = true
            )
        }
    }
}

@Composable
private fun TreeFileRow(
    file: ManagedFile,
    depth: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val indent = (depth * 16).dp
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        onClick = onSelect,
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indent + 32.dp, end = 4.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = file.category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onOpen,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.action_open),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewPane(
    file: ManagedFile?,
    onOpenFile: (Long) -> Unit,
    resolveFile: (ManagedFile) -> File
) {
    if (file == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tags_preview_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val context = LocalContext.current
    val resolved = remember(file.id, file.storedPath) { resolveFile(file) }
    val previewType = PreviewType.of(file.mimeType, file.displayName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = file.category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onOpenFile(file.id) }) {
                Icon(
                    Icons.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.action_open)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${stringResource(file.category.labelRes())}  ·  ${formatSize(file.sizeBytes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        val previewModifier = Modifier
            .fillMaxWidth()
            .weight(1f)

        if (resolved.exists()) {
            when (previewType) {
                PreviewType.IMAGE -> ImagePreview(
                    file = resolved,
                    modifier = previewModifier
                )
                PreviewType.VIDEO -> Box(modifier = previewModifier) {
                    VideoPreview(resolved)
                }
                PreviewType.AUDIO -> Box(modifier = previewModifier) {
                    AudioPreview(resolved)
                }
                PreviewType.TEXT -> TextPreview(resolved, previewModifier)
                PreviewType.NONE -> NoPreviewCompact(
                    onOpen = {
                        ExternalOpen.open(context, resolved, file.mimeType)
                    },
                    modifier = previewModifier
                )
            }
        } else {
            NoPreviewCompact(
                onOpen = { onOpenFile(file.id) },
                modifier = previewModifier
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = formatTime(file.addedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoPreviewCompact(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_no_preview),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpen) {
                Text(stringResource(R.string.action_open))
            }
        }
    }
}

@Composable
private fun FlatList(
    state: TagsUiState,
    onClickTag: (Long) -> Unit,
    onAddChild: (Tag) -> Unit,
    onRename: (Tag) -> Unit,
    onMove: (Tag) -> Unit,
    onDelete: (Tag) -> Unit
) {
    if (state.rows.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tags_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(state.rows, key = { row -> rowKey(row) }) { row ->
            when (row) {
                is TagsRow.TagRow -> FlatTagRow(
                    tag = row.tag,
                    onClick = { onClickTag(row.tag.id) },
                    onAddChild = { onAddChild(row.tag) },
                    onRename = { onRename(row.tag) },
                    onMove = { onMove(row.tag) },
                    onDelete = { onDelete(row.tag) }
                )
                is TagsRow.FileRow -> Unit
            }
        }
    }
}

@Composable
private fun FlatTagRow(
    tag: Tag,
    onClick: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
            TagRowMenu(
                onAddChild = onAddChild,
                onRename = onRename,
                onMove = onMove,
                onDelete = onDelete,
                compact = false
            )
        }
    }
}

@Composable
private fun TagRowMenu(
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    compact: Boolean
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { open = true },
            modifier = if (compact) Modifier.size(28.dp) else Modifier
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                modifier = if (compact) Modifier.size(18.dp) else Modifier
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tags_add_child)) },
                leadingIcon = { Icon(Icons.Filled.SubdirectoryArrowRight, null) },
                onClick = { open = false; onAddChild() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tags_rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { open = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tags_move)) },
                leadingIcon = { Icon(Icons.Filled.DriveFileMove, null) },
                onClick = { open = false; onMove() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

@Composable
private fun TagEditDialog(
    initial: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(stringResource(R.string.tags_name)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun MoveTagDialog(
    tag: Tag,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val forbidden = remember(tag, allTags) {
        val byParent = allTags.groupBy { it.parentId }
        val out = mutableSetOf(tag.id)
        val stack = ArrayDeque<Long>()
        stack.addLast(tag.id)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            byParent[current].orEmpty().forEach {
                if (out.add(it.id)) stack.addLast(it.id)
            }
        }
        out
    }
    val candidates = remember(allTags, forbidden) {
        allTags.filter { it.id !in forbidden }.sortedBy { it.name.lowercase() }
    }
    var selection by remember { mutableStateOf<Long?>(tag.parentId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_move_title, tag.name)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                item {
                    ParentOption(
                        label = stringResource(R.string.tags_no_parent),
                        selected = selection == null,
                        onClick = { selection = null }
                    )
                }
                items(candidates, key = { it.id }) { t ->
                    ParentOption(
                        label = t.name,
                        selected = selection == t.id,
                        onClick = { selection = t.id }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun ParentOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
