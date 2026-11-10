@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }

    var showRomaji by remember { mutableStateOf(prefs.getBoolean("show_romaji", true)) }
    var showHindiHints by remember { mutableStateOf(prefs.getBoolean("show_hindi_hints", true)) }

    var subTab by remember { mutableIntStateOf(0) } // 0: Acoustic Studio, 1: Bento Sentence Dock, 2: Spoken Slang

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("🎧 Audio Studio", "🍱 Bento Builder", "⚡ Tokyo Slang"),
            selectedIndex = subTab,
            onSelect = { subTab = it }
        )

        Spacer(Modifier.height(8.dp))

        // Live 1-Tap X-Ray Toggle Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "X-RAY SCRIPT OVERLAY:",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BentoXRayToggleBar(
                showRomaji = showRomaji,
                onToggleRomaji = {
                    showRomaji = it
                    prefs.edit().putBoolean("show_romaji", it).apply()
                },
                showHindi = showHindiHints,
                onToggleHindi = {
                    showHindiHints = it
                    prefs.edit().putBoolean("show_hindi_hints", it).apply()
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        when (subTab) {
            0 -> BentoTileStudioExerciseView(
                isAudioListeningMode = true,
                showRomaji = showRomaji,
                showHindiHints = showHindiHints,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            1 -> BentoTileStudioExerciseView(
                isAudioListeningMode = false,
                showRomaji = showRomaji,
                showHindiHints = showHindiHints,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            else -> ContractionsLabView(onSpeak = onSpeak)
        }
    }
}

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
private fun BentoTileStudioExerciseView(
    isAudioListeningMode: Boolean,
    showRomaji: Boolean,
    showHindiHints: Boolean,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val pool = DataRepository.allSentences
    var currentIdx by remember(isAudioListeningMode) { mutableIntStateOf(0) }
    val sentence = pool[currentIdx.coerceIn(0, pool.lastIndex)]

    val correctTokens = remember(sentence) { tokenizeJapaneseSentence(sentence.jp) }
    val distractorPool = remember(sentence) {
        listOf("ひと", "か", "さん", "ホテル", "どこ", "ません")
            .filter { it !in correctTokens }
            .shuffled()
            .take(2)
    }
    val allBankTiles = remember(sentence) {
        (correctTokens + distractorPool).mapIndexed { index, text -> index to text }.shuffled()
    }

    var selectedTiles by remember(sentence, isAudioListeningMode) { mutableStateOf(listOf<Pair<Int, String>>()) }
    var checkedState by remember(sentence, isAudioListeningMode) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(sentence, isAudioListeningMode) {
        onSpeak(sentence.jp, ListeningSpeed.NATURAL)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAudioListeningMode) {
                DuoCharacterAudioPrompt(
                    characterEmoji = "🎧",
                    onPlayNormal = { onSpeak(sentence.jp, ListeningSpeed.NATURAL) },
                    onPlaySlow = { onSpeak(sentence.jp, ListeningSpeed.SLOW) }
                )
            } else {
                DuoCharacterSentencePrompt(
                    characterEmoji = "🍱",
                    promptText = sentence.en,
                    subHintText = if (showHindiHints) "🇮🇳 ${sentence.hi}" else "",
                    onSpeakPrompt = { onSpeak(sentence.jp, ListeningSpeed.NATURAL) }
                )
            }

            // Magnetic Bento Sentence Dock
            DuoRuledAnswerArea(
                selectedTiles = selectedTiles,
                showRomaji = showRomaji,
                checkedResult = checkedState,
                onRemoveTile = { pair ->
                    if (checkedState == null) {
                        selectedTiles = selectedTiles - pair
                    }
                }
            )

            // Fixed-Slot Word Blocks
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allBankTiles.forEach { tilePair ->
                    val alreadyUsed = tilePair in selectedTiles
                    DuoWordTile(
                        text = tilePair.second,
                        showRomaji = showRomaji,
                        isPlaceholderSlot = alreadyUsed,
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

        if (checkedState == null) {
            Duo3DButton(
                text = "VERIFY SENTENCE (確認)",
                enabled = selectedTiles.isNotEmpty(),
                onClick = {
                    val userBuilt = selectedTiles.joinToString("") { it.second }
                    val targetBuilt = correctTokens.joinToString("")
                    val passed = (userBuilt == targetBuilt)
                    checkedState = passed
                    if (passed) onAwardXp(20)
                    onSpeak(sentence.jp, ListeningSpeed.NATURAL)
                },
                topColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DuoBottomResultBanner(
                isCorrect = checkedState == true,
                correctAnswerText = "${sentence.jp} — ${sentence.en}",
                romajiSubtitle = "${sentence.romaji} • 🇮🇳 ${sentence.hi}",
                onContinue = {
                    currentIdx = (currentIdx + 1) % pool.size
                }
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
                        Text(
                            text = "${c.formalForm}  ➡️  ${c.casualSpoken}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.MacawBlue
                        )
                        IconButton(onClick = { onSpeak(c.exCasual, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = DuoColors.MacawBlue)
                        }
                    }
                    Text(
                        text = "🇮🇳 ${c.explanationHi}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text("📘 Polite: ${c.exFormal}", fontSize = 12.sp)
                        Text("🔥 Spoken: ${c.exCasual}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.FeatherGreen)
                        Text("💬 ${c.exMeaning}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
