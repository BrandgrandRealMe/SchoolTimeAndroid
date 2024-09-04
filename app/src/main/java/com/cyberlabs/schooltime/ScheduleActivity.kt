package com.cyberlabs.schooltime;

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val scheduleParser = ScheduleParser(this)

        lifecycleScope.launch {
            updateSchedule(scheduleParser)
        }
    }

    private suspend fun updateSchedule(scheduleParser: ScheduleParser) {
        val parentView: ViewGroup = findViewById(R.id.schedule_container)
        withContext(Dispatchers.IO) {
            val bellSchedule = Data.bellSchedule
            if (bellSchedule !== null) {
                scheduleParser.createTextBoxes(bellSchedule, parentView)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}