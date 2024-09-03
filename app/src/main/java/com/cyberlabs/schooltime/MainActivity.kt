package com.cyberlabs.schooltime // Replace with your actual package name

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.cyberlabs.schooltime.utils.Utils


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val urlSetting = Utils.getUrlSetting(this);
        val fabSettings: FloatingActionButton = findViewById(R.id.fab_settings)
        fabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}