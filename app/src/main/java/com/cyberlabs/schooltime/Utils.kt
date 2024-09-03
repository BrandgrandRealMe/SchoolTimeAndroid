package com.cyberlabs.schooltime.utils


import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

class Utils {
    companion object {
        fun getUrlSetting(context: Context): String {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("url_setting", "default_url") ?: "default_url"
        }
    }
}

