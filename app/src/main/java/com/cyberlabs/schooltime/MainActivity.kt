package com.cyberlabs.schooltime // Replace with your actual package name

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {
    var nextClass: Bell? = null
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

    suspend fun updatePage() {
        val nextClassInText: TextView = findViewById(R.id.time_left_var_text)
        val periodText: TextView = findViewById(R.id.current_class_var_text)
        val nextPeriodText: TextView = findViewById(R.id.next_class_var_text)
        val nextClassInTextTitle: TextView = findViewById(R.id.time_left_text)
        val periodTextTitle: TextView = findViewById(R.id.current_class_text)
        val nextPeriodTextTitle: TextView = findViewById(R.id.next_class_text)
        val announcementFrame: FrameLayout = findViewById(R.id.announcement_frame)
        val announcementText: TextView = findViewById(R.id.announcement_text)
        val timeText: TextView = findViewById(R.id.current_time_text)

        checkForSettingsUpdate()
        val currentTime = getCurrentTime()
        timeText.text = "Time: ${getCurrentTimeAP()}"

        if (!isJsonEmpty(schedule)) {
            val scheduleStart = schedule.getString("start")
            val scheduleEnd = schedule.getString("end")

            if (isTimeBetween(currentTime, scheduleStart, scheduleEnd)) {
                val scheduleData = Schedule(
                    scheduleStart,
                    scheduleEnd,
                    parseBells(schedule.getJSONArray("bells"))
                )

                val classInfo = getClass(scheduleData, currentTime)

                if (classInfo.currentClass != null) {
                    nextClassInText.visibility = View.VISIBLE
                    periodText.visibility = View.VISIBLE
                    nextPeriodText.visibility = View.VISIBLE
                    nextClassInTextTitle.visibility = View.VISIBLE
                    periodTextTitle.visibility = View.VISIBLE
                    nextPeriodTextTitle.visibility = View.VISIBLE
                    announcementFrame.visibility = View.GONE

                    nextClass = classInfo.nextClass
                    periodText.text = classInfo.currentClass.title
                    nextPeriodText.text = classInfo.nextClass?.title ?: "None"
                    nextClassInText.text = classInfo.timeUntilNextClass?.let { formatSecondsTime(it) } ?: "None"
                } else {
                    // Passing period
                    nextClassInText.visibility = View.VISIBLE
                    periodText.visibility = View.GONE
                    nextPeriodText.visibility = View.GONE
                    nextClassInTextTitle.visibility = View.GONE
                    periodTextTitle.visibility = View.GONE
                    nextPeriodTextTitle.visibility = View.GONE
                    announcementFrame.visibility = View.VISIBLE
                    announcementText.visibility = View.VISIBLE
                    if (nextClass !== null) {
                        nextClassInText.visibility = View.VISIBLE
                        nextClassInText.text = formatSecondsTime(getTimeDifference(currentTime, nextClass!!.startTime))
                    } else {
                        nextClassInText.visibility = View.GONE
                    }
                    announcementText.text = "Passing Period!\nGet To Class!"
                }
            } else {
                // Outside of school hours
                setOutOfSchoolUI(nextClassInText, periodText, nextPeriodText, nextClassInTextTitle,
                    periodTextTitle, nextPeriodTextTitle, announcementFrame, announcementText)
                announcementText.text = "School Is Out!"
            }
        } else {
            // Schedule not loaded yet
            setOutOfSchoolUI(nextClassInText, periodText, nextPeriodText, nextClassInTextTitle,
                periodTextTitle, nextPeriodTextTitle, announcementFrame, announcementText)
            announcementText.text = "Loading Classes!"
        }
    }

    private fun setOutOfSchoolUI(nextClassInText: TextView, periodText: TextView, nextPeriodText: TextView,
                                 nextClassInTextTitle: TextView, periodTextTitle: TextView, nextPeriodTextTitle: TextView,
                                 announcementFrame: FrameLayout, announcementText: TextView) {
        nextClassInText.visibility = View.GONE
        periodText.visibility = View.GONE
        nextPeriodText.visibility = View.GONE
        nextClassInTextTitle.visibility = View.GONE
        periodTextTitle.visibility = View.GONE
        nextPeriodTextTitle.visibility = View.GONE
        announcementFrame.visibility = View.VISIBLE
        announcementText.visibility = View.VISIBLE
    }
}