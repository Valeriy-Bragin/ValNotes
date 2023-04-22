package com.meriniguan.notepad.model.note.entities

import com.meriniguan.notepad.utils.NoteInfo

data class NoteListItem(
    val note: Note,
    val noteInfo: NoteInfo = NoteInfo.DATE_CREATED,
    val isReducedView: Boolean = false
) {

    companion object {
        fun fromNote(note: Note, noteInfo: NoteInfo, isReducedView: Boolean): NoteListItem =
            NoteListItem(
                note = note,
                noteInfo = noteInfo,
                isReducedView = isReducedView
            )
    }
}