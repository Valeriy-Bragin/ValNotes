package com.meriniguan.notepad.model.note.room.tuples

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class DeleteNoteTuple(
    @ColumnInfo(name = "note_id") @PrimaryKey val id: Long
)