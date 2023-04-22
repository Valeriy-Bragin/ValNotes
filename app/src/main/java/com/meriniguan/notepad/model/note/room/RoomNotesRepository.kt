package com.meriniguan.notepad.model.note.room

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.meriniguan.notepad.model.image.entities.Image
import com.meriniguan.notepad.model.image.room.tuples.DeleteImageTuple
import com.meriniguan.notepad.model.image.room.ImageDbEntity
import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.model.note.entities.NotesPageLoader
import com.meriniguan.notepad.model.note.entities.NotesPagingSource
import com.meriniguan.notepad.model.note.room.tuples.DeleteNoteTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteContentTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteLastModificationDateTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteSelectionTuple
import com.meriniguan.notepad.utils.Selection
import com.meriniguan.notepad.model.preferences.SortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomNotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val ioDispatcher: CoroutineDispatcher
) : NotesRepository {

    override fun getPagedNotes(searchQuery: String, sortOrder: SortOrder, selection: Selection): Flow<PagingData<Note>> {
        val loader: NotesPageLoader = { pageSize, pageIndex ->
            getNotes(pageSize, pageIndex, searchQuery, sortOrder, selection)
        }
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            pagingSourceFactory = { NotesPagingSource(loader) }
        ).flow
    }

    override suspend fun addNote(note: Note): Long = withContext(ioDispatcher) {
        return@withContext noteDao.addNote(NoteDbEntity.fromNote(note))
    }

    override suspend fun updateNote(updateNoteContentTuple: UpdateNoteContentTuple) =
        withContext(ioDispatcher) {
            noteDao.updateNoteContent(updateNoteContentTuple)
        }

    override suspend fun updateNoteLastModificationDate(noteId: Long) {
        noteDao.updateNoteDateUpdated(UpdateNoteLastModificationDateTuple(noteId, System.currentTimeMillis()))
    }

    override suspend fun deleteNote(noteId: Long) = withContext(ioDispatcher) {
        noteDao.deleteNote(DeleteNoteTuple(noteId))
    }

    override suspend fun deleteTrashedNotes() {
        noteDao.deleteTrashedNotes()
    }

    override suspend fun archiveNote(noteId: Long) = withContext(ioDispatcher) {
        noteDao.updateNoteSelection(
            UpdateNoteSelectionTuple(
                id = noteId,
                isArchived = true,
                isTrashed = false
            )
        )
    }

    override suspend fun trashNote(noteId: Long) = withContext(ioDispatcher) {
        noteDao.updateNoteSelection(
            UpdateNoteSelectionTuple(
                id = noteId,
                isArchived = false,
                isTrashed = true
            )
        )
    }

    override suspend fun restoreNote(noteId: Long) = withContext(ioDispatcher) {
        noteDao.updateNoteSelection(
            UpdateNoteSelectionTuple(
                id = noteId,
                isArchived = false,
                isTrashed = false
            )
        )
    }

    override fun getImagesByNoteId(noteId: Long):Flow<List<Image>> {
        return noteDao.getNoteById(noteId).map { noteWithImages ->
            try {
                noteWithImages.images.map { imageDbEntity ->
                    imageDbEntity.toImage()
                }
            } catch (e: NullPointerException) {
                emptyList()
            }
        }
    }

    override suspend fun addImage(image: Image) = withContext(ioDispatcher) {
        noteDao.addImage(ImageDbEntity.fromImage(image))
    }

    override suspend fun deleteImage(imageId: Long) = withContext(ioDispatcher) {
        noteDao.deleteImage(DeleteImageTuple(imageId))
    }

    private suspend fun getNotes(
        pageSize: Int,
        pageIndex: Int,
        searchQuery: String,
        sortOrder: SortOrder,
        selection: Selection
    ): List<Note> = withContext(ioDispatcher) {
        val offset = pageIndex * pageSize
        return@withContext noteDao.getNotes(pageSize, offset, searchQuery, sortOrder, selection)
            .map(NoteWithImages::toNote)
    }

    companion object {
        const val PAGE_SIZE = 80
    }
}