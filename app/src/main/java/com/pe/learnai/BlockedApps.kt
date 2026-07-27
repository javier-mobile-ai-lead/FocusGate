package com.pe.learnai

data class AppInfo(val pkg: String, val name: String, val emoji: String)

object BlockedApps {
    val catalog = listOf(
        AppInfo("com.zhiliaoapp.musically", "TikTok", "🎵"),
        AppInfo("com.ss.android.ugc.trill", "TikTok (Regional)", "🎵"),
        AppInfo("com.whatsapp", "WhatsApp", "💬"),
        AppInfo("com.instagram.android", "Instagram", "📷"),
        AppInfo("com.google.android.youtube", "YouTube", "▶️"),
        AppInfo("com.twitter.android", "X / Twitter", "🐦"),
        AppInfo("com.facebook.katana", "Facebook", "👥"),
        AppInfo("com.snapchat.android", "Snapchat", "👻"),
        AppInfo("com.reddit.frontpage", "Reddit", "🤖"),
        AppInfo("com.facebook.orca", "Messenger", "💭"),
        AppInfo("com.discord", "Discord", "🎮"),
        AppInfo("com.linkedin.android", "LinkedIn", "💼"),
        AppInfo("com.pinterest", "Pinterest", "📌"),
        AppInfo("com.twitch.android.viewer", "Twitch", "🎮"),
    )
}
