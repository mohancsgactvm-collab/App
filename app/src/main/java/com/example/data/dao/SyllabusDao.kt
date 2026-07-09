package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Course
import com.example.data.model.Module
import com.example.data.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: Int)

    @Query("SELECT * FROM modules WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getModulesForCourse(courseId: Int): Flow<List<Module>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: Module): Long

    @Query("SELECT * FROM topics WHERE moduleId = :moduleId ORDER BY orderIndex ASC")
    fun getTopicsForModule(moduleId: Int): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Update
    suspend fun updateTopic(topic: Topic)

    @Query("SELECT * FROM topics WHERE id = :topicId")
    suspend fun getTopicById(topicId: Int): Topic?

    @Query("SELECT * FROM topics WHERE moduleId IN (SELECT id FROM modules WHERE courseId = :courseId) ORDER BY orderIndex ASC")
    suspend fun getAllTopicsForCourse(courseId: Int): List<Topic>
}
