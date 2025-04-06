package com.vishnu.robotoperator.hilt

import android.content.Context
import androidx.room.Room
import com.vishnu.robotoperator.data.AnnotationDao
import com.vishnu.robotoperator.data.AnnotationRepository
import com.vishnu.robotoperator.db.RobotOperatorDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): RobotOperatorDatabase {
        return Room.databaseBuilder(
            context,
            RobotOperatorDatabase::class.java,
            "robot_operator_database"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAnnotationDao(robotOperatorDatabase: RobotOperatorDatabase): AnnotationDao {
        return robotOperatorDatabase.annotationDao()
    }

    @Provides
    @Singleton
    fun provideAnnotationRepository(annotationDao: AnnotationDao): AnnotationRepository {
        return AnnotationRepository(annotationDao)
    }
}