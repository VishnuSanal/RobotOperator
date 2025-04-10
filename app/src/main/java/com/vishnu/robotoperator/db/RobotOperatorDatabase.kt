package com.vishnu.robotoperator.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vishnu.robotoperator.data.AnnotationDao
import com.vishnu.robotoperator.model.WallAnnotation

@Database(
    entities = [WallAnnotation::class],
    version = 3,
)
abstract class RobotOperatorDatabase : RoomDatabase() {
    abstract fun annotationDao(): AnnotationDao

    companion object {
        const val DATABASE_NAME = "robot_operator_db"
    }
}
