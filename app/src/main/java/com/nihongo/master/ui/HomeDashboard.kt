@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*
import kotlin.math.PI
import kotlin.math.sin

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

    val activeLevel = level ?: JlptLevel.N5
    val curriculumUnits = remember(activeLevel) { CurriculumEngine.buildUnitsForLevel(activeLevel) }

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

    // SRS Queue Calculation
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

    var activeMode by remember { mutableIntStateOf(0) } // 0 = Winding Path, 1 = SRS Deck
    var selectedGuidebookUnit by remember { mutableStateOf<CurriculumUnit?>(null) }
    var activeLessonNode by remember { mutableStateOf<Pair<CurriculumUnit, PathNodeSpec>?>(null) }

    // Determine the first uncompleted node in the current level track so we can highlight it as "CURRENT"
    val allNodesFlat = remember(curriculumUnits) { curriculumUnits.flatMap { it.nodes } }
    val currentActiveNodeId = remember(allNodesFlat, completedNodeIds) {
        allNodesFlat.firstOrNull { it.id !in completedNodeIds }?.id ?: allNodesFlat.lastOrNull()?.id
    }
    val completedInTrack = remember(allNodesFlat, completedNodeIds) {
        allNodesFlat.count { it.id in completedNodeIds }
    }

    // If user tapped a node on the path, launch the full-screen interactive multi-stage lesson
    activeLessonNode?.let { (unit, nodeSpec) ->
        InteractivePathNodeSession(
            unit = unit,
            node = nodeSpec,
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
        // Compact Top Track Status & Mode Toggle Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeLevel.label} • ${activeLevel.title}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Completed: $completedInTrack / ${allNodesFlat.size} Lessons • Mastered: ${masteredIds.size} Words",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = activeMode == 0,
                            onClick = { activeMode = 0 },
                            label = {
                                Text("🗺️ Path", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
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
                }
            }
        }

        if (activeMode == 0) {
            // WINDING CHAPTER PATH (Like Duolingo Screenshots)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                itemsIndexed(curriculumUnits) { unitIdx, unit ->
                    UnitSectionHeaderBanner(
                        unit = unit,
                        completedCount = unit.nodes.count { it.id in completedNodeIds },
                        totalCount = unit.nodes.size,
                        onOpenGuidebook = { selectedGuidebookUnit = unit }
                    )

                    WindingUnitNodesTrail(
                        unit = unit,
                        completedNodeIds = completedNodeIds,
                        currentActiveNodeId = currentActiveNodeId,
                        onNodeClick = { clickedNode ->
                            activeLessonNode = unit to clickedNode
                        }
                    )

                    // Divider showing next unit's topic title between units
                    if (unitIdx < curriculumUnits.lastIndex) {
                        val nextUnit = curriculumUnits[unitIdx + 1]
                        NextUnitTopicDivider(nextTitle = nextUnit.title)
                    }
                }
            }
        } else {
            // FULL INTERACTIVE SM-2 SRS FLASHCARD QUEUE
            SrsReviewDeckSection(
                dueCards = dueCards,
                masteredCount = masteredIds.size,
                totalVocabCount = vocabPool.size,
                onGradeSrs = onGradeSrs,
                onSpeak = onSpeak,
                onSwitchToPath = { activeMode = 0 }
            )
        }
    }

    // UNIT GUIDEBOOK MODAL (Opens when tapping the notebook icon on any Unit Banner)
    selectedGuidebookUnit?.let { unit ->
        UnitGuidebookModal(
            unit = unit,
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
// 1. UNIT BANNER HEADER WITH DIAGONAL SHINE & GUIDEBOOK BUTTON
// =========================================================================
@Composable
private fun UnitSectionHeaderBanner(
    unit: CurriculumUnit,
    completedCount: Int,
    totalCount: Int,
    onOpenGuidebook: () -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val lipColor = Color(unit.lipHex)
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(lipColor)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accentColor)
        ) {
            // Subtle diagonal highlight stripes like Duolingo section banners
            Canvas(modifier = Modifier.matchParentSize()) {
                val stripePath = Path().apply {
                    moveTo(size.width * 0.55f, 0f)
                    lineTo(size.width * 0.72f, 0f)
                    lineTo(size.width * 0.52f, size.height)
                    lineTo(size.width * 0.35f, size.height)
                    close()
                }
                drawPath(stripePath, Color.White.copy(alpha = 0.10f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SECTION ${unit.sectionNumber}, UNIT ${unit.unitNumber}  •  ${unit.durationLabel.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = unit.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "🇮🇳 ${unit.subtitleHi}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Vertical separator line + Guidebook Icon Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(44.dp)
                            .background(lipColor.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(lipColor.copy(alpha = 0.45f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { onOpenGuidebook() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Unit Guidebook",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "$completedCount/$totalCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. WINDING S-CURVE NODES TRAIL + SIDE CULTURAL MASCOT BADGE
// =========================================================================
@Composable
private fun WindingUnitNodesTrail(
    unit: CurriculumUnit,
    completedNodeIds: Set<String>,
    currentActiveNodeId: String?,
    onNodeClick: (PathNodeSpec) -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val lipColor = Color(unit.lipHex)
    val completedCount = unit.nodes.count { it.id in completedNodeIds }
    val starsEarned = when {
        completedCount >= unit.nodes.size -> 3
        completedCount >= unit.nodes.size / 2 -> 2
        completedCount >= 1 -> 1
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            unit.nodes.forEachIndexed { idx, node ->
                // Smooth S-curve wave offset: 0 -> -56dp -> -78dp -> -40dp -> +36dp -> +72dp -> 0dp
                val angle = (idx.toDouble() / 2.0) * (PI / 2.0)
                val directionSign = if (unit.unitNumber % 2 == 1) -1f else 1f
                val waveOffsetDp = (sin(angle).toFloat() * 68f * directionSign).dp

                val isCompleted = node.id in completedNodeIds
                val isCurrent = node.id == currentActiveNodeId

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(94.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Render Side Mascot Badge opposite to the S-curve peak around the middle node
                    if (idx == 2) {
                        val mascotOffset = (-waveOffsetDp.value * 1.45f).dp
                        SideCulturalMascotBadge(
                            emoji = unit.mascotEmoji,
                            caption = unit.mascotCaption,
                            stars = starsEarned,
                            accentColor = accentColor,
                            modifier = Modifier.offset(x = mascotOffset)
                        )
                    }

                    // The 3D Tactile Node on the Winding Curve
                    Box(modifier = Modifier.offset(x = waveOffsetDp)) {
                        WindingPathNodeButton(
                            node = node,
                            unitAccent = accentColor,
                            unitLip = lipColor,
                            isCompleted = isCompleted,
                            isCurrent = isCurrent,
                            onClick = { onNodeClick(node) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WindingPathNodeButton(
    node: PathNodeSpec,
    unitAccent: Color,
    unitLip: Color,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val lockedTop = if (isDark) Color(0xFF283942) else Color(0xFFE2E8F0)
    val lockedLip = if (isDark) Color(0xFF1A262D) else Color(0xFFCBD5E1)
    val lockedIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val topColor = when {
        node.type == PathNodeType.CHEST_REWARD && (isCompleted || isCurrent) -> DuoColors.BeeGold
        isCompleted || isCurrent -> unitAccent
        else -> lockedTop
    }
    val bottomLip = when {
        node.type == PathNodeType.CHEST_REWARD && (isCompleted || isCurrent) -> DuoColors.BeeGoldDark
        isCompleted || isCurrent -> unitLip
        else -> lockedLip
    }
    val contentColor = if (isCompleted || isCurrent) Color.White else lockedIconColor

    Box(contentAlignment = Alignment.TopCenter) {
        // Outer halo ring if this is the active "CURRENT" step
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = if (isCurrent) 18.dp else 4.dp)
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(4.dp, unitAccent.copy(alpha = 0.40f), CircleShape)
                )
            }

            // 3D Tactile Circular Node (or Rounded Square for Chest)
            val nodeShape = if (node.type == PathNodeType.CHEST_REWARD) RoundedCornerShape(20.dp) else CircleShape
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(nodeShape)
                    .background(bottomLip)
                    .clickable { onClick() }
                    .padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(nodeShape)
                        .background(topColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle top shine arc
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .size(width = 32.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isCompleted || isCurrent) 0.25f else 0.08f))
                    )

                    Text(
                        text = node.type.badgeIcon,
                        fontSize = if (node.type == PathNodeType.KANA_DOJO) 27.sp else 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                }
            }

            // Completed small golden crown badge in bottom-right corner
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DuoColors.BeeGold)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = OnPastel)
                }
            }
        }

        // Floating "START" callout bubble above the current active node
        if (isCurrent) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, unitAccent),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "START • ${node.title}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = unitAccent
                )
            }
        }
    }
}

@Composable
private fun SideCulturalMascotBadge(
    emoji: String,
    caption: String,
    stars: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f))
                .border(2.dp, accentColor.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 38.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..3) {
                Text(
                    text = "★",
                    fontSize = 13.sp,
                    color = if (i <= stars) DuoColors.BeeGold else Color.Gray.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun NextUnitTopicDivider(nextTitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
        )
        Text(
            text = nextTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
        )
    }
}

// =========================================================================
// 3. UNIT GUIDEBOOK MODAL (KEY PHRASES, VOCABULARY & GRAMMAR TIPS)
// =========================================================================
@Composable
private fun UnitGuidebookModal(
    unit: CurriculumUnit,
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
                    Text(
                        text = "📖 UNIT ${unit.unitNumber} GUIDEBOOK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
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
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hindi Concept Tip Box
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 मुख्य नियम (Key Concept)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = unit.guidebookTipHi,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Key Phrases Section
                Text(
                    text = "🗣️ KEY PHRASES (Tap to hear audio)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                unit.unitSentences.forEach { sent ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeak(sent.jp, ListeningSpeed.NATURAL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sent.jp, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(sent.romaji, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                Text("${sent.en} • 🇮🇳 ${sent.hi}", fontSize = 12.sp)
                            }
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = accentColor)
                        }
                    }
                }

                // Unit Vocabulary Checklist
                Text(
                    text = "📚 UNIT VOCABULARY (${unit.unitVocab.size} Words)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                unit.unitVocab.forEach { w ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeak(w.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${w.kanji} 【${w.kana}】", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text(w.romaji, fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                text = "${w.en}\n🇮🇳 ${w.hi}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = "START UNIT LESSON →",
                onClick = onStartNextNode,
                topColor = accentColor,
                lipColor = lipColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// =========================================================================
// 4. IN-PLACE INTERACTIVE NODE LESSON SESSION (4 STAGES PER NODE!)
// Stage 0: Study Flashcards (Kana / Words / Grammar with Slow & Normal Audio)
// Stage 1: Recognition Quiz (4-Choice Meaning or Kana Sound)
// Stage 2: Audio Listening Comprehension Quiz
// Stage 3: Sentence Word-Tile Builder
// =========================================================================
@Composable
private fun InteractivePathNodeSession(
    unit: CurriculumUnit,
    node: PathNodeSpec,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onClose: () -> Unit,
    onComplete: () -> Unit
) {
    val accentColor = Color(unit.accentHex)
    val lipColor = Color(unit.lipHex)

    val words = remember(node, unit) { node.vocabList.ifEmpty { unit.unitVocab }.ifEmpty { DataRepository.vocab.take(5) } }
    val sentences = remember(node, unit) { node.sentenceList.ifEmpty { unit.unitSentences }.ifEmpty { DataRepository.allSentences.take(3) } }
    val kanaItems = remember(node, unit) { node.kanaList.ifEmpty { unit.unitKana } }

    var stage by remember { mutableIntStateOf(0) } // 0..3
    val totalStages = 4

    // Stage 0: Discovery Flashcard index
    var cardIdx by remember { mutableIntStateOf(0) }

    // Stage 1: Recognition Quiz state
    val quizTargetWord = remember(words) { words.first() }
    val quizOptions = remember(quizTargetWord) {
        val wrong = DataRepository.vocab
            .filter { it.id != quizTargetWord.id }
            .shuffled()
            .take(3)
            .map { "${it.en} (${it.hi})" }
        val correct = "${quizTargetWord.en} (${quizTargetWord.hi})"
        (wrong + correct).shuffled()
    }
    val correctQuizIdx = quizOptions.indexOf("${quizTargetWord.en} (${quizTargetWord.hi})")
    var selectedQuizIdx by remember { mutableStateOf<Int?>(null) }

    // Stage 2: Audio Listening Quiz state
    val audioTargetWord = remember(words) { words.last() }
    val audioOptions = remember(audioTargetWord) {
        val wrong = DataRepository.vocab
            .filter { it.id != audioTargetWord.id }
            .shuffled()
            .take(3)
            .map { "${it.kanji} 【${it.kana}】 — ${it.hi}" }
        val correct = "${audioTargetWord.kanji} 【${audioTargetWord.kana}】 — ${audioTargetWord.hi}"
        (wrong + correct).shuffled()
    }
    val correctAudioIdx = audioOptions.indexOf("${audioTargetWord.kanji} 【${audioTargetWord.kana}】 — ${audioTargetWord.hi}")
    var selectedAudioIdx by remember { mutableStateOf<Int?>(null) }

    // Stage 3: Sentence Word-Tile Builder state
    val targetSentence = remember(sentences) { sentences.first() }
    val sentenceTokens = remember(targetSentence) { splitSentenceForLesson(targetSentence.jp) }
    val allBankTokens = remember(sentenceTokens) {
        val distractors = listOf("は", "を", "に", "で", "ません").filter { it !in sentenceTokens }.take(2)
        (sentenceTokens + distractors).mapIndexed { idx, t -> idx to t }.shuffled()
    }
    var pickedTiles by remember { mutableStateOf(listOf<Pair<Int, String>>()) }
    var sentenceChecked by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Lesson")
                    }
                    LinearProgressIndicator(
                        progress = { (stage + 1).toFloat() / totalStages },
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+${node.xpReward} XP",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "UNIT ${unit.unitNumber} • ${node.title.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = "🇮🇳 ${node.subtitleHi}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            when (stage) {
                // STAGE 0: INTERACTIVE DISCOVERY CARDS
                0 -> {
                    if (node.type == PathNodeType.KANA_DOJO && kanaItems.isNotEmpty()) {
                        Text(
                            text = "Tap each Kana card to hear native pronunciation:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            kanaItems.forEach { k ->
                                Duo3DCard(
                                    modifier = Modifier.width(98.dp),
                                    borderColor = accentColor,
                                    lipColor = lipColor,
                                    onClick = { onSpeak(k.char, ListeningSpeed.NATURAL) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🔊", fontSize = 11.sp)
                                        Text(k.char, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(k.romaji, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                                    }
                                }
                            }
                        }
                    } else {
                        val currentWord = words[cardIdx.coerceIn(0, words.lastIndex)]
                        LaunchedEffect(currentWord) {
                            onSpeak(currentWord.kana, ListeningSpeed.NATURAL)
                        }

                        Duo3DCard(
                            borderColor = accentColor,
                            lipColor = lipColor,
                            onClick = { onSpeak(currentWord.kana, ListeningSpeed.NATURAL) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "WORD ${cardIdx + 1} OF ${words.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(currentWord.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = "${currentWord.kana} (${currentWord.romaji})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = accentColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${currentWord.en} • 🇮🇳 ${currentWord.hi}",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("📝 ${currentWord.exJp}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(currentWord.exRomaji, fontSize = 11.sp, color = Color.Gray)
                                Text(currentWord.exEn, fontSize = 12.sp)
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
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cardIdx > 0) {
                                OutlinedButton(
                                    onClick = { cardIdx-- },
                                    modifier = Modifier.weight(1f)
                                ) { Text("← Prev Word") }
                            }
                            if (cardIdx + 1 < words.size) {
                                Duo3DButton(
                                    text = "Next Word (${cardIdx + 2}/${words.size}) →",
                                    onClick = { cardIdx++ },
                                    topColor = accentColor,
                                    lipColor = lipColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    node.grammarPoint?.let { gp ->
                        Duo3DCard(
                            borderColor = accentColor.copy(alpha = 0.5f),
                            lipColor = lipColor
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("📖 GRAMMAR FOCUS: ${gp.title}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = accentColor)
                                Spacer(Modifier.height(4.dp))
                                Text("Structure: ${gp.structure}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("🇮🇳 ${gp.hindiTip}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Duo3DButton(
                        text = "CONTINUE TO QUIZ →",
                        onClick = {
                            stage = 1
                            onSpeak(quizTargetWord.kana, ListeningSpeed.NATURAL)
                        },
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // STAGE 1: RECOGNITION QUIZ
                1 -> {
                    Duo3DCard(
                        borderColor = accentColor,
                        lipColor = lipColor,
                        onClick = { onSpeak(quizTargetWord.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Select the correct meaning:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text(quizTargetWord.kanji, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                            Text("🔊 ${quizTargetWord.kana} (${quizTargetWord.romaji})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }

                    quizOptions.forEachIndexed { idx, opt ->
                        val isSel = selectedQuizIdx == idx
                        val isCorr = idx == correctQuizIdx
                        val bg = when {
                            selectedQuizIdx == null -> MaterialTheme.colorScheme.surface
                            isCorr -> DuoColors.FeatherGreenBg
                            isSel -> DuoColors.CardinalRedBg
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val border = when {
                            selectedQuizIdx == null -> DuoColors.LightBorder
                            isCorr -> DuoColors.FeatherGreen
                            isSel -> DuoColors.CardinalRed
                            else -> DuoColors.LightBorder
                        }

                        Duo3DCard(
                            containerColor = bg,
                            borderColor = border,
                            lipColor = border,
                            onClick = {
                                if (selectedQuizIdx == null) {
                                    selectedQuizIdx = idx
                                    onSpeak(quizTargetWord.kana, ListeningSpeed.NATURAL)
                                }
                            }
                        ) {
                            Text(
                                text = opt,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (selectedQuizIdx != null && (isCorr || isSel)) OnPastel else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (selectedQuizIdx != null) {
                        Duo3DButton(
                            text = "NEXT: LISTENING CHALLENGE →",
                            onClick = {
                                stage = 2
                                onSpeak(audioTargetWord.kana, ListeningSpeed.NATURAL)
                            },
                            topColor = accentColor,
                            lipColor = lipColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // STAGE 2: AUDIO LISTENING MATCH
                2 -> {
                    Duo3DCard(
                        borderColor = accentColor,
                        lipColor = lipColor,
                        onClick = { onSpeak(audioTargetWord.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("What word do you hear?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Text("🔊", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onSpeak(audioTargetWord.kana, ListeningSpeed.SLOW) }) {
                                    Text("🐢 0.65x Slow")
                                }
                                OutlinedButton(onClick = { onSpeak(audioTargetWord.kana, ListeningSpeed.NATURAL) }) {
                                    Text("🔊 1.0x Normal")
                                }
                            }
                        }
                    }

                    audioOptions.forEachIndexed { idx, opt ->
                        val isSel = selectedAudioIdx == idx
                        val isCorr = idx == correctAudioIdx
                        val bg = when {
                            selectedAudioIdx == null -> MaterialTheme.colorScheme.surface
                            isCorr -> DuoColors.FeatherGreenBg
                            isSel -> DuoColors.CardinalRedBg
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val border = when {
                            selectedAudioIdx == null -> DuoColors.LightBorder
                            isCorr -> DuoColors.FeatherGreen
                            isSel -> DuoColors.CardinalRed
                            else -> DuoColors.LightBorder
                        }

                        Duo3DCard(
                            containerColor = bg,
                            borderColor = border,
                            lipColor = border,
                            onClick = {
                                if (selectedAudioIdx == null) {
                                    selectedAudioIdx = idx
                                    onSpeak(audioTargetWord.kana, ListeningSpeed.NATURAL)
                                }
                            }
                        ) {
                            Text(
                                text = opt,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (selectedAudioIdx != null && (isCorr || isSel)) OnPastel else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (selectedAudioIdx != null) {
                        Duo3DButton(
                            text = "FINAL STEP: BUILD SENTENCE →",
                            onClick = {
                                stage = 3
                                onSpeak(targetSentence.jp, ListeningSpeed.NATURAL)
                            },
                            topColor = accentColor,
                            lipColor = lipColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // STAGE 3: SENTENCE WORD-TILE BUILDER
                else -> {
                    Duo3DCard(
                        borderColor = accentColor,
                        lipColor = lipColor,
                        onClick = { onSpeak(targetSentence.jp, ListeningSpeed.NATURAL) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Build this Japanese sentence:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Spacer(Modifier.height(4.dp))
                            Text("🇮🇳 ${targetSentence.hi}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("🇬🇧 ${targetSentence.en}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Selected Tiles Tray
                    Duo3DCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        borderColor = accentColor.copy(alpha = 0.5f),
                        lipColor = lipColor
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 84.dp)
                                .padding(12.dp)
                        ) {
                            if (pickedTiles.isEmpty()) {
                                Text("👇 Tap Japanese word tiles below in order...", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pickedTiles.forEach { pair ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.5.dp, accentColor),
                                            modifier = Modifier.clickable {
                                                if (sentenceChecked == null) pickedTiles = pickedTiles - pair
                                            }
                                        ) {
                                            Text(
                                                text = pair.second,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Word Bank Tiles
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allBankTokens.forEach { pair ->
                            val used = pair in pickedTiles
                            Surface(
                                color = if (used) Color.Gray.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, if (used) Color.Transparent else DuoColors.LightBorder),
                                modifier = Modifier.clickable(enabled = !used && sentenceChecked == null) {
                                    onSpeak(pair.second, ListeningSpeed.NATURAL)
                                    pickedTiles = pickedTiles + pair
                                }
                            ) {
                                Text(
                                    text = pair.second,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (used) Color.Transparent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (sentenceChecked == null) {
                        Duo3DButton(
                            text = "CHECK SENTENCE",
                            onClick = {
                                val built = pickedTiles.joinToString("") { it.second }
                                val expected = sentenceTokens.joinToString("")
                                sentenceChecked = (built == expected)
                                onSpeak(targetSentence.jp, ListeningSpeed.NATURAL)
                            },
                            enabled = pickedTiles.isNotEmpty(),
                            topColor = DuoColors.FeatherGreen,
                            lipColor = DuoColors.FeatherGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val passed = sentenceChecked == true
                        Duo3DCard(
                            containerColor = if (passed) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                            borderColor = if (passed) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                            lipColor = if (passed) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (passed) "🎉 Lesson Complete! (+${node.xpReward} XP)" else "✅ Correct Sentence: ${targetSentence.jp}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = OnPastel
                                )
                                Text(
                                    text = targetSentence.romaji,
                                    fontSize = 12.sp,
                                    color = OnPastel.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(10.dp))
                                Duo3DButton(
                                    text = "CLAIM +${node.xpReward} XP & UNLOCK NEXT →",
                                    onClick = onComplete,
                                    topColor = DuoColors.FeatherGreen,
                                    lipColor = DuoColors.FeatherGreenDark,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
// 5. FULL SM-2 SPACED REPETITION FLASHCARD REVIEW SECTION
// =========================================================================
@Composable
private fun SrsReviewDeckSection(
    dueCards: List<VocabItem>,
    masteredCount: Int,
    totalVocabCount: Int,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🧠 SM-2 Spaced Repetition Deck", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(
                            "Mastered: $masteredCount / $totalVocabCount words",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(color = DuoColors.FoxOrangeBg, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "Due: ${dueCards.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = DuoColors.FoxOrangeDark
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (dueCards.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "All Daily SRS Reviews Complete!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = DuoColors.FeatherGreen
                        )
                        Spacer(Modifier.height(12.dp))
                        Duo3DButton(
                            text = "BACK TO WINDING CHAPTER PATH →",
                            onClick = onSwitchToPath,
                            topColor = DuoColors.MacawBlue,
                            lipColor = DuoColors.MacawBlueDark
                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = Color(cardWord.level.color), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${cardWord.level.label} • ${cardWord.pitchType.label}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                IconButton(onClick = { onSpeak(cardWord.kana, ListeningSpeed.NATURAL) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = DuoColors.MacawBlue)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(cardWord.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)

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
                                    Text(
                                        text = "${cardWord.en} • 🇮🇳 ${cardWord.hi}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DuoColors.FeatherGreen
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("📝 ${cardWord.exJp}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(cardWord.exEn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!showSrsAnswer) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "👆 Tap Card to Flip & Hear Pronunciation",
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
