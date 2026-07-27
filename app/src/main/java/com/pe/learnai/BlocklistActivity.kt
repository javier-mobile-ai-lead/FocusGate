package com.pe.learnai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pe.learnai.ui.theme.AILearnEngTheme
import kotlinx.coroutines.launch

class BlocklistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AILearnEngTheme {
                BlocklistScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlocklistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val blockedPkgs by BlocklistManager.blockedPackagesFlow(context)
        .collectAsState(initial = BlocklistManager.defaultPackages)

    var customInput by remember { mutableStateOf("") }

    val catalogPkgSet = remember { BlockedApps.catalog.map { it.pkg }.toSet() }
    val customBlocked = remember(blockedPkgs) {
        blockedPkgs.filter { it !in catalogPkgSet }.sorted()
    }

    val bg = Color(0xFF0D0D1A)
    val cardBg = Color(0xFF1A1A2E)

    fun addCustomPkg() {
        val pkg = customInput.trim()
        if (pkg.isNotBlank()) {
            scope.launch { BlocklistManager.setBlocked(context, pkg, true) }
            customInput = ""
            keyboard?.hide()
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps", color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←  Back", color = Color(0xFF7B8BB2))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Catalog ────────────────────────────────────────────────
            item {
                Text(
                    "Suggested apps",
                    fontSize = 13.sp,
                    color = Color(0xFF7B8BB2),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(BlockedApps.catalog, key = { it.pkg }) { app ->
                val checked = app.pkg in blockedPkgs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(app.emoji, fontSize = 22.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.name,
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(app.pkg, fontSize = 10.sp, color = Color(0xFF555577))
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { on ->
                                scope.launch { BlocklistManager.setBlocked(context, app.pkg, on) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color(0xFF555577),
                                uncheckedTrackColor = Color(0xFF2A2A3E)
                            )
                        )
                    }
                }
            }

            // ── Custom apps ────────────────────────────────────────────
            if (customBlocked.isNotEmpty()) {
                item {
                    Text(
                        "Custom apps",
                        fontSize = 13.sp,
                        color = Color(0xFF7B8BB2),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(customBlocked, key = { it }) { pkg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📦", fontSize = 22.sp)
                            Text(
                                pkg,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                scope.launch { BlocklistManager.setBlocked(context, pkg, false) }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF9A9A)
                                )
                            }
                        }
                    }
                }
            }

            // ── Add custom package ─────────────────────────────────────
            item {
                Text(
                    "Add custom package",
                    fontSize = 13.sp,
                    color = Color(0xFF7B8BB2),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        placeholder = {
                            Text("com.example.app", color = Color(0xFF555577), fontSize = 13.sp)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addCustomPkg() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF2A2A3E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF4CAF50)
                        )
                    )
                    Button(
                        onClick = { addCustomPkg() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
