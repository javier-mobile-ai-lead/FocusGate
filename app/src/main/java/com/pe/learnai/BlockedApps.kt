package com.pe.learnai

object BlockedApps {
    val packages = setOf(
        "com.zhiliaoapp.musically",    // TikTok
        "com.ss.android.ugc.trill",    // TikTok (alt)
        "com.whatsapp",                // WhatsApp
        "com.instagram.android",       // Instagram
        "com.google.android.youtube",  // YouTube
        "com.twitter.android",         // X / Twitter
        "com.facebook.katana",         // Facebook
        "com.snapchat.android",        // Snapchat
        "com.reddit.frontpage",        // Reddit
        "com.facebook.orca",           // Messenger
    )

    fun isBlocked(pkg: String) = pkg in packages
}
