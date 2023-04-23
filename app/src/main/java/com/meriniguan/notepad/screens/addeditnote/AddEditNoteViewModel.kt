package com.meriniguan.notepad.screens.addeditnote

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.work.*
import com.meriniguan.notepad.R
import com.meriniguan.notepad.EMPTY_NOTE_DISCARDED_RESULT_OK
import com.meriniguan.notepad.NOTE_UPDATED_RESULT_OK
import com.meriniguan.notepad.model.image.entities.Image
import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.model.note.room.tuples.UpdateNoteContentTuple
import com.meriniguan.notepad.screens.addeditnote.adapters.ImagesAdapter
import com.meriniguan.notepad.screens.addeditnote.workers.EditNoteWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val NOTE_UPDATED_REQUEST_KEY = "note_updated"
const val NOTE_UPDATED_RESULT_KEY = "note_updated_result"

const val EMPTY_NOTE_DISCARDED_REQUEST_KEY = "empty_note_discarded"
const val EMPTY_NOTE_DISCARDED_RESULT_KEY = "empty_note_discarded_result"

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val repository: NotesRepository,
    application: Application,
    private val state: SavedStateHandle
) : ViewModel(), ImagesAdapter.OnImageClickListener {

    private var newlyAddedNoteId: Long? = null
    private var note: Note = Note("")
    private var addImageAtLaunch: Boolean = state["addImageAtLaunch"] ?: false

    var imagesFlow: Flow<List<Image>> = emptyFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private var originalTitle = ""
    private var originalContent = ""

    init {
        viewModelScope.launch {
            val initNoteJob = initNote()
            initNoteJob.join()

            initOriginalTextValues()

            val initImagesJob = initImages()
            initImagesJob.join()

            if (isAdding && addImageAtLaunch) {
                showCaptureImageWithCameraScreen()
            }

            startObservingImages()
        }
    }

    private fun initOriginalTextValues() {
        originalTitle = state.get<String>("title") ?: note.title
        originalContent = state.get<String>("content") ?: note.text
    }

    var title = originalTitle
        set(value) {
            field = value
            state["title"] = value
        }

    var content = originalContent
        set(value) {
            field = value
            state["content"] = value
        }

    private val workManager = WorkManager.getInstance(application)

    private val isDataNeededToSave: Boolean
        get() = ((originalTitle != title || originalContent != content)
                && !(title.isBlank() && content.isBlank()))

    private var isTextDataChanged: Boolean = false
    private var isImageAdded: Boolean = false

    private var imagesCount = 0

    private fun initNote() = viewModelScope.launch {
        val argumentNote: Note? = state["note"]
        if (argumentNote == null) {
            val newNote = Note(text = "")
            newlyAddedNoteId = repository.addNote(newNote)
            note = newNote.copy(id = newlyAddedNoteId!!)
        } else {
            note = argumentNote
        }
    }

    private fun initImages() = viewModelScope.launch {
        imagesFlow = repository.getImagesByNoteId(note.id)
    }

    private fun startObservingImages() = viewModelScope.launch {
        eventChannel.send(Event.StartObservingImages)
    }

    private val isAdding
        get() = newlyAddedNoteId != null

    private var isNavigatedBack = false

    fun onAttachImageClick() = viewModelScope.launch {
        eventChannel.send(Event.ShowSelectMethodOfTakingImageScreen)
    }

    fun onImageSelected(uri: Uri) = viewModelScope.launch {
        val imageUri = uri.toString()
        repository.addImage(Image(uri = imageUri, noteHolderId = note.id))
        isImageAdded = true
        repository.updateNoteLastModificationDate(note.id)
    }

    private fun showCaptureImageWithCameraScreen() = viewModelScope.launch {
        eventChannel.send(Event.ShowPickImageWithCameraScreen)
    }

    fun onPickImageFromGalleryClick() = viewModelScope.launch {
        eventChannel.send(Event.ShowPickImageFromGalleryScreen)
    }

    fun onCaptureImageWithCameraClick() = viewModelScope.launch {
        showCaptureImageWithCameraScreen()
    }

    fun onBackPressed() {
        handleNavigationBack()
    }

    fun onSupportNavigateUp() {
        handleNavigationBack()
    }

    fun onFragmentStop() {
        if (!isNavigatedBack && isDataNeededToSave) {
            // this means user exited app => save data with WorkManager
            saveData()
            isTextDataChanged = true
        }
    }

    private fun handleNavigationBack() = viewModelScope.launch {
        isNavigatedBack = true
        if (isDataNeededToSave || isImageAdded) {
            repository.updateNote(UpdateNoteContentTuple(note.id, content, title, System.currentTimeMillis()))
            isTextDataChanged = true
            setNoteUpdatedResult()
        } else {
            if (!isTextDataChanged && isAdding && imagesCount == 0) {
                repository.deleteNote(note.id)
            }
            if (isAdding) {
                setEmptyNoteDiscardedResult()
            }
        }
    }

    private fun saveData() = viewModelScope.launch{
        val editWorkRequestBuilder = OneTimeWorkRequestBuilder<EditNoteWorker>()
        editWorkRequestBuilder.setInputData(createInputDataForEditNote())
        workManager.beginUniqueWork(
            "EditNoteWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            editWorkRequestBuilder.build()
        ).enqueue()
    }

    private fun createInputDataForEditNote(): Data {
        val noteId = newlyAddedNoteId ?: note.id
        val builder = Data.Builder()
        builder.putString("title", title)
        builder.putString("content", content)
        builder.putLong("id", noteId)
        return builder.build()
    }

    fun onImagesListCollected(size: Int) {
        imagesCount = size
    }

    private fun setEmptyNoteDiscardedResult() = viewModelScope.launch {
        eventChannel.send(
            Event.SetFragmentResult(
                EMPTY_NOTE_DISCARDED_REQUEST_KEY,
                EMPTY_NOTE_DISCARDED_RESULT_KEY,
                EMPTY_NOTE_DISCARDED_RESULT_OK
            )
        )
        eventChannel.send(Event.NavigateBack)
    }

    private fun setNoteUpdatedResult() = viewModelScope.launch {
        eventChannel.send(
            Event.SetFragmentResult(
                NOTE_UPDATED_REQUEST_KEY,
                NOTE_UPDATED_RESULT_KEY,
                NOTE_UPDATED_RESULT_OK
            )
        )
        eventChannel.send(Event.NavigateBack)
    }

    override fun onImageLongClick(imageId: Long) {
        deleteImage(imageId)
    }

    private fun deleteImage(imageId: Long) = viewModelScope.launch {
        repository.deleteImage(imageId)
        repository.updateNoteLastModificationDate(note.id)
        imagesCount -= 1
    }

    fun onReminderClick() = viewModelScope.launch {
        eventChannel.send(Event.ShowTimePicker)
    }

    fun onTimePicked(dateReminded: Long) = viewModelScope.launch {
        repository.updateNoteDateReminded(note.id, dateReminded)
        eventChannel.send(Event.SetReminderAlarm(note.id.toInt(), dateReminded, note.title, note.text))
        eventChannel.send(Event.UpdateDateRemindedUI(dateReminded))
    }

    fun onReminderLongClick() = viewModelScope.launch {
        eventChannel.send(Event.ShowConfirmDeleteReminderScreen(R.string.remove_reminder, R.string.ok))
    }

    fun onDeleteReminderConfirmed() = viewModelScope.launch {
        repository.updateNoteDateReminded(note.id, 0)
        eventChannel.send(Event.CancelReminderAlarm(note.id.toInt()))
    }

    sealed class Event {
        data class SetFragmentResult(
            val requestKey: String,
            val resultKey: String,
            val result: Int
        ) : Event()
        object ShowSelectMethodOfTakingImageScreen : Event()
        object ShowPickImageWithCameraScreen : Event()
        object StartObservingImages : Event()
        object ShowPickImageFromGalleryScreen : Event()
        object NavigateBack : Event()

        object ShowTimePicker : Event()

        data class SetReminderAlarm(
            val requestKey: Int,
            val dateReminded: Long,
            val title: String,
            val content: String
        ) : Event()

        data class UpdateDateRemindedUI(val dateReminded: Long) : Event()

        data class ShowConfirmDeleteReminderScreen(
            @StringRes val messageRes: Int,
            @StringRes val buttonTextRes: Int
        ) : Event()

        data class CancelReminderAlarm(val requestKey: Int) : Event()
    }
}