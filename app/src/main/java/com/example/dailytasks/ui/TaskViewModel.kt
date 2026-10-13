package com.example.dailytasks.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytasks.data.TaskItem
import com.example.dailytasks.data.TaskRepository
import com.example.dailytasks.util.TaskReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)

    val tasks: StateFlow<List<TaskItem>> = repository.tasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun addTask(text: String) {
        if (text.isNotBlank()) {
            val newTask = TaskItem(text = text.trim())
            val updatedList = tasks.value + newTask
            viewModelScope.launch {
                repository.saveTasks(updatedList)
            }
        }
    }

    fun toggleTask(task: TaskItem, isCompleted: Boolean, context: Context) {
        val updatedList = tasks.value.map {
            if (it.id == task.id) {
                it.copy(
                    isCompleted = isCompleted,
                    reminderTimeMillis = if (isCompleted) null else it.reminderTimeMillis
                )
            } else {
                it
            }
        }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
            if (isCompleted) {
                TaskReminderScheduler.cancelReminder(context, task.id)
            } else if (task.reminderTimeMillis != null && task.reminderTimeMillis > System.currentTimeMillis()) {
                TaskReminderScheduler.scheduleReminder(context, task.id, task.text, task.reminderTimeMillis)
            }
        }
    }

    fun deleteTask(task: TaskItem, context: Context) {
        val updatedList = tasks.value.filter { it.id != task.id }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
            TaskReminderScheduler.cancelReminder(context, task.id)
        }
    }

    fun setReminder(task: TaskItem, reminderTimeMillis: Long, context: Context) {
        val updatedList = tasks.value.map {
            if (it.id == task.id) {
                it.copy(reminderTimeMillis = reminderTimeMillis)
            } else {
                it
            }
        }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
            if (reminderTimeMillis > System.currentTimeMillis()) {
                TaskReminderScheduler.scheduleReminder(context, task.id, task.text, reminderTimeMillis)
            }
        }
    }

    fun cancelReminder(task: TaskItem, context: Context) {
        val updatedList = tasks.value.map {
            if (it.id == task.id) {
                it.copy(reminderTimeMillis = null)
            } else {
                it
            }
        }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
            TaskReminderScheduler.cancelReminder(context, task.id)
        }
    }
}
