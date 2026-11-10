@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nihongo.master.*

private val TOKYO_STATION_NAMES = listOf(
    "01 • 東京 TOKYO" to "Shinkansen Terminal",
    "02 • 有楽町 YURAKUCHO" to "Pointer & Pronoun Plaza",
    "03 • 新橋 SHIMBASHI" to "Family & Guild District",
    "04 • 品川 SHINAGAWA" to "Katakana Tech Hub",
    "05 • 渋谷 SHIBUYA" to "Fashion & Travel Crossing",
    "06 • 原宿 HARAJUKU" to "Yen & Counter Arcade",
    "07 • 新宿 SHINJUKU" to "Clocktower & Calendar",
    "08 • 築地 TSUKIJI" to "Ramen & Izakaya Alley",
    "09 • 秋葉原 AKIHABARA" to "Home & Gadget Street",
    "10 • 浅草 ASAKUSA" to "Kaminarimon Compass",
    "11 • 上野 UENO" to "Express Transit Hub",
    "12 • 池袋 IKEBUKURO" to "Daily Action Dojo",
    "13 • 目黒 MEGURO" to "Te-Form & Favor Bridge",
    "14 • 鎌倉 KAMAKURA" to "Weather & Feeling Coast",
    "15 • 京都 KYOTO" to "Sakura & Nature Shrine",
    "16 • 富士山 MT. FUJI" to "JLPT N5 Grand Summit"
)

private val KANJI_STEP_NUMERALS = listOf("壱", "弐", "参", "四", "五", "六", "極")

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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }

    val appLanguage = LocalAppLanguage.current
    val showRomaji = LocalShowRomaji.current

    val curriculumUnits = remember { CurriculumEngine.buildUnitsForLevel(JlptLevel.N5) }

    var completedNodeIds by remember {
        mutableStateOf(prefs.getStringSet("completed_path_nodes_v2", emptySet())?.toSet() ?: emptySet())
    }

    fun markNodeCompleted(nodeId: String, rewardXp: Int) {
        if (nodeId !in completedNodeIds) {
            completedNodeIds = completedNodeIds + nodeId
            prefs.edit().putStringSet("completed_path_nodes_v2", completedNodeIds).apply()
        }
        onAwardXp(rewardXp)
    }

    val today = SrsEngine.currentEpochDay()
    val vocabPool = DataRepository.vocab
    val dueCards = remember(vocabPool, srsStates) {
        val reviews = vocabPool.filter { w -> srsStates[w.id]?.let { it.dueEpochDay <= today } == true }
        val newToday = srsStates.values.count { it.firstSeenDay == today }
        val newAllowed = (SrsEngine.NEW_CARDS_PER_DAY - newToday).coerceAtLeast(0)
        val fresh = vocabPool.filter { srsStates[it.id] == null }.take(newAllowed)
        reviews + fresh
    }

    val allNodesFlat = remember(curriculumUnits) { curriculumUnits.flatMap { it.nodes } }
    val currentActiveNodeId = remember(allNodesFlat, completedNodeIds) {
        allNodesFlat.firstOrNull { it.id !in completedNodeIds }?.id ?: allNodesFlat.lastOrNull()?.id
    }
    val currentActiveUnitId = remember(curriculumUnits, currentActiveNodeId) {
        curriculumUnits.firstOrNull { u -> u.nodes.any { it.id == currentActiveNodeId } }?.id
            ?: curriculumUnits.first().id
    }

    var expandedUnitId by remember(currentActiveUnitId) { mutableStateOf<String?>(currentActiveUnitId) }
    var activeMode by remember { mutableIntStateOf(0) } // 0 = Shinkansen Line, 1 = SRS Deck
    var selectedGuidebookUnit by remember { mutableStateOf<CurriculumUnit?>(null) }
    var activeLessonNode by remember { mutableStateOf<Pair<CurriculumUnit, PathNodeSpec>?>(null) }

    activeLessonNode?.let { (unit, nodeSpec) ->
        ProgressiveBentoLessonStudio(
            unit = unit,
            node = nodeSpec,
            appLanguage = appLanguage,
            showRomaji = showRomaji,
            onSpeak = onSpeak,
            onClose = { activeLessonNode = null },
            onComplete = {
                markNodeCompleted(nodeSpec.id, nodeSpec.xpReward)
                activeLessonNode = null
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Clean 2-Tab Switcher (No wrapped 4-chip clutter!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            DuoSegmentedTabs(
                tabs = listOf(
                    "🚅 JR-N5 Chapters (16)",
                    "🧠 SRS Flashcards (${dueCards.size})"
                ),
                selectedIndex = activeMode,
                onSelect = { activeMode = it },
                activeColor = DuoColors.FeatherGreen,
                activeLipColor = DuoColors.FeatherGreenDark
            )
        }

        if (activeMode == 0) {
            val completedTotal = allNodesFlat.count { it.id in completedNodeIds }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    JrPassSummaryBanner(
                        completedLessons = completedTotal,
                        totalLessons = allNodesFlat.size,
                        masteredWords = masteredIds.size,
                        totalWords = vocabPool.size,
                        streak = streak,
                        xp = xp
                    )
                    Spacer(Modifier.height(16.dp))
                }

                itemsIndexed(curriculumUnits) { idx, unit ->
                    val stationInfo = TOKYO_STATION_NAMES.getOrElse(idx) {
                        "${idx + 1} • STATION" to "N5 Sector"
                    }
                    val isCurrentStation = unit.id == currentActiveUnitId
                    val isExpanded = unit.id == expandedUnitId

                    ShinkansenStationUnitItem(
                        unit = unit,
                        appLanguage = appLanguage,
                        stationBadge = stationInfo.first,
                        isLastStation = idx == curriculumUnits.lastIndex,
                        isCurrentStation = isCurrentStation,
                        isExpanded = isExpanded,
                        completedNodeIds = completedNodeIds,
                        currentActiveNodeId = currentActiveNodeId,
                        onToggleExpand = {
                            expandedUnitId = if (isExpanded) null else unit.id
                        },
                        onOpenGuidebook = { selectedGuidebookUnit = unit },
                        onLaunchNode = { nodeSpec ->
                            activeLessonNode = unit to nodeSpec
                        }
                    )
                }
            }
        } else {
            SrsReviewDeckSection(
                dueCards = dueCards,
                masteredCount = masteredIds.size,
                totalVocabCount = vocabPool.size,
                appLanguage = appLanguage,
                showRomaji = showRomaji,
                onGradeSrs = onGradeSrs,
                onSpeak = onSpeak,
                onSwitchToPath = { activeMode = 0 }
            )
        }
    }

    selectedGuidebookUnit?.let { unit ->
        UnitGuidebookModal(
            unit = unit,
            appLanguage = appLanguage,
            showRomaji = showRomaji,
            onSpeak = onSpeak,
            onDismiss = { selectedGuidebookUnit = null },
            onStartNextNode = {
                val nextNode = unit.nodes.firstOrNull { it.id !in completedNodeIds } ?: unit.nodes.first()
                selectedGuidebookUnit = null
                activeLessonNode = unit to nextNode
            }
        )
    }
}

@Composable
private fun JrPassSummaryBanner(
    completedLessons: Int,
    totalLessons: Int,
    masteredWords: Int,
    totalWords: Int,
    streak: Int,
    xp: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, DuoColors.FeatherGreen.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DuoColors.FeatherGreen,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "JR-N5 PASS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tokyo → Mt. Fuji Express",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "$completedLessons / $totalLessons Stamps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DuoColors.FeatherGreen
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (completedLessons.toFloat() / totalLessons.coerceAtLeast(1)).coerceIn(0.04f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = DuoColors.FeatherGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📚 Vocab Bank: $masteredWords / $totalWords",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🔥 $streak Day Streak  •  ⚡ $xp XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoColors.FoxOrange
                )
            }
        }
    }
}

@Composable
private fun ShinkansenStationUnitItem(
    unit: CurriculumUnit,
    appLanguage: AppLanguage,
    stationBadge: String,
    isLastStation: Boolean,
    isCurrentStation: Boolean,
    isExpanded: Boolean,
    completedNodeIds: Set<String>,
    currentActiveNodeId: String?,
    onToggleExpand: () -> Unit,
    onOpenGuidebook: () -> Unit,
    onLaunchNode: (PathNodeSpec) -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val completedInUnit = unit.nodes.count { it.id in completedNodeIds }
    val isUnitFullyDone = completedInUnit == unit.nodes.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isUnitFullyDone -> DuoColors.FeatherGreen
                            isCurrentStation -> accentColor
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = if (isCurrentStation) 2.5.dp else 1.5.dp,
                        color = if (isCurrentStation) Color.White else accentColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isUnitFullyDone -> "済"
                        isCurrentStation -> "🚅"
                        else -> "${unit.unitNumber}"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isUnitFullyDone || isCurrentStation) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLastStation) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            if (isUnitFullyDone) DuoColors.FeatherGreen
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = if (isCurrentStation || isExpanded) 2.dp else 1.dp,
                color = if (isCurrentStation || isExpanded) accentColor else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stationBadge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                            Text(
                                text = unit.durationLabel,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onOpenGuidebook() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Word List",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Words (${unit.unitVocab.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = unit.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = unit.subtitle(appLanguage),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(unit.mascotEmoji, fontSize = 28.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            unit.nodes.forEach { n ->
                                val done = n.id in completedNodeIds
                                val curr = n.id == currentActiveNodeId
                                Box(
                                    modifier = Modifier
                                        .size(width = if (curr) 20.dp else 12.dp, height = 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                done -> DuoColors.FeatherGreen
                                                curr -> accentColor
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                )
                            }
                        }
                        Text(
                            text = "$completedInUnit/${unit.nodes.size} Stamps ${if (isExpanded) "▲" else "▼"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        unit.nodes.forEachIndexed { nodeIdx, node ->
                            val isDone = node.id in completedNodeIds
                            val isCurr = node.id == currentActiveNodeId
                            val kanjiNum = KANJI_STEP_NUMERALS.getOrElse(nodeIdx) { "${nodeIdx + 1}" }

                            BentoLessonStampRow(
                                kanjiNumeral = kanjiNum,
                                node = node,
                                appLanguage = appLanguage,
                                accentColor = accentColor,
                                isCompleted = isDone,
                                isCurrent = isCurr,
                                onStart = { onLaunchNode(node) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoLessonStampRow(
    kanjiNumeral: String,
    node: PathNodeSpec,
    appLanguage: AppLanguage,
    accentColor: Color,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onStart: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = when {
                isCurrent -> accentColor
                isCompleted -> DuoColors.FeatherGreen.copy(alpha = 0.45f)
                else -> Color.Transparent
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStart() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isCompleted -> DuoColors.CardinalRed.copy(alpha = 0.14f)
                                isCurrent -> accentColor
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isCompleted) DuoColors.CardinalRed else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCompleted) "済" else kanjiNumeral,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isCompleted -> DuoColors.CardinalRed
                            isCurrent -> Color.White
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = node.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isCurrent) {
                            Surface(
                                color = accentColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "NEXT",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    Text(
                        text = node.subtitle(appLanguage),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = if (isCurrent) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isCompleted) "↻ +${node.xpReward}XP" else "▶ +${node.xpReward}XP",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// =========================================================================
// 3. PROGRESSIVE BENTO LESSON STUDIO
//    - PHASE 1: Teach all words in this lesson first (with audio + example sentence)
//    - PHASE 2: Quiz ONLY on the words taught in Phase 1
//    - PHASE 3: Build sentences ONLY from the words taught in Phase 1!
// =========================================================================
private enum class LessonStepType {
    TEACH_WORD,
    QUIZ_MEANING,
    BUILD_TAUGHT_WORD,
    BUILD_TAUGHT_SENTENCE,
    LISTEN_TAUGHT_SENTENCE
}

private data class ProgressiveLessonStep(
    val type: LessonStepType,
    val phaseLabel: String,
    val badgeEmoji: String,
    val promptMain: String,
    val promptSub: String,
    val audioText: String,
    val targetTokens: List<String>,
    val bankTokens: List<String>,
    val explanationLine: String,
    val vocabFocus: VocabItem? = null
)

@Composable
private fun ProgressiveBentoLessonStudio(
    unit: CurriculumUnit,
    node: PathNodeSpec,
    appLanguage: AppLanguage,
    showRomaji: Boolean,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onClose: () -> Unit,
    onComplete: () -> Unit
) {
    val accentColor = Color(unit.accentHex)

    // Pick 5 core words from this lesson node so the learner genuinely learns them and then practices them
    val lessonWords = remember(node, unit) {
        node.vocabList.ifEmpty { unit.unitVocab }.take(5).ifEmpty { DataRepository.vocab.take(5) }
    }

    val steps = remember(lessonWords, appLanguage) {
        val list = mutableListOf<ProgressiveLessonStep>()

        // PHASE 1: TEACH EACH WORD FIRST (So the user is NEVER tested on an unseen word!)
        lessonWords.forEachIndexed { idx, w ->
            list.add(
                ProgressiveLessonStep(
                    type = LessonStepType.TEACH_WORD,
                    phaseLabel = "PHASE 1: LEARN WORD ${idx + 1} OF ${lessonWords.size}",
                    badgeEmoji = unit.mascotEmoji,
                    promptMain = w.kanji,
                    promptSub = w.meaning(appLanguage),
                    audioText = w.kana,
                    targetTokens = listOf(w.kana),
                    bankTokens = emptyList(),
                    explanationLine = "${w.exJp} — ${w.exEn}",
                    vocabFocus = w
                )
            )
        }

        // PHASE 2: QUIZ ONLY ON THE TAUGHT WORDS (Distractors come strictly from `lessonWords`!)
        lessonWords.take(3).forEachIndexed { idx, w ->
            val targetMeaning = w.meaning(appLanguage)
            val wrongMeanings = lessonWords
                .filter { it.id != w.id }
                .map { it.meaning(appLanguage) }
                .distinct()
                .shuffled()
                .take(3)

            list.add(
                ProgressiveLessonStep(
                    type = LessonStepType.QUIZ_MEANING,
                    phaseLabel = "PHASE 2: WORD CHECK",
                    badgeEmoji = "🎯",
                    promptMain = w.headwordWithReading,
                    promptSub = w.romaji,
                    audioText = w.kana,
                    targetTokens = listOf(targetMeaning),
                    bankTokens = (wrongMeanings + targetMeaning).shuffled(),
                    explanationLine = "${w.headwordWithReading} (${w.romaji}) = $targetMeaning",
                    vocabFocus = w
                )
            )

            // Also test selecting the Japanese word from the taught pool
            val wrongJp = lessonWords
                .filter { it.id != w.id }
                .map { it.kanji }
                .distinct()
                .shuffled()
                .take(3)

            list.add(
                ProgressiveLessonStep(
                    type = LessonStepType.BUILD_TAUGHT_WORD,
                    phaseLabel = "PHASE 2: RECALL JAPANESE WORD",
                    badgeEmoji = "🍱",
                    promptMain = targetMeaning,
                    promptSub = if (idx == 0) "Select the matching word you just learned" else "",
                    audioText = w.kana,
                    targetTokens = listOf(w.kanji),
                    bankTokens = (wrongJp + w.kanji).distinct().shuffled(),
                    explanationLine = "${w.headwordWithReading} (${w.romaji}) = $targetMeaning",
                    vocabFocus = w
                )
            )
        }

        // PHASE 3: SENTENCE FORMATION USING TAUGHT WORDS + TAUGHT DISTRACTORS ONLY!
        val wordsWithSentences = lessonWords.filter { it.exJp.isNotBlank() }.take(3)
        wordsWithSentences.forEachIndexed { idx, w ->
            val sentTokens = splitSentenceForLesson(w.exJp)
            // Distractor tiles come strictly from the other words taught in THIS lesson!
            val taughtDistractors = lessonWords
                .filter { it.id != w.id }
                .map { it.kanji }
                .filter { it !in sentTokens }
                .take(2)

            list.add(
                ProgressiveLessonStep(
                    type = if (idx % 2 == 0) LessonStepType.BUILD_TAUGHT_SENTENCE else LessonStepType.LISTEN_TAUGHT_SENTENCE,
                    phaseLabel = "PHASE 3: FORM SENTENCE WITH 「${w.kanji}」",
                    badgeEmoji = "🚅",
                    promptMain = if (appLanguage == AppLanguage.HINDI) "${w.exEn} (${w.kanji} = ${w.hi})" else w.exEn,
                    promptSub = "Word learned: ${w.headwordWithReading} = ${w.meaning(appLanguage)}",
                    audioText = w.exJp,
                    targetTokens = sentTokens,
                    bankTokens = (sentTokens + taughtDistractors).shuffled(),
                    explanationLine = "${w.exJp} (${w.exRomaji}) — ${w.exEn}",
                    vocabFocus = w
                )
            )
        }

        list
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    val currentEx = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val indexedBank = remember(currentEx) {
        currentEx.bankTokens.mapIndexed { idx, token -> idx to token }
    }

    var selectedTiles by remember(stepIndex) { mutableStateOf(listOf<Pair<Int, String>>()) }
    var checkedResult by remember(stepIndex) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(currentEx) {
        onSpeak(currentEx.audioText, ListeningSpeed.NATURAL)
    }

    fun nextStepOrFinish() {
        if (stepIndex + 1 < steps.size) {
            stepIndex++
        } else {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }

                        LinearProgressIndicator(
                            progress = { (stepIndex + 1).toFloat() / steps.size },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "${stepIndex + 1}/${steps.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }

                    Text(
                        text = currentEx.phaseLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (currentEx.type == LessonStepType.TEACH_WORD) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    Duo3DButton(
                        text = "I LEARNED THIS WORD • NEXT →",
                        onClick = ::nextStepOrFinish,
                        topColor = accentColor,
                        lipColor = Color(unit.lipHex),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (checkedResult != null) {
                DuoBottomResultBanner(
                    isCorrect = checkedResult == true,
                    correctAnswerText = currentEx.targetTokens.joinToString(" "),
                    romajiSubtitle = currentEx.explanationLine,
                    onContinue = ::nextStepOrFinish
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Duo3DButton(
                        text = "CHECK ANSWER",
                        enabled = selectedTiles.isNotEmpty(),
                        onClick = {
                            val built = selectedTiles.joinToString("") { it.second }
                            val expected = currentEx.targetTokens.joinToString("")
                            checkedResult = (built == expected)
                            onSpeak(currentEx.audioText, ListeningSpeed.NATURAL)
                        },
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (currentEx.type) {
                // PHASE 1: CLEAN WORD TEACHING CARD (With Example Sentence Breakdown)
                LessonStepType.TEACH_WORD -> {
                    val v = currentEx.vocabFocus ?: DataRepository.vocab.first()
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(2.dp, accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeak(v.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = v.type.label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { onSpeak(v.kana, ListeningSpeed.SLOW) }) {
                                        Text("🐢 0.65x", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { onSpeak(v.kana, ListeningSpeed.NATURAL) }) {
                                        Text("🔊 1.0x", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text(v.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                            if (v.kanji != v.kana) {
                                Text(
                                    text = v.kana,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DuoColors.MacawBlue
                                )
                            }
                            if (showRomaji) {
                                Text(
                                    text = v.romaji,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = DuoColors.FeatherGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = v.meaning(appLanguage),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.FeatherGreen
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                            // Example sentence using this exact word (tappable for audio)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeak(v.exJp, ListeningSpeed.NATURAL) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "EXAMPLE SENTENCE (Tap to hear):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text("🔊 ${v.exJp}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    if (showRomaji) {
                                        Text(v.exRomaji, fontSize = 12.sp, color = DuoColors.MacawBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(v.exEn, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // PHASE 2: 4-CARD MEANING QUIZ (ONLY TAUGHT WORDS)
                LessonStepType.QUIZ_MEANING -> {
                    DuoCharacterSentencePrompt(
                        characterEmoji = currentEx.badgeEmoji,
                        promptText = currentEx.promptMain,
                        subHintText = if (showRomaji) currentEx.promptSub else "",
                        onSpeakPrompt = { onSpeak(currentEx.audioText, ListeningSpeed.NATURAL) }
                    )

                    indexedBank.forEach { pair ->
                        val isSel = selectedTiles.firstOrNull()?.first == pair.first
                        Duo3DCard(
                            containerColor = if (isSel) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                            borderColor = if (isSel) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            lipColor = if (isSel) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                if (checkedResult == null) {
                                    selectedTiles = listOf(pair)
                                    onSpeak(currentEx.audioText, ListeningSpeed.NATURAL)
                                }
                            }
                        ) {
                            Text(
                                text = pair.second,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // PHASE 2 & 3: WORD & SENTENCE FORMATION FROM TAUGHT WORDS ONLY
                else -> {
                    if (currentEx.type == LessonStepType.LISTEN_TAUGHT_SENTENCE) {
                        DuoCharacterAudioPrompt(
                            characterEmoji = currentEx.badgeEmoji,
                            onPlayNormal = { onSpeak(currentEx.audioText, ListeningSpeed.NATURAL) },
                            onPlaySlow = { onSpeak(currentEx.audioText, ListeningSpeed.SLOW) }
                        )
                        if (currentEx.promptSub.isNotBlank()) {
                            Text(
                                text = "💡 ${currentEx.promptSub}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoColors.MacawBlue
                            )
                        }
                    } else {
                        DuoCharacterSentencePrompt(
                            characterEmoji = currentEx.badgeEmoji,
                            promptText = currentEx.promptMain,
                            subHintText = currentEx.promptSub,
                            onSpeakPrompt = { onSpeak(currentEx.audioText, ListeningSpeed.NATURAL) }
                        )
                    }

                    DuoRuledAnswerArea(
                        selectedTiles = selectedTiles,
                        showRomaji = showRomaji,
                        checkedResult = checkedResult,
                        onRemoveTile = { pair ->
                            if (checkedResult == null) {
                                selectedTiles = selectedTiles - pair
                            }
                        }
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        indexedBank.forEach { tilePair ->
                            val isUsed = tilePair in selectedTiles
                            DuoWordTile(
                                text = tilePair.second,
                                showRomaji = showRomaji,
                                isPlaceholderSlot = isUsed,
                                onClick = {
                                    if (!isUsed && checkedResult == null) {
                                        onSpeak(tilePair.second, ListeningSpeed.NATURAL)
                                        selectedTiles = selectedTiles + tilePair
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun splitSentenceForLesson(jp: String): List<String> {
    val clean = jp.replace("。", "").replace("？", "").replace("！", "").trim()
    val twoChar = setOf("から", "まで", "より", "だけ")
    val oneChar = setOf('は', 'が', 'を', 'に', 'で', 'へ', 'と', 'も', 'の')
    val out = mutableListOf<String>()
    val cur = StringBuilder()
    var i = 0
    while (i < clean.length) {
        val ch = clean[i]
        if (ch == '、' || ch == ' ') {
            if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() }
            i++
            continue
        }
        val pair = if (i + 1 < clean.length) clean.substring(i, i + 2) else ""
        if (pair in twoChar && cur.isNotEmpty()) {
            out.add(cur.toString()); out.add(pair); cur.clear(); i += 2
        } else if (ch in oneChar && cur.isNotEmpty()) {
            out.add(cur.toString()); out.add(ch.toString()); cur.clear(); i++
        } else {
            cur.append(ch); i++
        }
    }
    if (cur.isNotEmpty()) out.add(cur.toString())
    return out.ifEmpty { listOf(clean) }
}

// =========================================================================
// 4. COMPLETELY FIXED STATION HANDBOOK MODAL (NO EMPTY BOX OR VERTICAL TEXT!)
// =========================================================================
@Composable
private fun UnitGuidebookModal(
    unit: CurriculumUnit,
    appLanguage: AppLanguage,
    showRomaji: Boolean,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onDismiss: () -> Unit,
    onStartNextNode: () -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val lipColor = Color(unit.lipHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.84f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📖 STATION ${unit.unitNumber} HANDBOOK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = unit.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scrollable LazyColumn of Tip + Clean Word Cards (Zero squeezing or empty gaps!)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            color = accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 CHAPTER KEY TIP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = unit.guidebookTip(appLanguage),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "📚 CHAPTER VOCABULARY (${unit.unitVocab.size} Words — Tap to hear)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(unit.unitVocab) { w ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpeak(w.kana, ListeningSpeed.NATURAL) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = w.headwordWithReading,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Listen",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (showRomaji) {
                                    Text(
                                        text = w.romaji,
                                        fontSize = 12.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = w.meaning(appLanguage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.FeatherGreen
                                )

                                if (w.exJp.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "📝 ${w.exJp}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = w.exEn,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Duo3DButton(
                    text = "START STATION LESSON →",
                    onClick = onStartNextNode,
                    topColor = accentColor,
                    lipColor = lipColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SrsReviewDeckSection(
    dueCards: List<VocabItem>,
    masteredCount: Int,
    totalVocabCount: Int,
    appLanguage: AppLanguage,
    showRomaji: Boolean,
    onGradeSrs: (String, SrsGrade) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onSwitchToPath: () -> Unit
) {
    var activeSrsIndex by remember(dueCards) { mutableIntStateOf(0) }
    var showSrsAnswer by remember(dueCards) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Duo3DCard {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("🧠 SM-2 Spaced Repetition Deck", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Mastered: $masteredCount / $totalVocabCount N5 words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))

                if (dueCards.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💮", fontSize = 48.sp)
                        Text("All Daily SRS Reviews Complete!", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DuoColors.FeatherGreen)
                        Spacer(Modifier.height(12.dp))
                        Duo3DButton(text = "RETURN TO JR-N5 CHAPTERS →", onClick = onSwitchToPath)
                    }
                } else {
                    val cardWord = dueCards[activeSrsIndex.coerceIn(0, dueCards.lastIndex)]
                    Duo3DCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        borderColor = DuoColors.MacawBlue,
                        lipColor = DuoColors.MacawBlueDark,
                        onClick = {
                            showSrsAnswer = !showSrsAnswer
                            onSpeak(cardWord.kana, ListeningSpeed.NATURAL)
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showRomaji) {
                                Text(cardWord.romaji, fontSize = 13.sp, color = DuoColors.MacawBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(cardWord.kanji, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                            if (showSrsAnswer) {
                                Spacer(Modifier.height(8.dp))
                                if (cardWord.kanji != cardWord.kana) {
                                    Text(cardWord.kana, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DuoColors.MacawBlue)
                                }
                                Text(
                                    text = cardWord.meaning(appLanguage),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.FeatherGreen
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(cardWord.exJp, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(cardWord.exEn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Spacer(Modifier.height(10.dp))
                                Text("👆 Tap to flip card", fontSize = 12.sp, color = DuoColors.MacawBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showSrsAnswer) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SrsGrade.entries.forEach { grade ->
                                val col = Color(grade.colorHex)
                                Duo3DButton(
                                    text = "${grade.label}\n+${grade.xpReward}XP",
                                    onClick = {
                                        onGradeSrs(cardWord.id, grade)
                                        showSrsAnswer = false
                                        activeSrsIndex = 0
                                    },
                                    topColor = col,
                                    lipColor = col.copy(alpha = 0.75f),
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
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
    }
}
