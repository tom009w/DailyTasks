package com.example.dailytasks

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytasks.data.TaskItem
import com.example.dailytasks.ui.TaskViewModel
import com.example.dailytasks.ui.theme.DailyTasksTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyTasksTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel()
) {
    var taskText by remember { mutableStateOf("") }
    val tasks by viewModel.tasks.collectAsState()
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }
    var taskForReminderDialog by remember { mutableStateOf<TaskItem?>(null) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notification permission denied. Enable it in App Settings to receive task reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleAddTask() {
        if (taskText.isNotBlank()) {
            viewModel.addTask(taskText)
            taskText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Daily Tasks",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(text = "What do you need to do today?")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                label = { Text("Enter a task") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = { handleAddTask() },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Add")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { viewModel.toggleTask(task, !task.isCompleted, context) },
                            onLongClick = { taskForReminderDialog = task }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleTask(task, isChecked, context)
                            }
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (task.isCompleted) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                )
                            )
                            if (task.reminderTimeMillis != null && task.reminderTimeMillis > System.currentTimeMillis()) {
                                val label = if (task.isDailyReminder) "Daily" else "Reminder"
                                Text(
                                    text = "$label: ${formatReminderTime(task.reminderTimeMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { taskToDelete = task }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete task"
                            )
                        }
                    }
                }
            }
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete this task?") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task, context)
                        taskToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { taskToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    taskForReminderDialog?.let { task ->
        val hasReminder = task.reminderTimeMillis != null && task.reminderTimeMillis > System.currentTimeMillis()
        val reminderText = task.reminderTimeMillis?.let {
            if (it > System.currentTimeMillis()) {
                val typeStr = if (task.isDailyReminder) "Daily reminder" else "One-time reminder"
                "Current $typeStr:\n${formatReminderTime(it)}"
            } else null
        } ?: "No reminder set for this task."

        var showTypeSelection by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { taskForReminderDialog = null; showTypeSelection = false },
            title = { Text("Task Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Task: ${task.text}")
                    Text(reminderText)

                    if (showTypeSelection) {
                        Text("Select reminder type:", style = MaterialTheme.typography.titleSmall)
                        Button(
                            onClick = {
                                taskForReminderDialog = null
                                showTypeSelection = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showOneTimeDateTimePicker(context, task.reminderTimeMillis) { selectedMillis ->
                                        viewModel.setReminder(task, selectedMillis, isDaily = false, context)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("One-time reminder")
                        }
                        Button(
                            onClick = {
                                taskForReminderDialog = null
                                showTypeSelection = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showDailyTimePicker(context) { selectedMillis ->
                                        viewModel.setReminder(task, selectedMillis, isDaily = true, context)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Daily reminder")
                        }
                    }
                }
            },
            confirmButton = {
                if (!showTypeSelection) {
                    TextButton(
                        onClick = {
                            showTypeSelection = true
                        }
                    ) {
                        Text(if (hasReminder) "Change Reminder" else "Set Reminder")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasReminder && !showTypeSelection) {
                        TextButton(
                            onClick = {
                                viewModel.cancelReminder(task, context)
                                taskForReminderDialog = null
                            }
                        ) {
                            Text("Cancel Reminder")
                        }
                    }
                    TextButton(
                        onClick = {
                            taskForReminderDialog = null
                            showTypeSelection = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

fun formatReminderTime(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun showOneTimeDateTimePicker(
    context: Context,
    initialMillis: Long?,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (initialMillis != null && initialMillis > System.currentTimeMillis()) {
        calendar.timeInMillis = initialMillis
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0)
                    val selectedMillis = calendar.timeInMillis
                    if (selectedMillis <= System.currentTimeMillis()) {
                        Toast.makeText(
                            context,
                            "Cannot set reminder in the past",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onDateTimeSelected(selectedMillis)
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showDailyTimePicker(
    context: Context,
    onTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            onTimeSelected(target.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DailyTasksTheme {
        Greeting()
    }
}
