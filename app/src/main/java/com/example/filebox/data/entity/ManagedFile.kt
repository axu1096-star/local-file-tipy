package com.example.filebox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.filebox.domain.Category

@Entity(
    tableName = "managed_files",
    indices = [Index("category"), Index("added_at"), Index("display_name")]
)
data class ManagedFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "stored_path") val storedPath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val category: Category,
    @ColumnInfo(name = "source_uri") val sourceUri: String?,
    val note: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long
)
