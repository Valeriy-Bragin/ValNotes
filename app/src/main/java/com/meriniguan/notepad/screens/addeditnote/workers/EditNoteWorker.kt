package com.meriniguan.notepad.screens.addeditnote.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteContentTuple
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class EditNoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: NotesRepository
)  : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val title = inputData.getString("title") ?: ""
            val content = inputData.getString("content") ?: ""
            val id = inputData.getLong("id", -1)
            Log.i("!@#workerin", "$title $content $id")
            repository.updateNote(
                UpdateNoteContentTuple(
                    id = id,
                    title = title,
                    text = content,
                    dateUpdated = System.currentTimeMillis(),
                )
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}