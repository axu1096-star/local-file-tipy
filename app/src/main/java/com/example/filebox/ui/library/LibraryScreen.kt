package com.example.filebox.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.filebox.ui.LibraryFilter
import com.example.filebox.ui.common.BatchTagDialog
import com.example.filebox.ui.common.ExportProgressDialog
import com.example.filebox.ui.common.formatSize
import com.example.filebox.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    filter: LibraryFilter,
    onOpenFile: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    LaunchedEffect(filter) { viewModel.bind(filter) }
    val files by viewModel.files.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tagName by viewModel.tagName.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val export by viewModel.export.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri -> treeUri?.let { viewModel.saveSelectedTo(it) } }

    var showDelete by remember { mutableStateOf(false) }
    var tagDialogMode by remember { mutableStateOf<TagDialogMode?>(null) }

    LaunchedEffect(export.result) {
        val result = export.result ?: return@LaunchedEffect
        val msg = if (result.failed == 0) {
            context.getString(R.string.batch_save_done, result.success)
        } else {
            context.getString(R.string.batch_save_partial, result.success, result.failed)
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.consumeExportResult()
    }

    val title = when (filter) {
        is LibraryFilter.OfCategory -> stringResource(filter.category.labelRes())
        is LibraryFilter.OfTag -> tagName?.let { "#$it" } ?: "#"
        LibraryFilter.Untagged -> stringResource(R.string.library_no_tag)
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onExit = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onSave = { folderPicker.launch(null) },
                    onAddTag = { tagDialogMode = TagDialogMode.ADD },
                    onRemoveTag = { tagDialogMode = TagDialogMode.REMOVE },
                    onDelete = { showDelete = true }
                )
            } else {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
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
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.library_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.home_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = { it.file.id }) { fwt ->
                        FileRow(
                            name = fwt.file.displayName,
                            sizeLabel = formatSize(fwt.file.sizeBytes),
                            tags = fwt.tags.joinToString(" · ") { it.name },
                            categoryLabel = stringResource(fwt.file.category.labelRes()),
                            selectionMode = selectionMode,
                            selected = fwt.file.id in selectedIds,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(fwt.file.id)
                                } else {
                                    onOpenFile(fwt.file.id)
                                }
                            },
                            onLongClick = { viewModel.enterSelection(fwt.file.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.action_delete)) },
            text = {
                Text(stringResource(R.string.batch_delete_confirm, selectedIds.size))
            },
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

    ExportProgressDialog(export)
}

internal enum class TagDialogMode { ADD, REMOVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    onExit: () -> Unit,
    onSelectAll: () -> Unit,
    onSave: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.batch_selected, count)) },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.batch_exit))
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    Icons.Filled.SelectAll,
                    contentDescription = stringResource(R.string.batch_select_all)
                )
            }
            IconButton(enabled = count > 0, onClick = onSave) {
                Icon(
                    Icons.Filled.SaveAlt,
                    contentDescription = stringResource(R.string.batch_save)
                )
            }
            IconButton(enabled = count > 0, onClick = onAddTag) {
                Icon(
                    Icons.Filled.Label,
                    contentDescription = stringResource(R.string.batch_add_tag)
                )
            }
            IconButton(enabled = count > 0, onClick = onRemoveTag) {
                Icon(
                    Icons.Filled.LabelOff,
                    contentDescription = stringResource(R.string.batch_remove_tag)
                )
            }
            IconButton(enabled = count > 0, onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete)
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    name: String,
    sizeLabel: String,
    tags: String,
    categoryLabel: String,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (tags.isBlank()) "$categoryLabel · $sizeLabel"
                    else "$categoryLabel · $sizeLabel · $tags",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
