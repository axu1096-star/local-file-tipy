package com.example.filebox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.Tag
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import com.example.filebox.domain.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun importUris(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEach { uri ->
                runCatching { repo.importUri(uri) }
            }
        }
    }
}
