package com.nihongo.master.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    var subTab by remember { mutableIntStateOf(0) } // 0: Pitch & Shadow, 1: Chat Roleplay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("📈 Tokyo Pitch & Mic", "💬 Chat Roleplay"),
            selectedIndex = subTab,
            onSelect = { subTab = it },
            activeColor = DuoColors.CardinalRed,
            activeLipColor = DuoColors.CardinalRedDark
        )

        Spacer(Modifier.height(12.dp))

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
            RoleplayLabView(
                isRecording = isRecording,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
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
    val pool = remember(level) {
        if (level == null) DataRepository.vocab else DataRepository.words(level).ifEmpty { DataRepository.vocab }
    }
    if (pool.isEmpty()) return

    var currentIndex by remember(pool) { mutableIntStateOf(0) }
    val word = pool[currentIndex.coerceIn(0, pool.lastIndex)]
    var evalResult by remember(word) { mutableStateOf<PronunciationResult?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Word ${currentIndex + 1} / ${pool.size}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = Color(word.level.color),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${word.level.label} • ${word.pitchType.label}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Connected Pitch Accent Card
        item {
            Duo3DCard(
                borderColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(word.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "${word.kana} (${word.romaji})",
                        fontSize = 18.sp,
                        color = DuoColors.MacawBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${word.en} • 🇮🇳 ${word.hi}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.FeatherGreenDark
                    )

                    Spacer(Modifier.height(14.dp))

                    // Connected Canvas Pitch Graph
                    ConnectedPitchGraph(moras = word.pitchMoras)

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🇮🇳 ${word.pitchType.hiDesc}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    // 3-Speed Native Audio Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ListeningSpeed.entries.forEach { spd ->
                            Duo3DButton(
                                text = spd.label,
                                onClick = { onSpeak(word.kana, spd) },
                                topColor = if (spd == ListeningSpeed.NATURAL) DuoColors.MacawBlue else MaterialTheme.colorScheme.surfaceVariant,
                                lipColor = if (spd == ListeningSpeed.NATURAL) DuoColors.MacawBlueDark else DuoColors.LightLip,
                                textColor = if (spd == ListeningSpeed.NATURAL) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Tactile Mic Card
        item {
            Duo3DCard(
                containerColor = if (isRecording) DuoColors.CardinalRedBg else MaterialTheme.colorScheme.surface,
                borderColor = if (isRecording) DuoColors.CardinalRed else DuoColors.FeatherGreen,
                lipColor = if (isRecording) DuoColors.CardinalRedDark else DuoColors.FeatherGreenDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecording) "🔴 Listening... Speak Japanese clearly!" else "🎤 Tap Mic to Shadow Native Pronunciation",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isRecording) DuoColors.CardinalRedDark else MaterialTheme.colorScheme.onSurface
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
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) DuoColors.CardinalRed else DuoColors.FeatherGreen
                        )
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (lastSpokenText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You said: \"$lastSpokenText\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        evalResult?.let { res ->
            item {
                Duo3DCard(
                    containerColor = if (res.isPassed) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                    borderColor = if (res.isPassed) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                    lipColor = if (res.isPassed) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${res.feedbackTitle} (${res.scorePercent}%)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = OnPastel
                        )
                        Text(
                            text = res.feedbackDetail,
                            fontSize = 12.sp,
                            color = OnPastel
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Duo3DButton(
                    text = "← Previous",
                    onClick = {
                        evalResult = null
                        if (currentIndex > 0) currentIndex-- else currentIndex = pool.lastIndex
                    },
                    topColor = MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = DuoColors.LightLip,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Duo3DButton(
                    text = "Next Word →",
                    onClick = {
                        evalResult = null
                        currentIndex = (currentIndex + 1) % pool.size
                    },
                    topColor = DuoColors.FeatherGreen,
                    lipColor = DuoColors.FeatherGreenDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConnectedPitchGraph(moras: List<PitchMora>) {
    if (moras.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val count = moras.size
            val stepX = size.width / (count + 1)
            val highY = 12.dp.toPx()
            val lowY = size.height - 10.dp.toPx()

            val points = moras.mapIndexed { idx, pm ->
                Offset(stepX * (idx + 1), if (pm.isHigh) highY else lowY)
            }

            // Connect consecutive mora nodes
            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = Color(0xFF1CB0F6),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Draw nodes
            points.forEachIndexed { idx, pt ->
                val pm = moras[idx]
                val nodeColor = if (pm.hasDropAfter) Color(0xFFFF4B4B) else if (pm.isHigh) Color(0xFF1CB0F6) else Color(0xFF64748B)
                drawCircle(color = nodeColor, radius = if (pm.isHigh) 14f else 10f, center = pt)
                drawCircle(color = Color.White, radius = 5f, center = pt)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            moras.forEach { pm ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (pm.isHigh) "HIGH" else "low",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pm.hasDropAfter) DuoColors.CardinalRed else if (pm.isHigh) DuoColors.MacawBlue else Color.Gray
                    )
                    Text(
                        text = pm.mora + if (pm.hasDropAfter) "↘" else "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun RoleplayLabView(
    isRecording: Boolean,
    onStartListening: ((String) -> Unit) -> Unit,
    onStopListening: () -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var selectedScenario by remember { mutableStateOf(DataRepository.roleplayScenarios.first()) }
    var spokenFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DataRepository.roleplayScenarios) { sc ->
                    FilterChip(
                        selected = selectedScenario.id == sc.id,
                        onClick = {
                            selectedScenario = sc
                            spokenFeedback = null
                        },
                        label = { Text("${sc.level.label}: ${sc.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }
        }

        item {
            Duo3DCard(
                containerColor = DuoColors.MacawBlueBg,
                borderColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(selectedScenario.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = OnPastel)
                    Text("📍 ${selectedScenario.setting}", fontSize = 12.sp, color = OnPastel.copy(alpha = 0.8f))
                    Text("🗣️ Register: ${selectedScenario.politenessLevel}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.MacawBlueDark)
                }
            }
        }

        spokenFeedback?.let { msg ->
            item {
                Surface(
                    color = DuoColors.FeatherGreenBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = OnPastel
                    )
                }
            }
        }

        items(selectedScenario.lines) { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (line.isUserRole) Arrangement.End else Arrangement.Start
            ) {
                Duo3DCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    containerColor = if (line.isUserRole) DuoColors.FeatherGreenBg else MaterialTheme.colorScheme.surface,
                    borderColor = if (line.isUserRole) DuoColors.FeatherGreen else DuoColors.MacawBlue.copy(alpha = 0.5f),
                    lipColor = if (line.isUserRole) DuoColors.FeatherGreenDark else DuoColors.MacawBlueDark,
                    onClick = { onSpeak(line.jp, ListeningSpeed.NATURAL) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (line.isUserRole) "👤 YOU (Your Turn to Speak)" else "🗣️ ${line.speaker}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = if (line.isUserRole) DuoColors.FeatherGreenDark else DuoColors.MacawBlue
                            )
                            Row {
                                IconButton(
                                    onClick = { onSpeak(line.jp, ListeningSpeed.NATURAL) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = DuoColors.MacawBlue)
                                }
                                if (line.isUserRole) {
                                    IconButton(
                                        onClick = {
                                            if (isRecording) onStopListening()
                                            else onStartListening { spoken ->
                                                val res = PronunciationEvaluator.evaluate(spoken, line.jp, line.kana, line.romaji)
                                                spokenFeedback = "${res.feedbackTitle} (${res.scorePercent}%) — You said: \"$spoken\""
                                                if (res.isPassed) onAwardXp(15)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Speak", tint = DuoColors.CardinalRed)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = line.jp,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (line.isUserRole) OnPastel else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = line.romaji,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = line.en,
                            fontSize = 12.sp,
                            color = if (line.isUserRole) OnPastel else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "🇮🇳 ${line.hi}",
                            fontSize = 12.sp,
                            color = DuoColors.FeatherGreenDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
