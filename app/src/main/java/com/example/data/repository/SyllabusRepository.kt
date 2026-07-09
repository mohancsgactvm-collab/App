package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.example.data.dao.SyllabusDao
import com.example.data.model.Course
import com.example.data.model.Module
import com.example.data.model.Topic
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ParsedModule(val name: String, val topics: List<String>)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class SyllabusResponse(val modules: List<ParsedModule>)

class SyllabusRepository(private val dao: SyllabusDao) {

    val allCourses: Flow<List<Course>> = dao.getAllCourses()

    fun getModulesForCourse(courseId: Int): Flow<List<Module>> = dao.getModulesForCourse(courseId)

    fun getTopicsForModule(moduleId: Int): Flow<List<Topic>> = dao.getTopicsForModule(moduleId)

    suspend fun insertCourse(course: Course): Long = withContext(Dispatchers.IO) {
        dao.insertCourse(course)
    }

    suspend fun deleteCourse(courseId: Int) = withContext(Dispatchers.IO) {
        dao.deleteCourse(courseId)
    }

    suspend fun insertModule(module: Module): Long = withContext(Dispatchers.IO) {
        dao.insertModule(module)
    }

    suspend fun insertTopic(topic: Topic): Long = withContext(Dispatchers.IO) {
        dao.insertTopic(topic)
    }

    suspend fun updateTopic(topic: Topic) = withContext(Dispatchers.IO) {
        dao.updateTopic(topic)
    }

    suspend fun generateSyllabusStructure(courseName: String, syllabusText: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext false
        }

        val prompt = """
            You are an expert college professor helper. I will give you a raw course syllabus or a course description/topics.
            Analyze it and structure it into clean Modules (or Chapters) and individual Topics under each Module.
            
            Course Name: $courseName
            Raw Syllabus Input:
            $syllabusText
            
            Return the output strictly in the following JSON format:
            {
              "modules": [
                {
                  "name": "Module Title Here",
                  "topics": [
                    "Topic Title 1",
                    "Topic Title 2",
                    "Topic Title 3"
                  ]
                }
              ]
            }
            
            Return ONLY the valid JSON representation. No enclosing markdown wrapper like ```json.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(SyllabusResponse::class.java)
                val parsed = adapter.fromJson(jsonText)
                if (parsed != null && parsed.modules.isNotEmpty()) {
                    val courseId = dao.insertCourse(Course(name = courseName)).toInt()
                    for ((mIndex, mod) in parsed.modules.withIndex()) {
                        val moduleId = dao.insertModule(
                            Module(
                                courseId = courseId,
                                name = mod.name,
                                orderIndex = mIndex
                            )
                        ).toInt()
                        for ((tIndex, topName) in mod.topics.withIndex()) {
                            dao.insertTopic(
                                Topic(
                                    moduleId = moduleId,
                                    name = topName,
                                    orderIndex = tIndex
                                )
                            )
                        }
                    }
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun generateTopicNotes(courseName: String, moduleName: String, topic: Topic): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing or not configured in AI Studio's Secrets panel. Please check your setup."
        }

        val prompt = """
            You are a world-class college professor. Write highly structured, clean, and comprehensive academic study notes for the following topic:
            
            Course: $courseName
            Chapter/Module: $moduleName
            Topic: ${topic.name}
            
            Include:
            1. **Core Concept Overview**: Clear, concise definition and purpose.
            2. **Key Terminologies**: Define crucial terms.
            3. **Detailed Explanation**: Break down the concept into logical sections with clear explanations.
            4. **Illustrative Examples**: Provide concrete academic/practical examples or formulas. If relevant, include clear, commented code snippets (e.g., Python, Kotlin, Java, C++) or step-by-step mathematical derivations.
            5. **Key Takeaways & Review Questions**: Summarize the most important points and provide 2-3 concept-check questions.
            
            Format with clean, elegant markdown headers, bullet points, bold text, and code blocks for readability. Use standard academic language.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val notes = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!notes.isNullOrEmpty()) {
                dao.updateTopic(
                    topic.copy(
                        notesText = notes,
                        isGenerated = true,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                notes
            } else {
                "Error: No notes text returned from Gemini."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error while generating notes: ${e.message ?: "Unknown error"}"
        }
    }
}
