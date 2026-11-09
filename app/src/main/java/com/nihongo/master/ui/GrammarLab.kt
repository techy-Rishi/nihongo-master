package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
fun GrammarLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Grammar Points, 1: 10-Form Verb Conjugator

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("📖 Grammar & Particles") }
            Button(
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 1) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("⚙️ 10-Form Conjugator") }
        }

        Spacer(Modifier.height(14.dp))

        if (subTab == 0) {
            val points = remember(level) { if (level == null) JlptLevel.entries.flatMap { DataRepository.grammar(it) } else DataRepository.grammar(level) }
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(points) { g -> GrammarPointCard(g, onSpeak) }
            }
        } else {
            VerbConjugatorLabView(level = level, onSpeak = onSpeak)
        }
    }
}

@Composable
fun GrammarPointCard(g: GrammarPoint, onSpeak: (String, ListeningSpeed) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(g.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Surface(color = Color(g.level.color), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        g.level.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Structure: ${g.structure}", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, color = Color(0xFFB45309), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(g.explanation, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("🇮🇳 ${g.hindiTip}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)

            if (g.commonMistake.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("⚠️ Common Mistake: ${g.commonMistake}", fontSize = 12.sp, color = Color(0xFFC62828))
            }

            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable { onSpeak(g.exJp, ListeningSpeed.NATURAL) }
                    .padding(10.dp)
            ) {
                Text("🔊 ${g.exJp}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(g.exRomaji, fontSize = 11.sp, color = Color.Gray)
                Text(g.exEn, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun VerbConjugatorLabView(level: JlptLevel?, onSpeak: (String, ListeningSpeed) -> Unit) {
    val verbs = remember(level) { DataRepository.vocab.filter { it.type.isVerb && (level == null || it.level == level) } }
    var selectedVerb by remember(verbs) { mutableStateOf(verbs.firstOrNull()) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(verbs) { v ->
            FilterChip(
                selected = selectedVerb?.id == v.id,
                onClick = { selectedVerb = v },
                label = { Text("${v.kanji} (${v.en})") }
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    val verb = selectedVerb
    if (verb == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No verbs found for this level.") }
    } else {
        val forms = remember(verb) { VerbConjugator.forms(verb) }
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(forms) { f ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSpeak(f.reading, ListeningSpeed.NATURAL) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.name, fontSize = 11.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
                            Text("${f.jp} 【${f.reading}】", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(f.hint, fontSize = 12.sp, color = Color.DarkGray)
                        }
                        IconButton(onClick = { onSpeak(f.reading, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
