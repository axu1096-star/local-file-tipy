package com.example.filebox.data.repo

import com.example.filebox.data.db.ManagedFileDao
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.domain.Category
import com.example.filebox.domain.FileExporter
import com.example.filebox.domain.FileImporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val dao: ManagedFileDao,
    private val importer: FileImporter,
    private val exporter: FileExporter
) {
    fun observeRecent(limit: Int = 20): Flow<List<FileWithTags>> = dao.observeRecent(limit)
    fun observeAllWithTags(): Flow<List<FileWithTags>> = dao.observeAllWithTags()
    fun observeByCategory(category: Category): Flow<List<FileWithTags>> =
        dao.observeByCategory(category)
    fun observeByTag(tagId: Long): Flow<List<FileWithTags>> = dao.observeByTag(tagId)
    fun observeUntagged(): Flow<List<FileWithTags>> = dao.observeUntagged()
    fun observeWithTags(id: Long): Flow<FileWithTags?> =
        dao.observeWithTags(id).map { it.firstOrNull() }
    fun countByCategory(category: Category): Flow<Int> = dao.countByCategory(category)
    fun countAll(): Flow<Int> = dao.countAll()
    fun search(query: String): Flow<List<FileWithTags>> = dao.search(query)

    suspend fun updateNote(id: Long, note: String?) {
        val current = dao.findById(id) ?: return
        dao.update(current.copy(note = note))
    }

    suspend fun replaceTags(fileId: Long, tagIds: List<Long>) {
        dao.replaceTagsFor(fileId, tagIds)
    }

    suspend fun addTagToFiles(fileIds: List<Long>, tagId: Long) {
        fileIds.forEach { fileId ->
            dao.addTagCrossRef(com.example.filebox.data.entity.FileTagCrossRef(fileId, tagId))
        }
    }

    suspend fun removeTagFromFiles(fileIds: List<Long>, tagId: Long) {
        fileIds.forEach { fileId ->
            dao.removeTagCrossRef(fileId, tagId)
        }
    }

    suspend fun delete(file: ManagedFile) {
        dao.deleteById(file.id)
        runCatching { importer.resolveFile(file.storedPath).delete() }
    }

    suspend fun deleteAll(files: List<ManagedFile>) {
        files.forEach { delete(it) }
    }

    suspend fun importUri(uri: android.net.Uri): Long = importer.import(uri)

    suspend fun exportTo(
        files: List<ManagedFile>,
        treeUri: android.net.Uri,
        onProgress: (FileExporter.Progress) -> Unit = {}
    ): FileExporter.Result = exporter.exportTo(files, treeUri, onProgress)

    fun resolveFile(file: ManagedFile) = importer.resolveFile(file.storedPath)
}
