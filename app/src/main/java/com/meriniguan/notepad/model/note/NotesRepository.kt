package com.meriniguan.notepad.model.note

import androidx.paging.PagingData
import com.meriniguan.notepad.model.image.entities.Image
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteContentTuple
import com.meriniguan.notepad.utils.Selection
import com.meriniguan.notepad.model.preferences.SortOrder
import kotlinx.coroutines.flow.Flow

interface NotesRepository {

    fun getPagedNotes(searchQuery: String, sortOrder: SortOrder, selection: Selection): Flow<PagingData<Note>>

    suspend fun addNote(note: Note): Long

    suspend fun updateNote(updateNoteContentTuple: UpdateNoteContentTuple)

    suspend fun updateNoteLastModificationDate(noteId: Long)

    suspend fun updateNoteDateReminded(noteId: Long, dateReminded: Long)

    suspend fun deleteNote(noteId: Long)

    suspend fun deleteTrashedNotes()

    suspend fun archiveNote(noteId: Long)

    suspend fun trashNote(noteId: Long)

    suspend fun restoreNote(noteId: Long)

    fun getImagesByNoteId(noteId: Long): Flow<List<Image>>

    suspend fun addImage(image: Image)

    suspend fun deleteImage(imageId: Long)
}