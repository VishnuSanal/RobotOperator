package com.vishnu.robotoperator.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class WallAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallIndex: Int,
    val x1: Float, val y1: Float,  // Top-left corner in wall coordinates (0-1)
    val x2: Float, val y2: Float,  // Bottom-right corner in wall coordinates (0-1)
    val text: String
)