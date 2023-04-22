package com.meriniguan.notepad.screens.notes

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.meriniguan.notepad.EMPTY_NOTE_DISCARDED_RESULT_OK
import com.meriniguan.notepad.NOTE_UPDATED_RESULT_OK
import com.meriniguan.notepad.R
import com.meriniguan.notepad.model.image.entities.Image
import com.meriniguan.notepad.model.note.NotesRepository
import com.meriniguan.notepad.model.note.entities.Note
import com.meriniguan.notepad.model.note.entities.NoteListItem
import com.meriniguan.notepad.utils.NoteInfo
import com.meriniguan.notepad.utils.Selection
import com.meriniguan.notepad.model.preferences.PreferencesManager
import com.meriniguan.notepad.model.preferences.SortOrder
import com.meriniguan.notepad.screens.addeditnote.AddEditNoteViewModel
import com.meriniguan.notepad.screens.addeditnote.EMPTY_NOTE_DISCARDED_RESULT_KEY
import com.meriniguan.notepad.screens.addeditnote.NOTE_UPDATED_RESULT_KEY
import com.meriniguan.notepad.screens.notes.adapters.NotesAdapter
import com.meriniguan.notepad.utils.Quadruple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel(), NotesAdapter.OnItemClickListener {

    private val searchQuery = state.getLiveData("search_query", "")

    private val preferencesFlow = preferencesManager.preferencesFlow

    private var currentIsReducedView: Boolean = false

    private var selectionFlow = MutableStateFlow(Selection.NORMAL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = combine(
        searchQuery.asFlow(),
        preferencesFlow,
        selectionFlow,
    ) { query, userPreferences, selection ->
        Quadruple(query, userPreferences.sortOrder, userPreferences.isReducedView, selection)
    }.flatMapLatest { (query, sortOrder, isReducedView, selection) ->
        currentIsReducedView = isReducedView
        repository.getPagedNotes(query, sortOrder, selection).map {
            it.map { note ->
                NoteListItem.fromNote(note, getNoteInfo(sortOrder), isReducedView)
            }
        }
    }
        .cachedIn(viewModelScope)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    fun onPhotoSelected(uri: Uri) = viewModelScope.launch {
        repository.addImage(Image(uri.toString(), 4))
    }

    fun onSearchQuerySubmitted(query: String) = viewModelScope.launch {
        eventChannel.send(Event.ShowSearchFields(R.string.search_colon, query))
        if (searchQuery.value != query) {
            searchQuery.value = query
        }
    }

    fun onCancelSearchClick() = viewModelScope.launch {
        eventChannel.send(Event.HideSearchFields)
        eventChannel.send(Event.CollapseSearchView)
        searchQuery.value = ""
    }

    fun onSearchBackButtonClick() = viewModelScope.launch {
        eventChannel.send(Event.HideSearchFields)
        searchQuery.value = ""
    }

    fun getSearchQuery(): String? {
        return searchQuery.value
    }

    fun onSortTasksByTitleSelected() = viewModelScope.launch {
        preferencesManager.updateSortOrder(SortOrder.BY_TITLE)
    }

    fun onSortTasksByDateCreatedSelected() = viewModelScope.launch {
        preferencesManager.updateSortOrder(SortOrder.BY_DATE_CREATED)
    }

    fun onSortTasksByDateUpdatedSelected() = viewModelScope.launch {
        preferencesManager.updateSortOrder(SortOrder.BY_DATE_UPDATED)
    }

    fun onCreateMenu() = viewModelScope.launch {
        eventChannel.send(Event.SetChangeViewMenuItemTitle(getChangeViewMenuItemTitle()))
    }

    fun onChangeViewClick() = viewModelScope.launch {
        preferencesManager.toggleIsReducedView()
        eventChannel.send(Event.SetChangeViewMenuItemTitle(getChangeViewMenuItemTitle()))
    }

    fun onFabNoteClick() = viewModelScope.launch {
        eventChannel.send(Event.CloseFab)
        eventChannel.send(Event.ShowAddNoteScreen())
    }

    fun onFabCameraClick() = viewModelScope.launch {
        eventChannel.send(Event.CloseFab)
        eventChannel.send((Event.ShowAddNoteScreen(addImageAtLaunch = true)))
    }

    fun onFabCheckListClick() = viewModelScope.launch {
        eventChannel.send(Event.ShowMessage(R.string.checklist_message))
    }

    fun onNormalNotesSelected() = viewModelScope.launch {
        eventChannel.send(Event.ShowFab)
        if (selectionFlow.value != Selection.NORMAL) {
            eventChannel.send(Event.SetScreenTitle(R.string.notes))
            if (selectionFlow.value == Selection.TRASHED) {
                eventChannel.send(Event.SetupDefaultNotesMenu)
            }
            selectionFlow.value = Selection.NORMAL
        }
    }

    fun onArchivedNotesSelected() = viewModelScope.launch {
        eventChannel.send(Event.HideFab)
        if (selectionFlow.value != Selection.ARCHIVED) {
            eventChannel.send(Event.SetScreenTitle(R.string.archive))
            if (selectionFlow.value == Selection.TRASHED) {
                eventChannel.send(Event.SetupDefaultNotesMenu)
            }
            selectionFlow.value = Selection.ARCHIVED
        }
    }

    fun onTrashedNotesSelected() = viewModelScope.launch {
        eventChannel.send(Event.HideFab)
        if (selectionFlow.value != Selection.TRASHED) {
            eventChannel.send(Event.SetScreenTitle(R.string.trash))
            eventChannel.send(Event.SetupTrashedNotesMenu)
            selectionFlow.value = Selection.TRASHED
        }
    }

    private fun getChangeViewMenuItemTitle(): Int =
        if (currentIsReducedView) R.string.expanded_view else R.string.reduced_view

    private fun getNoteInfo(sortOrder: SortOrder): NoteInfo = when (sortOrder) {
        SortOrder.BY_TITLE -> NoteInfo.DATE_UPDATED
        SortOrder.BY_DATE_CREATED -> NoteInfo.DATE_CREATED
        SortOrder.BY_DATE_UPDATED -> NoteInfo.DATE_UPDATED
    }

    fun onNoteSwiped(noteListItem: NoteListItem) = viewModelScope.launch {
        val note = noteListItem.note
        when (selectionFlow.value) {
            Selection.NORMAL -> {
                archiveNote(note)
            }
            Selection.ARCHIVED -> {
                trashNote(note)
            }
            Selection.TRASHED -> {
                restoreNote(note)
                eventChannel.send(Event.ShowMessage(R.string.note_restored))
            }
        }
    }

    override fun onItemClick(noteListItem: NoteListItem) {
        viewModelScope.launch {
            eventChannel.send(Event.CloseFab)
            eventChannel.send(Event.ShowEditNoteScreen(noteListItem.note))
        }
    }

    private fun archiveNote(note: Note) = viewModelScope.launch {
        repository.archiveNote(note.id)
        eventChannel.send(
            Event.ShowUndoNoteArchivingMessage(R.string.note_archived, R.string.undo, note)
        )
        eventChannel.send(Event.InvalidateNotesList)
    }

    private fun trashNote(note: Note) = viewModelScope.launch {
        repository.trashNote(note.id)
        eventChannel.send(
            Event.ShowUndoNoteTrashingMessage(R.string.note_trashed, R.string.undo, note)
        )
        eventChannel.send(Event.InvalidateNotesList)
    }

    private fun restoreNote(note: Note) = viewModelScope.launch {
        repository.restoreNote(note.id)
        eventChannel.send(Event.InvalidateNotesList)
    }

    fun onUndoNoteArchivingClick(note: Note) = viewModelScope.launch {
        repository.restoreNote(note.id)
        eventChannel.send(Event.InvalidateNotesList)
    }

    fun onUndoNoteTrashingClick(note: Note) = viewModelScope.launch {
        repository.archiveNote(note.id)
        eventChannel.send(Event.InvalidateNotesList)
    }

    fun emptyTrash() = viewModelScope.launch {
        repository.deleteTrashedNotes()
        eventChannel.send(Event.ShowMessage(R.string.trash_emptied))
        eventChannel.send(Event.InvalidateNotesList)
    }

    fun onNoteUpdatedResult(bundle: Bundle) = viewModelScope.launch {
        Log.i("!@#setfragmentresult", "view model top")
        when (bundle.getInt(NOTE_UPDATED_RESULT_KEY)) {
            NOTE_UPDATED_RESULT_OK -> {
                Log.i("!@#setfragmentresult", "updated note handle result")
                handleResult(R.string.note_updated)
            }
        }
    }

    fun onEmptyNoteDiscardedResult(bundle: Bundle) = viewModelScope.launch {
        when (bundle.getInt(EMPTY_NOTE_DISCARDED_RESULT_KEY)) {
            EMPTY_NOTE_DISCARDED_RESULT_OK -> {
                handleResult(R.string.empty_note_discarded)
            }
        }
    }

    private suspend fun handleResult(messageRes: Int) {
        eventChannel.send(Event.ShowMessage(messageRes))
        eventChannel.send(Event.InvalidateNotesList)
    }

    sealed class Event {
        data class ShowSearchFields(
            @StringRes val messageRes: Int,
            val messageResArg: String
        ) : Event()

        object HideSearchFields : Event()

        object CollapseSearchView : Event()

        object CloseFab : Event()

        object HideFab : Event()

        object ShowFab: Event()

        data class SetChangeViewMenuItemTitle(@StringRes val titleRes: Int) : Event()

        data class ShowAddNoteScreen(val addImageAtLaunch: Boolean = false) : Event()

        data class ShowEditNoteScreen(val note: Note) : Event()

        data class ShowUndoNoteArchivingMessage(
            @StringRes val messageRes: Int,
            @StringRes val undoButtonTextRes: Int,
            val note: Note
        ) : Event()

        data class ShowUndoNoteTrashingMessage(
            @StringRes val messageRes: Int,
            @StringRes val undoButtonTextRes: Int,
            val note: Note
        ) : Event()

        data class ShowMessage(@StringRes val messageRes: Int) : Event()

        object InvalidateNotesList : Event()

        object SetupDefaultNotesMenu : Event()

        object SetupTrashedNotesMenu : Event()

        data class SetScreenTitle(@StringRes val titleRes: Int) : Event()
    }
}