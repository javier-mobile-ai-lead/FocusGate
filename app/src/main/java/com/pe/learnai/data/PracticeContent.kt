package com.pe.learnai.data

import android.content.Context
import android.util.Log
import java.time.LocalDate

enum class Topic(val label: String, val emoji: String, val description: String) {
    HR_INTERVIEW("HR Interview",   "💼", "Job interviews & career"),
    DAILY_STANDUP("Daily Standup", "🖥️", "Agile team ceremonies"),
    DIRECTIONS("Directions",       "🗺️", "Asking & giving directions"),
    MOBILE_DEV("Mobile Dev",       "📱", "App development talk"),
    DAILY_LIFE("Daily Life",       "☀️", "Everyday situations"),
    SMALL_TALK("Small Talk",       "💬", "Casual conversations"),
}

data class Phrase(val text: String, val category: String, val level: String = "B1")

object PracticeContent {

    private var phrasesByTopic: Map<Topic, List<Phrase>> = emptyMap()

    fun initialize(context: Context) {
        val result = mutableMapOf<Topic, MutableList<Phrase>>()
        try {
            context.assets.open("practice_content.txt").bufferedReader().use { reader ->
                var currentTopic: Topic? = null
                reader.forEachLine { raw ->
                    val line = raw.trim()
                    when {
                        line.startsWith("# TOPIC:") -> {
                            val topicKey = line.removePrefix("# TOPIC:").split("|")[0].trim()
                            currentTopic = Topic.values().find { it.name == topicKey }
                        }
                        line.isEmpty() || line.startsWith("#") -> Unit
                        else -> {
                            val parts = line.split("|").map { it.trim() }
                            if (parts.size >= 3 && currentTopic != null) {
                                val phrase = Phrase(
                                    text = parts[2],
                                    category = parts[1],
                                    level = parts[0]
                                )
                                result.getOrPut(currentTopic!!) { mutableListOf() }.add(phrase)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PracticeContent", "Failed to load practice_content.txt", e)
        }
        phrasesByTopic = result
    }

    fun getPhrasesForTopic(topic: Topic, count: Int = 3): List<Phrase> {
        val phrases = phrasesByTopic[topic] ?: return emptyList()
        if (phrases.isEmpty()) return emptyList()
        val dayOfYear = LocalDate.now().dayOfYear
        val start = (dayOfYear * count) % phrases.size
        return (0 until count).map { phrases[(start + it) % phrases.size] }
    }

    fun getDailyPhrases(count: Int = 3): List<Phrase> {
        val topic = Topic.values()[LocalDate.now().dayOfYear % Topic.values().size]
        return getPhrasesForTopic(topic, count)
    }
}
