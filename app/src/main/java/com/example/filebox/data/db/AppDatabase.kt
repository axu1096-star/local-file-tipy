package com.example.filebox.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.filebox.data.entity.FileTagCrossRef
import com.example.filebox.data.entity.ManagedFile
import com.example.filebox.data.entity.Tag

@Database(
    entities = [ManagedFile::class, Tag::class, FileTagCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun managedFileDao(): ManagedFileDao
    abstract fun tagDao(): TagDao

    companion object {
        const val NAME = "filebox.db"
    }
}
