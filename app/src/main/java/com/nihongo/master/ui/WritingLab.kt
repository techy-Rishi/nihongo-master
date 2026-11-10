package com.nihongo.master.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

private enum class KanaRowFilter(val label: String, val hiTip: String, val romajiSet: Set<String>?) {
    ALL_BASIC("All 46 Basic", "पूरे 46 मूल अक्षर (Gojūon)", null),
    VOWELS("Vowels (あ-お)", "5 मूल स्वर: अ (a), इ (i), उ (u), ए (e), ओ (o)", setOf("a", "i", "u", "e", "o")),
    K_ROW("K-Row (か-こ)", "क-वर्ग: ka, ki, ku, ke, ko", setOf("ka", "ki", "ku", "ke", "ko")),
    S_ROW("S-Row (さ-そ)", "ध्यान दें: し = 'shi' (सी नहीं, शी!)", setOf("sa", "shi", "su", "se", "so")),
    T_ROW("T-Row (た-と)", "ध्यान दें: ち = 'chi' और つ = 'tsu'", setOf("ta", "chi", "tsu", "te", "to")),
    N_ROW("N-Row (な-の)", "न-वर्ग: na, ni, nu, ne, no", setOf("na", "ni", "nu", "ne", "no")),
    H_ROW("H-Row (は-ほ)", "ध्यान दें: ふ = 'fu' (हल्के होंठों से फू)", setOf("ha", "hi", "fu", "he", "ho")),
    M_ROW("M-Row (ま-も)", "म-वर्ग: ma, mi, mu, me, mo", setOf("ma", "mi", "mu", "me", "mo")),
    YRW_ROW("Y/R/W/N (や-ん)", "य, र, व और नाक की ध्वनि ん (n)", setOf("ya", "yu", "yo", "ra", "ri", "ru", "re", "ro", "wa", "wo", "n")),
    DAKUTEN("Dakuten ゛゜ (25)", "बिंदु (゛) लगने से क→ग, स→ज़, त→द, ह→ब/प बन जाता है", null),
    COMBO("Yōon Combo (33)", "छोटा ゃ/ゅ/ょ जुड़कर क्या (kya), शा (sha), चा (cha) बनाता है", null)
}

private val HINDI_ROMAJI_MAP = mapOf(
    "a" to "अ", "i" to "इ", "u" to "उ", "e" to "ए", "o" to "ओ",
    "ka" to "का", "ki" to "की", "ku" to "कू", "ke" to "के", "ko" to "को",
    "sa" to "सा", "shi" to "शी", "su" to "सू", "se" to "से", "so" to "सो",
    "ta" to "ता", "chi" to "ची", "tsu" to "त्सु", "te" to "ते", "to" to "तो",
    "na" to "ना", "ni" to "नी", "nu" to "नू", "ne" to "ने", "no" to "नो",
    "ha" to "हा", "hi" to "ही", "fu" to "फू", "he" to "हे", "ho" to "हो",
    "ma" to "मा", "mi" to "मी", "mu" to "मू", "me" to "मे", "mo" to "मो",
    "ya" to "या", "yu" to "यू", "yo" to "यो",
    "ra" to "रा", "ri" to "री", "ru" to "रू", "re" to "रे", "ro" to "रो",
    "wa" to "वा", "wo" to "ओ (wo)", "n" to "न् / अं"
)

@Composable
fun WritingLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Kana Dojo, 1: 2x2 Kana Quiz, 2: Kanji Deck

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("あア Kana Dojo", "🎮 Kana Quiz", "✍️ Kanji Lab"),
            selectedIndex = subTab,
            onSelect = { subTab = it },
            activeColor = DuoColors.FeatherGreen,
            activeLipColor = DuoColors.FeatherGreenDark
        )

        Spacer(Modifier.height(12.dp))

        when (subTab) {
            0 -> KanaMasterDojoView(onSpeak = onSpeak, onAwardXp = onAwardXp)
            1 -> KanaInteractiveQuizView(onSpeak = onSpeak, onAwardXp = onAwardXp)
            else -> KanjiCanvasLabView(level = level, onSpeak = onSpeak, onAwardXp = onAwardXp)
        }
    }
}

@Composable
private fun KanaMasterDojoView(
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(KanaType.HIRAGANA) }
    var rowFilter by remember { mutableStateOf(KanaRowFilter.VOWELS) }
    var tracingKana by remember { mutableStateOf<KanaChar?>(null) }

    val filteredKana = remember(selectedType, rowFilter) {
        DataRepository.allKana.filter { k ->
            k.type == selectedType && when (rowFilter) {
                KanaRowFilter.ALL_BASIC -> k.group == KanaGroup.BASIC
                KanaRowFilter.DAKUTEN -> k.group == KanaGroup.VOICED
                KanaRowFilter.COMBO -> k.group == KanaGroup.COMBO
                else -> k.group == KanaGroup.BASIC && (rowFilter.romajiSet?.contains(k.romaji) == true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Script Toggle (Hiragana vs Katakana)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Duo3DButton(
                    text = "ひらがな Hiragana (Native)",
                    onClick = { selectedType = KanaType.HIRAGANA },
                    topColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlue else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlueDark else DuoColors.LightLip,
                    textColor = if (selectedType == KanaType.HIRAGANA) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Duo3DButton(
                    text = "カタカナ Katakana (Foreign)",
                    onClick = { selectedType = KanaType.KATAKANA },
                    topColor = if (selectedType == KanaType.KATAKANA) DuoColors.LilacPurple else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (selectedType == KanaType.KATAKANA) DuoColors.LilacPurpleDark else DuoColors.LightLip,
                    textColor = if (selectedType == KanaType.KATAKANA) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Row-by-Row Bite-Sized Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KanaRowFilter.entries) { filter ->
                    val selected = rowFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { rowFilter = filter },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DuoColors.FeatherGreenBg,
                            selectedLabelColor = OnPastel
                        )
                    )
                }
            }
        }

        // 3. Visual Hindi Guide Banner for Current Row
        item {
            Duo3DCard(
                containerColor = DuoColors.BeeGoldBg,
                borderColor = DuoColors.BeeGold,
                lipColor = DuoColors.BeeGoldDark
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💡 ${rowFilter.label} — Beginner Tip",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = OnPastel
                        )
                        Text(
                            text = "🇮🇳 ${rowFilter.hiTip} • अक्षर पर टैप करके आवाज़ सुनें या ✍️ से लिखकर देखें!",
                            fontSize = 12.sp,
                            color = OnPastel.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 4. Interactive Tactile 3D Kana Cards Grid
        val chunks = filteredKana.chunked(3)
        items(chunks) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { k ->
                    val hiSound = HINDI_ROMAJI_MAP[k.romaji] ?: k.romaji
                    Duo3DCard(
                        modifier = Modifier.weight(1f),
                        borderColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlue.copy(alpha = 0.5f) else DuoColors.LilacPurple.copy(alpha = 0.5f),
                        lipColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlueDark else DuoColors.LilacPurpleDark,
                        onClick = { onSpeak(k.char, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔊",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "✍️ Trace",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.FeatherGreenDark,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DuoColors.FeatherGreenBg)
                                        .clickable { tracingKana = k }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = k.char,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${k.romaji} ($hiSound)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlue
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Touch Stroke Canvas for tracing any Hiragana or Katakana!
    tracingKana?.let { kana ->
        CharacterTraceDialog(
            title = "Trace ${if (kana.type == KanaType.HIRAGANA) "Hiragana" else "Katakana"} 「${kana.char}」",
            subtitle = "Sound: ${kana.romaji} (${HINDI_ROMAJI_MAP[kana.romaji] ?: kana.romaji})",
            targetChar = kana.char,
            onSpeak = { onSpeak(kana.char, ListeningSpeed.NATURAL) },
            onDismiss = { tracingKana = null },
            onComplete = {
                onAwardXp(15)
                tracingKana = null
            }
        )
    }
}

@Composable
private fun KanaInteractiveQuizView(
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var quizFilter by remember { mutableStateOf(KanaRowFilter.ALL_BASIC) }
    var round by remember { mutableIntStateOf(0) }
    var comboStreak by remember { mutableIntStateOf(0) }
    var selectedIdx by remember(round, quizFilter) { mutableStateOf<Int?>(null) }

    val pool = remember(quizFilter) {
        val filtered = DataRepository.allKana.filter { k ->
            when (quizFilter) {
                KanaRowFilter.ALL_BASIC -> k.group == KanaGroup.BASIC
                KanaRowFilter.DAKUTEN -> k.group == KanaGroup.VOICED
                KanaRowFilter.COMBO -> k.group == KanaGroup.COMBO
                else -> k.group == KanaGroup.BASIC && (quizFilter.romajiSet?.contains(k.romaji) == true)
            }
        }
        filtered.ifEmpty { DataRepository.allKana }
    }

    val target = remember(pool, round) { pool.random() }
    val options = remember(pool, target, round) {
        val wrong = DataRepository.allKana
            .map { it.romaji }
            .distinct()
            .filter { it != target.romaji }
            .shuffled()
            .take(3)
        (wrong + target.romaji).shuffled()
    }
    val correctIndex = options.indexOf(target.romaji)

    LaunchedEffect(target) {
        onSpeak(target.char, ListeningSpeed.NATURAL)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KanaRowFilter.entries) { f ->
                    FilterChip(
                        selected = quizFilter == f,
                        onClick = { quizFilter = f; round++ },
                        label = { Text(f.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        item {
            Duo3DCard(
                borderColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark,
                onClick = { onSpeak(target.char, ListeningSpeed.NATURAL) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (target.type == KanaType.HIRAGANA) "HIRAGANA QUIZ" else "KATAKANA QUIZ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.MacawBlue
                        )
                        Text(
                            text = "🔥 Combo: $comboStreak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.FoxOrange
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = target.char,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "🔊 Tap to hear pronunciation",
                        fontSize = 12.sp,
                        color = DuoColors.MacawBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2x2 Tactile Choice Tiles
        item {
            val rows = options.indices.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { idx ->
                            val romaji = options[idx]
                            val hi = HINDI_ROMAJI_MAP[romaji]
                            val isSelected = selectedIdx == idx
                            val isCorrect = idx == correctIndex

                            val bg = when {
                                selectedIdx == null -> MaterialTheme.colorScheme.surface
                                isCorrect -> DuoColors.FeatherGreenBg
                                isSelected -> DuoColors.CardinalRedBg
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val border = when {
                                selectedIdx == null -> DuoColors.MacawBlue.copy(alpha = 0.4f)
                                isCorrect -> DuoColors.FeatherGreen
                                isSelected -> DuoColors.CardinalRed
                                else -> DuoColors.LightBorder
                            }

                            Duo3DCard(
                                modifier = Modifier.weight(1f),
                                containerColor = bg,
                                borderColor = border,
                                lipColor = border,
                                onClick = {
                                    if (selectedIdx == null) {
                                        selectedIdx = idx
                                        if (isCorrect) {
                                            comboStreak++
                                            onAwardXp(10)
                                        } else {
                                            comboStreak = 0
                                        }
                                        onSpeak(target.char, ListeningSpeed.NATURAL)
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = romaji,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selectedIdx != null && (isCorrect || isSelected)) OnPastel else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hi != null) {
                                        Text(
                                            text = "🇮🇳 $hi",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DuoColors.FeatherGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedIdx != null) {
            item {
                val won = selectedIdx == correctIndex
                Duo3DCard(
                    containerColor = if (won) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                    borderColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                    lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (won) "✅ बिल्कुल सही! (+10 XP)" else "❌ सही उत्तर: ${target.char} = ${target.romaji}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = OnPastel
                        )
                        Spacer(Modifier.height(8.dp))
                        Duo3DButton(
                            text = "CONTINUE →",
                            onClick = { round++ },
                            topColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                            lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanjiCanvasLabView(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val kanjiPool = remember(level) {
        if (level == null) DataRepository.allKanji else DataRepository.kanji(level).ifEmpty { DataRepository.allKanji }
    }
    var selectedKanji by remember(kanjiPool) { mutableStateOf(kanjiPool.firstOrNull()) }
    var showCanvas by remember { mutableStateOf(false) }

    val currentKanji = selectedKanji ?: kanjiPool.firstOrNull() ?: return

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Duo3DCard(
                borderColor = Color(currentKanji.level.color),
                lipColor = Color(currentKanji.level.color)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(currentKanji.level.color),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${currentKanji.level.label} • ${currentKanji.strokeCount} Strokes",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { onSpeak(currentKanji.kanji, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = DuoColors.MacawBlue)
                        }
                    }

                    Text(
                        text = currentKanji.kanji,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${currentKanji.meaningEn} • 🇮🇳 ${currentKanji.meaningHi}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.FeatherGreenDark
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KanjiInfoPill("Onyomi", currentKanji.onyomi, DuoColors.MacawBlueBg, Modifier.weight(1f))
                        KanjiInfoPill("Kunyomi", currentKanji.kunyomi, DuoColors.LilacPurpleBg, Modifier.weight(1f))
                        KanjiInfoPill("Radical", currentKanji.radical, DuoColors.FoxOrangeBg, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        color = DuoColors.BeeGoldBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Visual Story: ${currentKanji.mnemonic}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPastel
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Duo3DButton(
                        text = "✍️ Trace 「${currentKanji.kanji}」 on Touch Canvas (+20 XP)",
                        onClick = { showCanvas = true },
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(kanjiPool) { item ->
            val isSelected = item.kanji == currentKanji.kanji
            Duo3DCard(
                containerColor = if (isSelected) DuoColors.MacawBlueBg else MaterialTheme.colorScheme.surface,
                borderColor = if (isSelected) DuoColors.MacawBlue else DuoColors.LightBorder,
                lipColor = if (isSelected) DuoColors.MacawBlueDark else DuoColors.LightLip,
                onClick = { selectedKanji = item }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = item.kanji,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) OnPastel else MaterialTheme.colorScheme.onSurface
                        )
                        Column {
                            Text(
                                text = "${item.meaningEn} (${item.meaningHi})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (isSelected) OnPastel else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Strokes: ${item.strokeCount} • ${item.sampleWords.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCanvas) {
        CharacterTraceDialog(
            title = "Trace Kanji 「${currentKanji.kanji}」 (${currentKanji.meaningEn})",
            subtitle = "${currentKanji.strokeCount} Strokes • ${currentKanji.meaningHi}",
            targetChar = currentKanji.kanji,
            onSpeak = { onSpeak(currentKanji.kanji, ListeningSpeed.NATURAL) },
            onDismiss = { showCanvas = false },
            onComplete = {
                onAwardXp(20)
                showCanvas = false
            }
        )
    }
}

@Composable
private fun KanjiInfoPill(title: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPastel.copy(alpha = 0.65f))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = OnPastel, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CharacterTraceDialog(
    title: String,
    subtitle: String,
    targetChar: String,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showGuide by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = DuoColors.MacawBlue)
                    }
                }
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(265.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(2.5.dp, DuoColors.MacawBlue, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuide) {
                        Text(
                            text = targetChar,
                            fontSize = if (targetChar.length > 1) 110.sp else 175.sp,
                            color = Color.LightGray.copy(alpha = 0.38f),
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
                                val strokeColor = when (idx % 4) {
                                    0 -> DuoColors.MacawBlue
                                    1 -> DuoColors.FeatherGreen
                                    2 -> DuoColors.LilacPurple
                                    else -> DuoColors.FoxOrange
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { strokes.clear(); currentStroke = emptyList() }) {
                        Text("🔄 Clear")
                    }
                    OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeLast() }) {
                        Text("↩️ Undo")
                    }
                    OutlinedButton(onClick = { showGuide = !showGuide }) {
                        Text(if (showGuide) "🙈 Hide Guide" else "👁️ Guide")
                    }
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = "Finish Tracing (+XP)",
                onClick = onComplete,
                enabled = strokes.isNotEmpty(),
                topColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
