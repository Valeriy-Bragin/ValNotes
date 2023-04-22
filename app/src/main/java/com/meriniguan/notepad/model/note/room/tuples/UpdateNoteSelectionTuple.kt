package com.meriniguan.notepad.model.note.room.tuples

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class UpdateNoteSelectionTuple(
    @ColumnInfo(name = "note_id") @PrimaryKey val id: Long,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "is_trashed") val isTrashed: Boolean = false
)