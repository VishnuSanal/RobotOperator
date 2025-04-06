package com.vishnu.robotoperator.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val roomId: Long,
    val wallId: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val width: Float,
    val height: Float,
    val notes: String?
)