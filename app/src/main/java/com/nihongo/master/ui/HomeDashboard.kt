@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

// Authentic Tokyo Station Names for the 16 N5 Chapters
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

    val curriculumUnits = remember { CurriculumEngine.buildUnitsForLevel(JlptLevel.N5) }

    var completedNodeIds by remember {
        mutableStateOf(prefs.getStringSet("completed_path_nodes_v2", emptySet())?.toSet() ?: emptySet())
    }
    var showRomaji by remember {
        mutableStateOf(prefs.getBoolean("show_romaji", true))
    }
    var showHindiHints by remember {
        mutableStateOf(prefs.getBoolean("show_hindi_hints", true))
    }

    fun toggleRomaji(enabled: Boolean) {
        showRomaji = enabled
        prefs.edit().putBoolean("show_romaji", enabled).apply()
    }

    fun toggleHindiHints(enabled: Boolean) {
        showHindiHints = enabled
        prefs.edit().putBoolean("show_hindi_hints", enabled).apply()
    }

    fun markNodeCompleted(nodeId: String, rewardXp: Int) {
        if (nodeId !in completedNodeIds) {
            completedNodeIds = completedNodeIds + nodeId
            prefs.edit().putStringSet("completed_path_nodes_v2", completedNodeIds).apply()
        }
        onAwardXp(rewardXp)
    }

    // SRS Queue
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

    // Full-screen Bento Lesson Studio when a station stamp is tapped
    activeLessonNode?.let { (unit, nodeSpec) ->
        BentoInteractiveLessonStudio(
            unit = unit,
            node = nodeSpec,
            showRomaji = showRomaji,
            onToggleRomaji = ::toggleRomaji,
            showHindiHints = showHindiHints,
            onToggleHindi = ::toggleHindiHints,
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
        // Top Control Bar: Shinkansen Route vs SRS Deck + Live X-Ray Romaji Toggle
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeMode == 0,
                        onClick = { activeMode = 0 },
                        label = {
                            Text("🚅 JR-N5 Route (16)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    )
                    FilterChip(
                        selected = activeMode == 1,
                        onClick = { activeMode = 1 },
                        label = {
                            Text("🧠 SRS (${dueCards.size})", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    )
                }

                BentoXRayToggleBar(
                    showRomaji = showRomaji,
                    onToggleRomaji = ::toggleRomaji,
                    showHindi = showHindiHints,
                    onToggleHindi = ::toggleHindiHints
                )
            }
        }

        if (activeMode == 0) {
            val completedTotal = allNodesFlat.count { it.id in completedNodeIds }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // JR Pass Progress Header Card
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

                // 16 Shinkansen Station Cards connected by a vertical rail line
                itemsIndexed(curriculumUnits) { idx, unit ->
                    val stationInfo = TOKYO_STATION_NAMES.getOrElse(idx) {
                        "${idx + 1} • STATION" to "N5 Sector"
                    }
                    val isCurrentStation = unit.id == currentActiveUnitId
                    val isExpanded = unit.id == expandedUnitId

                    ShinkansenStationUnitItem(
                        unit = unit,
                        stationBadge = stationInfo.first,
                        stationZone = stationInfo.second,
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

// =========================================================================
// 1. JR-N5 RAIL PASS SUMMARY BANNER
// =========================================================================
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

// =========================================================================
// 2. SHINKANSEN STATION UNIT CARD + BENTO STAMP GRID
// =========================================================================
@Composable
private fun ShinkansenStationUnitItem(
    unit: CurriculumUnit,
    stationBadge: String,
    stationZone: String,
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
        // Left Shinkansen Rail Line + Station Circle Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            // Station Node Circle
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
                        width = if (isCurrentStation) 3.dp else 1.5.dp,
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

            // Continuous Vertical Rail Track Line
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

        // Right Station Bento Card
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
                // Station Header (Clickable to expand/collapse Bento Stamps)
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

                        // Guidebook Handbook Pill
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
                                    contentDescription = "Notes",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Notes",
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
                                text = "🇮🇳 ${unit.subtitleHi}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(unit.mascotEmoji, fontSize = 28.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Mini Stamp Dots Row + Progress Fraction
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

                // EXPANDABLE BENTO LESSON STAMPS (Replaces Duolingo's Popover!)
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
                // Japanese Ema / Hanko Stamp Box (`壱`, `弐`, `参`...)
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
                        text = node.subtitleHi,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Pill (`+25 XP ▶`)
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
// 3. BENTO INTERACTIVE LESSON STUDIO (WITH LIVE 1-TAP ROMAJI X-RAY BAR)
// =========================================================================
private enum class BentoExerciseType {
    WORD_DISCOVERY_CARD,
    TRANSLATE_WORD_BENTO,
    AUDIO_STUDIO_DECODE,
    SENTENCE_BENTO_DOCK,
    MEANING_MATCH_GRID
}

private data class BentoExerciseItem(
    val type: BentoExerciseType,
    val badgeEmoji: String,
    val promptMain: String,
    val promptSubHi: String,
    val audioText: String,
    val targetTokens: List<String>,
    val bankTokens: List<String>,
    val explanationLine: String,
    val vocabFocus: VocabItem? = null
)

@Composable
private fun BentoInteractiveLessonStudio(
    unit: CurriculumUnit,
    node: PathNodeSpec,
    showRomaji: Boolean,
    onToggleRomaji: (Boolean) -> Unit,
    showHindiHints: Boolean,
    onToggleHindi: (Boolean) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onClose: () -> Unit,
    onComplete: () -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val words = remember(node, unit) {
        node.vocabList.ifEmpty { unit.unitVocab }.ifEmpty { DataRepository.vocab.take(6) }
    }
    val sentences = remember(node, unit) {
        node.sentenceList.ifEmpty { unit.unitSentences }.ifEmpty { DataRepository.allSentences.take(3) }
    }

    val exercises = remember(words, sentences) {
        val list = mutableListOf<BentoExerciseItem>()
        val w1 = words.getOrElse(0) { DataRepository.vocab.first() }
        val w2 = words.getOrElse(1) { w1 }
        val w3 = words.getOrElse(2) { w1 }
        val s1 = sentences.getOrElse(0) { DataRepository.allSentences.first() }
        val s2 = sentences.getOrElse(1) { s1 }

        // 1. Word Discovery Card with Pitch Accent Graph
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.WORD_DISCOVERY_CARD,
                badgeEmoji = unit.mascotEmoji,
                promptMain = w1.kanji,
                promptSubHi = "${w1.en} • 🇮🇳 ${w1.hi}",
                audioText = w1.kana,
                targetTokens = listOf(w1.kana),
                bankTokens = emptyList(),
                explanationLine = "${w1.exJp} (${w1.exEn})",
                vocabFocus = w1
            )
        )

        // 2. Word Bento Translation
        val w1Distractors = words.filter { it.id != w1.id }.map { it.kana }.ifEmpty { listOf("ホテル", "どこ", "水") }.take(3)
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.TRANSLATE_WORD_BENTO,
                badgeEmoji = "🍱",
                promptMain = w1.en,
                promptSubHi = "🇮🇳 ${w1.hi}",
                audioText = w1.kana,
                targetTokens = listOf(w1.kana),
                bankTokens = (listOf(w1.kana) + w1Distractors).distinct().shuffled(),
                explanationLine = "${w1.kanji} 【${w1.kana} • ${w1.romaji}】 = ${w1.en}"
            )
        )

        // 3. Acoustic Studio Decode ("Listen & Assemble")
        val s1Tokens = splitSentenceForLesson(s1.jp)
        val s1Distractors = listOf("ひと", "か", "さん", "どこ").filter { it !in s1Tokens }.take(2)
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.AUDIO_STUDIO_DECODE,
                badgeEmoji = "🎧",
                promptMain = s1.jp,
                promptSubHi = "${s1.en} (🇮🇳 ${s1.hi})",
                audioText = s1.jp,
                targetTokens = s1Tokens,
                bankTokens = (s1Tokens + s1Distractors).shuffled(),
                explanationLine = "${s1.romaji} — ${s1.en}"
            )
        )

        // 4. 4-Card Meaning Match
        val w2Wrong = DataRepository.vocab.filter { it.id != w2.id }.shuffled().take(3).map { "${it.en} • 🇮🇳 ${it.hi}" }
        val w2Target = "${w2.en} • 🇮🇳 ${w2.hi}"
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.MEANING_MATCH_GRID,
                badgeEmoji = "⛩️",
                promptMain = w2.kanji,
                promptSubHi = "${w2.kana} (${w2.romaji})",
                audioText = w2.kana,
                targetTokens = listOf(w2Target),
                bankTokens = (w2Wrong + w2Target).shuffled(),
                explanationLine = "${w2.kanji} (${w2.romaji}) = $w2Target",
                vocabFocus = w2
            )
        )

        // 5. Full Sentence Bento Dock Assembly
        val s2Tokens = splitSentenceForLesson(s2.jp)
        val s2Distractors = listOf("は", "を", "ません", "ホテル").filter { it !in s2Tokens }.take(2)
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.SENTENCE_BENTO_DOCK,
                badgeEmoji = "🚅",
                promptMain = s2.en,
                promptSubHi = "🇮🇳 ${s2.hi}",
                audioText = s2.jp,
                targetTokens = s2Tokens,
                bankTokens = (s2Tokens + s2Distractors).shuffled(),
                explanationLine = "${s2.jp} (${s2.romaji})"
            )
        )

        // 6. Third Word Translation
        val w3Distractors = words.filter { it.id != w3.id }.map { it.kanji }.ifEmpty { listOf("駅", "本", "車") }.take(3)
        list.add(
            BentoExerciseItem(
                type = BentoExerciseType.TRANSLATE_WORD_BENTO,
                badgeEmoji = "🌸",
                promptMain = w3.en,
                promptSubHi = "🇮🇳 ${w3.hi}",
                audioText = w3.kana,
                targetTokens = listOf(w3.kanji),
                bankTokens = (listOf(w3.kanji) + w3Distractors).distinct().shuffled(),
                explanationLine = "${w3.kanji} 【${w3.kana} • ${w3.romaji}】 = ${w3.en}"
            )
        )

        list
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    val currentEx = exercises[stepIndex.coerceIn(0, exercises.lastIndex)]
    val indexedBank = remember(currentEx) {
        currentEx.bankTokens.mapIndexed { idx, token -> idx to token }
    }

    var selectedTiles by remember(stepIndex) { mutableStateOf(listOf<Pair<Int, String>>()) }
    var checkedResult by remember(stepIndex) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(currentEx) {
        onSpeak(currentEx.audioText, ListeningSpeed.NATURAL)
    }

    fun nextStepOrFinish() {
        if (stepIndex + 1 < exercises.size) {
            stepIndex++
        } else {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            // Original Top Studio Bar: Close Button + Segmented Step Blocks + Live 1-Tap Romaji/Hindi X-Ray Pills!
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }

                        // Segmented Bento Step Indicator
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            exercises.indices.forEach { i ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                i < stepIndex -> DuoColors.FeatherGreen
                                                i == stepIndex -> accentColor
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                )
                            }
                        }

                        Text(
                            text = "${stepIndex + 1}/${exercises.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }

                    // 1-Tap Live X-Ray Toggle Bar (Romaji & Hindi always 1 tap away!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UNIT ${unit.unitNumber} • ${node.title.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BentoXRayToggleBar(
                            showRomaji = showRomaji,
                            onToggleRomaji = onToggleRomaji,
                            showHindi = showHindiHints,
                            onToggleHindi = onToggleHindi
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (currentEx.type == BentoExerciseType.WORD_DISCOVERY_CARD) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    Duo3DButton(
                        text = "GOT IT • START PRACTICE →",
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
                    if (currentEx.type == BentoExerciseType.AUDIO_STUDIO_DECODE) {
                        TextButton(onClick = ::nextStepOrFinish) {
                            Text(
                                text = "SKIP AUDIO EXERCISE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Duo3DButton(
                        text = "VERIFY ANSWER (確認)",
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
                // 1. NEW WORD DISCOVERY CARD WITH PITCH MORAS
                BentoExerciseType.WORD_DISCOVERY_CARD -> {
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
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "新出単語 • NEW N5 WORD",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(v.kanji, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = if (showRomaji) "${v.kana}  •  ${v.romaji}" else v.kana,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlue
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (showHindiHints) "${v.en} • 🇮🇳 ${v.hi}" else v.en,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.FeatherGreen
                            )

                            Spacer(Modifier.height(14.dp))

                            // Live Pitch Accent Mora Pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                v.pitchMoras.forEach { pm ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (pm.isHigh) "▲" else "▽",
                                            fontSize = 10.sp,
                                            color = if (pm.isHigh) DuoColors.MacawBlue else Color.Gray
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = pm.mora + if (pm.hasDropAfter) "↘" else "",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))
                            Text("🔊 ${v.exJp}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            if (showRomaji) {
                                Text(v.exRomaji, fontSize = 12.sp, color = DuoColors.MacawBlue, fontWeight = FontWeight.SemiBold)
                            }
                            Text(v.exEn, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 2. 4-CARD MEANING MATCH
                BentoExerciseType.MEANING_MATCH_GRID -> {
                    DuoCharacterSentencePrompt(
                        characterEmoji = currentEx.badgeEmoji,
                        promptText = currentEx.promptMain,
                        subHintText = if (showRomaji) currentEx.promptSubHi else "",
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

                // 3. AUDIO STUDIO DECODE & BENTO SENTENCE ASSEMBLY
                else -> {
                    if (currentEx.type == BentoExerciseType.AUDIO_STUDIO_DECODE) {
                        DuoCharacterAudioPrompt(
                            characterEmoji = currentEx.badgeEmoji,
                            onPlayNormal = { onSpeak(currentEx.audioText, ListeningSpeed.NATURAL) },
                            onPlaySlow = { onSpeak(currentEx.audioText, ListeningSpeed.SLOW) }
                        )
                    } else {
                        DuoCharacterSentencePrompt(
                            characterEmoji = currentEx.badgeEmoji,
                            promptText = currentEx.promptMain,
                            subHintText = if (showHindiHints) currentEx.promptSubHi else "",
                            onSpeakPrompt = { onSpeak(currentEx.audioText, ListeningSpeed.NATURAL) }
                        )
                    }

                    // Magnetic Bento Sentence Dock
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

                    // Fixed-Slot Magnetic Word Blocks
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
// 4. STATION GUIDEBOOK MODAL & SRS DECK
// =========================================================================
@Composable
private fun UnitGuidebookModal(
    unit: CurriculumUnit,
    showRomaji: Boolean,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onDismiss: () -> Unit,
    onStartNextNode: () -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val lipColor = Color(unit.lipHex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📖 STATION ${unit.unitNumber} HANDBOOK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    Text(unit.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💡 मुख्य नियम (Key Tip)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                        Spacer(Modifier.height(4.dp))
                        Text(unit.guidebookTipHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text("📚 STATION VOCABULARY (${unit.unitVocab.size} Words)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                unit.unitVocab.forEach { w ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeak(w.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${w.kanji} 【${w.kana}】", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                if (showRomaji) Text(w.romaji, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            }
                            Text("${w.en}\n🇮🇳 ${w.hi}", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = "START STATION LESSON →",
                onClick = onStartNextNode,
                topColor = accentColor,
                lipColor = lipColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun SrsReviewDeckSection(
    dueCards: List<VocabItem>,
    masteredCount: Int,
    totalVocabCount: Int,
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
                        Duo3DButton(text = "RETURN TO JR-N5 ROUTE →", onClick = onSwitchToPath)
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
                                Text("${cardWord.kana} • ${cardWord.en} (🇮🇳 ${cardWord.hi})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.FeatherGreen)
                                Spacer(Modifier.height(6.dp))
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
                                    text = "${grade.label.split(" ")[0]}\n+${grade.xpReward}XP",
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
