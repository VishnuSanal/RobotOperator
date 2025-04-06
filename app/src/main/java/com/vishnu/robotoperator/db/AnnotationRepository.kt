package com.vishnu.robotoperator.data

import com.vishnu.robotoperator.model.WallAnnotation

class AnnotationRepository(private val annotationDao: AnnotationDao) {
    fun getAnnotationsForRoom() = annotationDao.getAllAnnotations()

    suspend fun addAnnotation(annotation: WallAnnotation) =
        annotationDao.insertAnnotation(annotation)

    suspend fun updateAnnotation(annotation: WallAnnotation) =
        annotationDao.updateAnnotation(annotation)

    suspend fun deleteAnnotation(annotation: WallAnnotation) =
        annotationDao.deleteAnnotation(annotation)
}