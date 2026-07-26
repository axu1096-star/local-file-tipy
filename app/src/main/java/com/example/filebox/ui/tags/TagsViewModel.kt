package com.example.filebox.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.Tag
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TagsViewMode { TREE, LIST }

data class TagRow(
    val tag: Tag,
    val depth: Int,
    val hasChildren: Boolean,
    val fileCount: Int,
    val expanded: Boolean
)

data class TagsUiState(
    val viewMode: TagsViewMode = TagsViewMode.TREE,
    val rows: List<TagRow> = emptyList(),
    val allTags: List<Tag> = emptyList()
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repo: TagRepository,
    fileRepo: FileRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(TagsViewMode.TREE)
    val viewMode: StateFlow<TagsViewMode> = _viewMode.asStateFlow()

    private val _expanded = MutableStateFlow<Set<Long>>(emptySet())

    private val tags: StateFlow<List<Tag>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filesWithTags: StateFlow<List<FileWithTags>> = fileRepo.observeAllWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TagsUiState> =
        combine(
            tags,
            filesWithTags,
            _viewMode,
            _expanded
        ) { all, files, mode, expanded ->
            val counts = countDirectFilesPerTag(files)
            val rows = when (mode) {
                TagsViewMode.LIST -> buildFlatRows(all, counts)
                TagsViewMode.TREE -> buildTreeRows(all, counts, expanded)
            }
            TagsUiState(
                viewMode = mode,
                rows = rows,
                allTags = all
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    fun setViewMode(mode: TagsViewMode) {
        _viewMode.value = mode
    }

    fun toggleExpanded(id: Long) {
        _expanded.value = _expanded.value.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
    }

    fun expandAll() {
        _expanded.value = tags.value.map { it.id }.toSet()
    }

    fun collapseAll() {
        _expanded.value = emptySet()
    }

    fun create(name: String, parentId: Long? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = repo.createOrGet(trimmed, parentId)
            if (parentId != null && id > 0) {
                _expanded.value = _expanded.value + parentId
            }
        }
    }

    fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.rename(id, trimmed) }
    }

    fun reparent(id: Long, newParentId: Long?) {
        viewModelScope.launch {
            val ok = repo.reparent(id, newParentId)
            if (ok && newParentId != null) {
                _expanded.value = _expanded.value + newParentId
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    private fun countDirectFilesPerTag(files: List<FileWithTags>): Map<Long, Int> {
        val out = HashMap<Long, Int>()
        files.forEach { fwt ->
            fwt.tags.forEach { t ->
                out[t.id] = (out[t.id] ?: 0) + 1
            }
        }
        return out
    }

    private fun buildFlatRows(all: List<Tag>, counts: Map<Long, Int>): List<TagRow> =
        all.sortedBy { it.name.lowercase() }
            .map {
                TagRow(
                    tag = it,
                    depth = 0,
                    hasChildren = false,
                    fileCount = counts[it.id] ?: 0,
                    expanded = false
                )
            }

    private fun buildTreeRows(
        all: List<Tag>,
        counts: Map<Long, Int>,
        expanded: Set<Long>
    ): List<TagRow> {
        val byParent: Map<Long?, List<Tag>> = all
            .groupBy { it.parentId }
            .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }

        val idSet = all.mapTo(mutableSetOf()) { it.id }
        val out = mutableListOf<TagRow>()

        fun visit(tag: Tag, depth: Int) {
            val children = byParent[tag.id].orEmpty()
            val hasChildren = children.isNotEmpty()
            val isOpen = expanded.contains(tag.id)
            out += TagRow(
                tag = tag,
                depth = depth,
                hasChildren = hasChildren,
                fileCount = counts[tag.id] ?: 0,
                expanded = isOpen
            )
            if (isOpen) {
                children.forEach { visit(it, depth + 1) }
            }
        }

        val roots = byParent[null].orEmpty() +
            all.filter { it.parentId != null && it.parentId !in idSet }
                .sortedBy { it.name.lowercase() }
        roots.forEach { visit(it, 0) }
        return out
    }
}
