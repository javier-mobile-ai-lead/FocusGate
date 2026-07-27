package com.pe.learnai

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionManager {
    private const val PREFS = "focus_gate_prefs"
    private const val KEY_DATE = "session_date"

    fun isSessionComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DATE, null) == today()
    }

    fun markComplete(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATE, today())
            .apply()
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
