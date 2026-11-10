@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
    var subTab by remember { mutableIntStateOf(0) } // 0: Word-Tile Builder, 1: Dictation Quiz, 2: Spoken Contractions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("🧩 Word Tiles", "🎧 Dictation", "⚡ Native Slang"),
            selectedIndex = subTab,
            onSelect = { subTab = it },
            activeColor = DuoColors.MacawBlue,
            activeLipColor = DuoColors.MacawBlueDark
        )

        Spacer(Modifier.height(12.dp))

        when (subTab) {
            0 -> SentenceTileBuilderView(level = level, onSpeak = onSpeak, onAwardXp = onAwardXp)
            1 -> DictationQuizView(level = level, onSpeak = onSpeak, onAwardXp = onAwardXp)
            else -> ContractionsLabView(onSpeak = onSpeak)
        }
    }
}

/** Splits any Japanese sentence into Duolingo-style clickable word & particle tiles! */
private fun tokenizeJapaneseSentence(jp: String): List<String> {
    val clean = jp.replace("。", "").replace("？", "").replace("！", "").trim()
    val twoCharParticles = setOf("から", "まで", "より", "だけ", "ほど")
    val oneCharParticles = setOf('は', 'が', 'を', 'に', 'で', 'へ', 'と', 'も', 'の')
    val tokens = mutableListOf<String>()
    val current = StringBuilder()

    var i = 0
    while (i < clean.length) {
        val ch = clean[i]
        if (ch == '、' || ch == ' ') {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
            i++
            continue
        }

        val twoChar = if (i + 1 < clean.length) clean.substring(i, i + 2) else ""
        if (twoChar in twoCharParticles && current.isNotEmpty()) {
            tokens.add(current.toString())
            tokens.add(twoChar)
            current.clear()
            i += 2
        } else if (ch in oneCharParticles && current.isNotEmpty()) {
            tokens.add(current.toString())
            tokens.add(ch.toString())
            current.clear()
            i++
        } else {
            current.append(ch)
            i++
        }
    }
    if (current.isNotEmpty()) tokens.add(current.toString())
    return tokens.ifEmpty { listOf(clean) }
}

@Composable
fun SentenceTileBuilderView(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val pool = remember(level) {
        if (level == null) DataRepository.allSentences
        else DataRepository.sentences(level).ifEmpty { DataRepository.allSentences }
    }
    var currentIdx by remember(pool) { mutableIntStateOf(0) }
    val sentence = pool[currentIdx.coerceIn(0, pool.lastIndex)]

    val correctTokens = remember(sentence) { tokenizeJapaneseSentence(sentence.jp) }
    val distractorPool = remember(sentence) {
        listOf("私", "は", "を", "に", "で", "ません", "ました", "とても")
            .filter { it !in correctTokens }
            .shuffled()
            .take(2)
    }
    val allBankTiles = remember(sentence) {
        (correctTokens + distractorPool).mapIndexed { index, text -> index to text }.shuffled()
    }

    var selectedTiles by remember(sentence) { mutableStateOf(listOf<Pair<Int, String>>()) }
    var checkedState by remember(sentence) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(sentence) {
        onSpeak(sentence.jp, ListeningSpeed.NATURAL)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sentence ${currentIdx + 1} / ${pool.size}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onSpeak(sentence.jp, ListeningSpeed.SLOW) }) {
                        Text("🐢 0.65x", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { onSpeak(sentence.jp, ListeningSpeed.NATURAL) }) {
                        Text("🔊 1.0x", fontSize = 12.sp)
                    }
                }
            }
        }

        // Prompt Card (Hindi + English Meaning)
        item {
            Duo3DCard(
                borderColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark,
                onClick = { onSpeak(sentence.jp, ListeningSpeed.NATURAL) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Translate & Build this Sentence:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.MacawBlue
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🇮🇳 ${sentence.hi}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "🇬🇧 ${sentence.en}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Answer Tray (Where tapped tiles land)
        item {
            Duo3DCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = when (checkedState) {
                    true -> DuoColors.FeatherGreen
                    false -> DuoColors.CardinalRed
                    null -> DuoColors.LightBorder
                },
                lipColor = when (checkedState) {
                    true -> DuoColors.FeatherGreenDark
                    false -> DuoColors.CardinalRedDark
                    null -> DuoColors.LightLip
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 95.dp)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "YOUR SENTENCE (Tap tile to remove):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (selectedTiles.isEmpty()) {
                        Text(
                            text = "👇 नीचे दिए गए शब्दों (Word Tiles) को सही क्रम में टैप करें...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedTiles.forEach { pair ->
                                WordTileChip(
                                    text = pair.second,
                                    isSelectedInBank = false,
                                    onClick = {
                                        if (checkedState == null) {
                                            selectedTiles = selectedTiles - pair
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shuffled Word Bank
        item {
            Text(
                text = "WORD BANK:",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allBankTiles.forEach { tilePair ->
                    val alreadyUsed = tilePair in selectedTiles
                    WordTileChip(
                        text = tilePair.second,
                        isSelectedInBank = alreadyUsed,
                        onClick = {
                            if (!alreadyUsed && checkedState == null) {
                                onSpeak(tilePair.second, ListeningSpeed.NATURAL)
                                selectedTiles = selectedTiles + tilePair
                            }
                        }
                    )
                }
            }
        }

        // Check / Next Button & Feedback Banner
        item {
            if (checkedState == null) {
                Duo3DButton(
                    text = "CHECK ANSWER",
                    onClick = {
                        val userBuilt = selectedTiles.joinToString("") { it.second }
                        val targetBuilt = correctTokens.joinToString("")
                        val passed = userBuilt == targetBuilt
                        checkedState = passed
                        if (passed) onAwardXp(20)
                        onSpeak(sentence.jp, ListeningSpeed.NATURAL)
                    },
                    enabled = selectedTiles.isNotEmpty(),
                    topColor = DuoColors.FeatherGreen,
                    lipColor = DuoColors.FeatherGreenDark,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val won = checkedState == true
                Duo3DCard(
                    containerColor = if (won) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                    borderColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                    lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (won) "✅ परफेक्ट वाक्य! (+20 XP)" else "❌ सही क्रम देखें:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = OnPastel
                        )
                        Text(
                            text = "${sentence.jp} (${sentence.romaji})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = OnPastel
                        )
                        if (sentence.casualJp.isNotBlank()) {
                            Text(
                                text = "💡 Casual Spoken: ${sentence.casualJp}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoColors.MacawBlueDark
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Duo3DButton(
                            text = "NEXT SENTENCE →",
                            onClick = {
                                currentIdx = (currentIdx + 1) % pool.size
                            },
                            topColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                            lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordTileChip(
    text: String,
    isSelectedInBank: Boolean,
    onClick: () -> Unit
) {
    val isParticle = text in setOf("は", "が", "を", "に", "で", "へ", "と", "から", "まで", "も", "の")
    val topColor = when {
        isSelectedInBank -> Color(0xFFE2E8F0)
        isParticle -> DuoColors.LilacPurpleBg
        else -> Color.White
    }
    val borderColor = when {
        isSelectedInBank -> Color(0xFFCBD5E1)
        isParticle -> DuoColors.LilacPurple
        else -> DuoColors.MacawBlue
    }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(borderColor)
            .clickable(enabled = !isSelectedInBank) { onClick() }
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(topColor)
                .border(1.5.dp, borderColor, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = if (isSelectedInBank) Color.Transparent else OnPastel,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
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
            Duo3DCard(
                containerColor = DuoColors.FoxOrangeBg,
                borderColor = DuoColors.FoxOrange,
                lipColor = DuoColors.FoxOrangeDark
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Real Anime & Tokyo Street Contractions",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = OnPastel
                    )
                    Text(
                        text = "किताबों की जापानी (Formal) और असल ज़िंदगी की तेज़ बोलचाल (Casual Spoken) का अंतर समझें!",
                        fontSize = 12.sp,
                        color = OnPastel.copy(alpha = 0.85f)
                    )
                }
            }
        }

        items(DataRepository.spokenContractions) { c ->
            Duo3DCard(
                borderColor = DuoColors.MacawBlue.copy(alpha = 0.5f),
                lipColor = DuoColors.MacawBlueDark,
                onClick = { onSpeak(c.exCasual, ListeningSpeed.NATURAL) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = c.formalForm,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("➡️", fontSize = 14.sp)
                            Surface(color = DuoColors.FeatherGreenBg, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = c.casualSpoken,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OnPastel
                                )
                            }
                        }
                        IconButton(onClick = { onSpeak(c.exCasual, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = DuoColors.MacawBlue)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🇮🇳 ${c.explanationHi}",
                        fontSize = 13.sp,
                        color = DuoColors.FeatherGreenDark,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text("📘 Textbook: ${c.exFormal}", fontSize = 12.sp)
                        Text("🔥 Native Spoken: ${c.exCasual}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.MacawBlue)
                        Text("💬 ${c.exMeaning}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DictationQuizView(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var round by remember { mutableIntStateOf(0) }
    val questions = remember(level, round) { DataRepository.makeQuiz(level, QuizMode.DICTATION) }
    var qIndex by remember(questions) { mutableIntStateOf(0) }
    var selected by remember(questions, qIndex) { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var finished by remember(questions) { mutableStateOf(false) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No dictation questions available.") }
        return
    }

    if (finished) {
        Duo3DCard(
            borderColor = DuoColors.FeatherGreen,
            lipColor = DuoColors.FeatherGreenDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆 Dictation Complete!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Score: $score / ${questions.size}", fontSize = 18.sp, color = DuoColors.FeatherGreen, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                Duo3DButton(text = "Play Again", onClick = { round++ })
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dictation Q ${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onSpeak(q.audio, ListeningSpeed.SLOW) }) { Text("🐢 0.65x") }
                    OutlinedButton(onClick = { onSpeak(q.audio, ListeningSpeed.NATURAL) }) { Text("🔊 1.0x") }
                }
            }
        }

        item {
            Duo3DCard(
                containerColor = DuoColors.MacawBlueBg,
                borderColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark,
                onClick = { onSpeak(q.audio, ListeningSpeed.NATURAL) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎧 Tap to Listen & Match the Sentence", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = OnPastel)
                    if (q.hint.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("💡 ${q.hint}", fontSize = 12.sp, color = DuoColors.MacawBlueDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(q.options.indices.toList()) { idx ->
            val opt = q.options[idx]
            val isSelected = selected == idx
            val isCorrect = idx == q.correctIndex
            val bg = when {
                selected == null -> MaterialTheme.colorScheme.surface
                isCorrect -> DuoColors.FeatherGreenBg
                isSelected -> DuoColors.CardinalRedBg
                else -> MaterialTheme.colorScheme.surface
            }
            val border = when {
                selected == null -> DuoColors.LightBorder
                isCorrect -> DuoColors.FeatherGreen
                isSelected -> DuoColors.CardinalRed
                else -> DuoColors.LightBorder
            }

            Duo3DCard(
                containerColor = bg,
                borderColor = border,
                lipColor = border,
                onClick = {
                    if (selected == null) {
                        selected = idx
                        if (isCorrect) {
                            score++
                            onAwardXp(15)
                        }
                        onSpeak(q.audio, ListeningSpeed.NATURAL)
                    }
                }
            ) {
                Text(
                    text = "${'A' + idx}.  $opt",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected != null && (isCorrect || isSelected)) OnPastel else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (selected != null) {
            item {
                Duo3DButton(
                    text = if (qIndex + 1 < questions.size) "NEXT QUESTION →" else "FINISH DICTATION",
                    onClick = {
                        selected = null
                        if (qIndex + 1 < questions.size) qIndex++ else finished = true
                    },
                    topColor = DuoColors.MacawBlue,
                    lipColor = DuoColors.MacawBlueDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
