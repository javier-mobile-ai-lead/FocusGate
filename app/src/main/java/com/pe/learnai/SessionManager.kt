package com.pe.learnai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

object SessionManager {
    private val SESSION_DATE     = stringPreferencesKey("session_date")      // date goal was last reached (streak)
    private val STREAK_COUNT     = intPreferencesKey("streak_count")
    private val COMPLETED_DATES  = stringSetPreferencesKey("completed_dates")
    private val CLIPS_PER_SESSION= intPreferencesKey("clips_per_session")
    private val SESSIONS_DATE    = stringPreferencesKey("sessions_date")     // date of today's session count
    private val SESSIONS_TODAY   = intPreferencesKey("sessions_today")       // sessions completed today
    private val SESSIONS_TARGET  = intPreferencesKey("sessions_target")      // sessions required per day
    private val UNLOCK_UNTIL     = longPreferencesKey("unlock_until")        // epoch ms: apps unlocked until this time
    private val COOLDOWN_HOURS   = intPreferencesKey("cooldown_hours")       // hours of free access after each session

    // true when all daily sessions are done (goal reached)
    fun sessionCompleteFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[SESSIONS_DATE] != today) return@map false
            (prefs[SESSIONS_TODAY] ?: 0) >= (prefs[SESSIONS_TARGET] ?: 1)
        }

    // true when apps should be accessible RIGHT NOW (cooldown active OR goal reached)
    fun isCurrentlyUnlockedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[SESSIONS_DATE] != today) return@map false
            val unlockUntil = prefs[UNLOCK_UNTIL] ?: 0L
            System.currentTimeMillis() < unlockUntil
        }

    // raw timestamp — used by AppBlockerService for a live time check on every event
    fun unlockUntilFlow(context: Context): Flow<Long> =
        context.dataStore.data.map { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[SESSIONS_DATE] != today) 0L else prefs[UNLOCK_UNTIL] ?: 0L
        }

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

    fun cooldownHoursFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[COOLDOWN_HOURS] ?: 2 }

    fun clipsPerSessionFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[CLIPS_PER_SESSION] ?: 3 }

    suspend fun setSessionsTarget(context: Context, target: Int) {
        context.dataStore.edit { it[SESSIONS_TARGET] = target.coerceIn(1, 10) }
    }

    suspend fun setCooldownHours(context: Context, hours: Int) {
        context.dataStore.edit { it[COOLDOWN_HOURS] = hours.coerceIn(1, 8) }
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
            val cooldownMs = ((prefs[COOLDOWN_HOURS] ?: 2) * 3_600_000L)
            val newSessions = sessionsToday + 1

            prefs[SESSIONS_DATE] = todayStr
            prefs[SESSIONS_TODAY] = newSessions
            prefs[UNLOCK_UNTIL] = if (newSessions >= target) {
                Long.MAX_VALUE  // goal reached — unlocked for the rest of the day
            } else {
                System.currentTimeMillis() + cooldownMs  // cooldown window
            }

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

    // Emergency unlock — bypasses sessions, grants full day unlock
    suspend fun markComplete(context: Context) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()

        context.dataStore.edit { prefs ->
            val target = prefs[SESSIONS_TARGET] ?: 1
            prefs[SESSIONS_DATE] = todayStr
            prefs[SESSIONS_TODAY] = target
            prefs[UNLOCK_UNTIL] = Long.MAX_VALUE

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
