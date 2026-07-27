package com.pe.learnai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pe.learnai.data.Topic
import com.pe.learnai.ui.theme.AILearnEngTheme

class TopicSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AILearnEngTheme {
                TopicSelectionScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun TopicSelectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val topicRows = Topic.values().toList().chunked(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .verticalScroll(rememberScrollState())
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("←  Back", color = Color(0xFF7B8BB2))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "What do you want to practice?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 30.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a scenario — each session is 3 phrases.",
                fontSize = 14.sp,
                color = Color(0xFF7B8BB2)
            )
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val allTopics = Topic.values().toList()
            val foundation = allTopics.first()
            val rest = allTopics.drop(1).chunked(2)

            // Foundations — full-width featured card
            FoundationCard(
                topic = foundation,
                onClick = {
                    context.startActivity(
                        Intent(context, PracticeActivity::class.java)
                            .putExtra("topic_ordinal", foundation.ordinal)
                    )
                }
            )

            // Rest — 2-column grid
            rest.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { topic ->
                        TopicCard(
                            topic = topic,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                context.startActivity(
                                    Intent(context, PracticeActivity::class.java)
                                        .putExtra("topic_ordinal", topic.ordinal)
                                )
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FoundationCard(topic: Topic, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A3B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(topic.emoji, fontSize = 36.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            topic.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1565C0).copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Start Here",
                                fontSize = 10.sp,
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        topic.description,
                        fontSize = 12.sp,
                        color = Color(0xFF7B8BB2),
                        lineHeight = 16.sp
                    )
                }
            }
            Text("→", fontSize = 20.sp, color = Color(0xFF4FC3F7))
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(topic.emoji, fontSize = 28.sp)
            Column {
                Text(
                    topic.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    topic.description,
                    fontSize = 11.sp,
                    color = Color(0xFF7B8BB2),
                    lineHeight = 14.sp
                )
            }
        }
    }
}
