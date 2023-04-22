package com.meriniguan.notepad.di

import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.room.RoomNotesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindNotesRepository(
        notesRepository: RoomNotesRepository
    ): NotesRepository
}