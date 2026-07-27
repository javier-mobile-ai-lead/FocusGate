package com.pe.learnai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pe.learnai.data.ConvTurn
import com.pe.learnai.data.PracticeContent
import com.pe.learnai.data.Topic
import com.pe.learnai.ui.theme.AILearnEngTheme
import kotlinx.coroutines.launch
import java.util.Locale

// ── State machine ─────────────────────────────────────────────────────────────

private sealed class CS {
    object AppSpeaking : CS()
    object UserReady : CS()
    object UserRecording : CS()
    data class UserResult(val score: Float, val recognized: String, val passed: Boolean) : CS()
    object ConvDone : CS()
}

// Chat history entry shown as a bubble
private data class ChatEntry(
    val isApp: Boolean,
    val text: String,
    val recognized: String? = null,
    val score: Float? = null,
    val passed: Boolean? = null
)

// ── Activity ──────────────────────────────────────────────────────────────────

class PracticeActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val toneHandler = Handler(Looper.getMainLooper())

    private val state = mutableStateOf<CS>(CS.AppSpeaking)
    private val turnIndex = mutableStateOf(0)
    private val chatHistory = mutableStateListOf<ChatEntry>()
    private val hasMicPerm = mutableStateOf(false)
    private var recognizerGen = 0

    private val topic by lazy {
        val ord = intent.getIntExtra("topic_ordinal", -1)
        if (ord >= 0 && ord < Topic.values().size) Topic.values()[ord] else null
    }
    private val conversation by lazy {
        topic?.let { PracticeContent.getConversationForTopic(it) }
    }
    private val turns get() = conversation?.turns ?: emptyList()
    private val topicLabel by lazy { topic?.let { "${it.emoji}  ${it.label}" } ?: "Practice" }

    private val micLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPerm.value = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkMicPerm()
        initTts()
        setContent {
            AILearnEngTheme {
                ConversationScreen(
                    topicLabel = topicLabel,
                    conversationTitle = conversation?.title ?: "",
                    chatHistory = chatHistory,
                    state = state.value,
                    turns = turns,
                    hasMicPerm = hasMicPerm.value,
                    onRecord = ::startRecording,
                    onContinue = ::advanceTurn,
                    onRetry = {
                        if (chatHistory.isNotEmpty() && !chatHistory.last().isApp) {
                            chatHistory.removeAt(chatHistory.size - 1)
                        }
                        state.value = CS.UserReady
                    },
                    onBack = ::finish
                )
            }
        }
    }

    private fun checkMicPerm() {
        hasMicPerm.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPerm.value) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id?.startsWith("app_") == true) mainHandler.post { advanceTurn() }
                }
                @Deprecated("Deprecated")
                override fun onError(id: String?) {
                    if (id?.startsWith("app_") == true) mainHandler.post { advanceTurn() }
                }
            })
            mainHandler.post { beginConversation() }
        }
    }

    private fun beginConversation() {
        if (turns.isEmpty()) { state.value = CS.ConvDone; return }
        turnIndex.value = 0
        chatHistory.clear()
        processCurrentTurn()
    }

    private fun processCurrentTurn() {
        val idx = turnIndex.value
        if (idx >= turns.size) {
            state.value = CS.ConvDone
            vibrateComplete()
            playCompleteSound()
            return
        }
        val turn = turns[idx]
        if (turn.isApp) {
            state.value = CS.AppSpeaking
            chatHistory.add(ChatEntry(isApp = true, text = turn.text))
            tts?.speak(turn.text, TextToSpeech.QUEUE_FLUSH, null, "app_$idx")
        } else {
            state.value = CS.UserReady
        }
    }

    private fun advanceTurn() {
        turnIndex.value++
        processCurrentTurn()
    }

    private fun startRecording() {
        if (!hasMicPerm.value) { micLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        tts?.stop()
        state.value = CS.UserRecording

        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.cancel()

        val gen = ++recognizerGen
        val expectedText = turns.getOrNull(turnIndex.value)?.text ?: ""

        recognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                if (gen != recognizerGen) return
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                val score = similarity(best, expectedText)
                val passed = score >= 0.25f
                chatHistory.add(ChatEntry(
                    isApp = false, text = expectedText,
                    recognized = best, score = score, passed = passed
                ))
                state.value = CS.UserResult(score, best, passed)
                if (passed) { vibrateSuccess(); playSuccessSound() }
                else { vibrateFail(); playFailSound() }
            }
            override fun onError(error: Int) {
                if (gen != recognizerGen) return
                state.value = CS.UserReady
            }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(buf: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        recognizer!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    private fun playSuccessSound() {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        tg.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        toneHandler.postDelayed({ tg.release() }, 350)
    }

    private fun playFailSound() {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        tg.startTone(ToneGenerator.TONE_PROP_NACK, 300)
        toneHandler.postDelayed({ tg.release() }, 450)
    }

    private fun playCompleteSound() {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        tg.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        toneHandler.postDelayed({ tg.startTone(ToneGenerator.TONE_PROP_ACK, 150) }, 220)
        toneHandler.postDelayed({ tg.startTone(ToneGenerator.TONE_CDMA_ANSWER, 250) }, 440)
        toneHandler.postDelayed({ tg.release() }, 750)
    }

    private fun vibrateSuccess() =
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))

    private fun vibrateFail() =
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60), -1))

    private fun vibrateComplete() =
        vibrator.vibrate(VibrationEffect.createWaveform(
            longArrayOf(0, 100, 80, 200), intArrayOf(0, 200, 0, 255), -1))

    override fun onDestroy() {
        toneHandler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop(); tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    private fun similarity(recognized: String, target: String): Float {
        val stopWords = setOf(
            "i", "a", "an", "the", "is", "am", "are", "was", "were", "be", "been",
            "it", "that", "this", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "have", "has", "had", "do", "does", "did",
            "will", "would", "could", "should", "may", "might", "not", "no", "so",
            "just", "also", "very", "really", "quite", "my", "your", "we", "you",
            "me", "us", "they", "their", "our", "its", "as", "if", "when", "about"
        )
        fun norm(s: String) = s.lowercase(Locale.US)
            .replace(Regex("[^a-z ]"), "")
            .split(" ").filter { it.isNotEmpty() && it !in stopWords }.toSet()
        val r = norm(recognized)
        val t = norm(target)
        return if (t.isEmpty()) 1f else r.intersect(t).size.toFloat() / t.size
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun ConversationScreen(
    topicLabel: String,
    conversationTitle: String,
    chatHistory: List<ChatEntry>,
    state: CS,
    turns: List<ConvTurn>,
    hasMicPerm: Boolean,
    onRecord: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val (sessionsDone, sessionsTarget) = SessionManager.sessionProgressFlow(context)
        .collectAsState(initial = Pair(0, 1)).value

    val userTurnsTotal = turns.count { !it.isApp }
    val userTurnsDone = chatHistory.count { !it.isApp }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) listState.animateScrollToItem(chatHistory.size - 1)
    }
    LaunchedEffect(state) {
        if (state is CS.ConvDone) scope.launch { SessionManager.incrementSession(context) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A))) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("←", color = Color(0xFF7B8BB2), fontSize = 18.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(topicLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (conversationTitle.isNotEmpty()) {
                    Text(conversationTitle, fontSize = 11.sp, color = Color(0xFF7B8BB2))
                }
            }
            Text(
                "$userTurnsDone/$userTurnsTotal",
                fontSize = 14.sp, color = Color(0xFF7B8BB2), fontWeight = FontWeight.Medium
            )
        }

        LinearProgressIndicator(
            progress = { if (userTurnsTotal > 0) userTurnsDone.toFloat() / userTurnsTotal else 0f },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF2A2A3E)
        )

        // ── Chat area ─────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(chatHistory) { entry -> ChatBubble(entry) }

            if (state == CS.AppSpeaking) {
                item { AppTypingIndicator() }
            }
        }

        // ── Action area ───────────────────────────────────────────────────────
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "action"
        ) { s ->
            when (s) {
                CS.ConvDone -> ConvDoneSection(sessionsDone, sessionsTarget, onBack)
                CS.UserReady, CS.UserRecording -> RecordSection(s, hasMicPerm, onRecord)
                is CS.UserResult -> ResultSection(s, onContinue, onRetry)
                CS.AppSpeaking -> Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(entry: ChatEntry) {
    if (entry.isApp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .background(Color(0xFF1A2A3E), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(14.dp, 10.dp)
            ) {
                Text(entry.text, fontSize = 15.sp, color = Color.White, lineHeight = 22.sp)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val passed = entry.passed ?: true
            val bgColor = if (passed) Color(0xFF1B3A1B) else Color(0xFF3A1B1B)
            val textColor = if (passed) Color(0xFF81C784) else Color(0xFFEF9A9A)
            val scoreColor = if (passed) Color(0xFF66BB6A) else Color(0xFFEF5350)

            // What the user said
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .background(bgColor, RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .padding(14.dp, 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val recognized = entry.recognized ?: ""
                    Text(
                        if (recognized.isNotEmpty()) "\"$recognized\"" else "(no speech detected)",
                        fontSize = 14.sp, color = textColor, lineHeight = 20.sp
                    )
                    val pct = ((entry.score ?: 0f) * 100).toInt()
                    Text(
                        if (passed) "✓ $pct% match" else "✗ $pct% match",
                        fontSize = 11.sp, color = scoreColor, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Model answer for reference
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .padding(14.dp, 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Model answer:", fontSize = 10.sp, color = Color(0xFF555577),
                        letterSpacing = 0.5.sp)
                    Text(entry.text, fontSize = 14.sp, color = Color(0xFFAAAAAA), lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun AppTypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1A2A3E), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(18.dp, 14.dp)
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(450), RepeatMode.Reverse, StartOffset(i * 130)
                        ), label = "dot$i"
                    )
                    Box(Modifier.size(7.dp)
                        .background(Color(0xFF4FC3F7).copy(alpha = alpha), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun RecordSection(state: CS, hasMicPerm: Boolean, onRecord: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state == CS.UserRecording) {
            WaveformAnimation()
        } else {
            Text("Your turn — respond in English", fontSize = 13.sp, color = Color(0xFF7B8BB2))
        }
        Button(
            onClick = onRecord,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = state == CS.UserReady && hasMicPerm,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6A1B9A),
                disabledContainerColor = Color(0xFF2A2A3E)
            )
        ) {
            Text(
                if (state == CS.UserRecording) "Listening…" else "🎤  Speak",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ResultSection(state: CS.UserResult, onContinue: () -> Unit, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.passed) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B8BB2))
            ) { Text("🎤  Try again") }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.weight(if (state.passed) 1f else 1f).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.passed) Color(0xFF2E7D32) else Color(0xFF1565C0)
            )
        ) { Text(if (state.passed) "Continue  →" else "Skip  →", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun WaveformAnimation() {
    val transition = rememberInfiniteTransition(label = "wave")
    val heights = List(5) { i ->
        transition.animateFloat(
            initialValue = 0.15f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(280 + i * 60, easing = FastOutSlowInEasing),
                RepeatMode.Reverse, StartOffset(i * 80)
            ), label = "bar$i"
        ).value
    }
    Row(
        modifier = Modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(Modifier.width(6.dp).fillMaxHeight(h)
                .background(Color(0xFF9C27B0), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun ConvDoneScreen(sessionsDone: Int, sessionsTarget: Int, onBack: () -> Unit) {
    val isFullyDone = sessionsDone >= sessionsTarget
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isFullyDone) "🎉  All Done Today!" else "✅  Conversation Complete!",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            "$sessionsDone / $sessionsTarget sessions",
            fontSize = 16.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold
        )
        Text(
            if (isFullyDone)
                "All blocked apps are now unlocked. Great work!"
            else
                "Keep it up — ${sessionsTarget - sessionsDone} more session(s) to unlock your apps.",
            fontSize = 14.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("Go Back", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ConvDoneSection(sessionsDone: Int, sessionsTarget: Int, onBack: () -> Unit) {
    ConvDoneScreen(sessionsDone, sessionsTarget, onBack)
}
