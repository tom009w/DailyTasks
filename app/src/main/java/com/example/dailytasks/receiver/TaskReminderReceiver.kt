package com.example.dailytasks.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dailytasks.MainActivity
import com.example.dailytasks.data.TaskRepository
import com.example.dailytasks.util.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskText = intent.getStringExtra("TASK_TEXT") ?: "Task Reminder"
        val isDaily = intent.getBooleanExtra("IS_DAILY", false)

        val channelId = "task_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for task reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Daily Task Reminder")
            .setContentText(taskText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)

        // If it's a daily reminder, schedule next occurrence for 24 hours later if task is still incomplete
        if (isDaily) {
            val appContext = context.applicationContext
            val repository = TaskRepository(appContext)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val tasks = repository.tasksFlow.first()
                    val task = tasks.find { it.id == taskId }
                    if (((task != null) && !task.isCompleted) && task.isDailyReminder) {
                        val nextTrigger = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                        val updatedTasks = tasks.map {
                            if (it.id == taskId) it.copy(reminderTimeMillis = nextTrigger) else it
                        }
                        repository.saveTasks(updatedTasks)
                        TaskReminderScheduler.scheduleReminder(appContext, taskId, taskText, nextTrigger, isDaily = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
