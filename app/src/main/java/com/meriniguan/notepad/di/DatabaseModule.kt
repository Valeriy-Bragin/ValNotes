package com.meriniguan.notepad.di

import android.app.Application
import androidx.room.Room
import com.meriniguan.notepad.model.AppDatabase
import com.meriniguan.notepad.model.note.room.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application
    ): AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "app_database")
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideNoteDao(
        appDatabase: AppDatabase
    ): NoteDao = appDatabase.getNoteDao()
}