package com.example.filebox.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import com.example.filebox.domain.Category
import com.example.filebox.ui.LibraryFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LibraryUiState(
    val title: String = "",
    val query: String = "",
    val files: List<FileWithTags> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: FileRepository,
    private val tagRepo: TagRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow<LibraryFilter?>(null)

    fun bind(filter: LibraryFilter) {
        if (_filter.value != filter) _filter.value = filter
    }

    fun setQuery(q: String) { _query.value = q }

    val tagName: StateFlow<String?> = _filter
        .flatMapLatest { f ->
            when (f) {
                is LibraryFilter.OfTag -> tagRepo.observeAll().map { list ->
                    list.firstOrNull { it.id == f.tagId }?.name
                }
                else -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val files: StateFlow<List<FileWithTags>> = combine(_filter, _query) { f, q -> f to q }
        .flatMapLatest { (f, q) ->
            val base = when (f) {
                is LibraryFilter.OfCategory -> repo.observeByCategory(f.category)
                is LibraryFilter.OfTag -> repo.observeByTag(f.tagId)
                LibraryFilter.Untagged -> repo.observeUntagged()
                null -> repo.observeRecent(500)
            }
            if (q.isBlank()) base else repo.search(q.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
