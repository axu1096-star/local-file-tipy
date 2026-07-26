package com.example.filebox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "file_tag_cross_ref",
    primaryKeys = ["file_id", "tag_id"],
    indices = [Index("tag_id"), Index("file_id")],
    foreignKeys = [
        ForeignKey(
            entity = ManagedFile::class,
            parentColumns = ["id"],
            childColumns = ["file_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FileTagCrossRef(
    @ColumnInfo(name = "file_id") val fileId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
