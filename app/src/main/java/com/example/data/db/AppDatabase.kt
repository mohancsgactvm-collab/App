package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.SyllabusDao
import com.example.data.model.Course
import com.example.data.model.Module
import com.example.data.model.Topic

@Database(entities = [Course::class, Module::class, Topic::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syllabusDao(): SyllabusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syllabus_notes_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
