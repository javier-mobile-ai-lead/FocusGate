package com.pe.learnai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

object SessionManager {
    private val SESSION_DATE = stringPreferencesKey("session_date")

    fun sessionCompleteFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SESSION_DATE] == LocalDate.now().toString() }

    suspend fun markComplete(context: Context) {
        context.dataStore.edit { it[SESSION_DATE] = LocalDate.now().toString() }
    }
}
