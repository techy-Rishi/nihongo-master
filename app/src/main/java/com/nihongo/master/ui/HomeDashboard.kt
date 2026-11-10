package com.nihongo.master.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

private val RANKS = listOf(
    0 to "🌱 初心者 Beginner",
    200 to "🥉 見習い Apprentice",
    600 to "🥈 学習者 Explorer",
    1500 to "🥇 達人 Samurai",
    3000 to "👑 名人 Nihongo Master"
)

private fun rankFor(xp: Int): String = RANKS.last { xp >= it.first }.second
private fun nextRankXp(xp: Int): Int = RANKS.firstOrNull { it.first > xp }?.first ?: (xp + 500)

private data class GuidedUnit(
    val unitNumber: Int,
    val title: String,
    val subtitleHi: String,
    val emoji: String,
    val topColor: Color,
    val lipColor: Color,
    val targetSection: AppSection,
    val sampleWords: List<VocabItem>
)

@Composable
fun HomeDashboardScreen(
    xp: Int,
    streak: Int,
    level: JlptLevel?,
    srsStates: Map<String, SrsCardState>,
    masteredIds: Set<String>,
    onGradeSrs: (String, SrsGrade) -> Unit,
    onNavigate: (AppSection) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit = {}
) {
    val today = SrsEngine.currentEpochDay()
    val vocabPool = remember(level) {
        if (level == null) DataRepository.vocab else DataRepository.words(level).ifEmpty { DataRepository.vocab }
    }
    val dueCards = remember(vocabPool, srsStates) {
        val reviews = vocabPool.filter { w -> srsStates[w.id]?.let { it.dueEpochDay <= today } == true }
        val newToday = srsStates.values.count { it.firstSeenDay == today }
        val newAllowed = (SrsEngine.NEW_CARDS_PER_DAY - newToday).coerceAtLeast(0)
        val fresh = vocabPool.filter { srsStates[it.id] == null }.take(newAllowed)
        reviews + fresh
    }
    val masteredInPool = vocabPool.count { it.id in masteredIds }
    val totalVocab = vocabPool.size.coerceAtLeast(1)

    var homeMode by remember { mutableIntStateOf(0) } // 0: Guided Learning Path, 1: SRS Flashcards
    var activeSrsIndex by remember(dueCards) { mutableIntStateOf(0) }
    var showSrsAnswer by remember(dueCards) { mutableStateOf(false) }
    var activeLessonUnit by remember { mutableStateOf<GuidedUnit?>(null) }

    val units = remember(vocabPool) {
        listOf(
            GuidedUnit(
                unitNumber = 1,
                title = "Hiragana & Katakana Basics",
                subtitleHi = "जापानी वर्णमाला (あいうえお) आवाज़ और स्ट्रोक्स के साथ सीखें",
                emoji = "🇯🇵",
                topColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark,
                targetSection = AppSection.WRITING,
                sampleWords = vocabPool.take(5)
            ),
            GuidedUnit(
                unitNumber = 2,
                title = "Greetings & Daily Survival",
                subtitleHi = "नमस्ते, धन्यवाद, माफ़ी और रोज़मर्रा के ज़रूरी शब्द",
                emoji = "👋",
                topColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark,
                targetSection = AppSection.SPEAKING,
                sampleWords = vocabPool.filter { it.type == WordType.OTHER }.ifEmpty { vocabPool.take(6) }
            ),
            GuidedUnit(
                unitNumber = 3,
                title = "SOV Sentence & Particles",
                subtitleHi = "हिन्दी जैसे वाक्य बनाना: は (Topic), を (Object), に/で",
                emoji = "🧩",
                topColor = DuoColors.LilacPurple,
                lipColor = DuoColors.LilacPurpleDark,
                targetSection = AppSection.GRAMMAR,
                sampleWords = vocabPool.filter { it.type == WordType.N }.take(6).ifEmpty { vocabPool.take(6) }
            ),
            GuidedUnit(
                unitNumber = 4,
                title = "Core Action Verbs & Food",
                subtitleHi = "खाना (食べる), पीना (飲む), जाना (行く) और बोलना",
                emoji = "🍜",
                topColor = DuoColors.FoxOrange,
                lipColor = DuoColors.FoxOrangeDark,
                targetSection = AppSection.LISTENING,
                sampleWords = vocabPool.filter { it.type.isVerb }.take(6).ifEmpty { vocabPool.take(6) }
            ),
            GuidedUnit(
                unitNumber = 5,
                title = "Adjectives & Expressing Feelings",
                subtitleHi = "बड़ा, छोटा, स्वादिष्ट, पसंद (好き) और मौसम बताना",
                emoji = "✨",
                topColor = DuoColors.CardinalRed,
                lipColor = DuoColors.CardinalRedDark,
                targetSection = AppSection.SPEAKING,
                sampleWords = vocabPool.filter { it.type == WordType.ADJI || it.type == WordType.ADJNA }.take(6).ifEmpty { vocabPool.take(6) }
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. DUOLINGO GAMIFIED HERO BANNER
        item {
            Duo3DCard(
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = DuoColors.MacawBlue.copy(alpha = 0.45f),
                lipColor = DuoColors.MacawBlueDark
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR LEAGUE RANK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlue
                            )
                            Text(
                                text = rankFor(xp),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = DuoColors.FoxOrangeBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔥 $streak d",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = DuoColors.FoxOrangeDark
                                )
                            }
                            Surface(
                                color = DuoColors.BeeGoldBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "⚡ $xp XP",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF9A6700)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val nextXp = nextRankXp(xp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Words Mastered: $masteredInPool / $totalVocab",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Next Rank: $xp / $nextXp XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (xp.toFloat() / nextXp.coerceAtLeast(1)).coerceIn(0.08f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape),
                        color = DuoColors.FeatherGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 2. MODE SWITCHER: GUIDED PATH vs DAILY SRS REVIEW
        item {
            DuoSegmentedTabs(
                tabs = listOf(
                    "🗺️ Beginner Path",
                    "🧠 SRS Cards (${dueCards.size} Due)"
                ),
                selectedIndex = homeMode,
                onSelect = { homeMode = it },
                activeColor = DuoColors.FeatherGreen,
                activeLipColor = DuoColors.FeatherGreenDark
            )
        }

        if (homeMode == 0) {
            // STEP-BY-STEP DUOLINGO LEARNING UNITS
            item {
                Duo3DCard(
                    containerColor = DuoColors.MacawBlueBg,
                    borderColor = DuoColors.MacawBlue,
                    lipColor = DuoColors.MacawBlueDark
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🦉", fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Beginner Tip: Step-by-Step चलें!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = OnPastel
                            )
                            Text(
                                text = "Pehle Unit 1 me Hiragana/Katakana आवाज़ के साथ पढ़ें, फिर हर Unit का 'Quick Lesson' खेलें।",
                                fontSize = 12.sp,
                                color = OnPastel.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            itemsIndexed(units) { index, unit ->
                DuoUnitCard(
                    unit = unit,
                    isUnlocked = true,
                    onStartQuickLesson = { activeLessonUnit = unit },
                    onOpenFullLab = { onNavigate(unit.targetSection) },
                    onSpeak = onSpeak
                )
            }
        } else {
            // 3D SPACED REPETITION FLASHCARD QUEUE
            item {
                Duo3DCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🧠 SM-2 Memory Flashcards",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Tap card to flip • Grade honestly to lock into long-term memory",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = DuoColors.FoxOrangeBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${dueCards.size} left",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = DuoColors.FoxOrangeDark
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (dueCards.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎉", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "All SRS Reviews Done For Today!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = DuoColors.FeatherGreenDark
                                )
                                Text(
                                    text = "Switch to '🗺️ Beginner Path' to practice new Units anytime.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val cardWord = dueCards[activeSrsIndex.coerceIn(0, dueCards.lastIndex)]
                            Duo3DCard(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                borderColor = DuoColors.MacawBlue.copy(alpha = 0.5f),
                                lipColor = DuoColors.MacawBlueDark,
                                onClick = {
                                    showSrsAnswer = !showSrsAnswer
                                    onSpeak(cardWord.kana, ListeningSpeed.NATURAL)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(cardWord.level.color),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${cardWord.level.label} •${cardWord.type.label}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        IconButton(onClick = { onSpeak(cardWord.kana, ListeningSpeed.NATURAL) }) {
                                            Icon(
                                                Icons.Default.VolumeUp,
                                                contentDescription = "Listen",
                                                tint = DuoColors.MacawBlue
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = cardWord.kanji,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    AnimatedVisibility(visible = showSrsAnswer) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 10.dp)
                                        ) {
                                            Text(
                                                text = "${cardWord.kana} (${cardWord.romaji})",
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DuoColors.MacawBlue
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Surface(
                                                color = DuoColors.FeatherGreenBg,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "${cardWord.en} • 🇮🇳 ${cardWord.hi}",
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = OnPastel
                                                )
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = "📝 ${cardWord.exJp}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = cardWord.exEn,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!showSrsAnswer) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "👆 Tap Card to Reveal Reading & Hindi Meaning",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DuoColors.MacawBlue
                                        )
                                    }
                                }
                            }

                            if (showSrsAnswer) {
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SrsGrade.entries.forEach { grade ->
                                        val topCol = Color(grade.colorHex)
                                        Duo3DButton(
                                            text = "${grade.label.split(" ")[0]}\n+${grade.xpReward}XP",
                                            onClick = {
                                                onGradeSrs(cardWord.id, grade)
                                                showSrsAnswer = false
                                                activeSrsIndex = 0
                                            },
                                            topColor = topCol,
                                            lipColor = topCol.copy(alpha = 0.75f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK LAB LAUNCHERS
        item {
            Text(
                text = "⚡ Quick Practice Dojos",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabShortcutCard(
                    title = "あア Kana & Kanji",
                    subtitle = "Hiragana, Katakana & Trace",
                    color = DuoColors.FeatherGreen,
                    lipColor = DuoColors.FeatherGreenDark,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(AppSection.WRITING) }

                LabShortcutCard(
                    title = "🧩 Grammar Lego",
                    subtitle = "Visual Rules & Verbs",
                    color = DuoColors.LilacPurple,
                    lipColor = DuoColors.LilacPurpleDark,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(AppSection.GRAMMAR) }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabShortcutCard(
                    title = "🗣️ Pitch & Speak",
                    subtitle = "Tokyo Pitch & Roleplay",
                    color = DuoColors.CardinalRed,
                    lipColor = DuoColors.CardinalRedDark,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(AppSection.SPEAKING) }

                LabShortcutCard(
                    title = "🎧 Word Tiles",
                    subtitle = "Sentence Builder & Slang",
                    color = DuoColors.MacawBlue,
                    lipColor = DuoColors.MacawBlueDark,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(AppSection.LISTENING) }
            }
        }
    }

    // INTERACTIVE BITE-SIZED UNIT LESSON DIALOG
    activeLessonUnit?.let { unit ->
        UnitMicroLessonModal(
            unit = unit,
            onSpeak = onSpeak,
            onDismiss = { activeLessonUnit = null },
            onCompleteUnit = {
                onAwardXp(30)
                activeLessonUnit = null
            }
        )
    }
}

@Composable
private fun DuoUnitCard(
    unit: GuidedUnit,
    isUnlocked: Boolean,
    onStartQuickLesson: () -> Unit,
    onOpenFullLab: () -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit
) {
    Duo3DCard(
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = unit.topColor,
        lipColor = unit.lipColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(unit.topColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(unit.emoji, fontSize = 24.sp)
                    }
                    Column {
                        Text(
                            text = "UNIT ${unit.unitNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = unit.topColor
                        )
                        Text(
                            text = unit.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "🇮🇳 ${unit.subtitleHi}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Preview Word Pills (Tappable for instant audio!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                unit.sampleWords.take(3).forEach { w ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSpeak(w.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔊 ${w.kanji}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
                            Text(w.hi, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Duo3DButton(
                    text = "▶️ Start Bite-Size Lesson",
                    onClick = onStartQuickLesson,
                    topColor = unit.topColor,
                    lipColor = unit.lipColor,
                    modifier = Modifier.weight(1.3f)
                )
                Duo3DButton(
                    text = "Open Lab →",
                    onClick = onOpenFullLab,
                    topColor = MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = DuoColors.LightLip,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.7f)
                )
            }
        }
    }
}

@Composable
private fun UnitMicroLessonModal(
    unit: GuidedUnit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onDismiss: () -> Unit,
    onCompleteUnit: () -> Unit
) {
    val words = unit.sampleWords.ifEmpty { DataRepository.vocab.take(5) }
    var step by remember { mutableIntStateOf(0) }
    val currentWord = words[step.coerceIn(0, words.lastIndex)]

    LaunchedEffect(currentWord) {
        onSpeak(currentWord.kana, ListeningSpeed.NATURAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${unit.emoji} Unit${unit.unitNumber} (${step + 1}/${words.size})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / words.size.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = unit.topColor
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Duo3DCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    borderColor = unit.topColor,
                    lipColor = unit.lipColor,
                    onClick = { onSpeak(currentWord.kana, ListeningSpeed.NATURAL) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(currentWord.kanji, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "${currentWord.kana} •${currentWord.romaji}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoColors.MacawBlue
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = DuoColors.FeatherGreenBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${currentWord.en} • 🇮🇳 ${currentWord.hi}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPastel,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("🔊 ${currentWord.exJp}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(currentWord.exRomaji, fontSize = 11.sp, color = Color.Gray)
                        Text(currentWord.exEn, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSpeak(currentWord.kana, ListeningSpeed.SLOW) }) {
                        Text("🐢 0.65x Slow")
                    }
                    OutlinedButton(onClick = { onSpeak(currentWord.exJp, ListeningSpeed.NATURAL) }) {
                        Text("🔊 Sentence")
                    }
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = if (step + 1 < words.size) "Next Card →" else "🎉 Finish Lesson (+30 XP)",
                onClick = {
                    if (step + 1 < words.size) step++ else onCompleteUnit()
                },
                topColor = unit.topColor,
                lipColor = unit.lipColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun LabShortcutCard(
    title: String,
    subtitle: String,
    color: Color,
    lipColor: Color = color.copy(alpha = 0.75f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Duo3DCard(
        modifier = modifier,
        containerColor = color,
        borderColor = lipColor,
        lipColor = lipColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
