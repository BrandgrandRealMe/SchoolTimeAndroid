package com.cyberlabs.schooltime // Replace with your actual package name

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SettingsKeys {
    val sourceURL = stringPreferencesKey("sourceURL")
}

data class ClassInfo(
    val currentClass: Bell?,
    val nextClass: Bell?,
    val timeUntilNextClass: Long?
)

interface ScheduleCallback {
    fun onScheduleReceived(jsonObject: JSONObject?)
    fun onScheduleError(errorMessage: String)
}

fun isJsonEmpty(json: JSONObject): Boolean {
    return json.length() == 0
}

fun getTimeDifference(startTime: String, endTime: String): Long {
    val formatter = DateTimeFormatter.ofPattern("h:mm:ssa")
    val startLocalTime = LocalTime.parse(startTime, formatter)
    val endLocalTime = LocalTime.parse(endTime, formatter)

    val startSeconds = startLocalTime.toSecondOfDay().toLong()
    val endSeconds = endLocalTime.toSecondOfDay().toLong()
    var diff = endSeconds - startSeconds
    if (diff < 0) {
        diff += Duration.ofDays(1).seconds
    }
    return diff
}

fun getCurrentTimeAP(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    return currentTime.format(formatter)
}

fun getCurrentTime(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("h:mm:ssa")
    return currentTime.format(formatter)
}


fun formatTime(timeString: String): String {
    // Split the time string by ":" to separate hours, minutes, and seconds
    val parts = timeString.split(":")

    // Extract only hours, minutes, and AM/PM part
    val formattedTime = "${parts[0]}:${parts[1]}${timeString.takeLast(2)}"

    return formattedTime
}

fun formatSecondsTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return "${if (hours > 0) "${hours}h " else ""}" +
            "${if (minutes > 0) "${minutes}m " else ""}" +
            "${remainingSeconds}s"
}

fun isTimeBetween(currentTime: String, startTime: String, endTime: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("h:mma")

    val current = LocalTime.parse(formatTime(currentTime), formatter)
    val start = LocalTime.parse(formatTime(startTime), formatter)
    val end = LocalTime.parse(formatTime(endTime), formatter)

    val currentMinutes = current.hour * 60 + current.minute
    var startMinutes = start.hour * 60 + start.minute
    var endMinutes = end.hour * 60 + end.minute

    if (endMinutes < startMinutes) {
        endMinutes += 24 * 60 // Adjust for crossing midnight
    }

    return currentMinutes in startMinutes until endMinutes
}

fun getSchedule(url: String, callback: ScheduleCallback) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            println("URL: $url") // Log the URL being used
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val stringBuilder = StringBuilder()
                connection.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        stringBuilder.append(line)
                    }
                }
                Data.bellSchedule = stringBuilder.toString()
                val jsonObject = JSONObject(stringBuilder.toString())
                callback.onScheduleReceived(jsonObject)
            } else {
                println("Error fetching JSON: HTTP response code $responseCode")
                callback.onScheduleError("Error fetching JSON: HTTP response code $responseCode")
            }
        } catch (e: Exception) {
            println("Error fetching JSON: ${e.message}")
            e.printStackTrace() // Log the full exception stack trace
            callback.onScheduleError("Error fetching JSON: ${e.message}")
        }
    }
}

fun getClass(schedule: Schedule, currentTime: String): ClassInfo {
    var currentClass: Bell? = null
    var nextClass: Bell? = null
    var timeUntilNextClass: Long? = null

    for ((index, bell) in schedule.bells.withIndex()) {
        if (isTimeBetween(currentTime, bell.startTime, bell.stopTime)) {
            currentClass = bell
            timeUntilNextClass = getTimeDifference(currentTime, bell.stopTime)
            if (index < schedule.bells.size - 1) {
                nextClass = schedule.bells[index + 1]
            }
            break
        }
    }

    if (currentClass == null && schedule.bells.isNotEmpty()) {
        // If we're not in any class, the next class is the first one
        nextClass = schedule.bells.first()
        timeUntilNextClass = getTimeDifference(currentTime, nextClass.startTime)
    }

    return ClassInfo(currentClass, nextClass, timeUntilNextClass)
}

fun parseBells(bellsArray: JSONArray): List<Bell> {
    val bells = mutableListOf<Bell>()
    for (i in 0 until bellsArray.length()) {
        val bellObject = bellsArray.getJSONObject(i)
        val bell = Bell(
            bellObject.getInt("id"),
            bellObject.getString("title"),
            bellObject.getString("startTime"),
            bellObject.getString("stopTime")
        )
        bells.add(bell)
    }
    return bells
}

