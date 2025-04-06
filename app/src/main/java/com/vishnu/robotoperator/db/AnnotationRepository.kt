package com.vishnu.robotoperator.data

import com.vishnu.robotoperator.model.AnnotationEntity

class AnnotationRepository(private val annotationDao: AnnotationDao) {
    fun getAnnotationsForRoom(roomId: Long) = annotationDao.getAnnotationsForRoom(roomId)

    suspend fun addAnnotation(annotation: AnnotationEntity) =
        annotationDao.insertAnnotation(annotation)

    suspend fun updateAnnotation(annotation: AnnotationEntity) =
        annotationDao.updateAnnotation(annotation)

    suspend fun deleteAnnotation(annotation: AnnotationEntity) =
        annotationDao.deleteAnnotation(annotation)
}