package com.example.dailytasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytasks.data.TaskItem
import com.example.dailytasks.data.TaskRepository
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

    fun toggleTask(task: TaskItem, isCompleted: Boolean) {
        val updatedList = tasks.value.map {
            if (it.id == task.id) {
                it.copy(isCompleted = isCompleted)
            } else {
                it
            }
        }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
        }
    }

    fun deleteTask(task: TaskItem) {
        val updatedList = tasks.value.filter { it.id != task.id }
        viewModelScope.launch {
            repository.saveTasks(updatedList)
        }
    }
}
