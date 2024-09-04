package com.cyberlabs.schooltime // Replace with your actual package name

import android.content.Context
import com.cyberlabs.schooltime.Data
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val sourceURL = stringPreferencesKey("sourceURL")
}

class MainActivity : AppCompatActivity() {
    private var created = false
    private var url = ""
    private var schedule = JSONObject()

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
}