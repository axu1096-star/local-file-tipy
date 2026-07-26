package com.example.filebox.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.Tag
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

data class TagNode(
    val tag: Tag,
    val depth: Int,
    val hasChildren: Boolean,
    val expanded: Boolean
)

data class TagsUiState(
    val viewMode: TagsViewMode = TagsViewMode.TREE,
    val nodes: List<TagNode> = emptyList(),
    val allTags: List<Tag> = emptyList()
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repo: TagRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(TagsViewMode.TREE)
    val viewMode: StateFlow<TagsViewMode> = _viewMode.asStateFlow()

    private val _expanded = MutableStateFlow<Set<Long>>(emptySet())

    private val tags: StateFlow<List<Tag>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TagsUiState> =
        combine(tags, _viewMode, _expanded) { all, mode, expanded ->
            val nodes = when (mode) {
                TagsViewMode.LIST -> buildFlatNodes(all)
                TagsViewMode.TREE -> buildTreeNodes(all, expanded)
            }
            TagsUiState(viewMode = mode, nodes = nodes, allTags = all)
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

    private fun buildFlatNodes(all: List<Tag>): List<TagNode> =
        all.sortedBy { it.name.lowercase() }
            .map { TagNode(it, depth = 0, hasChildren = false, expanded = false) }

    private fun buildTreeNodes(all: List<Tag>, expanded: Set<Long>): List<TagNode> {
        val byParent: Map<Long?, List<Tag>> = all
            .groupBy { it.parentId }
            .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
        val idSet = all.mapTo(mutableSetOf()) { it.id }
        val out = mutableListOf<TagNode>()

        fun visit(tag: Tag, depth: Int) {
            val children = byParent[tag.id].orEmpty()
            val isOpen = expanded.contains(tag.id)
            out += TagNode(
                tag = tag,
                depth = depth,
                hasChildren = children.isNotEmpty(),
                expanded = isOpen
            )
            if (isOpen) {
                children.forEach { visit(it, depth + 1) }
            }
        }

        // Roots: parentId == null, plus any orphaned tag whose parent no longer exists.
        val roots = byParent[null].orEmpty() +
            all.filter { it.parentId != null && it.parentId !in idSet }
                .sortedBy { it.name.lowercase() }
        roots.forEach { visit(it, 0) }
        return out
    }
}
