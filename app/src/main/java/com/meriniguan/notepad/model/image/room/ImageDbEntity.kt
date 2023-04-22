package com.meriniguan.notepad.model.image.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meriniguan.notepad.model.image.entities.Image

@Entity(tableName = "images")
data class ImageDbEntity(
    val uri: String,
    @ColumnInfo(name = "note_holder_id") val noteHolderId: Long,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "image_id") val id: Long = 0
) {

    fun toImage(): Image = Image(uri, noteHolderId, id)

    companion object {
        fun fromImage(image: Image): ImageDbEntity = ImageDbEntity(
            uri = image.uri,
            noteHolderId = image.noteHolderId,
            id = image.id
        )
    }
}