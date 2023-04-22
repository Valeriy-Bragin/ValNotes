package com.meriniguan.notepad.model.note.room

import androidx.room.Embedded
import androidx.room.Relation
import com.meriniguan.notepad.model.image.room.ImageDbEntity
import com.meriniguan.notepad.model.note.entities.Note

data class NoteWithImages(
    @Embedded val note: NoteDbEntity,
    @Relation(
        parentColumn = "note_id",
        entityColumn = "note_holder_id"
    )
    val images: List<ImageDbEntity>
) {

    fun toNote(): Note = Note(
        text = note.text,
        title = note.title,
        firstImageUri = if (images.isNotEmpty()) images.first().uri else "empty",
        isArchived = note.isArchived,
        isTrashed = note.isTrashed,
        dateCreated = note.dateCreated,
        dateUpdated = note.dateUpdated,
        id = note.id
    )
}
