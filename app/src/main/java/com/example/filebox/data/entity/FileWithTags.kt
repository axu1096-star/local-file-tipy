package com.example.filebox.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class FileWithTags(
    @Embedded val file: ManagedFile,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = FileTagCrossRef::class,
            parentColumn = "file_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<Tag>
)
