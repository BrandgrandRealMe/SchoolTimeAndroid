package com.cyberlabs.schooltime // Replace with your actual package name

import com.cyberlabs.schooltime.MainActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        val saveButton: Button = findViewById(R.id.btn_save)
        saveButton.setOnClickListener {
            lifecycleScope.launch {
                saveSettings()
            }
        }

        val exitButton: Button = findViewById(R.id.btn_exit)
        exitButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun saveSettings() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val sourceUrl = sharedPreferences.getString("source_url", "https://belljsonghs.pages.dev/api/bell.json?defualt")

        if (sourceUrl != null) {
            saveSetting(sourceUrl)
        } else {
            Toast.makeText(this, "Settings not saved", Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private suspend fun saveSetting(newValue: String) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.sourceURL] = newValue
        }
        Toast.makeText(this, "Settings saved as $newValue", Toast.LENGTH_SHORT).show()
    }
}