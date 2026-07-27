package com.pe.learnai.data

import android.content.Context
import android.util.Log
import java.time.LocalDate

enum class Topic(val label: String, val emoji: String, val description: String) {
    FOUNDATIONS("Foundations",     "🗣️", "Conversational essentials"),
    HR_INTERVIEW("HR Interview",   "💼", "Job interviews & career"),
    DAILY_STANDUP("Daily Standup", "🖥️", "Agile team ceremonies"),
    DIRECTIONS("Directions",       "🗺️", "Asking & giving directions"),
    MOBILE_DEV("Mobile Dev",       "📱", "App development talk"),
    DAILY_LIFE("Daily Life",       "☀️", "Everyday situations"),
    SMALL_TALK("Small Talk",       "💬", "Casual conversations"),
}

data class ConvTurn(
    val isApp: Boolean,
    val text: String
)

data class Conversation(
    val title: String,
    val turns: List<ConvTurn>
)

object PracticeContent {

    private var convsByTopic: Map<Topic, List<Conversation>> = emptyMap()

    fun initialize(context: Context) {
        val result = mutableMapOf<Topic, MutableList<Conversation>>()
        try {
            context.assets.open("practice_content.txt").bufferedReader().use { reader ->
                var currentTopic: Topic? = null
                var currentTitle = ""
                var currentTurns = mutableListOf<ConvTurn>()

                fun saveConv() {
                    if (currentTopic != null && currentTurns.isNotEmpty()) {
                        result.getOrPut(currentTopic!!) { mutableListOf() }
                            .add(Conversation(currentTitle, currentTurns.toList()))
                    }
                }

                reader.forEachLine { raw ->
                    val line = raw.trim()
                    when {
                        line.startsWith("# CONV:") -> {
                            saveConv()
                            val parts = line.removePrefix("# CONV:").split("|")
                            currentTopic = Topic.values().find { it.name == parts[0].trim() }
                            currentTitle = if (parts.size >= 2) parts[1].trim() else ""
                            currentTurns = mutableListOf()
                        }
                        line.startsWith("APP:") ->
                            currentTurns.add(ConvTurn(isApp = true, text = line.removePrefix("APP:").trim()))
                        line.startsWith("YOU:") ->
                            currentTurns.add(ConvTurn(isApp = false, text = line.removePrefix("YOU:").trim()))
                    }
                }
                saveConv()
            }
        } catch (e: Exception) {
            Log.e("PracticeContent", "Failed to load practice_content.txt", e)
        }
        convsByTopic = result
    }

    fun getConversationForTopic(topic: Topic): Conversation? {
        val convs = convsByTopic[topic] ?: return null
        if (convs.isEmpty()) return null
        return convs[LocalDate.now().dayOfYear % convs.size]
    }
}
