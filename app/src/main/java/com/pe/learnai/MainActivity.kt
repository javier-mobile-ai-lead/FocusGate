package com.pe.learnai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pe.learnai.ui.theme.AILearnEngTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AILearnEngTheme { HomeScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Trigger recomposition by recreating (simple approach for state refresh)
    }
}

private fun isAccessibilityEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return flat.contains("${context.packageName}/${AppBlockerService::class.java.name}")
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    var sessionComplete by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sessionComplete = SessionManager.isSessionComplete(context)
        accessibilityEnabled = isAccessibilityEnabled(context)
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

        // Today's status card
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
                    Text(
                        text = if (sessionComplete) "✅" else "⏳",
                        fontSize = 36.sp
                    )
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

        // Practice button
        if (!sessionComplete) {
            item {
                Button(
                    onClick = { context.startActivity(Intent(context, PracticeActivity::class.java)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        text = "Start English Practice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Accessibility service setup card
        if (!accessibilityEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1F00))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "⚙️  Setup Required",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC02)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enable the FocusGate Accessibility Service so the app can block distracting apps.",
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC02)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Open Accessibility Settings",
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Blocked apps list
        item {
            Text(
                text = "Blocked Apps",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        val appNames = mapOf(
            "com.zhiliaoapp.musically" to ("TikTok" to "🎵"),
            "com.whatsapp" to ("WhatsApp" to "💬"),
            "com.instagram.android" to ("Instagram" to "📷"),
            "com.google.android.youtube" to ("YouTube" to "▶️"),
            "com.twitter.android" to ("X / Twitter" to "🐦"),
            "com.facebook.katana" to ("Facebook" to "👥"),
            "com.snapchat.android" to ("Snapchat" to "👻"),
            "com.reddit.frontpage" to ("Reddit" to "🤖"),
            "com.facebook.orca" to ("Messenger" to "💭"),
        )

        items(appNames.entries.toList()) { (pkg, nameEmoji) ->
            val (name, emoji) = nameEmoji
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
                        Text(text = name, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(text = pkg, fontSize = 11.sp, color = Color(0xFF555577))
                    }
                    Text(
                        text = if (sessionComplete) "🔓" else "🔒",
                        fontSize = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Practice English daily to keep your apps unlocked.\nEach session resets at midnight.",
                fontSize = 12.sp,
                color = Color(0xFF555577),
                lineHeight = 18.sp
            )
        }
    }
}
