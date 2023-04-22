package com.meriniguan.notepad.model.note.room.tuples

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class UpdateNoteContentTuple(
    @ColumnInfo(name = "note_id") @PrimaryKey val id: Long,
    val text: String,
    val title: String,
    @ColumnInfo(name = "date_updated") val dateUpdated: Long
)