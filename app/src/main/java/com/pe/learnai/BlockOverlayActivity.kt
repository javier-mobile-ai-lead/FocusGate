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

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Back goes home, not back into the blocked app
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            AILearnEngTheme {
                val context = LocalContext.current
                val sessionDone by SessionManager.sessionCompleteFlow(context)
                    .collectAsState(initial = false)

                LaunchedEffect(sessionDone) {
                    if (sessionDone) finish()
                }

                BlockOverlayScreen(
                    onPracticeClick = {
                        startActivity(Intent(this, PracticeActivity::class.java))
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
    onPracticeClick: () -> Unit,
    onGoHomeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
                text = "Complete your daily English practice to unlock this app for the rest of the day.",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPracticeClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(
                    text = "Do Today's Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(onClick = onGoHomeClick) {
                Text(text = "Go Home", color = Color(0xFF7B8BB2))
            }
        }
    }
}
