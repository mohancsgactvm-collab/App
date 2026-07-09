package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Module(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val name: String,
    val orderIndex: Int
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Module::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moduleId: Int,
    val name: String,
    val orderIndex: Int,
    val notesText: String = "",
    val isGenerated: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
