package com.example.studentmanager

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.studentmanager.data.Student
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StudentManagerApp()
            }
        }
    }
}

@Composable
private fun StudentManagerApp(studentViewModel: StudentViewModel = viewModel()) {
    val navController = rememberNavController()
    val students by studentViewModel.students.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "students") {
        composable("students") {
            StudentListScreen(
                students = students,
                onStudentClick = { navController.navigate("student/${it.id}") },
                onAddClick = { navController.navigate("student-form") }
            )
        }
        composable(
            route = "student/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { entry ->
            val studentId = entry.arguments?.getLong("studentId") ?: return@composable
            val student by studentViewModel.observeStudent(studentId)
                .collectAsStateWithLifecycle(initialValue = null)
            StudentDetailScreen(
                student = student,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("student-form?studentId=${it.id}") },
                onDelete = { selected, completed ->
                    studentViewModel.deleteStudent(selected, completed)
                }
            )
        }
        composable(
            route = "student-form?studentId={studentId}",
            arguments = listOf(
                navArgument("studentId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { entry ->
            val studentId = entry.arguments?.getLong("studentId") ?: 0L
            val studentFlow: Flow<Student?> = if (studentId == 0L) {
                remember(studentId) { flowOf(null) }
            } else {
                studentViewModel.observeStudent(studentId)
            }
            val student by studentFlow.collectAsStateWithLifecycle(initialValue = null)
            StudentFormScreen(
                student = student,
                isEditing = studentId != 0L,
                onBack = { navController.popBackStack() },
                onAdd = { studentViewModel.addStudent(it) { navController.popBackStack() } },
                onUpdate = {
                    studentViewModel.updateStudent(it) { navController.popBackStack() }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentListScreen(
    students: List<Student>,
    onStudentClick: (Student) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.student_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text(stringResource(R.string.add_student))
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_students))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onStudentClick(student) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentAvatar(student.avatar, Modifier.size(56.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(student.studentNumber)
                                Text(student.email)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailScreen(
    student: Student?,
    onBack: () -> Unit,
    onEdit: (Student) -> Unit,
    onDelete: (Student, () -> Unit) -> Unit
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.student_details)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            )
        }
    ) { padding ->
        if (student == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StudentAvatar(student.avatar, Modifier.size(144.dp))
                Spacer(Modifier.height(20.dp))
                StudentField(stringResource(R.string.full_name), student.fullName)
                StudentField(stringResource(R.string.student_number), student.studentNumber)
                StudentField(stringResource(R.string.email), student.email)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { onEdit(student) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.edit))
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && student != null) {
        ConfirmationDialog(
            message = stringResource(R.string.confirm_delete),
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(student, onBack)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFormScreen(
    student: Student?,
    isEditing: Boolean,
    onBack: () -> Unit,
    onAdd: (Student) -> Unit,
    onUpdate: (Student) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existingId = student?.id ?: 0L
    var fullName by rememberSaveable(existingId) { mutableStateOf(student?.fullName.orEmpty()) }
    var studentNumber by rememberSaveable(existingId) {
        mutableStateOf(student?.studentNumber.orEmpty())
    }
    var email by rememberSaveable(existingId) { mutableStateOf(student?.email.orEmpty()) }
    var avatar by rememberSaveable(existingId) { mutableStateOf(student?.avatar.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showUpdateConfirmation by rememberSaveable { mutableStateOf(false) }
    var pendingStudent by remember { mutableStateOf<Student?>(null) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    avatar = withContext(Dispatchers.IO) {
                        val directory = File(context.filesDir, "avatars").apply { mkdirs() }
                        val destination = File(directory, "${UUID.randomUUID()}.image")
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("Selected image stream is unavailable.")
                        input.use { source ->
                            destination.outputStream().use { output -> source.copyTo(output) }
                        }
                        destination.absolutePath
                    }
                    avatarError = null
                } catch (error: IOException) {
                    avatarError = context.getString(
                        R.string.avatar_load_error,
                        error.localizedMessage.orEmpty()
                    )
                } catch (error: SecurityException) {
                    avatarError = context.getString(
                        R.string.avatar_load_error,
                        error.localizedMessage.orEmpty()
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.edit_student else R.string.add_student))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    ) { padding ->
        if (student != null || !isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudentAvatar(avatar, Modifier.size(80.dp))
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(stringResource(R.string.choose_photo))
                    }
                }
                OutlinedTextField(
                    value = avatar.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                        .orEmpty(),
                    onValueChange = {
                        avatar = it
                        avatarError = null
                    },
                    label = { Text(stringResource(R.string.avatar_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                avatarError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = { Text(stringResource(R.string.student_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        val nameValue = fullName.trim()
                        val numberValue = studentNumber.trim()
                        val emailValue = email.trim()
                        if (nameValue.isEmpty() || numberValue.isEmpty() ||
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()
                        ) {
                            validationError = context.getString(R.string.validation_error)
                        } else {
                            validationError = null
                            val value = Student(
                                id = existingId,
                                fullName = nameValue,
                                studentNumber = numberValue,
                                email = emailValue,
                                avatar = avatar.trim()
                            )
                            if (!isEditing) {
                                onAdd(value)
                            } else {
                                pendingStudent = value
                                showUpdateConfirmation = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (showUpdateConfirmation) {
        ConfirmationDialog(
            message = stringResource(R.string.confirm_update),
            onDismiss = { showUpdateConfirmation = false },
            onConfirm = {
                showUpdateConfirmation = false
                pendingStudent?.let(onUpdate)
            }
        )
    }
}

@Composable
private fun StudentField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StudentAvatar(avatar: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageModel: Any? = when {
        avatar.startsWith("http://") || avatar.startsWith("https://") -> avatar
        avatar.isNotBlank() -> File(avatar)
        else -> null
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.student_avatar),
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape),
        placeholder = null
    )
}

@Composable
private fun ConfirmationDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.yes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) }
        }
    )
}
