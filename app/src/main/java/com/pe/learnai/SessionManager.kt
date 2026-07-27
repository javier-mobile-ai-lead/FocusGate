package com.pe.learnai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

object SessionManager {
    private val SESSION_DATE = stringPreferencesKey("session_date")
    private val STREAK_COUNT = intPreferencesKey("streak_count")
    private val COMPLETED_DATES = stringSetPreferencesKey("completed_dates")

    fun sessionCompleteFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SESSION_DATE] == LocalDate.now().toString() }

    fun streakFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[STREAK_COUNT] ?: 0 }

    fun historyFlow(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[COMPLETED_DATES] ?: emptySet() }

    suspend fun markComplete(context: Context) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()

        context.dataStore.edit { prefs ->
            if (prefs[SESSION_DATE] == todayStr) return@edit // already marked today

            val lastCompleted = prefs[SESSION_DATE]
            val currentStreak = prefs[STREAK_COUNT] ?: 0
            val newStreak = if (lastCompleted == yesterdayStr) currentStreak + 1 else 1

            prefs[SESSION_DATE] = todayStr
            prefs[STREAK_COUNT] = newStreak
            prefs[COMPLETED_DATES] = (prefs[COMPLETED_DATES] ?: emptySet()) + todayStr
        }
    }
}
