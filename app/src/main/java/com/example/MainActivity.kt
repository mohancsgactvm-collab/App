package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Course
import com.example.data.model.Module
import com.example.data.model.Topic
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ModuleWithTopics
import com.example.ui.viewmodel.SyllabusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    SyllabusNotesApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SyllabusNotesApp(
    modifier: Modifier = Modifier,
    viewModel: SyllabusViewModel = viewModel()
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val selectedCourse by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val modulesWithTopics by viewModel.modulesWithTopics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isGeneratingSyllabus by viewModel.isGeneratingSyllabus.collectAsStateWithLifecycle()
    val generatingTopicId by viewModel.generatingTopicId.collectAsStateWithLifecycle()
    val activeNoteTopic by viewModel.activeNoteTopic.collectAsStateWithLifecycle()
    val activeNoteModule by viewModel.activeNoteModule.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()

    var showAddCourseDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main view content containing Courses and Modules
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp) // extra padding for overlap
        ) {
            // Dashboard Banner Card
            DashboardHeader(
                onAddCourseClick = { showAddCourseDialog = true }
            )

            // Course Selector Section
            CourseSelectorSection(
                courses = courses,
                selectedCourse = selectedCourse,
                onCourseSelect = { viewModel.selectCourse(it) },
                onAddCourseClick = { showAddCourseDialog = true },
                onDeleteCourse = { viewModel.deleteCourse(it) }
            )

            // Active Syllabus Content
            selectedCourse?.let { course ->
                SyllabusContentSection(
                    course = course,
                    modulesWithTopics = modulesWithTopics,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    generatingTopicId = generatingTopicId,
                    onGenerateNotes = { module, topic -> viewModel.generateNotesForTopic(module, topic) },
                    onTopicClick = { module, topic -> viewModel.openNoteViewer(module, topic) },
                    onAddModule = { viewModel.addModuleManually(it) },
                    onAddTopic = { moduleId, topicName -> viewModel.addTopicManually(moduleId, topicName) }
                )
            } ?: run {
                // Empty course state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "No courses",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Courses Available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add a course manually or paste a syllabus text to generate a full module structure with Gemini AI.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddCourseDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Course")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Course")
                        }
                    }
                }
            }
        }

        // Full Screen Note Workspace Overlay
        AnimatedVisibility(
            visible = activeNoteTopic != null && activeNoteModule != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val currentTopic = activeNoteTopic
            val currentModule = activeNoteModule
            if (currentTopic != null && currentModule != null) {
                NoteWorkspaceScreen(
                    course = selectedCourse,
                    module = currentModule,
                    topic = currentTopic,
                    isEditMode = isEditMode,
                    isGenerating = generatingTopicId == currentTopic.id,
                    onBackClick = { viewModel.closeNoteViewer() },
                    onEditModeChange = { viewModel.isEditMode.value = it },
                    onSaveNotes = { viewModel.updateActiveNoteText(it) },
                    onRegenerateNotes = { viewModel.generateNotesForTopic(currentModule, currentTopic) }
                )
            }
        }

        // Progress bar for AI syllabus creation
        if (isGeneratingSyllabus) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Analyzing Syllabus...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gemini AI is parsing course topics and structuring them into organized modules.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Add Course Dialog
        if (showAddCourseDialog) {
            AddCourseDialog(
                onDismiss = { showAddCourseDialog = false },
                onAddManual = { courseName ->
                    viewModel.addCourseManually(courseName)
                    showAddCourseDialog = false
                },
                onAddAI = { courseName, syllabusText ->
                    viewModel.parseSyllabus(courseName, syllabusText) { success ->
                        if (!success) {
                            // Handled with error check inside parsing
                        }
                    }
                    showAddCourseDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardHeader(
    onAddCourseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner_college_notes),
                contentDescription = "Dashboard Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Beautiful dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            // Header Details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "Syllabus Notes AI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instantly parse college course syllabi and generate structured, comprehensive study notes powered by Gemini 3.5 Flash.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CourseSelectorSection(
    courses: List<Course>,
    selectedCourse: Course?,
    onCourseSelect: (Course) -> Unit,
    onAddCourseClick: () -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Courses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onAddCourseClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Course")
            }
        }

        if (courses.isEmpty()) {
            Text(
                text = "No courses added. Click 'Add Course' to begin.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(courses) { course ->
                    val isSelected = selectedCourse?.id == course.id
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Course?") },
                            text = { Text("Are you sure you want to delete '${course.name}'? All module structured notes inside will be permanently deleted.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteCourse(course)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable { onCourseSelect(course) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Course Icon",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Course",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = course.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyllabusContentSection(
    course: Course,
    modulesWithTopics: List<ModuleWithTopics>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    generatingTopicId: Int?,
    onGenerateNotes: (Module, Topic) -> Unit,
    onTopicClick: (Module, Topic) -> Unit,
    onAddModule: (String) -> Unit,
    onAddTopic: (Int, String) -> Unit
) {
    var showAddModuleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = course.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Syllabus Modules",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showAddModuleDialog = true }) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add Module",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Topic Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search syllabus topics...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (modulesWithTopics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "This course syllabus is empty.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showAddModuleDialog = true }) {
                        Text("+ Create a Module/Chapter")
                    }
                }
            }
        } else {
            // Filter modules and topics based on search
            val filteredModules = modulesWithTopics.map { mt ->
                val filteredTopics = mt.topics.filter { topic ->
                    topic.name.contains(searchQuery, ignoreCase = true) ||
                            topic.notesText.contains(searchQuery, ignoreCase = true)
                }
                ModuleWithTopics(mt.module, filteredTopics)
            }.filter { it.topics.isNotEmpty() || mtHasEmptySearchMatch(it.module.name, searchQuery) }

            if (filteredModules.isEmpty()) {
                Text(
                    text = "No topics match your search query.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredModules.forEach { item ->
                    ModuleCard(
                        module = item.module,
                        topics = item.topics,
                        generatingTopicId = generatingTopicId,
                        onGenerateNotes = { onGenerateNotes(item.module, it) },
                        onTopicClick = { onTopicClick(item.module, it) },
                        onAddTopic = { onAddTopic(item.module.id, it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showAddModuleDialog) {
        var moduleName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddModuleDialog = false },
            title = { Text("Add New Module / Chapter") },
            text = {
                OutlinedTextField(
                    value = moduleName,
                    onValueChange = { moduleName = it },
                    placeholder = { Text("e.g. Module 1: Introduction") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (moduleName.isNotBlank()) {
                            onAddModule(moduleName.trim())
                            showAddModuleDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModuleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun mtHasEmptySearchMatch(moduleName: String, query: String): Boolean {
    return query.isEmpty() || moduleName.contains(query, ignoreCase = true)
}

@Composable
fun ModuleCard(
    module: Module,
    topics: List<Topic>,
    generatingTopicId: Int?,
    onGenerateNotes: (Topic) -> Unit,
    onTopicClick: (Topic) -> Unit,
    onAddTopic: (String) -> Unit
) {
    var showAddTopicField by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = module.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showAddTopicField = !showAddTopicField }) {
                    Icon(
                        imageVector = if (showAddTopicField) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Toggle add topic",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = showAddTopicField) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTopicName,
                        onValueChange = { newTopicName = it },
                        placeholder = { Text("New topic name...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTopicName.isNotBlank()) {
                                onAddTopic(newTopicName.trim())
                                newTopicName = ""
                                showAddTopicField = false
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Add Topic",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (topics.isEmpty()) {
                Text(
                    text = "No topics inside this module. Add a topic above to start.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                topics.forEach { topic ->
                    val isGenerating = generatingTopicId == topic.id
                    TopicRowItem(
                        topic = topic,
                        isGenerating = isGenerating,
                        onGenerateNotes = { onGenerateNotes(topic) },
                        onTopicClick = { onTopicClick(topic) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun TopicRowItem(
    topic: Topic,
    isGenerating: Boolean,
    onGenerateNotes: () -> Unit,
    onTopicClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGenerating) {
                if (topic.isGenerated) onTopicClick() else onGenerateNotes()
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (topic.isGenerated) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (topic.isGenerated) Icons.Default.Book else Icons.Default.AutoAwesome,
                    contentDescription = "Status Icon",
                    tint = if (topic.isGenerated) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = topic.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (topic.isGenerated) {
                        Text(
                            text = "Notes Generated",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "No notes generated yet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (topic.isGenerated) {
                    TextButton(onClick = onTopicClick) {
                        Text("View", fontSize = 12.sp)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View notes")
                    }
                } else {
                    Button(
                        onClick = onGenerateNotes,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Notes", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteWorkspaceScreen(
    course: Course?,
    module: Module,
    topic: Topic,
    isEditMode: Boolean,
    isGenerating: Boolean,
    onBackClick: () -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onSaveNotes: (String) -> Unit,
    onRegenerateNotes: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var textState by remember(topic.notesText) { mutableStateOf(topic.notesText) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Note Workspace Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${course?.name ?: ""}  •  ${module.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = topic.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header Operations
                Row {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                onSaveNotes(textState)
                                onEditModeChange(false)
                                Toast.makeText(context, "Notes Saved Successfully", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Notes")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(topic.notesText))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            enabled = topic.notesText.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy Notes")
                        }

                        IconButton(onClick = { onEditModeChange(true) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Notes")
                        }

                        IconButton(onClick = onRegenerateNotes) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Notes")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (isGenerating) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Generating complete study notes...",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isEditMode) {
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        placeholder = { Text("Write your notes here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                } else {
                    if (topic.notesText.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Notes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Notes Generated", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Generate rich, comprehensive study notes with Gemini AI for this topic.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = onRegenerateNotes) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate with Gemini")
                                }
                            }
                        }
                    } else {
                        // Styled Render of generated notes
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            MarkdownTextRenderer(markdown = topic.notesText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownTextRenderer(markdown: String) {
    val lines = markdown.split("\n")
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End of code block - render it
                CodeBlock(code = codeBlockBuilder.toString())
                codeBlockBuilder.setLength(0)
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
        } else if (inCodeBlock) {
            codeBlockBuilder.append(line).append("\n")
        } else {
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.substring(2),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.substring(3),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.substring(4),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                        Text("•", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = parseBoldMarkdown(trimmed.substring(2)),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBoldMarkdown(trimmed),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source Code",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Copy", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalScrollableCode(code = code)
        }
    }
}

@Composable
fun HorizontalScrollableCode(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun parseBoldMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val boldIndex = text.indexOf("**", cursor)
            if (boldIndex == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, boldIndex))
            val endBoldIndex = text.indexOf("**", boldIndex + 2)
            if (endBoldIndex == -1) {
                append("**")
                cursor = boldIndex + 2
            } else {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified)) {
                    append(text.substring(boldIndex + 2, endBoldIndex))
                }
                cursor = endBoldIndex + 2
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onAddManual: (String) -> Unit,
    onAddAI: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var courseName by remember { mutableStateOf("") }
    var syllabusText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add New Course",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("AI Parse", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual", fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name") },
                    placeholder = { Text("e.g. Data Structures & Algorithms") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = syllabusText,
                        onValueChange = { syllabusText = it },
                        label = { Text("Paste Syllabus / Topics") },
                        placeholder = { Text("Paste chapter names, topics, or full syllabus here and Gemini AI will structure it automatically.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = "Creating manually will build a basic placeholder chapter and topic that you can fully customize.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (courseName.isNotBlank()) {
                                if (selectedTab == 0) {
                                    if (syllabusText.isNotBlank()) {
                                        onAddAI(courseName.trim(), syllabusText.trim())
                                    }
                                } else {
                                    onAddManual(courseName.trim())
                                }
                            }
                        },
                        enabled = courseName.isNotBlank() && (selectedTab == 1 || syllabusText.isNotBlank()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (selectedTab == 0) "Analyze Syllabus" else "Create")
                    }
                }
            }
        }
    }
}
