package com.pe.learnai.data

data class Phrase(val text: String, val category: String)

object PracticeContent {
    private val allPhrases = listOf(
        Phrase("The weather is beautiful today", "Daily life"),
        Phrase("I enjoy learning new languages every day", "Learning"),
        Phrase("Could you please repeat that more slowly", "Conversation"),
        Phrase("I am working hard to improve my English", "Learning"),
        Phrase("Practice makes perfect if you stay consistent", "Motivation"),
        Phrase("The early bird catches the worm", "Proverb"),
        Phrase("Every cloud has a silver lining", "Proverb"),
        Phrase("Actions speak louder than words", "Proverb"),
        Phrase("Time flies when you are having fun", "Proverb"),
        Phrase("I would like to order a cup of coffee", "Daily life"),
        Phrase("Can you help me find the nearest station", "Travel"),
        Phrase("Learning English opens many new opportunities", "Motivation"),
        Phrase("Speaking practice helps build real confidence", "Learning"),
        Phrase("I really enjoy watching movies in English", "Daily life"),
        Phrase("Could you tell me what time it is please", "Conversation"),
    )

    fun getDailyPhrases(count: Int = 3): List<Phrase> {
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val start = (day * count) % allPhrases.size
        return (0 until count).map { allPhrases[(start + it) % allPhrases.size] }
    }
}
