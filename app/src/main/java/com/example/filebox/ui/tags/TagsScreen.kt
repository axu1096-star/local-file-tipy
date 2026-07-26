package com.example.filebox.ui.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filebox.R
import com.example.filebox.data.entity.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    onOpenTag: (Long) -> Unit,
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
        if (state.nodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.nodes, key = { it.tag.id }) { node ->
                    TagRow(
                        node = node,
                        showIndent = state.viewMode == TagsViewMode.TREE,
                        onToggleExpand = { viewModel.toggleExpanded(node.tag.id) },
                        onClickTag = { onOpenTag(node.tag.id) },
                        onAddChild = { creatingChildOf = node.tag },
                        onRename = { editing = node.tag },
                        onMove = { moving = node.tag },
                        onDelete = { deleting = node.tag }
                    )
                }
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
private fun TagRow(
    node: TagNode,
    showIndent: Boolean,
    onToggleExpand: () -> Unit,
    onClickTag: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val indent = if (showIndent) (node.depth * 20).dp else 0.dp
    Surface(
        onClick = onClickTag,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp + indent, end = 4.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIndent) {
                if (node.hasChildren) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (node.expanded) {
                                Icons.Filled.ExpandMore
                            } else {
                                Icons.Filled.ChevronRight
                            },
                            contentDescription = null
                        )
                    }
                } else {
                    Spacer(Modifier.width(32.dp))
                }
            }
            Text(
                text = node.tag.name,
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
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun TagRowMenu(
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
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
    // Compute the set of forbidden targets: the tag itself and all its descendants.
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
