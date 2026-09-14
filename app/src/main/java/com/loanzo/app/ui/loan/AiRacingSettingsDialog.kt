package com.loanzo.app.ui.loan

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.ai.AiConfigManager
import com.loanzo.app.data.ai.AiRaceResult
import com.loanzo.app.data.ai.MultiAiRaceEngine
import com.loanzo.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRacingSettingsDialog(
    configManager: AiConfigManager,
    multiAiRaceEngine: MultiAiRaceEngine,
    onDismiss: () -> Unit
) {
    var llm7Key by remember { mutableStateOf(configManager.llm7ApiKey) }
    var sambaKey by remember { mutableStateOf(configManager.sambaNovaApiKey) }
    var cfToken by remember { mutableStateOf(configManager.cloudflareToken) }
    var cfAccountId by remember { mutableStateOf(configManager.cloudflareAccountId) }

    var isBenchmarking by remember { mutableStateOf(false) }
    var benchmarkResult by remember { mutableStateOf<AiRaceResult?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Gold500.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Gold500)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "3-Way Multi-AI Racing Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Concurrent dispatch • First-valid-response wins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benchmark Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Live Race Benchmark",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isBenchmarking = true
                                    benchmarkResult = null
                                    benchmarkResult = multiAiRaceEngine.raceQuery("Ping test. Respond 'P2P Ready' in 2 words.")
                                    isBenchmarking = false
                                }
                            },
                            enabled = !isBenchmarking,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isBenchmarking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Navy900)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Racing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Race", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (benchmarkResult is AiRaceResult.Success) {
                        val res = benchmarkResult as AiRaceResult.Success
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald400.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Winner: ${res.providerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${res.latencyMs} ms", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald400)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${res.content.take(120)}\"",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Configured AI Racers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Racer 1: LLM7.io
            OutlinedTextField(
                value = llm7Key,
                onValueChange = {
                    llm7Key = it
                    configManager.llm7ApiKey = it
                },
                label = { Text("Racer 1: LLM7.io API Key") },
                placeholder = { Text("LLM7 API Key") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Racer 2: SambaNova
            OutlinedTextField(
                value = sambaKey,
                onValueChange = {
                    sambaKey = it
                    configManager.sambaNovaApiKey = it
                },
                label = { Text("Racer 2: SambaNova API Key") },
                placeholder = { Text("SambaNova API Key") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Racer 3: Cloudflare
            OutlinedTextField(
                value = cfToken,
                onValueChange = {
                    cfToken = it
                    configManager.cloudflareToken = it
                },
                label = { Text("Racer 3: Cloudflare Workers AI Token") },
                placeholder = { Text("Cloudflare API Token") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cfAccountId,
                onValueChange = {
                    cfAccountId = it
                    configManager.cloudflareAccountId = it
                },
                label = { Text("Cloudflare Account ID (Optional)") },
                placeholder = { Text("e.g. 076ec8c6ef1ad...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
