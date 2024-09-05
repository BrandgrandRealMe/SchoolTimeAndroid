package com.cyberlabs.schooltime

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

data class Bell(val id: Int, val title: String, val startTime: String, val stopTime: String)
data class Schedule(val start: String, val end: String, val bells: List<Bell>)

class ScheduleParser(private val context: Context) {
    fun formatTime(timeString: String): String {
        // Split the time string by ":" to separate hours, minutes, and seconds
        val parts = timeString.split(":")

        // Extract only hours, minutes, and AM/PM part
        val formattedTime = "${parts[0]}:${parts[1]}${timeString.takeLast(2)}"

        return formattedTime
    }

    fun createTextBoxes(jsonString: String, parent: ViewGroup) {
        println("Error fetching schedule data: $jsonString")
        val jsonObject = JSONObject(jsonString)
        val schedule = Schedule(
            jsonObject.getString("start"),
            jsonObject.getString("end"),
            parseBells(jsonObject.getJSONArray("bells"))
        )

        for (bell in schedule.bells) {
            val textView = TextView(context)
            textView.text = "${bell.title} ${formatTime(bell.startTime)} - ${formatTime(bell.stopTime)}"

            // Add padding to the TextView
            val paddingInDp = 8
            val scale = context.resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
            textView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, 0, 16) // Add some bottom margin for spacing

            textView.layoutParams = layoutParams
            parent.addView(textView)
        }
    }
}

    private fun parseBells(bellsArray: JSONArray): List<Bell> {
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
