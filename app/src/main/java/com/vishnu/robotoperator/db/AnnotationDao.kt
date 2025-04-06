package com.vishnu.robotoperator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vishnu.robotoperator.model.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE roomId = :roomId")
    fun getAnnotationsForRoom(roomId: Long): Flow<List<AnnotationEntity>>

    @Insert
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)
}
