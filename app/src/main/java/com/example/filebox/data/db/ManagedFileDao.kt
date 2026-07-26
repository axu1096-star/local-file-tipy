package com.example.filebox.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.filebox.data.entity.FileTagCrossRef
import com.example.filebox.data.entity.FileWithTags
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.domain.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedFileDao {

    @Insert
    suspend fun insert(file: ManagedFile): Long

    @Update
    suspend fun update(file: ManagedFile)

    @Query("DELETE FROM managed_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM managed_files WHERE id = :id")
    suspend fun findById(id: Long): ManagedFile?

    @Transaction
    @Query("SELECT * FROM managed_files WHERE id = :id LIMIT 1")
    fun observeWithTags(id: Long): Flow<List<FileWithTags>>

    @Transaction
    @Query("SELECT * FROM managed_files ORDER BY added_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FileWithTags>>

    @Transaction
    @Query("SELECT * FROM managed_files ORDER BY added_at DESC")
    fun observeAllWithTags(): Flow<List<FileWithTags>>

    @Transaction
    @Query("SELECT * FROM managed_files WHERE category = :category ORDER BY added_at DESC")
    fun observeByCategory(category: Category): Flow<List<FileWithTags>>

    @Transaction
    @Query(
        "SELECT mf.* FROM managed_files mf " +
            "INNER JOIN file_tag_cross_ref x ON x.file_id = mf.id " +
            "WHERE x.tag_id = :tagId ORDER BY mf.added_at DESC"
    )
    fun observeByTag(tagId: Long): Flow<List<FileWithTags>>

    @Transaction
    @Query(
        "SELECT mf.* FROM managed_files mf " +
            "WHERE mf.id NOT IN (SELECT file_id FROM file_tag_cross_ref) " +
            "ORDER BY mf.added_at DESC"
    )
    fun observeUntagged(): Flow<List<FileWithTags>>

    @Query("SELECT COUNT(*) FROM managed_files WHERE category = :category")
    fun countByCategory(category: Category): Flow<Int>

    @Query("SELECT COUNT(*) FROM managed_files")
    fun countAll(): Flow<Int>

    @Transaction
    @Query(
        "SELECT mf.* FROM managed_files mf " +
            "WHERE mf.display_name LIKE '%' || :query || '%' " +
            "OR mf.id IN (SELECT x.file_id FROM file_tag_cross_ref x " +
            "  INNER JOIN tags t ON t.id = x.tag_id " +
            "  WHERE t.name LIKE '%' || :query || '%') " +
            "ORDER BY mf.added_at DESC"
    )
    fun search(query: String): Flow<List<FileWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagCrossRef(ref: FileTagCrossRef)

    @Query("DELETE FROM file_tag_cross_ref WHERE file_id = :fileId AND tag_id = :tagId")
    suspend fun removeTagCrossRef(fileId: Long, tagId: Long)

    @Query("DELETE FROM file_tag_cross_ref WHERE file_id = :fileId")
    suspend fun clearTagsFor(fileId: Long)

    @Transaction
    suspend fun replaceTagsFor(fileId: Long, tagIds: List<Long>) {
        clearTagsFor(fileId)
        tagIds.forEach { addTagCrossRef(FileTagCrossRef(fileId, it)) }
    }
}
