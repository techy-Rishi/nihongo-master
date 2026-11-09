package com.nihongo.master.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

@Composable
fun WritingLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val kanjiPool = remember(level) { if (level == null) DataRepository.allKanji else DataRepository.kanji(level) }
    var selectedKanji by remember(kanjiPool) { mutableStateOf(kanjiPool.firstOrNull()) }
    var showCanvas by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("✍️ Kanji Writing & Stroke Order Lab", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text("Master radical breakdowns, stroke counts, and touch drawing", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        if (selectedKanji == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No Kanji found.") }
            return
        }

        val kItem = selectedKanji!!

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Fixed: LevelBadge usage
                Surface(color = Color(kItem.level.color), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "${kItem.level.label} • Strokes: ${kItem.strokeCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(kItem.kanji, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text("${kItem.meaningEn} • ${kItem.meaningHi}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Onyomi (Chinese)", fontSize = 11.sp, color = Color.Gray)
                        Text(kItem.onyomi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kunyomi (Japanese)", fontSize = 11.sp, color = Color.Gray)
                        Text(kItem.kunyomi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Radical (部首)", fontSize = 11.sp, color = Color.Gray)
                        Text(kItem.radical, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // Fixed: SemiWordBreak typo replaced with SemiBold
                    Text("💡 Mnemonic: ${kItem.mnemonic}", modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showCanvas = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✍️ Open Stroke Drawing Canvas") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Select Kanji to Practice:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(kanjiPool) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (item.kanji == kItem.kanji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { selectedKanji = item }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(item.kanji, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Column {
                                Text(item.meaningEn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Strokes: ${item.strokeCount} · ${item.radical}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Surface(color = Color(item.level.color), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                item.level.label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCanvas) {
        KanjiCanvasDialog(
            kanjiItem = kItem, // Fixed: k -> kItem reference
            onDismiss = { showCanvas = false },
            onComplete = {
                onAwardXp(20)
                showCanvas = false
            }
        )
    }
}

@Composable
fun KanjiCanvasDialog(kanjiItem: KanjiItem, onDismiss: () -> Unit, onComplete: () -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showGuide by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Trace 「${kanjiItem.kanji}」 (${kanjiItem.meaningEn})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${kanjiItem.strokeCount} strokes", fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuide) {
                        Text(
                            text = kanjiItem.kanji,
                            fontSize = 180.sp,
                            color = Color.LightGray.copy(alpha = 0.3f),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset -> currentStroke = listOf(offset) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke = currentStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            strokes.add(currentStroke)
                                            currentStroke = emptyList()
                                        }
                                    },
                                    onDragCancel = { currentStroke = emptyList() }
                                )
                            }
                    ) {
                        drawLine(Color.LightGray, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawLine(Color.LightGray, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2f)

                        val all = strokes.toList() + listOf(currentStroke)
                        all.forEachIndexed { idx, pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                val color = when (idx % 4) {
                                    0 -> Color(0xFFBC002D)
                                    1 -> Color(0xFF1E3A8A)
                                    2 -> Color(0xFF047857)
                                    else -> Color(0xFF6A1B9A)
                                }
                                drawPath(path, color, style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { strokes.clear(); currentStroke = emptyList() }) { Text("Clear Canvas") }
                    OutlinedButton(onClick = { showGuide = !showGuide }) { Text(if (showGuide) "Hide Guide" else "Show Guide") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onComplete) { Text("Finish (+20 XP)") }
        }
    )
}
