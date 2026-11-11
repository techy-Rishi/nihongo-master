package com.nihongo.master.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*
import kotlin.math.roundToInt

// =========================================================================
// SPECIFIC ROWS FOR ROW-WISE OR OVERALL PRACTICE
// =========================================================================
private enum class KanaRowFilter(
    val label: String,
    val romajiSet: Set<String>?
) {
    ALL_OVERALL("All Characters (Overall)", null),
    VOWELS("A-Row (あ い う え お)", setOf("a", "i", "u", "e", "o")),
    K_ROW("K-Row (か き く け こ)", setOf("ka", "ki", "ku", "ke", "ko")),
    S_ROW("S-Row (さ し す せ そ)", setOf("sa", "shi", "su", "se", "so")),
    T_ROW("T-Row (た ち つ て と)", setOf("ta", "chi", "tsu", "te", "to")),
    N_ROW("N-Row (な に ぬ ね の)", setOf("na", "ni", "nu", "ne", "no")),
    H_ROW("H-Row (は ひ ふ へ ほ)", setOf("ha", "hi", "fu", "he", "ho")),
    M_ROW("M-Row (ま み む め も)", setOf("ma", "mi", "mu", "me", "mo")),
    Y_ROW("Y-Row (や ゆ よ)", setOf("ya", "yu", "yo")),
    R_ROW("R-Row (ら り る れ ろ)", setOf("ra", "ri", "ru", "re", "ro")),
    W_ROW("W/N-Row (わ を ん)", setOf("wa", "wo", "n")),
    DAKUTEN("Dakuten ゛゜ (25)", null),
    COMBO("Yōon Combo (33)", null);

    fun filterKana(type: KanaType): List<KanaChar> {
        return DataRepository.allKana.filter { k ->
            k.type == type && when (this) {
                ALL_OVERALL -> k.group == KanaGroup.BASIC
                DAKUTEN -> k.group == KanaGroup.VOICED
                COMBO -> k.group == KanaGroup.COMBO
                else -> k.group == KanaGroup.BASIC && (romajiSet?.contains(k.romaji) == true)
            }
        }
    }
}

/**
 * Builds the authentic 5-column Gojūon grid rows with `null` for empty slots
 * (`や _ ゆ _ よ`, `わ _ _ _ を`, `ん _ _ _ _`) matching the screenshot!
 */
private fun buildGojuonFiveColumnRows(type: KanaType, basicKana: List<KanaChar>): List<List<KanaChar?>> {
    val byRomaji = basicKana.filter { it.type == type && it.group == KanaGroup.BASIC }.associateBy { it.romaji }
    val rowTemplates = listOf(
        listOf("a", "i", "u", "e", "o"),
        listOf("ka", "ki", "ku", "ke", "ko"),
        listOf("sa", "shi", "su", "se", "so"),
        listOf("ta", "chi", "tsu", "te", "to"),
        listOf("na", "ni", "nu", "ne", "no"),
        listOf("ha", "hi", "fu", "he", "ho"),
        listOf("ma", "mi", "mu", "me", "mo"),
        listOf("ya", null, "yu", null, "yo"),
        listOf("ra", "ri", "ru", "re", "ro"),
        listOf("wa", null, null, null, "wo"),
        listOf("n", null, null, null, null)
    )
    return rowTemplates.map { row -> row.map { r -> if (r == null) null else byRomaji[r] } }
}

// =========================================================================
// FORGIVING BITMAP OVERLAP TRACING EVALUATOR (NO PERCENTAGE TEXT SHOWN)
// =========================================================================
private data class TraceEvaluation(
    val isPassed: Boolean,
    val feedbackText: String
)

private object TraceAccuracyEvaluator {
    private const val GRID_SIZE = 120

    fun evaluate(
        targetChar: String,
        strokes: List<List<Offset>>,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): TraceEvaluation {
        if (strokes.isEmpty() || canvasWidthPx <= 10f || canvasHeightPx <= 10f) {
            return TraceEvaluation(false, "Trace the character strokes on the canvas.")
        }

        val scaleX = GRID_SIZE / canvasWidthPx
        val scaleY = GRID_SIZE / canvasHeightPx

        val refBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val refCanvas = android.graphics.Canvas(refBitmap)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = if (targetChar.length > 1) GRID_SIZE * 0.48f else GRID_SIZE * 0.72f
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 6f
        }
        val fontMetrics = textPaint.fontMetrics
        val centerY = (GRID_SIZE / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        refCanvas.drawText(targetChar, GRID_SIZE / 2f, centerY, textPaint)

        val userBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val userCanvas = android.graphics.Canvas(userBitmap)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (pts in strokes) {
            if (pts.size == 1) {
                userCanvas.drawPoint(pts[0].x * scaleX, pts[0].y * scaleY, strokePaint)
            } else if (pts.size > 1) {
                val p = android.graphics.Path()
                p.moveTo(pts.first().x * scaleX, pts.first().y * scaleY)
                for (i in 1 until pts.size) {
                    p.lineTo(pts[i].x * scaleX, pts[i].y * scaleY)
                }
                userCanvas.drawPath(p, strokePaint)
            }
        }

        var refPixels = 0
        var coveredRefPixels = 0
        var userPixels = 0
        var onTrackUserPixels = 0

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val refOn = android.graphics.Color.alpha(refBitmap.getPixel(x, y)) > 40
                val userOn = android.graphics.Color.alpha(userBitmap.getPixel(x, y)) > 40
                if (refOn) {
                    refPixels++
                    if (userOn) coveredRefPixels++
                }
                if (userOn) {
                    userPixels++
                    if (refOn) onTrackUserPixels++
                }
            }
        }

        if (refPixels == 0 || userPixels < 25) {
            return TraceEvaluation(false, "Keep tracing to complete the character shape.")
        }

        val coverage = coveredRefPixels.toFloat() / refPixels.toFloat()
        val precision = onTrackUserPixels.toFloat() / userPixels.toFloat()
        val rawScore = (coverage * 0.62f + precision * 0.38f) * 125f
        val accuracy = rawScore.roundToInt().coerceIn(10, 99)
        val passed = accuracy >= 55 && coverage >= 0.32f

        val feedback = when {
            passed -> "✨ Well done! Progress bar increased."
            coverage < 0.32f -> "⚠️ Complete all strokes of 「$targetChar」"
            else -> "🔄 Follow the guide shape a little closer"
        }

        return TraceEvaluation(passed, feedback)
    }
}

// =========================================================================
// SILENT 0.5% STEP MASTERY STORAGE (1 unit = 0.5%, 200 units = full bar)
// =========================================================================
private const val MAX_MASTERY_UNITS = 200 // 200 * 0.5% = 100% full bar

private fun loadKanaMasteryMap(prefs: android.content.SharedPreferences): Map<String, Int> {
    val raw = prefs.getStringSet("kana_mastery_half_pct_v1", emptySet()).orEmpty()
    val out = mutableMapOf<String, Int>()
    for (entry in raw) {
        val parts = entry.split(":")
        if (parts.size == 2) {
            parts[1].toIntOrNull()?.let { out[parts[0]] = it.coerceIn(0, MAX_MASTERY_UNITS) }
        }
    }
    return out
}

private fun saveKanaMasteryMap(prefs: android.content.SharedPreferences, map: Map<String, Int>) {
    val set = map.entries.map { "${it.key}:${it.value}" }.toSet()
    prefs.edit().putStringSet("kana_mastery_half_pct_v1", set).apply()
}

/** Returns progress fraction `0f..1f` where each trace or quiz adds `0.005f` (0.5%). */
private fun charProgressFraction(charStr: String, masteryMap: Map<String, Int>): Float =
    ((masteryMap[charStr] ?: 0).coerceIn(0, MAX_MASTERY_UNITS) / MAX_MASTERY_UNITS.toFloat())

// =========================================================================
// MAIN KANA & KANJI SCREEN (MATCHING SCREENSHOT LAYOUT)
// =========================================================================
@Composable
fun WritingLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", android.content.Context.MODE_PRIVATE) }
    val appLanguage = LocalAppLanguage.current

    var kanaMastery by remember {
        mutableStateOf(loadKanaMasteryMap(prefs))
    }

    fun incrementKanaMasteryByHalfPercent(charStr: String) {
        val currentUnits = kanaMastery[charStr] ?: 0
        val nextUnits = (currentUnits + 1).coerceAtMost(MAX_MASTERY_UNITS)
        val updated = kanaMastery.toMutableMap().apply { put(charStr, nextUnits) }
        kanaMastery = updated
        saveKanaMasteryMap(prefs, updated)
    }

    var topTab by remember { mutableIntStateOf(0) } // 0: HIRAGANA, 1: KATAKANA, 2: KANJI
    var activePracticeRow by remember { mutableStateOf<KanaRowFilter?>(null) }

    // If user tapped "LEARN THE CHARACTERS", open the Interactive Row/Overall Quiz & Trace Session
    activePracticeRow?.let { initialRow ->
        val scriptType = if (topTab == 1) KanaType.KATAKANA else KanaType.HIRAGANA
        KanaInteractivePracticeOverlay(
            initialScript = scriptType,
            initialRow = initialRow,
            kanaMastery = kanaMastery,
            onIncrementMastery = ::incrementKanaMasteryByHalfPercent,
            onSpeak = onSpeak,
            onAwardXp = onAwardXp,
            onClose = { activePracticeRow = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Underline Tab Bar: HIRAGANA | KATAKANA | KANJI (Matches Screenshot!)
        val tabTitles = listOf("HIRAGANA", "KATAKANA", "KANJI")
        TabRow(
            selectedTabIndex = topTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = DuoColors.MacawBlue,
            indicator = { tabPositions ->
                if (topTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[topTab]),
                        height = 3.dp,
                        color = DuoColors.MacawBlue
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { idx, title ->
                val selected = topTab == idx
                Tab(
                    selected = selected,
                    onClick = { topTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (selected) DuoColors.MacawBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (topTab) {
            0 -> CleanFiveColumnKanaGridScreen(
                scriptType = KanaType.HIRAGANA,
                kanaMastery = kanaMastery,
                onIncrementMastery = ::incrementKanaMasteryByHalfPercent,
                onStartPractice = { chosenRow -> activePracticeRow = chosenRow },
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            1 -> CleanFiveColumnKanaGridScreen(
                scriptType = KanaType.KATAKANA,
                kanaMastery = kanaMastery,
                onIncrementMastery = ::incrementKanaMasteryByHalfPercent,
                onStartPractice = { chosenRow -> activePracticeRow = chosenRow },
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            else -> KanjiCanvasLabView(
                appLanguage = appLanguage,
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
        }
    }
}

// =========================================================================
// 5-COLUMN KANA GRID WITH SILENT GOLDEN PROGRESS BARS (MATCHING SCREENSHOT)
// =========================================================================
@Composable
private fun CleanFiveColumnKanaGridScreen(
    scriptType: KanaType,
    kanaMastery: Map<String, Int>,
    onIncrementMastery: (String) -> Unit,
    onStartPractice: (KanaRowFilter) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var selectedRowFilter by remember { mutableStateOf(KanaRowFilter.ALL_OVERALL) }
    var tracingQueue by remember { mutableStateOf<List<KanaChar>>(emptyList()) }
    var tracingIndex by remember { mutableIntStateOf(0) }

    val gojuonRows = remember(scriptType) {
        buildGojuonFiveColumnRows(scriptType, DataRepository.allKana)
    }
    val dakutenRows = remember(scriptType) {
        DataRepository.allKana.filter { it.type == scriptType && it.group == KanaGroup.VOICED }.chunked(5)
    }
    val comboRows = remember(scriptType) {
        DataRepository.allKana.filter { it.type == scriptType && it.group == KanaGroup.COMBO }.chunked(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. "LEARN THE CHARACTERS" Button (Matches Screenshot!)
        item {
            Duo3DButton(
                text = if (selectedRowFilter == KanaRowFilter.ALL_OVERALL) "LEARN THE CHARACTERS"
                else "PRACTICE ${selectedRowFilter.label.uppercase()}",
                onClick = { onStartPractice(selectedRowFilter) },
                topColor = DuoColors.MacawBlue,
                lipColor = DuoColors.MacawBlueDark,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Row Filter Strip (Overall vs Specific Row: A, K, S, T, N, H, M, Y, R, W)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                items(KanaRowFilter.entries) { filter ->
                    val selected = selectedRowFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRowFilter = filter },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // 3. 5-Column Kana Cards Grid (With Empty Slots for ya/yu/yo, wa/wo, n)
        if (selectedRowFilter == KanaRowFilter.ALL_OVERALL) {
            items(gojuonRows) { rowSlots ->
                val nonNullRowChars = rowSlots.filterNotNull()
                FiveColumnKanaRow(
                    slots = rowSlots,
                    kanaMastery = kanaMastery,
                    onTapChar = { clicked ->
                        onSpeak(clicked.char, ListeningSpeed.NATURAL)
                        tracingQueue = nonNullRowChars
                        tracingIndex = nonNullRowChars.indexOf(clicked).coerceAtLeast(0)
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "DAKUTEN & HANDAKUTEN (゛゜)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(dakutenRows) { rowChars ->
                val paddedSlots: List<KanaChar?> = rowChars + List((5 - rowChars.size).coerceAtLeast(0)) { null }
                FiveColumnKanaRow(
                    slots = paddedSlots,
                    kanaMastery = kanaMastery,
                    onTapChar = { clicked ->
                        onSpeak(clicked.char, ListeningSpeed.NATURAL)
                        tracingQueue = rowChars
                        tracingIndex = rowChars.indexOf(clicked).coerceAtLeast(0)
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "YŌON COMBO SOUNDS (ゃ ゅ ょ)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(comboRows) { rowChars ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowChars.forEach { k ->
                        KanaProgressTile(
                            kana = k,
                            progressFraction = charProgressFraction(k.char, kanaMastery),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSpeak(k.char, ListeningSpeed.NATURAL)
                                tracingQueue = rowChars
                                tracingIndex = rowChars.indexOf(k).coerceAtLeast(0)
                            }
                        )
                    }
                }
            }
        } else {
            // Specific Row Selected
            val rowChars = selectedRowFilter.filterKana(scriptType)
            val chunks = rowChars.chunked(5)
            items(chunks) { chunk ->
                val padded: List<KanaChar?> = chunk + List((5 - chunk.size).coerceAtLeast(0)) { null }
                FiveColumnKanaRow(
                    slots = padded,
                    kanaMastery = kanaMastery,
                    onTapChar = { clicked ->
                        onSpeak(clicked.char, ListeningSpeed.NATURAL)
                        tracingQueue = rowChars
                        tracingIndex = rowChars.indexOf(clicked).coerceAtLeast(0)
                    }
                )
            }
        }
    }

    // Tapping any character card opens the Trace Modal for that row
    if (tracingQueue.isNotEmpty()) {
        val currentKana = tracingQueue[tracingIndex.coerceIn(0, tracingQueue.lastIndex)]
        val progress = charProgressFraction(currentKana.char, kanaMastery)
        CharacterTraceDialog(
            title = "Trace 「${currentKana.char}」 (${currentKana.romaji})",
            progressFraction = progress,
            targetChar = currentKana.char,
            hasNextInQueue = tracingIndex + 1 < tracingQueue.size,
            onSpeak = { onSpeak(currentKana.char, ListeningSpeed.NATURAL) },
            onDismiss = { tracingQueue = emptyList() },
            onPassedTrace = {
                onIncrementMastery(currentKana.char)
                onAwardXp(15)
            },
            onNextChar = {
                if (tracingIndex + 1 < tracingQueue.size) {
                    tracingIndex++
                } else {
                    tracingQueue = emptyList()
                }
            }
        )
    }
}

@Composable
private fun FiveColumnKanaRow(
    slots: List<KanaChar?>,
    kanaMastery: Map<String, Int>,
    onTapChar: (KanaChar) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val emptySlotColor = if (isDark) Color(0xFF1A2730) else Color(0xFFE8EEF3)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slots.forEach { kana ->
            if (kana == null) {
                // Blank recessed placeholder block (like in や _ ゆ _ よ and わ _ _ _ を)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(emptySlotColor)
                )
            } else {
                KanaProgressTile(
                    kana = kana,
                    progressFraction = charProgressFraction(kana.char, kanaMastery),
                    modifier = Modifier.weight(1f),
                    onClick = { onTapChar(kana) }
                )
            }
        }
    }
}

/**
 * Individual Kana Tile matching the screenshot:
 * - Character on top
 * - Romaji below
 * - Golden progress bar at the bottom (NO percentage numbers!)
 * - Golden border & golden text when progress is high/mastered!
 */
@Composable
private fun KanaProgressTile(
    kana: KanaChar,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val isHighlightedGold = progressFraction >= 0.5f
    val goldColor = Color(0xFFFFC800)
    val goldLip = Color(0xFFD9A600)

    val borderCol = when {
        isHighlightedGold -> goldColor
        isDark -> DuoColors.DarkBorder
        else -> DuoColors.LightBorder
    }
    val lipCol = when {
        isHighlightedGold -> goldLip
        isDark -> DuoColors.DarkLip
        else -> DuoColors.LightLip
    }
    val cardBg = if (isDark) DuoColors.DarkCard else Color.White
    val charColor = when {
        isHighlightedGold -> goldColor
        isDark -> Color.White
        else -> Color(0xFF0F172A)
    }
    val romajiColor = when {
        isHighlightedGold -> goldColor.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val trackColor = if (isDark) Color(0xFF263540) else Color(0xFFE2E8F0)

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(86.dp)
            .clip(shape)
            .background(lipCol)
            .clickable { onClick() }
            .padding(bottom = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(cardBg)
                .border(1.8.dp, borderCol, shape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = kana.char,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = charColor
            )
            Text(
                text = kana.romaji,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = romajiColor
            )

            // Silent Golden Progress Bar at the bottom of the card (No % numbers!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceIn(0.12f, 1f))
                            .clip(CircleShape)
                            .background(goldColor)
                    )
                }
            }
        }
    }
}

// =========================================================================
// INTERACTIVE KANA PRACTICE OVERLAY (OVERALL OR SPECIFIC ROW: QUIZ + TRACE)
// =========================================================================
@Composable
private fun KanaInteractivePracticeOverlay(
    initialScript: KanaType,
    initialRow: KanaRowFilter,
    kanaMastery: Map<String, Int>,
    onIncrementMastery: (String) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialScript) }
    var selectedRow by remember { mutableStateOf(initialRow) }
    var round by remember { mutableIntStateOf(0) }
    var selectedIdx by remember(round, selectedRow, selectedType) { mutableStateOf<Int?>(null) }
    var showTraceModal by remember { mutableStateOf(false) }

    val activePool = remember(selectedRow, selectedType, round) {
        if (selectedRow == KanaRowFilter.ALL_OVERALL) {
            val allForScript = DataRepository.allKana.filter { it.type == selectedType }
            allForScript.shuffled().sortedBy { kanaMastery[it.char] ?: 0 }.take(15).ifEmpty { DataRepository.allKana }
        } else {
            selectedRow.filterKana(selectedType).ifEmpty { DataRepository.allKana }
        }
    }

    val target = remember(activePool, round) { activePool[round % activePool.size] }
    val options = remember(activePool, target, round) {
        val sameRowWrong = activePool.map { it.romaji }.distinct().filter { it != target.romaji }.shuffled().take(2)
        val extraWrong = DataRepository.allKana.map { it.romaji }.distinct()
            .filter { it != target.romaji && it !in sameRowWrong }
            .shuffled()
            .take(3 - sameRowWrong.size)
        (sameRowWrong + extraWrong + target.romaji).shuffled()
    }
    val correctIndex = options.indexOf(target.romaji)
    val targetProgress = charProgressFraction(target.char, kanaMastery)

    LaunchedEffect(target) {
        onSpeak(target.char, ListeningSpeed.NATURAL)
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
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
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Text(
                            text = selectedRow.label,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        OutlinedButton(onClick = { showTraceModal = true }) {
                            Text("✍️ Trace", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Switch between Overall and Specific Rows anytime
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(KanaRowFilter.entries) { f ->
                            FilterChip(
                                selected = selectedRow == f,
                                onClick = { selectedRow = f; round++ },
                                label = { Text(f.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select the correct sound",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Big Character Card with Silent Golden Progress Bar
            Duo3DCard(
                borderColor = Color(0xFFFFC800),
                lipColor = Color(0xFFD9A600),
                onClick = { onSpeak(target.char, ListeningSpeed.NATURAL) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = target.char,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (targetProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(targetProgress.coerceIn(0.12f, 1f))
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC800))
                            )
                        }
                    }
                }
            }

            // 2x2 Sound Choices
            val rows = options.indices.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { idx ->
                        val romaji = options[idx]
                        val isSelected = selectedIdx == idx
                        val isCorrect = idx == correctIndex

                        val bg = when {
                            selectedIdx == null -> MaterialTheme.colorScheme.surface
                            isCorrect -> DuoColors.FeatherGreenBg
                            isSelected -> DuoColors.CardinalRedBg
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val border = when {
                            selectedIdx == null -> if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder
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
                                        onIncrementMastery(target.char)
                                        onAwardXp(10)
                                    }
                                    onSpeak(target.char, ListeningSpeed.NATURAL)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = romaji,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedIdx != null && (isCorrect || isSelected)) OnPastel else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (selectedIdx != null) {
                val won = selectedIdx == correctIndex
                DuoBottomResultBanner(
                    isCorrect = won,
                    correctAnswerText = "${target.char} = ${target.romaji}",
                    onContinue = { round++ }
                )
            }
        }
    }

    if (showTraceModal) {
        CharacterTraceDialog(
            title = "Trace 「${target.char}」 (${target.romaji})",
            progressFraction = targetProgress,
            targetChar = target.char,
            hasNextInQueue = false,
            onSpeak = { onSpeak(target.char, ListeningSpeed.NATURAL) },
            onDismiss = { showTraceModal = false },
            onPassedTrace = {
                onIncrementMastery(target.char)
                onAwardXp(15)
            }
        )
    }
}

// =========================================================================
// TAB 2: 103 OFFICIAL N5 KANJI DECK + TRACE CANVAS
// =========================================================================
@Composable
private fun KanjiCanvasLabView(
    appLanguage: AppLanguage,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val kanjiPool = DataRepository.allKanji
    var selectedKanji by remember { mutableStateOf(kanjiPool.firstOrNull()) }
    var showCanvas by remember { mutableStateOf(false) }

    val currentKanji = selectedKanji ?: kanjiPool.firstOrNull() ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Duo3DCard(
                borderColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark
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
                            color = DuoColors.FeatherGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "N5 KANJI • ${currentKanji.strokeCount} Strokes",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { onSpeak(currentKanji.kanji, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = DuoColors.MacawBlueDark)
                        }
                    }

                    Text(
                        text = currentKanji.kanji,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = currentKanji.meaning(appLanguage),
                        fontSize = 18.sp,
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
                            text = "💡 ${currentKanji.mnemonic}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPastel
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Duo3DButton(
                        text = "✍️ Trace 「${currentKanji.kanji}」",
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
                borderColor = if (isSelected) DuoColors.MacawBlueDark else DuoColors.LightBorder,
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
                                text = item.meaning(appLanguage),
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
        val idxInPool = kanjiPool.indexOf(currentKanji).coerceAtLeast(0)
        CharacterTraceDialog(
            title = "Trace Kanji 「${currentKanji.kanji}」 (${currentKanji.meaning(appLanguage)})",
            progressFraction = 0.5f,
            targetChar = currentKanji.kanji,
            hasNextInQueue = idxInPool + 1 < kanjiPool.size,
            onSpeak = { onSpeak(currentKanji.kanji, ListeningSpeed.NATURAL) },
            onDismiss = { showCanvas = false },
            onPassedTrace = { onAwardXp(20) },
            onNextChar = {
                if (idxInPool + 1 < kanjiPool.size) {
                    selectedKanji = kanjiPool[idxInPool + 1]
                } else {
                    showCanvas = false
                }
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

// =========================================================================
// CHARACTER TRACE DIALOG WITH SILENT GOLDEN PROGRESS BAR (NO % TEXT)
// =========================================================================
@Composable
fun CharacterTraceDialog(
    title: String,
    progressFraction: Float,
    targetChar: String,
    hasNextInQueue: Boolean = false,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
    onPassedTrace: () -> Unit,
    onNextChar: () -> Unit = onDismiss
) {
    val strokes = remember(targetChar) { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember(targetChar) { mutableStateOf<List<Offset>>(emptyList()) }
    var showGuide by remember { mutableStateOf(true) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var evaluation by remember(targetChar) { mutableStateOf<TraceEvaluation?>(null) }
    var masteryAwardedForThisChar by remember(targetChar) { mutableStateOf(false) }

    LaunchedEffect(targetChar) {
        onSpeak()
    }

    fun evaluateNow() {
        val res = TraceAccuracyEvaluator.evaluate(
            targetChar = targetChar,
            strokes = strokes.toList(),
            canvasWidthPx = canvasSize.width.toFloat(),
            canvasHeightPx = canvasSize.height.toFloat()
        )
        evaluation = res
        if (res.isPassed && !masteryAwardedForThisChar) {
            masteryAwardedForThisChar = true
            onPassedTrace()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = DuoColors.MacawBlueDark)
                    }
                }
                // Silent Golden Mastery Bar (No % text!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val displayedFraction = (progressFraction + if (masteryAwardedForThisChar) 0.05f else 0f).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(displayedFraction)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC800))
                    )
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(
                            width = 2.5.dp,
                            color = when (evaluation?.isPassed) {
                                true -> DuoColors.FeatherGreen
                                false -> DuoColors.FoxOrange
                                null -> DuoColors.MacawBlue
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .onSizeChanged { canvasSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuide) {
                        Text(
                            text = targetChar,
                            fontSize = if (targetChar.length > 1) 110.sp else 172.sp,
                            color = Color(0xFFCBD5E1).copy(alpha = 0.55f),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(targetChar) {
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
                                            evaluateNow()
                                        }
                                    },
                                    onDragCancel = { currentStroke = emptyList() }
                                )
                            }
                    ) {
                        drawLine(Color(0xFFE2E8F0), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawLine(Color(0xFFE2E8F0), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2f)

                        val all = strokes.toList() + listOf(currentStroke)
                        all.forEachIndexed { idx, pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                val strokeColor = when (idx % 4) {
                                    0 -> DuoColors.MacawBlueDark
                                    1 -> DuoColors.FeatherGreenDark
                                    2 -> DuoColors.LilacPurpleDark
                                    else -> DuoColors.FoxOrangeDark
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val eval = evaluation
                Surface(
                    color = when (eval?.isPassed) {
                        true -> DuoColors.FeatherGreenBg
                        false -> DuoColors.FoxOrangeBg
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = eval?.feedbackText ?: "✍️ Trace over 「$targetChar」 with your finger",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (eval != null) OnPastel else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                        evaluation = null
                    }) {
                        Text("🔄 Clear")
                    }
                    OutlinedButton(onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes.removeAt(strokes.lastIndex)
                            if (strokes.isEmpty()) evaluation = null else evaluateNow()
                        }
                    }) {
                        Text("↩️ Undo")
                    }
                    OutlinedButton(onClick = { showGuide = !showGuide }) {
                        Text(if (showGuide) "🙈 Hide Guide" else "👁️ Guide")
                    }
                }
            }
        },
        confirmButton = {
            val passed = evaluation?.isPassed == true
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasNextInQueue) {
                    Duo3DButton(
                        text = if (passed) "NEXT IN ROW →" else "TRACE TO CONTINUE",
                        enabled = passed,
                        onClick = onNextChar,
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Duo3DButton(
                        text = if (passed) "DONE" else "TRACE FIRST",
                        enabled = passed,
                        onClick = onDismiss,
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}
