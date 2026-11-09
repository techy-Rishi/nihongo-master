package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

private val RANKS = listOf(
    0 to "初心者 Beginner",
    200 to "見習い Apprentice",
    600 to "学習者 Learner",
    1500 to "達人 Expert",
    3000 to "名人 Master"
)

private fun rankFor(xp: Int): String = RANKS.last { xp >= it.first }.second

@Composable
fun HomeDashboardScreen(
    xp: Int,
    streak: Int,
    level: JlptLevel?,
    srsStates: Map<String, SrsCardState>,
    masteredIds: Set<String>,
    onGradeSrs: (String, SrsGrade) -> Unit,
    onNavigate: (AppSection) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit
) {
    val today = SrsEngine.currentEpochDay()
    val vocabPool = remember(level) { if (level == null) DataRepository.vocab else DataRepository.words(level) }
    val dueCards = remember(vocabPool, srsStates) {
        // reviews that are due + only a limited number of NEW cards per day
        val reviews = vocabPool.filter { w -> srsStates[w.id]?.let { it.dueEpochDay <= today } == true }
        val newToday = srsStates.values.count { it.firstSeenDay == today }
        val newAllowed = (SrsEngine.NEW_CARDS_PER_DAY - newToday).coerceAtLeast(0)
        val fresh = vocabPool.filter { srsStates[it.id] == null }.take(newAllowed)
        reviews + fresh
    }
    val masteredInPool = vocabPool.count { it.id in masteredIds }
    var activeSrsIndex by remember(dueCards) { mutableIntStateOf(0) }
    var showSrsAnswer by remember(dueCards) { mutableStateOf(false) }

    val spotlight = remember { DataRepository.vocab.randomOrNull() }
    val totalVocab = vocabPool.size.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Rank & XP Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT RANK", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(rankFor(xp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                            Text("🔥 $streak Days", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Mastered: $masteredInPool / $totalVocab words", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (masteredInPool.toFloat() / totalVocab).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // 2. SM-2 Spaced Repetition Due Queue Widget
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧠 Spaced Repetition Queue", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                            Text("Due: ${dueCards.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    if (dueCards.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("🎉 All SRS reviews completed for today!", fontWeight = FontWeight.Bold, color = Color(0xFF047857), fontSize = 14.sp)
                        }
                    } else {
                        val cardWord = dueCards[activeSrsIndex.coerceIn(0, dueCards.lastIndex)]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .clickable { showSrsAnswer = !showSrsAnswer }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(cardWord.kanji, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                            if (showSrsAnswer) {
                                Spacer(Modifier.height(6.dp))
                                Text(cardWord.kana, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("${cardWord.en} · ${cardWord.hi}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                Text(cardWord.exJp, fontSize = 13.sp)
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Text("👆 Tap to reveal review answer", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (showSrsAnswer) {
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SrsGrade.entries.forEach { grade ->
                                    Button(
                                        onClick = {
                                            onGradeSrs(cardWord.id, grade)
                                            showSrsAnswer = false
                                            activeSrsIndex = 0
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(grade.colorHex)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(grade.label.split(" ")[0], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pro Lab Quick Navigators
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabShortcutCard("🗣️ Speaking Lab", "Pitch & Shadow", Color(0xFFBC002D), Modifier.weight(1f)) { onNavigate(AppSection.SPEAKING) }
                LabShortcutCard("🎧 Listening Lab", "Speed & Dictation", Color(0xFF1E3A8A), Modifier.weight(1f)) { onNavigate(AppSection.LISTENING) }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabShortcutCard("✍️ Writing Lab", "Kanji Canvas", Color(0xFF047857), Modifier.weight(1f)) { onNavigate(AppSection.WRITING) }
                LabShortcutCard("📖 Grammar Lab", "10-Form Verbs", Color(0xFF6A1B9A), Modifier.weight(1f)) { onNavigate(AppSection.GRAMMAR) }
            }
        }

        // 4. Word Spotlight (skipped if vocab is empty)
        if (spotlight != null) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🌟 Daily Word Spotlight", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                        IconButton(onClick = { onSpeak(spotlight.kana, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("${spotlight.kanji} 【${spotlight.kana}】", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${spotlight.en} • ${spotlight.hi}", fontSize = 14.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(spotlight.exJp, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(spotlight.exEn, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        }
    }
}

@Composable
fun LabShortcutCard(title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(90.dp).clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}
