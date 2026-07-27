package com.pe.learnai

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pe.learnai.ui.theme.AILearnEngTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AILearnEngTheme { HomeScreen() } }
    }
}

private fun isAccessibilityEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return flat.contains("${context.packageName}/${AppBlockerService::class.java.name}")
}

private fun hasOverlayPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

private fun scheduleReminder(context: android.content.Context) {
    val now = Calendar.getInstance()
    val target = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
    }
    val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(target.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        DailyReminderWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var accessibilityEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var serviceActive by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    val sessionComplete by SessionManager.sessionCompleteFlow(context).collectAsState(initial = false)

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — notifications work if granted */ }

    // First launch: request notification permission + schedule daily reminder
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scheduleReminder(context)
    }

    // Refresh permissions + service state on every screen resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = isAccessibilityEnabled(context)
                overlayEnabled = hasOverlayPermission(context)
                serviceActive = AppBlockerService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupDone = accessibilityEnabled && overlayEnabled

    // Emergency unlock dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Emergency unlock", color = Color.White) },
            text = {
                Text(
                    "This will unlock all blocked apps for today without completing an English session.\n\nAre you sure?",
                    color = Color(0xFFAAAAAA)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { SessionManager.markComplete(context) }
                    showEmergencyDialog = false
                }) {
                    Text("Unlock anyway", color = Color(0xFFEF9A9A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel", color = Color(0xFF7B8BB2))
                }
            }
        )
    }

    val bg = Color(0xFF0D0D1A)
    val cardBg = Color(0xFF1A1A2E)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 48.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text = "🔒 FocusGate",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Master English to unlock your apps",
                    fontSize = 14.sp,
                    color = Color(0xFF7B8BB2)
                )
            }
        }

        // ── "All set" banner (when fully ready) ───────────────────────
        if (setupDone && serviceActive) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3A1B))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚡", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "FocusGate is active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF81C784)
                            )
                            Text(
                                text = "Monitoring foreground apps in real time",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        // ── Setup section ──────────────────────────────────────────────
        item {
            Text(
                text = "Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Accessibility service card
        item {
            SetupCard(
                emoji = "⚙️",
                title = "Accessibility Service",
                description = "Lets FocusGate detect when a blocked app opens.",
                done = accessibilityEnabled,
                activeBadge = if (accessibilityEnabled) {
                    if (serviceActive) "Active" else "Enabled – connecting…"
                } else null,
                buttonLabel = "Open Accessibility Settings",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }

        // Overlay permission card
        item {
            SetupCard(
                emoji = "🪟",
                title = "Display Over Other Apps",
                description = "Required to show the blocking screen on top of apps.",
                done = overlayEnabled,
                activeBadge = null,
                buttonLabel = "Grant Overlay Permission",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
        }

        // Android 13+ sideloaded APK note
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !accessibilityEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1200))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📱  Android 13+ — Sideloaded APK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFCC02)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "If you installed this APK manually, Android may block the Accessibility toggle. " +
                                "In Accessibility settings, long-press FocusGate and tap \"Allow restricted settings\".",
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // ── Today's session ────────────────────────────────────────────
        item {
            Text(
                text = "Today's Session",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (sessionComplete) Color(0xFF1B3A1B) else Color(0xFF3A1B1B)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = if (sessionComplete) "✅" else "⏳", fontSize = 36.sp)
                    Column {
                        Text(
                            text = "Today's Session",
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA)
                        )
                        Text(
                            text = if (sessionComplete) "Complete — apps unlocked!" else "Not done — apps locked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionComplete) Color(0xFF81C784) else Color(0xFFEF9A9A)
                        )
                    }
                }
            }
        }

        if (!sessionComplete) {
            item {
                Button(
                    onClick = { context.startActivity(Intent(context, PracticeActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = setupDone,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        text = if (setupDone) "Start English Practice" else "Complete setup first",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { showEmergencyDialog = true }) {
                        Text(
                            text = "Emergency unlock",
                            color = Color(0xFF3A3A55),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ── Blocked apps list ──────────────────────────────────────────
        item {
            Text(
                text = "Blocked Apps",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        val appNames = listOf(
            Triple("com.zhiliaoapp.musically", "TikTok", "🎵"),
            Triple("com.ss.android.ugc.trill", "TikTok (Regional)", "🎵"),
            Triple("com.whatsapp", "WhatsApp", "💬"),
            Triple("com.instagram.android", "Instagram", "📷"),
            Triple("com.google.android.youtube", "YouTube", "▶️"),
            Triple("com.twitter.android", "X / Twitter", "🐦"),
            Triple("com.facebook.katana", "Facebook", "👥"),
            Triple("com.snapchat.android", "Snapchat", "👻"),
            Triple("com.reddit.frontpage", "Reddit", "🤖"),
            Triple("com.facebook.orca", "Messenger", "💭"),
        )

        items(appNames) { (pkg, name, emoji) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = pkg, fontSize = 11.sp, color = Color(0xFF555577))
                    }
                    Text(text = if (sessionComplete) "🔓" else "🔒", fontSize = 18.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Practice English daily to keep your apps unlocked.\nEach session resets at midnight.",
                fontSize = 12.sp,
                color = Color(0xFF555577),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SetupCard(
    emoji: String,
    title: String,
    description: String,
    done: Boolean,
    activeBadge: String?,
    buttonLabel: String,
    onClick: () -> Unit
) {
    val isFullyActive = done && activeBadge == "Active"
    val isConnecting = done && activeBadge != null && activeBadge != "Active"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFullyActive -> Color(0xFF1B3A1B)
                done -> Color(0xFF1A2A00)
                else -> Color(0xFF2A1F00)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when {
                        isFullyActive -> "✅"
                        isConnecting -> "🟡"
                        done -> "✅"
                        else -> emoji
                    },
                    fontSize = 20.sp
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isFullyActive -> Color(0xFF81C784)
                            isConnecting -> Color(0xFFFFCC02)
                            done -> Color(0xFF81C784)
                            else -> Color(0xFFFFCC02)
                        }
                    )
                    if (activeBadge != null) {
                        Text(
                            text = activeBadge,
                            fontSize = 12.sp,
                            color = if (isFullyActive) Color(0xFF4CAF50) else Color(0xFFAA8800)
                        )
                    }
                }
            }
            if (!done) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFFCCCCCC),
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC02)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonLabel, color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
