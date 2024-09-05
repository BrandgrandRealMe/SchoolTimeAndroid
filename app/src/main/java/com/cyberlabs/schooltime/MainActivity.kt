package com.cyberlabs.schooltime // Replace with your actual package name

import android.content.Context
import com.cyberlabs.schooltime.Data
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val sourceURL = stringPreferencesKey("sourceURL")
}

class MainActivity : AppCompatActivity() {
    private var created = false
    private var url = ""
    private var schedule = JSONObject()
    private lateinit var updateJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fabSettings: FloatingActionButton = findViewById(R.id.fab_settings)
        fabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val fabSchedule: FloatingActionButton = findViewById(R.id.fab_schedule)
        fabSchedule.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        }

        created = true
        lifecycleScope.launch {
            checkForSettingsUpdate()

        }

        // Start the periodic update
        startPeriodicUpdate()
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

    private fun startPeriodicUpdate() {
        updateJob = lifecycleScope.launch {
            while (isActive) {
                updatePage()
                delay(500) // 500 milliseconds = 0.5 seconds
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the update job when the activity is destroyed
        if (::updateJob.isInitialized) {
            updateJob.cancel()
        }
    }

    fun getCurrentTimeAP(): String {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return currentTime.format(formatter)
    }
    private fun parseBells(bellsArray: JSONArray): List<Bell> {
        val bells = mutableListOf<Bell>()
        Log.d("Bells", bells.toString())
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
    fun getCurrentTime(): String {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("h:mm:ssa")
        return currentTime.format(formatter)
    }

    private suspend fun getSetting(): String? {
        return dataStore.data.map { preferences ->
            preferences[SettingsKeys.sourceURL]
        }.distinctUntilChanged()
            .first()
    }


    suspend fun checkForSettingsUpdate(): Boolean {
        val urlFromSettings = getSetting()
        if (urlFromSettings != url) {
            if (urlFromSettings != null) {
                url = urlFromSettings
            }
            updateSchedule(url)
            return true
        }
        return false
    }
    interface ScheduleCallback {
        fun onScheduleReceived(jsonObject: JSONObject?)
        fun onScheduleError(errorMessage: String)
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

    private fun updateSchedule(url: String) {
        getSchedule(url, object : ScheduleCallback {
            override fun onScheduleReceived(jsonObject: JSONObject?) {
                schedule = jsonObject ?: JSONObject()
                // Update UI or perform any other actions with the fetched JSON data
            }

            override fun onScheduleError(errorMessage: String) {
                // Handle the error case, such as displaying an error message
                println("Error fetching schedule data: $errorMessage")
            }
        })
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

    suspend fun updatePage() {
        var nextClass: Bell? = null
        var currentClass: String? = null
        var nextClassIn: Long? = null
        var nextClassInString: String? = null

        val timeText: TextView = findViewById(R.id.current_time_text)

        checkForSettingsUpdate()
        val currentTime = getCurrentTime() // Ensure this returns a string in "HH:mm:ss" format
        timeText.text = "Time: ${getCurrentTimeAP()}" // This can still be in 12-hour format for display

        if (!isJsonEmpty(schedule)) {
            val scheduleData = Schedule(
                schedule.getString("start"),
                schedule.getString("end"),
                parseBells(schedule.getJSONArray("bells"))
            )

            for (bell in scheduleData.bells) {
                val startTime = bell.startTime
                val endTime = bell.stopTime
                if (isTimeBetween(currentTime, startTime, endTime)) {
                    println("found Class")
                    // Current time is between start and end time of this bell
                    currentClass = bell.title
                    nextClassIn = getTimeDifference(currentTime, endTime)
                    nextClassInString = "${formatSecondsTime(nextClassIn)}"
                    val bellIndex = scheduleData.bells.indexOf(bell)
                    if (bellIndex < scheduleData.bells.size - 1) {
                        nextClass = scheduleData.bells[bellIndex + 1]
                    }
                }

            }

            val nextClassInText: TextView = findViewById(R.id.time_left_var_text)
            nextClassInText.text = nextClassInString ?: "None"

            val periodText: TextView = findViewById(R.id.current_class_var_text)
            periodText.text = currentClass ?: "None"

            val nextPeriodText: TextView = findViewById(R.id.next_class_var_text)
            nextPeriodText.text = nextClass?.title ?: "None"
        } else {
            val periodText: TextView = findViewById(R.id.current_class_var_text)
            periodText.text = "Loading Schedule"
        }
    }
}