package com.pe.learnai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pe.learnai.data.PracticeContent
import com.pe.learnai.data.Phrase
import com.pe.learnai.ui.theme.AILearnEngTheme
import java.util.Locale

private enum class RoundState { IDLE, TTS_SPEAKING, MIC_LISTENING, SUCCESS, FAIL }

class PracticeActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    private val roundState = mutableStateOf(RoundState.IDLE)
    private val currentRoundIndex = mutableStateOf(0)
    private val recognizedText = mutableStateOf("")
    private val attempts = mutableStateOf(0)
    private val sessionDone = mutableStateOf(false)
    private val ttsReady = mutableStateOf(false)
    private val hasMicPermission = mutableStateOf(false)
    private val statusMessage = mutableStateOf("")

    private val phrases: List<Phrase> by lazy { PracticeContent.getDailyPhrases(3) }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission.value = granted
        if (!granted) statusMessage.value = "Microphone permission is needed for speaking practice."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkMicPermission()
        setupTts()

        setContent {
            AILearnEngTheme {
                PracticeScreen(
                    phrases = phrases,
                    currentRound = currentRoundIndex.value,
                    roundState = roundState.value,
                    recognizedText = recognizedText.value,
                    attempts = attempts.value,
                    sessionDone = sessionDone.value,
                    hasMicPermission = hasMicPermission.value,
                    statusMessage = statusMessage.value,
                    onListenClick = { speakPhrase(phrases[currentRoundIndex.value].text) },
                    onSpeakClick = { startListening() },
                    onNextRound = { advanceRound() },
                    onTryAgain = {
                        roundState.value = RoundState.IDLE
                        recognizedText.value = ""
                        statusMessage.value = ""
                    },
                    onDone = { finish() }
                )
            }
        }
    }

    private fun checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            hasMicPermission.value = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        roundState.value = RoundState.TTS_SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        roundState.value = RoundState.IDLE
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        roundState.value = RoundState.IDLE
                    }
                })
                ttsReady.value = true
            }
        }
    }

    private fun speakPhrase(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "phrase")
    }

    private fun startListening() {
        if (!hasMicPermission.value) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusMessage.value = "Speech recognition is not available on this device."
            return
        }
        tts?.stop()
        roundState.value = RoundState.MIC_LISTENING
        recognizedText.value = ""
        statusMessage.value = ""
        attempts.value++

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull() ?: ""
                    recognizedText.value = best
                    val target = phrases[currentRoundIndex.value].text
                    if (similarity(best, target) >= 0.55f) {
                        roundState.value = RoundState.SUCCESS
                        statusMessage.value = "Great job!"
                    } else {
                        roundState.value = RoundState.FAIL
                        statusMessage.value = "Try again — say it more clearly."
                    }
                }
                override fun onError(error: Int) {
                    recognizedText.value = ""
                    roundState.value = RoundState.FAIL
                    statusMessage.value = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again!"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap Speak and try again."
                        else -> "Error listening. Please try again."
                    }
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        }
    }

    private fun advanceRound() {
        val next = currentRoundIndex.value + 1
        if (next >= phrases.size) {
            SessionManager.markComplete(this)
            sessionDone.value = true
        } else {
            currentRoundIndex.value = next
            roundState.value = RoundState.IDLE
            recognizedText.value = ""
            attempts.value = 0
            statusMessage.value = ""
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    private fun similarity(recognized: String, target: String): Float {
        fun normalize(s: String) = s.lowercase(Locale.US)
            .replace(Regex("[^a-z ]"), "")
            .split(" ")
            .filter { it.isNotEmpty() }
            .toSet()
        val r = normalize(recognized)
        val t = normalize(target)
        if (t.isEmpty()) return 0f
        return r.intersect(t).size.toFloat() / t.size.toFloat()
    }
}

@Composable
private fun PracticeScreen(
    phrases: List<Phrase>,
    currentRound: Int,
    roundState: RoundState,
    recognizedText: String,
    attempts: Int,
    sessionDone: Boolean,
    hasMicPermission: Boolean,
    statusMessage: String,
    onListenClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onNextRound: () -> Unit,
    onTryAgain: () -> Unit,
    onDone: () -> Unit
) {
    val bg = Color(0xFF0D0D1A)
    val cardBg = Color(0xFF1A1A2E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (sessionDone) {
            SessionCompleteView(onDone = onDone)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Practice",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${currentRound + 1} / ${phrases.size}",
                        fontSize = 14.sp,
                        color = Color(0xFF7B8BB2)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { (currentRound.toFloat() + if (roundState == RoundState.SUCCESS) 1f else 0f) / phrases.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF2A2A3E)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Phrase card
                if (phrases.isNotEmpty()) {
                    val phrase = phrases[currentRound]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = phrase.category,
                                fontSize = 12.sp,
                                color = Color(0xFF7B8BB2),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = phrase.text,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Instructions
                Text(
                    text = "1. Listen to the phrase\n2. Repeat it out loud",
                    fontSize = 14.sp,
                    color = Color(0xFF7B8BB2),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onListenClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = roundState == RoundState.IDLE || roundState == RoundState.FAIL,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            disabledContainerColor = Color(0xFF2A2A3E)
                        )
                    ) {
                        Text(
                            text = if (roundState == RoundState.TTS_SPEAKING) "Playing..." else "🔊 Listen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onSpeakClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = (roundState == RoundState.IDLE || roundState == RoundState.FAIL) && hasMicPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7B1FA2),
                            disabledContainerColor = Color(0xFF2A2A3E)
                        )
                    ) {
                        Text(
                            text = if (roundState == RoundState.MIC_LISTENING) "Listening..." else "🎤 Speak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feedback area
                AnimatedVisibility(
                    visible = roundState == RoundState.SUCCESS || roundState == RoundState.FAIL || recognizedText.isNotEmpty()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (roundState == RoundState.SUCCESS) Color(0xFF1B3A1B) else Color(0xFF3A1B1B),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (recognizedText.isNotEmpty()) {
                            Text(
                                text = "You said: \"$recognizedText\"",
                                fontSize = 14.sp,
                                color = Color(0xFFCCCCCC),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (statusMessage.isNotEmpty()) {
                            Text(
                                text = statusMessage,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (roundState == RoundState.SUCCESS) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Next / Try again
                when (roundState) {
                    RoundState.SUCCESS -> {
                        Button(
                            onClick = onNextRound,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(
                                text = if (currentRound + 1 >= phrases.size) "Complete Session ✓" else "Next Phrase →",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    RoundState.FAIL -> {
                        OutlinedButton(
                            onClick = onTryAgain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A))
                        ) {
                            Text("Try Again", fontSize = 15.sp)
                        }
                    }
                    else -> {}
                }

                if (attempts > 0 && roundState != RoundState.SUCCESS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Attempts: $attempts",
                        fontSize = 12.sp,
                        color = Color(0xFF555577)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteView(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color(0xFF1B3A1B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", fontSize = 48.sp, color = Color(0xFF81C784))
        }

        Text(
            text = "Session Complete!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "All blocked apps are now unlocked for today. Keep up the great work!",
            fontSize = 16.sp,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Go Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
