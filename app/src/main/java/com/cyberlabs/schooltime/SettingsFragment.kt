package com.cyberlabs.schooltime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import com.cyberlabs.schooltime.MainActivity
import androidx.preference.PreferenceFragmentCompat



class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val discordPreference = findPreference("visit_discord") as Preference?
        discordPreference?.setOnPreferenceClickListener {
            val discordUrl = "https://discord.gg/Bm6fMsA"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordUrl))
            startActivity(intent)
            true
        }

        val fdroidPreference = findPreference("visit_fdroid") as Preference?
        fdroidPreference?.setOnPreferenceClickListener {
            val fdroidUrl = "https://github.com/BrandgrandRealMe/SchoolTimeAndroid"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fdroidUrl))
            startActivity(intent)
            true
        }

        val githubPreference = findPreference("visit_github_repo") as Preference?
        githubPreference?.setOnPreferenceClickListener {
            val githubUrl = "https://github.com/BrandgrandRealMe/SchoolTimeAndroid"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
            startActivity(intent)
            true
        }

    }
}
