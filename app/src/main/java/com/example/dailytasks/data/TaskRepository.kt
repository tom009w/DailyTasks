package com.example.dailytasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_tasks_prefs")

class TaskRepository(private val appContext: Context) {

    private val tasksKey = stringPreferencesKey("tasks_json")

    val tasksFlow: Flow<List<TaskItem>> = appContext.dataStore.data.map { preferences ->
        val jsonString = preferences[tasksKey] ?: "[]"
        parseJsonToTasks(jsonString)
    }

    suspend fun saveTasks(tasks: List<TaskItem>) {
        val jsonString = parseTasksToJson(tasks)
        appContext.dataStore.edit { preferences ->
            preferences[tasksKey] = jsonString
        }
    }

    private fun parseJsonToTasks(jsonString: String): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val reminderTime = if (obj.has("reminderTimeMillis") && !obj.isNull("reminderTimeMillis")) {
                    obj.getLong("reminderTimeMillis")
                } else {
                    null
                }
                list.add(
                    TaskItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        isCompleted = obj.getBoolean("isCompleted"),
                        reminderTimeMillis = reminderTime,
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseTasksToJson(tasks: List<TaskItem>): String {
        val jsonArray = JSONArray()
        for ((id, text, isCompleted, reminderTimeMillis) in tasks) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("text", text)
            obj.put("isCompleted", isCompleted)
            if (reminderTimeMillis != null) {
                obj.put("reminderTimeMillis", reminderTimeMillis)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
