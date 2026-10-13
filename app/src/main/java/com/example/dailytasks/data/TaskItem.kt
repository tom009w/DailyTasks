package com.example.dailytasks.data

import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val reminderTimeMillis: Long? = null,
)
