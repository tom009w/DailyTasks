package com.example.dailytasks.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailytasks.data.TaskRepository
import com.example.dailytasks.util.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = TaskRepository(context.applicationContext)
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val tasks = repository.tasksFlow.first()
                    val currentTime = System.currentTimeMillis()
                    for (task in tasks) {
                        if (!task.isCompleted && task.reminderTimeMillis != null && task.reminderTimeMillis > currentTime) {
                            TaskReminderScheduler.scheduleReminder(
                                context.applicationContext,
                                task.id,
                                task.text,
                                task.reminderTimeMillis
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
