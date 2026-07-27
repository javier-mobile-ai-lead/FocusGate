package com.pe.learnai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object BlocklistManager {
    private val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")

    val defaultPackages: Set<String> = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.whatsapp",
        "com.instagram.android",
        "com.google.android.youtube",
        "com.twitter.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.facebook.orca",
    )

    fun blockedPackagesFlow(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[BLOCKED_PACKAGES] ?: defaultPackages }

    suspend fun setBlocked(context: Context, pkg: String, blocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_PACKAGES] ?: defaultPackages
            prefs[BLOCKED_PACKAGES] = if (blocked) current + pkg else current - pkg
        }
    }
}
