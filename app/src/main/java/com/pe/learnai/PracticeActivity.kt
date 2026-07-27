package com.pe.learnai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

class PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AILearnEngTheme {
                PlaceholderPracticeScreen(onComplete = { finish() })
            }
        }
    }
}

@Composable
private fun PlaceholderPracticeScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
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
                    Text("✓", fontSize = 48.sp, color = Color(0xFF81C784))
                }
                Text(
                    "Session Complete!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "All blocked apps are now unlocked for today.",
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Go Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("🎯", fontSize = 64.sp)
                Text(
                    "Daily English Session",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Phase 2 will replace this with a real shadowing exercise.\nFor now, tap to mark today done.",
                    fontSize = 15.sp,
                    color = Color(0xFF7B8BB2),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            SessionManager.markComplete(context)
                            done = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Mark Session Done ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
