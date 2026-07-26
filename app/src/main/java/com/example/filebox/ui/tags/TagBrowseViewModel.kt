package com.example.filebox.ui.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.data.entity.Tag
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TagBrowseUiState(
    val tagId: Long = 0L,
    val currentTag: Tag? = null,
    val parentTag: Tag? = null,
    val childTags: List<Tag> = emptyList(),
    val files: List<ManagedFile> = emptyList(),
    val selectedFile: ManagedFile? = null
)

@HiltViewModel
class TagBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fileRepo: FileRepository,
    tagRepo: TagRepository
) : ViewModel() {

    private val tagId: Long = savedStateHandle["tagId"] ?: 0L
    private val _selectedFileId = MutableStateFlow<Long?>(null)

    private val tags: StateFlow<List<Tag>> = tagRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filesWithTags: StateFlow<List<FileWithTags>> = fileRepo.observeAllWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TagBrowseUiState> =
        combine(tags, filesWithTags, _selectedFileId) { all, files, selectedId ->
            val current = all.firstOrNull { it.id == tagId }
            val parent = current?.parentId?.let { pid -> all.firstOrNull { it.id == pid } }
            val children = all
                .filter { it.parentId == tagId }
                .sortedBy { it.name.lowercase() }
            val direct = files
                .filter { fwt -> fwt.tags.any { it.id == tagId } }
                .map { it.file }
                .sortedByDescending { it.addedAt }
            val selected = selectedId?.let { id -> direct.firstOrNull { it.id == id } }
            TagBrowseUiState(
                tagId = tagId,
                currentTag = current,
                parentTag = parent,
                childTags = children,
                files = direct,
                selectedFile = selected
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TagBrowseUiState(tagId = tagId)
        )

    fun selectFile(id: Long?) {
        _selectedFileId.value = id
    }

    fun resolveFile(file: ManagedFile) = fileRepo.resolveFile(file)
}
