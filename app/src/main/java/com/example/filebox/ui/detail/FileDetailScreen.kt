package com.example.filebox.ui.detail

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filebox.R
import com.example.filebox.domain.Category
import com.example.filebox.domain.PreviewType
import com.example.filebox.ui.common.formatSize
import com.example.filebox.ui.common.formatTime
import com.example.filebox.ui.common.icon
import com.example.filebox.ui.common.labelRes
import com.example.filebox.ui.detail.preview.AudioPreview
import com.example.filebox.ui.detail.preview.ImagePreview
import com.example.filebox.ui.detail.preview.TextPreview
import com.example.filebox.ui.detail.preview.VideoPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileDetailScreen(
    fileId: Long,
    onBack: () -> Unit,
    viewModel: FileDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val file = state.file

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { uri ->
            viewModel.exportTo(uri) { ok ->
                val msg = if (ok) {
                    context.getString(R.string.batch_save_done, 1)
                } else {
                    context.getString(R.string.batch_save_partial, 0, 1)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var noteDraft by remember(file?.file?.id) { mutableStateOf(file?.file?.note.orEmpty()) }
    LaunchedEffect(file?.file?.note) { noteDraft = file?.file?.note.orEmpty() }

    var showDelete by remember { mutableStateOf(false) }
    var showNewTag by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file?.file?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(enabled = file != null, onClick = {
                        folderPicker.launch(null)
                    }) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = stringResource(R.string.detail_save))
                    }
                    IconButton(enabled = file != null, onClick = {
                        val f = viewModel.resolveFile() ?: return@IconButton
                        ExternalOpen.share(context, f, file!!.file.mimeType)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                    }
                    IconButton(enabled = file != null, onClick = {
                        val f = viewModel.resolveFile() ?: return@IconButton
                        ExternalOpen.open(context, f, file!!.file.mimeType)
                    }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = stringResource(R.string.action_open))
                    }
                    IconButton(enabled = file != null, onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            )
        }
    ) { padding ->
        if (file == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("...") }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val resolved = remember(file.file.storedPath) { viewModel.resolveFile() }
            val previewType = PreviewType.of(file.file.mimeType, file.file.displayName)
            if (resolved != null && resolved.exists()) {
                when (previewType) {
                    PreviewType.IMAGE -> ImagePreview(
                        file = resolved,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                    PreviewType.VIDEO -> VideoPreview(resolved)
                    PreviewType.AUDIO -> AudioPreview(resolved)
                    PreviewType.TEXT -> Card(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) { TextPreview(resolved, Modifier.fillMaxSize()) }
                    PreviewType.NONE -> NoPreviewCard(file.file.category)
                }
            } else {
                NoPreviewCard(file.file.category)
            }

            Spacer(Modifier.height(16.dp))

            MetaRow(
                label = stringResource(R.string.detail_type),
                value = "${stringResource(file.file.category.labelRes())}  ·  ${file.file.mimeType}"
            )
            MetaRow(stringResource(R.string.detail_size), formatSize(file.file.sizeBytes))
            MetaRow(stringResource(R.string.detail_added), formatTime(file.file.addedAt))

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.action_edit_tags),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))

            val selectedIds = file.tags.map { it.id }.toSet()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.allTags.forEach { t ->
                    FilterChip(
                        selected = t.id in selectedIds,
                        onClick = { viewModel.toggleTag(t) },
                        label = { Text(t.name) }
                    )
                }
                AssistChip(
                    onClick = { showNewTag = true; newTagName = "" },
                    label = { Text("+ ${stringResource(R.string.tags_new)}") }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.detail_note), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                placeholder = { Text(stringResource(R.string.detail_note_hint)) },
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = { viewModel.updateNote(noteDraft) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.detail_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete(onDone = onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showNewTag) {
        AlertDialog(
            onDismissRequest = { showNewTag = false },
            title = { Text(stringResource(R.string.tags_new)) },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.tags_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newTagName.trim()
                    if (name.isNotEmpty()) viewModel.createTagAndAttach(name)
                    showNewTag = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewTag = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NoPreviewCard(category: Category) {
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_no_preview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
