package com.meriniguan.notepad.model.image.entities

data class Image(
    val uri: String,
    val noteHolderId: Long,
    val id: Long = 0
) {
}