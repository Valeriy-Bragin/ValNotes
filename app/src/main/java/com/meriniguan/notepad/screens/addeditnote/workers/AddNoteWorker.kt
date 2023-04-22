package com.meriniguan.notepad.screens.addeditnote.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.screens.addeditnote.NOTE_UPDATED_REQUEST_KEY
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AddNoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: NotesRepository
)  :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val title = inputData.getString("title") ?: ""
            val content = inputData.getString("content") ?: ""
            val id = repository.addNote(Note(title = title, text = content))

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}