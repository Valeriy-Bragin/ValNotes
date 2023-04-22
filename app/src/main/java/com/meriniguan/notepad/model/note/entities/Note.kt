package com.meriniguan.notepad.model.note.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.DateFormat

@Parcelize
data class Note(
    val text: String,
    val title: String = "",
    val firstImageUri: String = "empty",
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateUpdated: Long = System.currentTimeMillis(),
    val id: Long = 0
) : Parcelable {
    val dateCreatedFormatted: String
        get() = DateFormat.getDateTimeInstance().format(dateCreated)

    val dateUpdatedFormatted: String
        get() = DateFormat.getDateTimeInstance().format(dateUpdated)
}