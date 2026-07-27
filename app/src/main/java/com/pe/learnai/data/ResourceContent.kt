package com.pe.learnai.data

enum class ResourceType(val label: String) {
    YOUTUBE("YouTube"),
    PODCAST("Podcast"),
    WEBSITE("Website"),
}

data class Resource(
    val emoji: String,
    val title: String,
    val description: String,
    val whyItHelps: String,
    val level: String,
    val type: ResourceType,
    val url: String,
)

data class ResourceCategory(
    val title: String,
    val subtitle: String,
    val resources: List<Resource>,
)

object ResourceContent {

    val categories = listOf(

        ResourceCategory(
            title = "🎧 Real Listening",
            subtitle = "Native speed — el salto más importante de TTS a inglés real",
            resources = listOf(
                Resource(
                    emoji = "⚡",
                    title = "Fireship",
                    description = "Videos de 100 segundos y tutoriales de tech en inglés rápido y moderno.",
                    whyItHelps = "Vocabulario dev real a velocidad nativa. Pausa cada 30 seg y repite en voz alta.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=fireship+channel"
                ),
                Resource(
                    emoji = "🤖",
                    title = "Android Developers",
                    description = "Canal oficial de Google — talks, Now in Android, Architecture guides.",
                    whyItHelps = "Exactamente el inglés técnico que usan tus futuros clientes y colegas.",
                    level = "B2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=android+developers+google+channel"
                ),
                Resource(
                    emoji = "🎓",
                    title = "TED-Ed",
                    description = "Lecciones animadas de 5 min en inglés claro, bien articulado, con subtítulos.",
                    whyItHelps = "Ideal para A2. Activa subtítulos en INGLÉS (no español) — ese es el truco.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=ted-ed+lessons"
                ),
                Resource(
                    emoji = "🎙️",
                    title = "Syntax.fm — Web Dev Podcast",
                    description = "Podcast semanal de desarrollo web — conversación casual entre devs.",
                    whyItHelps = "Entrena tu oído para entender inglés conversacional tech, no solo presentaciones.",
                    level = "B1",
                    type = ResourceType.PODCAST,
                    url = "https://syntax.fm"
                ),
                Resource(
                    emoji = "📺",
                    title = "Slow English — Beginner Tech",
                    description = "Videos tech grabados a velocidad reducida, específicos para A2-B1.",
                    whyItHelps = "Puente entre TTS y velocidad nativa real — perfecto para tu nivel actual.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=slow+english+technology+beginners"
                ),
            )
        ),

        ResourceCategory(
            title = "💼 Upwork en inglés",
            subtitle = "Lo específico que necesitas para conseguir tu primer contrato",
            resources = listOf(
                Resource(
                    emoji = "📝",
                    title = "Cómo escribir proposals que ganan",
                    description = "Estructura, tono y frases exactas para propuestas en inglés que convierten.",
                    whyItHelps = "Una buena proposal en inglés es el 80% de ganar el contrato. Empieza aquí.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=how+to+write+upwork+proposal+win"
                ),
                Resource(
                    emoji = "🤝",
                    title = "Comunicación con clientes",
                    description = "Cómo manejar scope, revisiones, deadlines y pagos de forma profesional.",
                    whyItHelps = "Los clientes te juzgan en cada mensaje. Estos videos cubren los escenarios reales.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=freelance+client+communication+english+professional"
                ),
                Resource(
                    emoji = "🧾",
                    title = "Upwork profile en inglés",
                    description = "Cómo escribir tu título, overview y skills para atraer clientes USA/UK.",
                    whyItHelps = "Tu perfil es tu primera impresión — necesita inglés claro y con confianza.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=upwork+profile+tips+english+freelancer"
                ),
                Resource(
                    emoji = "💬",
                    title = "Frases clave para devs freelance",
                    description = "Vocabulario específico: milestone, deliverable, scope creep, retainer, NDA.",
                    whyItHelps = "Si no conoces estos términos en inglés, pierdes credibilidad con clientes senior.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=freelance+english+vocabulary+developers"
                ),
            )
        ),

        ResourceCategory(
            title = "🎙️ Técnica de Shadowing",
            subtitle = "Cómo sacarle el máximo a la práctica de repetición",
            resources = listOf(
                Resource(
                    emoji = "📖",
                    title = "Qué es shadowing y cómo hacerlo",
                    description = "Guía paso a paso de la técnica — por qué funciona y cómo practicarla sola.",
                    whyItHelps = "Entender el método hace que tus sesiones de FocusGate sean 3x más efectivas.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=english+shadowing+technique+how+to+learn"
                ),
                Resource(
                    emoji = "🗣️",
                    title = "Rachel's English — Pronunciación",
                    description = "Pronunciación americana, posición de boca, connected speech y linking.",
                    whyItHelps = "Corrige los patrones de acento que hacen que suenes no-nativo a clientes de USA.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=rachel%27s+english+american+pronunciation"
                ),
                Resource(
                    emoji = "🏃",
                    title = "Shadowing con tech talks",
                    description = "Cómo usar videos de Google I/O, WWDC y conferencias para shadowing real.",
                    whyItHelps = "Practicas el vocabulario exacto que usarás en entrevistas y reuniones de trabajo.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=google+io+tech+talk+english+practice"
                ),
            )
        ),

        ResourceCategory(
            title = "🇺🇸 Entrevistas USA / internacional",
            subtitle = "Específico para conseguir trabajo en empresa extranjera",
            resources = listOf(
                Resource(
                    emoji = "💻",
                    title = "Technical interview en inglés",
                    description = "Cómo explicar tu código, hacer preguntas y pensar en voz alta (think aloud).",
                    whyItHelps = "Empresas USA esperan que narres tu razonamiento. Es una habilidad muy específica.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=software+engineering+technical+interview+english"
                ),
                Resource(
                    emoji = "🧠",
                    title = "STAR Method — preguntas de comportamiento",
                    description = "Situation, Task, Action, Result — el framework que usan TODAS las empresas USA.",
                    whyItHelps = "Cada pregunta behavioral en inglés sigue este patrón. Memorízalo con tus ejemplos.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=star+method+behavioral+interview+english"
                ),
                Resource(
                    emoji = "🌍",
                    title = "Remote work culture en inglés",
                    description = "Comunicación async, Slack etiquette, y dinámica de equipos remotos.",
                    whyItHelps = "La mayoría de roles USA son remotos — la cultura importa tanto como el idioma.",
                    level = "B1",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=remote+work+english+communication+async"
                ),
                Resource(
                    emoji = "📧",
                    title = "Emails profesionales en inglés",
                    description = "Tono, estructura y frases para emails de trabajo con equipos internacionales.",
                    whyItHelps = "El email es tu primera impresión escrita — errores comunes revelan tu nivel real.",
                    level = "A2",
                    type = ResourceType.YOUTUBE,
                    url = "https://www.youtube.com/results?search_query=professional+email+english+work"
                ),
            )
        ),

    )
}
