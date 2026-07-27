package com.pe.learnai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pe.learnai.data.Resource
import com.pe.learnai.data.ResourceCategory
import com.pe.learnai.data.ResourceContent
import com.pe.learnai.data.ResourceType
import com.pe.learnai.ui.theme.AILearnEngTheme

class LearningHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AILearnEngTheme { HubScreen(onBack = { finish() }) } }
    }
}

@Composable
private fun HubScreen(onBack: () -> Unit) {
    val bg = Color(0xFF0D0D1A)
    val cardBg = Color(0xFF1A1A2E)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 48.dp)
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("←  Back", color = Color(0xFF7B8BB2))
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Learning Hub", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Contenido curado para tu objetivo: inglés para trabajo freelance e internacional",
                    fontSize = 13.sp,
                    color = Color(0xFF7B8BB2),
                    lineHeight = 18.sp
                )
            }
        }

        ResourceContent.categories.forEach { category ->
            item { CategorySection(category = category, cardBg = cardBg) }
        }
    }
}

@Composable
private fun CategorySection(category: ResourceCategory, cardBg: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(category.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(category.subtitle, fontSize = 12.sp, color = Color(0xFF7B8BB2), lineHeight = 16.sp)
        Spacer(Modifier.height(2.dp))
        category.resources.forEach { resource ->
            ResourceCard(resource = resource, cardBg = cardBg)
        }
    }
}

@Composable
private fun ResourceCard(resource: Resource, cardBg: Color) {
    val context = LocalContext.current

    val typeColor = when (resource.type) {
        ResourceType.YOUTUBE -> Color(0xFFEF5350)
        ResourceType.PODCAST -> Color(0xFFAB47BC)
        ResourceType.WEBSITE -> Color(0xFF42A5F5)
        ResourceType.APP     -> Color(0xFF66BB6A)
    }
    val levelColor = when (resource.level) {
        "A2"  -> Color(0xFF66BB6A)
        "B1"  -> Color(0xFFFFB74D)
        else  -> Color(0xFFEF5350)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title row + tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(resource.emoji, fontSize = 18.sp)
                    Text(
                        resource.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TagLabel(resource.level, levelColor)
                    TagLabel(resource.type.label, typeColor)
                }
            }

            Text(resource.description, fontSize = 13.sp, color = Color(0xFFCCCCCC), lineHeight = 18.sp)

            Text(
                "💡 ${resource.whyItHelps}",
                fontSize = 12.sp,
                color = Color(0xFF7B8BB2),
                lineHeight = 16.sp
            )

            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.url)))
                },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Abrir →", fontSize = 13.sp, color = Color(0xFF64B5F6))
            }
        }
    }
}

@Composable
private fun TagLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
