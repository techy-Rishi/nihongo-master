package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

@Composable
fun SpeakingLabScreen(
    level: JlptLevel?,
    isRecording: Boolean,
    lastSpokenText: String,
    onStartListening: ((String) -> Unit) -> Unit,
    onStopListening: () -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Pitch & Shadow, 1: Roleplay Dialogues

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("📈 Pitch & Shadow") }
            Button(
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 1) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("🎭 Roleplay Scenarios") }
        }

        Spacer(Modifier.height(14.dp))

        if (subTab == 0) {
            ShadowingLabView(
                level = level,
                isRecording = isRecording,
                lastSpokenText = lastSpokenText,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
        } else {
            RoleplayLabView(onSpeak = onSpeak, onAwardXp = onAwardXp)
        }
    }
}

@Composable
fun ShadowingLabView(
    level: JlptLevel?,
    isRecording: Boolean,
    lastSpokenText: String,
    onStartListening: ((String) -> Unit) -> Unit,
    onStopListening: () -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val pool = remember(level) { if (level == null) DataRepository.vocab else DataRepository.words(level) }
    var currentIndex by remember(pool) { mutableIntStateOf(0) }
    val word = pool.getOrElse(currentIndex) { DataRepository.vocab.first() }

    var evalResult by remember(word) { mutableStateOf<PronunciationResult?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Word ${currentIndex + 1} / ${pool.size}", fontWeight = FontWeight.Bold, color = Color.Gray)
                Surface(color = Color(word.level.color), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "${word.level.label} • ${word.pitchType.label}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Pitch Accent Visualizer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(word.kanji, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
                    Text(word.kana, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${word.en} · ${word.hi}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))

                    Spacer(Modifier.height(16.dp))
                    Text("Tokyo Pitch Accent Graph (高低アクセント)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    // Pitch Moras Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        word.pitchMoras.forEach { pm ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (pm.isHigh) 16.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(if (pm.isHigh) MaterialTheme.colorScheme.primary else Color.Gray)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(pm.mora, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { onSpeak(word.kana, ListeningSpeed.SLOW) }) { Text("🐢 0.65x Slow") }
                        Button(onClick = { onSpeak(word.kana, ListeningSpeed.NATURAL) }) { Text("🔊 1.0x Native") }
                    }
                }
            }
        }

        // Mic & Evaluation Area
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isRecording) "🔴 Listening... Speak clearly into mic" else "Tap mic to shadow native pronunciation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    FilledIconButton(
                        onClick = {
                            if (isRecording) {
                                onStopListening()
                            } else {
                                onStartListening { spoken ->
                                    val res = PronunciationEvaluator.evaluate(spoken, word.kanji, word.kana, word.romaji)
                                    evalResult = res
                                    if (res.isPassed) onAwardXp(15)
                                }
                            }
                        },
                        modifier = Modifier.size(68.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", modifier = Modifier.size(32.dp))
                    }

                    if (lastSpokenText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text("You said: \"$lastSpokenText\"", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
        }

        evalResult?.let { res ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (res.isPassed) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${res.feedbackTitle} (${res.scorePercent}%)", fontWeight = FontWeight.ExtraBold, color = if (res.isPassed) Color(0xFF047857) else Color(0xFFC62828))
                        Text(res.feedbackDetail, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        evalResult = null
                        if (currentIndex > 0) currentIndex-- else currentIndex = pool.lastIndex
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("← Previous") }
                Button(
                    onClick = {
                        evalResult = null
                        currentIndex = (currentIndex + 1) % pool.size
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Next Word →") }
            }
        }
    }
}

@Composable
fun RoleplayLabView(onSpeak: (String, ListeningSpeed) -> Unit, onAwardXp: (Int) -> Unit) {
    var selectedScenario by remember { mutableStateOf(DataRepository.roleplayScenarios.first()) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DataRepository.roleplayScenarios) { sc ->
                    FilterChip(
                        selected = selectedScenario.id == sc.id,
                        onClick = { selectedScenario = sc },
                        label = { Text(sc.title, fontSize = 12.sp) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(selectedScenario.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Setting: ${selectedScenario.setting}", fontSize = 12.sp, color = Color.Gray)
                    Text("Style: ${selectedScenario.politenessLevel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(selectedScenario.lines) { line ->
            Card(
                colors = CardDefaults.cardColors(containerColor = if (line.isUserRole) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { onSpeak(line.jp, ListeningSpeed.NATURAL) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${if (line.isUserRole) "👤 You" else "🗣️ ${line.speaker}"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (line.isUserRole) MaterialTheme.colorScheme.primary else Color.DarkGray)
                        Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(line.jp, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(line.romaji, fontSize = 11.sp, color = Color.Gray)
                    Text(line.en, fontSize = 13.sp)
                    Text("🇮🇳 ${line.hi}", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Button(
                onClick = { onAwardXp(25) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎉 Complete Roleplay (+25 XP)") }
        }
    }
}
