package com.example.filebox.di

import android.content.Context
import androidx.room.Room
import com.example.filebox.data.db.AppDatabase
import com.example.filebox.data.db.ManagedFileDao
import com.example.filebox.data.db.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFileDao(db: AppDatabase): ManagedFileDao = db.managedFileDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}
