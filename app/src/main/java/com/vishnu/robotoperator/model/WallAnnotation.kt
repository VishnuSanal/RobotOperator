package com.vishnu.robotoperator.model

import android.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class WallAnnotation(
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,

    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    var type: AnnotationType = AnnotationType.OBSTACLE,
    var color: Int = Color.RED,
)