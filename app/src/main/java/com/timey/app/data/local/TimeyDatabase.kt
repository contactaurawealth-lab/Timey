package com.timey.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.timey.app.data.local.dao.StudySessionDao
import com.timey.app.data.local.entity.StudySessionEntity

@Database(
    entities = [StudySessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TimeyDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: TimeyDatabase? = null

        fun getInstance(context: Context): TimeyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeyDatabase::class.java,
                    "timey_study.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
