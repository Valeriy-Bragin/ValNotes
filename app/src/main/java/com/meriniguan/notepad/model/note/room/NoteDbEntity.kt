package com.meriniguan.notepad.model.note.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.meriniguan.notepad.model.note.entities.Note

@Entity(tableName = "notes", indices = [Index("text")])
data class NoteDbEntity(
    val text: String,
    val title: String = "",
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "is_trashed") val isTrashed: Boolean = false,
    @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_updated") val dateUpdated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_reminded") val dateReminded: Long = 0,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "note_id") val id: Long = 0
) {
    companion object {
        fun fromNote(note: Note): NoteDbEntity = NoteDbEntity(
            text = note.text,
            title = note.title,
            isArchived = note.isArchived,
            isTrashed = note.isTrashed,
            dateCreated = note.dateCreated,
            dateUpdated = note.dateUpdated,
            dateReminded = note.dateReminded,
            id = note.id
        )
    }
}