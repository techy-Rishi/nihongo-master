package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

@Composable
fun ListeningLabScreen(
    level: JlptLevel?,
    srsStates: Map<String, SrsCardState>,
    onGradeSrs: (String, SrsGrade) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Contractions Lab, 1: Dictation Quiz

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("🎧 Spoken Contractions") }
            Button(
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 1) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("📝 Dictation Quiz") }
        }

        Spacer(Modifier.height(14.dp))

        if (subTab == 0) {
            ContractionsLabView(onSpeak = onSpeak)
        } else {
            DictationQuizView(level = level, onSpeak = onSpeak, onAwardXp = onAwardXp)
        }
    }
}

@Composable
fun ContractionsLabView(onSpeak: (String, ListeningSpeed) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Real Native Japanese Slang & Contractions (〜てる、〜ちゃう、〜なきゃ)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        }

        items(DataRepository.spokenContractions) { c ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Formal: ${c.formalForm}", fontSize = 12.sp, color = Color.Gray)
                            Text("Casual Spoken: ${c.casualSpoken}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onSpeak(c.casualSpoken, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("💡 ${c.explanationHi}", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text("Formal: ${c.exFormal}", fontSize = 12.sp)
                        Text("Spoken: ${c.exCasual}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Meaning: ${c.exMeaning}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DictationQuizView(level: JlptLevel?, onSpeak: (String, ListeningSpeed) -> Unit, onAwardXp: (Int) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val questions = remember(level, round) { DataRepository.makeQuiz(level, QuizMode.DICTATION) }
    var qIndex by remember(questions) { mutableIntStateOf(0) }
    var selected by remember(questions) { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var finished by remember(questions) { mutableStateOf(false) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No dictation questions available.") }
        return
    }

    if (finished) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎧 Dictation Complete!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Score: $score / ${questions.size}", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { round++ }) { Text("Play Again") }
            }
        }
        return
    }

    val q = questions[qIndex.coerceIn(0, questions.lastIndex)]

    LaunchedEffect(q) {
        onSpeak(q.audio, ListeningSpeed.NATURAL)
    }

    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dictation Q ${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.Bold)
                Button(onClick = { onSpeak(q.audio, ListeningSpeed.SLOW) }) { Text("🔊 Replay 0.65x") }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.instruction, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(10.dp))
                    Text("🎧", fontSize = 48.sp)
                    Text("Listen to audio and select the exact sentence", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(q.options.indices.toList()) { idx ->
            val opt = q.options[idx]
            val isSelected = selected == idx
            val isCorrect = idx == q.correctIndex
            val bg = when {
                selected == null -> MaterialTheme.colorScheme.surface
                isCorrect -> Color(0xFFD1FAE5)
                isSelected -> Color(0xFFFEE2E2)
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable(enabled = selected == null) {
                    selected = idx
                    if (isCorrect) {
                        score++
                        onAwardXp(15)
                    }
                    onSpeak(q.audio, ListeningSpeed.NATURAL)
                }
            ) {
                Text(text = "${'A' + idx}. $opt", modifier = Modifier.padding(14.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (selected != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (selected == q.correctIndex) "✅ Correct! (+15 XP)" else "❌ Incorrect", fontWeight = FontWeight.ExtraBold)
                        Text(q.explanation, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        selected = null
                        if (qIndex + 1 < questions.size) qIndex++ else finished = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (qIndex + 1 < questions.size) "Next →" else "Finish Dictation") }
            }
        }
    }
}
