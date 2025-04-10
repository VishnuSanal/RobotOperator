package com.vishnu.robotoperator.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class WallAnnotation(
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    var type: AnnotationType = AnnotationType.SPRAY_AREA,

    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)