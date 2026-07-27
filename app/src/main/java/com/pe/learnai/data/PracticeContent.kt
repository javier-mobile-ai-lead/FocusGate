package com.pe.learnai.data

import java.time.LocalDate

enum class Topic(val label: String, val emoji: String, val description: String) {
    HR_INTERVIEW("HR Interview",   "💼", "Job interviews & career"),
    DAILY_STANDUP("Daily Standup", "🖥️", "Agile team ceremonies"),
    DIRECTIONS("Directions",       "🗺️", "Asking & giving directions"),
    MOBILE_DEV("Mobile Dev",       "📱", "App development talk"),
    DAILY_LIFE("Daily Life",       "☀️", "Everyday situations"),
    SMALL_TALK("Small Talk",       "💬", "Casual conversations"),
}

data class Phrase(val text: String, val category: String)

object PracticeContent {

    private val phrasesByTopic: Map<Topic, List<Phrase>> = mapOf(

        Topic.HR_INTERVIEW to listOf(
            Phrase("Tell me a little bit about yourself and your professional background.", "Introduction"),
            Phrase("What would you say is your greatest professional strength?", "Strengths"),
            Phrase("Can you describe a challenging situation and how you handled it?", "Behavior"),
            Phrase("Where do you see yourself professionally in the next five years?", "Goals"),
            Phrase("Why are you interested in leaving your current position?", "Motivation"),
            Phrase("How do you handle working under pressure or tight deadlines?", "Work style"),
            Phrase("Can you walk me through your most recent project in detail?", "Experience"),
            Phrase("What do you consider your most significant professional achievement?", "Achievement"),
            Phrase("How do you keep yourself updated with the latest technology trends?", "Growth"),
            Phrase("Do you have any questions for us about the role or the team?", "Closing"),
        ),

        Topic.DAILY_STANDUP to listOf(
            Phrase("Yesterday I finished implementing the user authentication flow.", "Yesterday"),
            Phrase("Today I'm planning to work on the push notification module.", "Today"),
            Phrase("I'm currently blocked waiting for the API documentation from the backend team.", "Blockers"),
            Phrase("I submitted the pull request and it's ready for code review.", "Update"),
            Phrase("We might need to adjust the sprint goal based on the new requirements.", "Planning"),
            Phrase("I'll need about two more days to complete the feature end to end.", "Estimation"),
            Phrase("The bug was caused by a race condition in the background thread.", "Technical"),
            Phrase("Can we schedule a quick sync after the standup to go over the design?", "Coordination"),
            Phrase("I merged the feature branch and deployed it to the staging environment.", "Deployment"),
            Phrase("I'm going to pair with Sarah today to unblock the onboarding screen issue.", "Collaboration"),
        ),

        Topic.DIRECTIONS to listOf(
            Phrase("Excuse me, could you tell me how to get to the nearest metro station?", "Asking"),
            Phrase("Turn left at the traffic light and walk about two blocks north.", "Giving"),
            Phrase("It's right across the street from the main shopping mall.", "Landmark"),
            Phrase("You'll want to take the number four bus and get off at Central Avenue.", "Transport"),
            Phrase("Sorry, I'm not from around here — you might want to check Google Maps.", "Apologizing"),
            Phrase("Keep going straight for about five minutes and you really can't miss it.", "Distance"),
            Phrase("The office is on the third floor, take the elevator on your right.", "Indoor"),
            Phrase("Make a U-turn here and the entrance will be on your left side.", "Correction"),
            Phrase("Is it within walking distance or should I take a taxi from here?", "Asking"),
            Phrase("Go past the pharmacy, then take the second right after the school.", "Complex"),
        ),

        Topic.MOBILE_DEV to listOf(
            Phrase("I'm building the new feature using Jetpack Compose with a clean MVVM architecture.", "Architecture"),
            Phrase("The API is returning a four oh three error so I need to refresh the auth token.", "Debugging"),
            Phrase("We should write unit tests for this use case before merging it to main.", "Quality"),
            Phrase("The app is crashing on Android twelve due to a background service restriction.", "Issue"),
            Phrase("I optimized the list rendering and reduced the scroll lag by around sixty percent.", "Performance"),
            Phrase("Can you review my pull request? I added comments explaining the tricky parts.", "Collaboration"),
            Phrase("We need to handle the case where the user has no internet connection gracefully.", "Edge case"),
            Phrase("I'm going to refactor this class to follow the single responsibility principle.", "Refactoring"),
            Phrase("The Kotlin Flow is not emitting values because the coroutine scope was cancelled.", "Debugging"),
            Phrase("We should add a loading skeleton instead of a spinner to improve perceived performance.", "UX"),
        ),

        Topic.DAILY_LIFE to listOf(
            Phrase("Could you recommend a good restaurant near the office for a team lunch?", "Food"),
            Phrase("I'll have a large oat milk latte with one shot of espresso, please.", "Ordering"),
            Phrase("Do you know what time the pharmacy on the corner closes on weekdays?", "Services"),
            Phrase("I need to reschedule our meeting — something urgent came up this afternoon.", "Calendar"),
            Phrase("The traffic was really bad this morning so I ended up taking the subway.", "Commute"),
            Phrase("Could you keep it down a bit? I'm currently on an important video call.", "Office"),
            Phrase("I'm going to grab a quick lunch — do you want me to bring you anything?", "Kindness"),
            Phrase("Let's catch up over coffee sometime this week if you're available.", "Social"),
            Phrase("I have a dentist appointment at noon so I'll be back by early afternoon.", "Schedule"),
            Phrase("Could you sign for the package? I'll be in a meeting until three o'clock.", "Favor"),
        ),

        Topic.SMALL_TALK to listOf(
            Phrase("How was your weekend? Did you do anything fun or interesting?", "Weekend"),
            Phrase("Have you seen the series everyone has been talking about lately?", "Entertainment"),
            Phrase("The weather has been so unpredictable this week, hasn't it?", "Weather"),
            Phrase("Are you going to the team lunch on Friday? I heard it's at a really nice place.", "Team"),
            Phrase("I can't believe the holidays are already coming up so fast this year.", "Holidays"),
            Phrase("How long have you been working in the software industry?", "Background"),
            Phrase("I'm still getting used to the new office layout — it's quite different.", "Office"),
            Phrase("Have you tried the new coffee machine? It actually makes a decent espresso.", "Office"),
            Phrase("Are you watching the World Cup? Last night's match was absolutely incredible.", "Sports"),
            Phrase("I just got back from vacation — I really needed that break to recharge.", "Life"),
        ),
    )

    fun getPhrasesForTopic(topic: Topic, count: Int = 3): List<Phrase> {
        val phrases = phrasesByTopic[topic] ?: return emptyList()
        val dayOfYear = LocalDate.now().dayOfYear
        val start = (dayOfYear * count) % phrases.size
        return (0 until count).map { phrases[(start + it) % phrases.size] }
    }

    // Fallback: rotates through topics by day
    fun getDailyPhrases(count: Int = 3): List<Phrase> {
        val topic = Topic.values()[LocalDate.now().dayOfYear % Topic.values().size]
        return getPhrasesForTopic(topic, count)
    }
}
