package com.meriniguan.notepad.model.image.room.tuples

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class DeleteImageTuple(
    @ColumnInfo(name = "image_id") @PrimaryKey val id: Long,
)