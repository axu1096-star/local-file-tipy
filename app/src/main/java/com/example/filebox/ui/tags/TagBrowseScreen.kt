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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun TagBrowseScreen(
    onBack: () -> Unit,
    onOpenChildTag: (Long) -> Unit,
    onOpenFile: (Long) -> Unit,
    viewModel: TagBrowseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateChild by remember { mutableStateOf(false) }
    var listVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.currentTag?.name
                            ?: stringResource(R.string.tags_browse_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { listVisible = !listVisible }) {
                        Icon(
                            imageVector = if (listVisible) {
                                Icons.Filled.MenuOpen
                            } else {
                                Icons.Filled.ViewSidebar
                            },
                            contentDescription = stringResource(
                                if (listVisible) R.string.tags_browse_hide_list
                                else R.string.tags_browse_show_list
                            )
                        )
                    }
                    IconButton(onClick = { showCreateChild = true }) {
                        Icon(
                            Icons.Filled.CreateNewFolder,
                            contentDescription = stringResource(R.string.tags_add_child)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (listVisible) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                ) {
                    BrowseList(
                        state = state,
                        onOpenChildTag = onOpenChildTag,
                        onSelectFile = viewModel::selectFile,
                        onOpenFile = onOpenFile
                    )
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight())
            }
            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
            ) {
                PreviewPane(
                    file = state.selectedFile,
                    onOpenFile = onOpenFile,
                    resolveFile = viewModel::resolveFile
                )
            }
        }
    }

    if (showCreateChild) {
        CreateChildTagDialog(
            title = state.currentTag?.let {
                stringResource(R.string.tags_new_child_of, it.name)
            } ?: stringResource(R.string.tags_add_child),
            onDismiss = { showCreateChild = false },
            onConfirm = {
                viewModel.createChild(it)
                showCreateChild = false
            }
        )
    }
}

@Composable
private fun CreateChildTagDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
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
    state: TagBrowseUiState,
    onOpenChildTag: (Long) -> Unit,
    onSelectFile: (Long?) -> Unit,
    onOpenFile: (Long) -> Unit
) {
    if (state.childTags.isEmpty() && state.files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tags_browse_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (state.childTags.isNotEmpty()) {
            items(state.childTags, key = { "t:${it.id}" }) { tag ->
                ChildTagRow(tag = tag, onClick = { onOpenChildTag(tag.id) })
            }
        }
        if (state.childTags.isNotEmpty() && state.files.isNotEmpty()) {
            item(key = "divider") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        if (state.files.isNotEmpty()) {
            items(state.files, key = { "f:${it.id}" }) { file ->
                FileRowItem(
                    file = file,
                    selected = state.selectedFile?.id == file.id,
                    onSelect = { onSelectFile(file.id) },
                    onOpen = { onOpenFile(file.id) }
                )
            }
        }
    }
}

@Composable
private fun ChildTagRow(tag: Tag, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FileRowItem(
    file: ManagedFile,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = file.category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
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
