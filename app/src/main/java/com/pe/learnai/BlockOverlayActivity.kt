package com.pe.learnai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.pe.learnai.ui.theme.AILearnEngTheme
import kotlinx.coroutines.launch

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Back goes home, not back into the blocked app
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            AILearnEngTheme {
                val context = LocalContext.current
                val isUnlocked by SessionManager.isCurrentlyUnlockedFlow(context)
                    .collectAsState(initial = false)
                val (sessionsDone, sessionsTarget) = SessionManager.sessionProgressFlow(context)
                    .collectAsState(initial = Pair(0, 1)).value

                LaunchedEffect(isUnlocked) {
                    if (isUnlocked) finish()
                }

                BlockOverlayScreen(
                    sessionsDone = sessionsDone,
                    sessionsTarget = sessionsTarget,
                    onPracticeClick = {
                        startActivity(Intent(this, TopicSelectionActivity::class.java))
                    },
                    onGoHomeClick = { goHome() }
                )
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

@Composable
private fun BlockOverlayScreen(
    sessionsDone: Int,
    sessionsTarget: Int,
    onPracticeClick: () -> Unit,
    onGoHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEmergencyDialog by remember { mutableStateOf(false) }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Emergency unlock", color = Color.White) },
            text = {
                Text(
                    "This will unlock all blocked apps for today without completing a session.\n\nAre you sure?",
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "🔒", fontSize = 72.sp)

            Text(
                text = "App Blocked",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = if (sessionsTarget - sessionsDone == 1)
                    "One more session and apps are unlocked for the rest of the day!"
                else
                    "Practice to unlock apps for a couple of hours.\n${sessionsDone} / ${sessionsTarget} sessions done today.",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onPracticeClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(
                    text = "Practice Now  ($sessionsDone / $sessionsTarget)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(onClick = onGoHomeClick) {
                Text(text = "Go Home", color = Color(0xFF7B8BB2))
            }

            Spacer(modifier = Modifier.height(16.dp))

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
