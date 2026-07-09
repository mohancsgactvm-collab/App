package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Course
import com.example.data.model.Module
import com.example.data.model.Topic
import com.example.data.repository.SyllabusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ModuleWithTopics(
    val module: Module,
    val topics: List<Topic>
)

class SyllabusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SyllabusRepository
    val courses: StateFlow<List<Course>>

    val selectedCourse = MutableStateFlow<Course?>(null)
    val searchQuery = MutableStateFlow("")

    val isGeneratingSyllabus = MutableStateFlow(false)
    val generatingTopicId = MutableStateFlow<Int?>(null)

    val activeNoteTopic = MutableStateFlow<Topic?>(null)
    val activeNoteModule = MutableStateFlow<Module?>(null)
    val isEditMode = MutableStateFlow(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SyllabusRepository(database.syllabusDao())
        
        courses = repository.allCourses
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Automatically select the first course if none is selected
        viewModelScope.launch {
            courses.collectLatest { list ->
                if (selectedCourse.value == null && list.isNotEmpty()) {
                    selectedCourse.value = list.first()
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val modulesWithTopics: StateFlow<List<ModuleWithTopics>> = selectedCourse
        .flatMapLatest { course ->
            if (course == null) {
                flowOf(emptyList())
            } else {
                repository.getModulesForCourse(course.id).flatMapLatest { modules ->
                    if (modules.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = modules.map { module ->
                            repository.getTopicsForModule(module.id).map { topics ->
                                ModuleWithTopics(module, topics)
                            }
                        }
                        combine(flows) { it.toList() }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectCourse(course: Course) {
        selectedCourse.value = course
        activeNoteTopic.value = null
        activeNoteModule.value = null
        isEditMode.value = false
    }

    fun parseSyllabus(courseName: String, syllabusText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isGeneratingSyllabus.value = true
            val success = repository.generateSyllabusStructure(courseName, syllabusText)
            isGeneratingSyllabus.value = false
            onComplete(success)
            if (success) {
                // The newly inserted course will be top of the list, select it
                courses.firstOrNull()?.firstOrNull()?.let {
                    selectedCourse.value = it
                }
            }
        }
    }

    fun addCourseManually(courseName: String) {
        viewModelScope.launch {
            val courseId = repository.insertCourse(Course(name = courseName)).toInt()
            // Add a default Module and Topic to get them started
            val moduleId = repository.insertModule(
                Module(
                    courseId = courseId,
                    name = "Module 1: Getting Started",
                    orderIndex = 0
                )
            ).toInt()
            repository.insertTopic(
                Topic(
                    moduleId = moduleId,
                    name = "Introduction Topic",
                    orderIndex = 0
                )
            )
            // Select the new course
            selectedCourse.value = Course(id = courseId, name = courseName)
        }
    }

    fun addModuleManually(moduleName: String) {
        val course = selectedCourse.value ?: return
        viewModelScope.launch {
            val currentModules = modulesWithTopics.value
            val newOrder = currentModules.size
            repository.insertModule(
                Module(
                    courseId = course.id,
                    name = moduleName,
                    orderIndex = newOrder
                )
            )
        }
    }

    fun addTopicManually(moduleId: Int, topicName: String) {
        viewModelScope.launch {
            val moduleWithTopic = modulesWithTopics.value.find { it.module.id == moduleId }
            val newOrder = moduleWithTopic?.topics?.size ?: 0
            repository.insertTopic(
                Topic(
                    moduleId = moduleId,
                    name = topicName,
                    orderIndex = newOrder
                )
            )
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course.id)
            if (selectedCourse.value?.id == course.id) {
                selectedCourse.value = null
                activeNoteTopic.value = null
                activeNoteModule.value = null
                isEditMode.value = false
            }
        }
    }

    fun generateNotesForTopic(module: Module, topic: Topic) {
        val course = selectedCourse.value ?: return
        viewModelScope.launch {
            generatingTopicId.value = topic.id
            val notes = repository.generateTopicNotes(course.name, module.name, topic)
            generatingTopicId.value = null
            
            // If the user currently has this topic open, refresh active view
            if (activeNoteTopic.value?.id == topic.id) {
                activeNoteTopic.value = topic.copy(
                    notesText = notes,
                    isGenerated = true,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }

    fun updateActiveNoteText(newText: String) {
        val topic = activeNoteTopic.value ?: return
        viewModelScope.launch {
            val updated = topic.copy(
                notesText = newText,
                isGenerated = newText.isNotEmpty(),
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTopic(updated)
            activeNoteTopic.value = updated
        }
    }

    fun openNoteViewer(module: Module, topic: Topic) {
        activeNoteTopic.value = topic
        activeNoteModule.value = module
        isEditMode.value = false
    }

    fun closeNoteViewer() {
        activeNoteTopic.value = null
        activeNoteModule.value = null
        isEditMode.value = false
    }
}
