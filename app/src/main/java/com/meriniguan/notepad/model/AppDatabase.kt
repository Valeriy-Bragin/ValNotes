package com.meriniguan.notepad.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meriniguan.notepad.model.image.room.ImageDbEntity
import com.meriniguan.notepad.model.note.room.NoteDao
import com.meriniguan.notepad.model.note.room.NoteDbEntity

@Database(entities = [NoteDbEntity::class, ImageDbEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getNoteDao(): NoteDao

}