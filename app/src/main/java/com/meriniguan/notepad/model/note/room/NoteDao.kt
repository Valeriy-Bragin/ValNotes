package com.meriniguan.notepad.model.note.room

import androidx.room.*
import com.meriniguan.notepad.model.image.room.tuples.DeleteImageTuple
import com.meriniguan.notepad.model.image.room.ImageDbEntity
import com.meriniguan.notepad.model.note.room.tuples.DeleteNoteTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteContentTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteLastModificationDateTuple
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteSelectionTuple
import com.meriniguan.notepad.utils.Selection
import com.meriniguan.notepad.model.preferences.SortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    fun getNotes(limit: Int, offset: Int, searchQuery: String, sortOrder: SortOrder, selection: Selection): List<NoteWithImages> =
        if (selection == Selection.NORMAL && sortOrder == SortOrder.BY_TITLE) {
            getNormalNotesSortedByTitle(limit, offset, searchQuery)
        } else if (selection == Selection.NORMAL && sortOrder == SortOrder.BY_DATE_CREATED) {
            getNormalNotesSortedByDateCreated(limit, offset, searchQuery)
        } else if (selection == Selection.NORMAL && sortOrder == SortOrder.BY_DATE_UPDATED) {
            getNormalNotesSortedByDateUpdated(limit, offset, searchQuery)
        }
        else if (selection == Selection.ARCHIVED && sortOrder == SortOrder.BY_TITLE) {
            getArchivedNotesSortedByTitle(limit, offset, searchQuery)
        } else if (selection == Selection.ARCHIVED && sortOrder == SortOrder.BY_DATE_CREATED) {
            getArchivedNotesSortedByDateCreated(limit, offset, searchQuery)
        } else if (selection == Selection.ARCHIVED && sortOrder == SortOrder.BY_DATE_UPDATED) {
            getArchivedNotesSortedByDateUpdated(limit, offset, searchQuery)
        }
        else if (selection == Selection.TRASHED && sortOrder == SortOrder.BY_TITLE) {
            getTrashedNotesSortedByTitle(limit, offset, searchQuery)
        } else if (selection == Selection.TRASHED && sortOrder == SortOrder.BY_DATE_CREATED) {
            getTrashedNotesSortedByDateCreated(limit, offset, searchQuery)
        } else if (selection == Selection.TRASHED && sortOrder == SortOrder.BY_DATE_UPDATED) {
            getTrashedNotesSortedByDateUpdated(limit, offset, searchQuery)
        }
        else {
            throw IllegalStateException(
                "Type of notes and sort principle are specified incorrectly."
            )
        }

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 0 AND is_archived = 0 " +
            "ORDER BY title " +
            "LIMIT :limit OFFSET :offset")
    fun getNormalNotesSortedByTitle(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 0 AND is_archived = 0 " +
            "ORDER BY date_created DESC " +
            "LIMIT :limit OFFSET :offset")
    fun getNormalNotesSortedByDateCreated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 0 AND is_archived = 0 " +
            "ORDER BY date_updated DESC " +
            "LIMIT :limit OFFSET :offset")
    fun getNormalNotesSortedByDateUpdated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_archived = 1 " +
            "ORDER BY title " +
            "LIMIT :limit OFFSET :offset")
    fun getArchivedNotesSortedByTitle(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_archived = 1 " +
            "ORDER BY date_created " +
            "LIMIT :limit OFFSET :offset")
    fun getArchivedNotesSortedByDateCreated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_archived = 1 " +
            "ORDER BY date_updated " +
            "LIMIT :limit OFFSET :offset")
    fun getArchivedNotesSortedByDateUpdated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 1 " +
            "ORDER BY title " +
            "LIMIT :limit OFFSET :offset")
    fun getTrashedNotesSortedByTitle(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 1 " +
            "ORDER BY date_created " +
            "LIMIT :limit OFFSET :offset")
    fun getTrashedNotesSortedByDateCreated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes " +
            "WHERE (text LIKE '%' || :searchQuery || '%' OR title LIKE '%' || :searchQuery || '%')" +
            "AND is_trashed = 1 " +
            "ORDER BY date_updated " +
            "LIMIT :limit OFFSET :offset")
    fun getTrashedNotesSortedByDateUpdated(limit: Int, offset: Int, searchQuery: String): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes")
    fun getAllNotes(): List<NoteWithImages>

    @Transaction
    @Query("SELECT * FROM notes WHERE note_id = :id")
    fun getNoteById(id: Long): Flow<NoteWithImages>

    @Insert
    suspend fun addNote(noteDbEntity: NoteDbEntity): Long

    @Update(entity = NoteDbEntity::class)
    suspend fun updateNoteContent(updateTuple: UpdateNoteContentTuple)

    @Update(entity = NoteDbEntity::class)
    suspend fun updateNoteSelection(updateSelectionTuple: UpdateNoteSelectionTuple)

    @Update(entity = NoteDbEntity::class)
    suspend fun updateNoteDateUpdated(updateTuple: UpdateNoteLastModificationDateTuple)

    @Delete(entity = NoteDbEntity::class)
    suspend fun deleteNote(deleteNoteTuple: DeleteNoteTuple)

    @Query("DELETE FROM notes WHERE is_trashed = 1")
    suspend fun deleteTrashedNotes()

    @Insert
    fun addImage(imageDbEntity: ImageDbEntity)

    @Delete(entity = ImageDbEntity::class)
    fun deleteImage(deleteTuple: DeleteImageTuple)
}