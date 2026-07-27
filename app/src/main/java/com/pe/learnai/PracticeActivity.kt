package com.pe.learnai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pe.learnai.data.Phrase
import com.pe.learnai.data.PracticeContent
import com.pe.learnai.data.Topic
import com.pe.learnai.ui.theme.AILearnEngTheme
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

// ── State machine ─────────────────────────────────────────────────────────────

private sealed class PS {
    object Ready : PS()
    object Playing : PS()
    object Recording : PS()
    data class Result(val score: Float, val recognized: String, val passed: Boolean) : PS()
    object Done : PS()
}

// ── Activity ──────────────────────────────────────────────────────────────────

class PracticeActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }
    private val toneHandler = Handler(Looper.getMainLooper())

    private val state = mutableStateOf<PS>(PS.Ready)
    private val clipIndex = mutableStateOf(0)
    private val attempts = mutableStateOf(0)
    private val hasMicPerm = mutableStateOf(false)

    private val topic by lazy {
        val ord = intent.getIntExtra("topic_ordinal", -1)
        if (ord >= 0 && ord < Topic.values().size) Topic.values()[ord] else null
    }
    private val clips by lazy {
        topic?.let { PracticeContent.getPhrasesForTopic(it) } ?: PracticeContent.getDailyPhrases(3)
    }
    private val topicLabel by lazy { topic?.let { "${it.emoji}  ${it.label}" } ?: "Daily Practice" }
    private var recognizerGen = 0

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
                PracticeScreen(
                    clips = clips,
                    topicLabel = topicLabel,
                    clipIndex = clipIndex.value,
                    state = state.value,
                    hasMicPerm = hasMicPerm.value,
                    attempts = attempts.value,
                    onListen = { speakClip(clips[clipIndex.value].text) },
                    onRecord = ::startRecording,
                    onNext = ::advanceClip,
                    onRetry = { state.value = PS.Ready; attempts.value++ },
                    onDone = ::finish
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
                override fun onStart(id: String?) { state.value = PS.Playing }
                override fun onDone(id: String?) { state.value = PS.Ready }
                @Deprecated("Deprecated") override fun onError(id: String?) { state.value = PS.Ready }
            })
        }
    }

    private fun speakClip(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "clip")
    }

    private fun startRecording() {
        if (!hasMicPerm.value) { micLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        tts?.stop()
        state.value = PS.Recording

        recognizer?.destroy()
        val gen = ++recognizerGen
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    if (gen != recognizerGen) return
                    val best = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    val score = similarity(best, clips[clipIndex.value].text)
                    val passed = score >= 0.55f
                    state.value = PS.Result(score, best, passed)
                    if (passed) { vibrateSuccess(); playSuccessSound() }
                    else { vibrateFail(); playFailSound() }
                }
                override fun onError(error: Int) {
                    if (gen != recognizerGen) return
                    state.value = PS.Result(0f, "", false)
                }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rms: Float) {}
                override fun onBufferReceived(buf: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        }
    }

    private fun advanceClip() {
        val next = clipIndex.value + 1
        if (next >= clips.size) {
            state.value = PS.Done
            vibrateComplete()
            playCompleteSound()
        } else {
            clipIndex.value = next
            state.value = PS.Ready
            attempts.value = 0
        }
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
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 200),
                intArrayOf(0, 200, 0, 255),
                -1
            )
        )

    override fun onDestroy() {
        toneHandler.removeCallbacksAndMessages(null)
        tts?.stop(); tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    private fun similarity(recognized: String, target: String): Float {
        fun norm(s: String) = s.lowercase(Locale.US)
            .replace(Regex("[^a-z ]"), "")
            .split(" ").filter { it.isNotEmpty() }.toSet()
        val r = norm(recognized)
        val t = norm(target)
        return if (t.isEmpty()) 0f else r.intersect(t).size.toFloat() / t.size
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun PracticeScreen(
    clips: List<Phrase>,
    topicLabel: String,
    clipIndex: Int,
    state: PS,
    hasMicPerm: Boolean,
    attempts: Int,
    onListen: () -> Unit,
    onRecord: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (sessionsDone, sessionsTarget) = SessionManager.sessionProgressFlow(context)
        .collectAsState(initial = Pair(0, 1)).value

    LaunchedEffect(state) {
        if (state is PS.Done) scope.launch { SessionManager.incrementSession(context) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state is PS.Done,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
            label = "screen"
        ) { isDone ->
            if (isDone) {
                SessionDoneScreen(
                    sessionsDone = sessionsDone,
                    sessionsTarget = sessionsTarget,
                    onDone = onDone
                )
            } else if (clips.isNotEmpty()) {
                ExerciseContent(
                    clip = clips[clipIndex],
                    topicLabel = topicLabel,
                    clipIndex = clipIndex,
                    totalClips = clips.size,
                    state = state,
                    hasMicPerm = hasMicPerm,
                    attempts = attempts,
                    onListen = onListen,
                    onRecord = onRecord,
                    onNext = onNext,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun ExerciseContent(
    clip: Phrase,
    topicLabel: String,
    clipIndex: Int,
    totalClips: Int,
    state: PS,
    hasMicPerm: Boolean,
    attempts: Int,
    onListen: () -> Unit,
    onRecord: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = (clipIndex.toFloat() + if (state is PS.Result && state.passed) 1f else 0f) / totalClips,
        animationSpec = tween(400),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(topicLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${clipIndex + 1} / $totalClips", fontSize = 14.sp, color = Color(0xFF7B8BB2))
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF2A2A3E)
        )

        Spacer(Modifier.height(32.dp))

        // ── Phrase card ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    clip.category.uppercase(),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF7B8BB2)
                )
                Spacer(Modifier.height(12.dp))
                // Show word-by-word highlight after result, plain text otherwise
                AnimatedContent(
                    targetState = state is PS.Result,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "phrase"
                ) { showHighlight ->
                    if (showHighlight && state is PS.Result) {
                        WordHighlight(target = clip.text, recognized = state.recognized)
                    } else {
                        Text(
                            clip.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Actions ───────────────────────────────────────────────────
        AnimatedContent(
            targetState = state is PS.Result,
            transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
            label = "actions"
        ) { showResult ->
            if (showResult && state is PS.Result) {
                ResultSection(
                    state = state,
                    isLast = clipIndex == totalClips - 1,
                    onNext = onNext,
                    onRetry = onRetry,
                    onListen = onListen
                )
            } else {
                ActionSection(
                    state = state,
                    hasMicPerm = hasMicPerm,
                    attempts = attempts,
                    onListen = onListen,
                    onRecord = onRecord
                )
            }
        }
    }
}

@Composable
private fun ActionSection(
    state: PS,
    hasMicPerm: Boolean,
    attempts: Int,
    onListen: () -> Unit,
    onRecord: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (state) {
                PS.Playing -> "Playing…"
                PS.Recording -> "Listening to you…"
                else -> if (attempts == 0) "1. Listen first  →  2. Repeat out loud" else "Listen again or try speaking"
            },
            fontSize = 13.sp,
            color = Color(0xFF7B8BB2),
            textAlign = TextAlign.Center
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onListen,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = state == PS.Ready,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    disabledContainerColor = Color(0xFF2A2A3E)
                )
            ) {
                Text(
                    if (state == PS.Playing) "Playing…" else "🔊  Listen",
                    fontSize = 15.sp
                )
            }
            Button(
                onClick = onRecord,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = state == PS.Ready && hasMicPerm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B1FA2),
                    disabledContainerColor = Color(0xFF2A2A3E)
                )
            ) {
                Text(
                    if (state == PS.Recording) "Listening…" else "🎤  Speak",
                    fontSize = 15.sp
                )
            }
        }

        AnimatedVisibility(visible = state == PS.Recording) {
            WaveformAnimation()
        }
    }
}

@Composable
private fun ResultSection(
    state: PS.Result,
    isLast: Boolean,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onListen: () -> Unit
) {
    val percent = (state.score * 100).roundToInt()
    val color = if (state.passed) Color(0xFF4CAF50) else Color(0xFFEF5350)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score ring
        val animatedScore by animateFloatAsState(
            targetValue = state.score,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "score"
        )
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier.size(88.dp),
                color = color,
                strokeWidth = 7.dp,
                trackColor = Color(0xFF2A2A3E)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (state.passed) "✓" else "✗", fontSize = 14.sp, color = color)
            }
        }

        Text(
            if (state.passed) "Great job! Keep going." else "Not quite — try again.",
            fontSize = 15.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )

        if (state.recognized.isNotEmpty()) {
            Text(
                "You said: \"${state.recognized}\"",
                fontSize = 13.sp,
                color = Color(0xFF7B8BB2),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(4.dp))

        if (state.passed) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(
                    if (isLast) "Complete Session ✓" else "Next →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onListen,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B8BB2))
                ) { Text("🔊  Listen again") }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                ) { Text("🎤  Try again") }
            }
        }
    }
}

@Composable
private fun WaveformAnimation() {
    val transition = rememberInfiniteTransition(label = "wave")
    val heights = List(5) { i ->
        transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(280 + i * 60, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(i * 80)
            ),
            label = "bar$i"
        ).value
    }
    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight(h)
                    .background(Color(0xFF9C27B0), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun WordHighlight(target: String, recognized: String) {
    fun norm(s: String) = s.lowercase(Locale.US)
        .replace(Regex("[^a-z ]"), "")
        .split(" ").filter { it.isNotEmpty() }.toSet()

    val recNorm = norm(recognized)
    val words = target.split(" ").filter { it.isNotEmpty() }

    val annotated = buildAnnotatedString {
        words.forEachIndexed { i, word ->
            val matched = word.lowercase(Locale.US).replace(Regex("[^a-z]"), "") in recNorm
            withStyle(
                SpanStyle(
                    color = if (matched) Color(0xFF81C784) else Color(0xFFEF9A9A),
                    fontWeight = if (matched) FontWeight.Bold else FontWeight.Normal
                )
            ) { append(word) }
            if (i < words.size - 1) append(" ")
        }
    }
    Text(
        text = annotated,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        lineHeight = 32.sp
    )
}

@Composable
private fun SessionDoneScreen(sessionsDone: Int, sessionsTarget: Int, onDone: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    val isFullyDone = sessionsDone >= sessionsTarget

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .size(120.dp)
                .background(Color(0xFF1B3A1B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 56.sp, color = Color(0xFF81C784))
        }

        Spacer(Modifier.height(28.dp))

        Text(
            if (isFullyDone) "All Done Today!" else "Session Complete!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "$sessionsDone / $sessionsTarget sessions",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4CAF50)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (isFullyDone)
                "All blocked apps are now unlocked for today.\nKeep up the great work!"
            else
                "Great job! Come back later for your next session.\n${sessionsTarget - sessionsDone} more to unlock your apps.",
            fontSize = 16.sp,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Go Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
