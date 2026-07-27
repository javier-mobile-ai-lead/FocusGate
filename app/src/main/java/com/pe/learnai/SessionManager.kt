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
    private val SESSION_DATE = stringPreferencesKey("session_date")        // date goal was last reached (streak)
    private val STREAK_COUNT = intPreferencesKey("streak_count")
    private val COMPLETED_DATES = stringSetPreferencesKey("completed_dates")
    private val CLIPS_PER_SESSION = intPreferencesKey("clips_per_session") // phrases per round
    private val SESSIONS_DATE = stringPreferencesKey("sessions_date")      // date of today's count
    private val SESSIONS_TODAY = intPreferencesKey("sessions_today")       // sessions completed today
    private val SESSIONS_TARGET = intPreferencesKey("sessions_target")     // sessions required per day

    // true when sessions_today >= sessions_target for today
    fun sessionCompleteFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[SESSIONS_DATE] != today) return@map false
            (prefs[SESSIONS_TODAY] ?: 0) >= (prefs[SESSIONS_TARGET] ?: 1)
        }

    // (done, target) for today — used to show progress in UI
    fun sessionProgressFlow(context: Context): Flow<Pair<Int, Int>> =
        context.dataStore.data.map { prefs ->
            val today = LocalDate.now().toString()
            val done = if (prefs[SESSIONS_DATE] == today) prefs[SESSIONS_TODAY] ?: 0 else 0
            val target = prefs[SESSIONS_TARGET] ?: 1
            Pair(done, target)
        }

    fun streakFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[STREAK_COUNT] ?: 0 }

    fun historyFlow(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[COMPLETED_DATES] ?: emptySet() }

    fun sessionsTargetFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[SESSIONS_TARGET] ?: 1 }

    fun clipsPerSessionFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[CLIPS_PER_SESSION] ?: 3 }

    suspend fun setSessionsTarget(context: Context, target: Int) {
        context.dataStore.edit { it[SESSIONS_TARGET] = target.coerceIn(1, 10) }
    }

    suspend fun setClipsPerSession(context: Context, count: Int) {
        context.dataStore.edit { it[CLIPS_PER_SESSION] = count.coerceIn(1, 15) }
    }

    // Called after each completed practice round
    suspend fun incrementSession(context: Context) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()

        context.dataStore.edit { prefs ->
            val currentDate = prefs[SESSIONS_DATE]
            val sessionsToday = if (currentDate == todayStr) prefs[SESSIONS_TODAY] ?: 0 else 0
            val target = prefs[SESSIONS_TARGET] ?: 1
            val newSessions = sessionsToday + 1

            prefs[SESSIONS_DATE] = todayStr
            prefs[SESSIONS_TODAY] = newSessions

            // Update streak + history the first time target is reached today
            if (newSessions >= target && prefs[SESSION_DATE] != todayStr) {
                val lastGoalDate = prefs[SESSION_DATE]
                val currentStreak = prefs[STREAK_COUNT] ?: 0
                val completedDates = prefs[COMPLETED_DATES] ?: emptySet()
                val newStreak = if (lastGoalDate == yesterdayStr || completedDates.contains(yesterdayStr))
                    currentStreak + 1 else 1
                prefs[SESSION_DATE] = todayStr
                prefs[STREAK_COUNT] = newStreak
                prefs[COMPLETED_DATES] = completedDates + todayStr
            }
        }
    }

    // Emergency unlock — forces sessions_today = target immediately
    suspend fun markComplete(context: Context) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()

        context.dataStore.edit { prefs ->
            val target = prefs[SESSIONS_TARGET] ?: 1
            prefs[SESSIONS_DATE] = todayStr
            prefs[SESSIONS_TODAY] = target

            if (prefs[SESSION_DATE] != todayStr) {
                val lastGoalDate = prefs[SESSION_DATE]
                val currentStreak = prefs[STREAK_COUNT] ?: 0
                val completedDates = prefs[COMPLETED_DATES] ?: emptySet()
                val newStreak = if (lastGoalDate == yesterdayStr) currentStreak + 1 else 1
                prefs[SESSION_DATE] = todayStr
                prefs[STREAK_COUNT] = newStreak
                prefs[COMPLETED_DATES] = completedDates + todayStr
            }
        }
    }
}
