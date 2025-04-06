package com.vishnu.robotoperator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vishnu.robotoperator.model.WallAnnotation

@Dao
public interface AnnotationDao {
    @Query("SELECT * FROM annotations")
    fun getAllAnnotations(): List<WallAnnotation>

    @Insert
    suspend fun insertAnnotation(annotation: WallAnnotation): Long

    @Update
    suspend fun updateAnnotation(annotation: WallAnnotation)

    @Delete
    suspend fun deleteAnnotation(annotation: WallAnnotation)
}
