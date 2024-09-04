package com.cyberlabs.schooltime

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

data class Bell(val id: Int, val title: String, val startTime: String, val stopTime: String)
data class Schedule(val start: String, val end: String, val bells: List<Bell>)

class ScheduleParser(private val context: Context) {
    fun createTextBoxes(jsonString: String, parent: ViewGroup) {
        println("Error fetching schedule data: $jsonString")
        val jsonObject = JSONObject(jsonString)
        val schedule = Schedule(
            jsonObject.getString("start"),
            jsonObject.getString("end"),
            parseBells(jsonObject.getJSONArray("bells"))
        )

        var previousTextViewId = View.NO_ID // Initialize with NO_ID
        for (bell in schedule.bells) {
            val textView = TextView(context)
            textView.id = View.generateViewId() // Generate a unique id for each TextView
            textView.text = "${bell.title} ${bell.startTime} - ${bell.stopTime}"

            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.BELOW, previousTextViewId)
            textView.layoutParams = layoutParams

            parent.addView(textView)

            previousTextViewId = textView.id // Update previousTextViewId to the current TextView's id
        }
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
}