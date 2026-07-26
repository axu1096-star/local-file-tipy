package com.example.filebox.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.filebox.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun findById(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name AND parent_id IS :parentId LIMIT 1")
    suspend fun findByNameInParent(name: String, parentId: Long?): Tag?

    @Query("UPDATE tags SET parent_id = :parentId WHERE id = :id")
    suspend fun updateParent(id: Long, parentId: Long?)
}
