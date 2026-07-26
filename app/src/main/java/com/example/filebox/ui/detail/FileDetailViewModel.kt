package com.example.filebox.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.Tag
import com.example.filebox.data.repo.FileRepository
import com.example.filebox.data.repo.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val file: FileWithTags? = null,
    val allTags: List<Tag> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fileRepo: FileRepository,
    private val tagRepo: TagRepository
) : ViewModel() {

    private val fileId: Long = savedStateHandle["fileId"] ?: 0L
    private val idFlow = MutableStateFlow(fileId)

    val state: StateFlow<DetailUiState> = idFlow
        .flatMapLatest { id ->
            combine(fileRepo.observeWithTags(id), tagRepo.observeAll()) { f, tags ->
                DetailUiState(file = f, allTags = tags)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleTag(tag: Tag) {
        val current = state.value.file ?: return
        val ids = current.tags.map { it.id }.toMutableSet()
        if (!ids.add(tag.id)) ids.remove(tag.id)
        viewModelScope.launch { fileRepo.replaceTags(current.file.id, ids.toList()) }
    }

    fun setTags(ids: List<Long>) {
        val current = state.value.file ?: return
        viewModelScope.launch { fileRepo.replaceTags(current.file.id, ids) }
    }

    fun createTagAndAttach(name: String) {
        viewModelScope.launch {
            val current = state.value.file ?: return@launch
            val tagId = tagRepo.createOrGet(name)
            if (tagId <= 0) return@launch
            val ids = current.tags.map { it.id }.toMutableSet().apply { add(tagId) }
            fileRepo.replaceTags(current.file.id, ids.toList())
        }
    }

    fun updateNote(note: String) {
        val current = state.value.file ?: return
        viewModelScope.launch { fileRepo.updateNote(current.file.id, note.ifBlank { null }) }
    }

    fun delete(onDone: () -> Unit) {
        val current = state.value.file ?: return
        viewModelScope.launch {
            fileRepo.delete(current.file)
            onDone()
        }
    }

    fun resolveFile() = state.value.file?.let { fileRepo.resolveFile(it.file) }

    fun exportTo(treeUri: android.net.Uri, onResult: (Boolean) -> Unit) {
        val current = state.value.file ?: return
        viewModelScope.launch {
            val result = fileRepo.exportTo(listOf(current.file), treeUri)
            onResult(result.success > 0)
        }
    }
}
