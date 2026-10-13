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
                        if ((!task.isCompleted && (task.reminderTimeMillis != null))) {
                            var triggerTime = task.reminderTimeMillis
                            if (task.isDailyReminder) {
                                while (triggerTime <= currentTime) {
                                    triggerTime += 24 * 60 * 60 * 1000
                                }
                                if (triggerTime != task.reminderTimeMillis) {
                                    val updatedTasks = tasks.map {
                                        if (it.id == task.id) it.copy(reminderTimeMillis = triggerTime) else it
                                    }
                                    repository.saveTasks(updatedTasks)
                                }
                            }
                            if (triggerTime > currentTime) {
                                TaskReminderScheduler.scheduleReminder(
                                    context.applicationContext,
                                    task.id,
                                    task.text,
                                    triggerTime,
                                    isDaily = task.isDailyReminder,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
