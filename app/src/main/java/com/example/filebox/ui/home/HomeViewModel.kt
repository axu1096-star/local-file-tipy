package com.example.filebox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.data.entity.Tag
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import com.example.filebox.domain.Category
import com.example.filebox.domain.FileExporter
import com.example.filebox.ui.common.ExportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val counts: Map<Category, Int> = emptyMap(),
    val totalCount: Int = 0,
    val recent: List<FileWithTags> = emptyList(),
    val rootTags: List<Tag> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FileRepository,
    tagRepo: TagRepository
) : ViewModel() {

    private val countsFlow = combine(
        Category.values().map { cat -> repo.countByCategory(cat) }
    ) { arr ->
        Category.values().mapIndexed { i, c -> c to arr[i] }.toMap()
    }

    private val rootTagsFlow = tagRepo.observeAll().map { all ->
        all.filter { it.parentId == null }.sortedBy { it.name.lowercase() }
    }

    val state: StateFlow<HomeUiState> = combine(
        countsFlow,
        repo.countAll(),
        repo.observeRecent(50),
        rootTagsFlow
    ) { counts, total, recent, rootTags ->
        HomeUiState(
            counts = counts,
            totalCount = total,
            recent = recent,
            rootTags = rootTags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val allTags: StateFlow<List<Tag>> = tagRepo.observeAll()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    fun enterSelection(id: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
        if (_selectedIds.value.isEmpty()) _selectionMode.value = false
    }

    fun selectAll() {
        _selectedIds.value = state.value.recent.map { it.file.id }.toSet()
        if (_selectedIds.value.isNotEmpty()) _selectionMode.value = true
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectionMode.value = false
    }

    private fun selectedFiles(): List<ManagedFile> {
        val ids = _selectedIds.value
        return state.value.recent.filter { it.file.id in ids }.map { it.file }
    }

    fun addTagToSelected(tagId: Long) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.addTagToFiles(ids, tagId)
            clearSelection()
        }
    }

    fun removeTagFromSelected(tagId: Long) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.removeTagFromFiles(ids, tagId)
            clearSelection()
        }
    }

    fun deleteSelected() {
        val targets = selectedFiles()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            repo.deleteAll(targets)
            clearSelection()
        }
    }

    private val _export = MutableStateFlow(ExportUiState())
    val export: StateFlow<ExportUiState> = _export.asStateFlow()

    fun saveSelectedTo(treeUri: android.net.Uri) {
        val targets = selectedFiles()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _export.value = ExportUiState(active = true, progress = FileExporter.Progress(0, targets.size, ""))
            val result = repo.exportTo(targets, treeUri) { p ->
                _export.value = _export.value.copy(progress = p)
            }
            _export.value = ExportUiState(active = false, result = result)
            clearSelection()
        }
    }

    fun consumeExportResult() {
        _export.value = _export.value.copy(result = null)
    }

    fun importUris(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEach { uri ->
                runCatching { repo.importUri(uri) }
            }
        }
    }
}
