@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    val appLanguage = LocalAppLanguage.current
    val showRomaji = LocalShowRomaji.current

    var subTab by remember { mutableIntStateOf(0) } // 0: Chapter Sentence Builder, 1: Audio Studio, 2: Spoken Slang
    val units = remember { CurriculumEngine.buildUnitsForLevel(JlptLevel.N5) }
    var selectedUnitIdx by remember { mutableIntStateOf(0) }
    val activeUnit = units[selectedUnitIdx.coerceIn(0, units.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("🍱 Sentence Builder", "🎧 Audio Decode", "⚡ Tokyo Slang"),
            selectedIndex = subTab,
            onSelect = { subTab = it }
        )

        Spacer(Modifier.height(10.dp))

        if (subTab < 2) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(units) { idx, u ->
                    FilterChip(
                        selected = selectedUnitIdx == idx,
                        onClick = { selectedUnitIdx = idx },
                        label = {
                            Text("Ch ${u.unitNumber}: ${u.mascotCaption}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when (subTab) {
            0 -> ChapterSentenceExerciseView(
                unit = activeUnit,
                isAudioListeningMode = false,
                appLanguage = appLanguage,
                showRomaji = showRomaji,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            1 -> ChapterSentenceExerciseView(
                unit = activeUnit,
                isAudioListeningMode = true,
                appLanguage = appLanguage,
                showRomaji = showRomaji,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            else -> ContractionsLabView(onSpeak = onSpeak)
        }
    }
}

@Composable
private fun ChapterSentenceExerciseView(
    unit: CurriculumUnit,
    isAudioListeningMode: Boolean,
    appLanguage: AppLanguage,
    showRomaji: Boolean,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val wordsWithEx = remember(unit) { unit.unitVocab.filter { it.exJp.isNotBlank() } }
    var currentIdx by remember(unit, isAudioListeningMode) { mutableIntStateOf(0) }
    val currentWord = wordsWithEx[currentIdx.coerceIn(0, wordsWithEx.lastIndex)]

    // Uses SmartJapaneseTokenizer so words like おはよう stay intact!
    val correctTokens = remember(currentWord) { SmartJapaneseTokenizer.tokenize(currentWord.exJp) }
    val distractorPool = remember(currentWord, unit) {
        unit.unitVocab
            .filter { it.id != currentWord.id }
            .map { it.kanji }
            .filter { it !in correctTokens }
            .shuffled()
            .take(2)
    }
    val allBankTiles = remember(currentWord) {
        (correctTokens + distractorPool).mapIndexed { index, text -> index to text }.shuffled()
    }

    var selectedTiles by remember(currentWord, isAudioListeningMode) { mutableStateOf(listOf<Pair<Int, String>>()) }
    var checkedState by remember(currentWord, isAudioListeningMode) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(currentWord, isAudioListeningMode) {
        onSpeak(currentWord.exJp, ListeningSpeed.NATURAL)
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
                    characterEmoji = unit.mascotEmoji,
                    onPlayNormal = { onSpeak(currentWord.exJp, ListeningSpeed.NATURAL) },
                    onPlaySlow = { onSpeak(currentWord.exJp, ListeningSpeed.SLOW) }
                )
            } else {
                val promptStr = if (appLanguage == AppLanguage.HINDI) {
                    "${currentWord.exEn}\n(${currentWord.headwordWithReading} = ${currentWord.hi})"
                } else {
                    currentWord.exEn
                }
                DuoCharacterSentencePrompt(
                    characterEmoji = unit.mascotEmoji,
                    promptText = promptStr,
                    subHintText = "Key Word: ${currentWord.headwordWithReading} (${currentWord.meaning(appLanguage)})",
                    onSpeakPrompt = { onSpeak(currentWord.exJp, ListeningSpeed.NATURAL) }
                )
            }

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
                text = "CHECK SENTENCE",
                enabled = selectedTiles.isNotEmpty(),
                onClick = {
                    val userBuilt = selectedTiles.joinToString("") { it.second }
                    val targetBuilt = correctTokens.joinToString("")
                    val passed = (userBuilt == targetBuilt)
                    checkedState = passed
                    if (passed) onAwardXp(20)
                    onSpeak(currentWord.exJp, ListeningSpeed.NATURAL)
                },
                topColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DuoBottomResultBanner(
                isCorrect = checkedState == true,
                correctAnswerText = "${currentWord.exJp} — ${currentWord.exEn}",
                romajiSubtitle = "${currentWord.exRomaji} • ${currentWord.headwordWithReading} = ${currentWord.meaning(appLanguage)}",
                onContinue = {
                    currentIdx = (currentIdx + 1) % wordsWithEx.size
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
                            color = DuoColors.MacawBlueDark
                        )
                        IconButton(onClick = { onSpeak(c.exCasual, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = DuoColors.MacawBlueDark)
                        }
                    }
                    Text(
                        text = c.explanationHi,
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
                        Text("🔥 Spoken: ${c.exCasual}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.FeatherGreenDark)
                        Text("💬 ${c.exMeaning}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
