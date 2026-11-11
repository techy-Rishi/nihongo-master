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
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// SPECIFIC ROWS FOR ROW-WISE OR OVERALL PRACTICE
// =========================================================================
private enum class KanaRowFilter(
    val label: String,
    val romajiSet: Set<String>?
) {
    ALL_OVERALL("All 46 Basic (Overall)", null),
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
// STRICT 3-STAGE CHARACTER TRACE ACCURACY VERIFIER
// Rejects 'X', random lines, incomplete strokes, and out-of-bounds scribbles!
// =========================================================================
private data class TraceEvaluation(
    val accuracyPercent: Int,
    val isPassed: Boolean,
    val feedbackText: String
)

private object TraceAccuracyEvaluator {
    private const val GRID_SIZE = 128

    fun evaluate(
        targetChar: String,
        strokes: List<List<Offset>>,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): TraceEvaluation {
        if (strokes.isEmpty() || canvasWidthPx <= 10f || canvasHeightPx <= 10f) {
            return TraceEvaluation(0, false, "✍️ Trace over 「$targetChar」 following the guide.")
        }

        val scaleX = GRID_SIZE / canvasWidthPx
        val scaleY = GRID_SIZE / canvasHeightPx

        // 1. Skeleton Mask: The strict centerline of the character
        val skeletonBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val skeletonCanvas = android.graphics.Canvas(skeletonBitmap)
        val skeletonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = if (targetChar.length > 1) GRID_SIZE * 0.46f else GRID_SIZE * 0.68f
            isFakeBoldText = true
            style = Paint.Style.FILL
        }
        val fm = skeletonPaint.fontMetrics
        val centerY = (GRID_SIZE / 2f) - (fm.ascent + fm.descent) / 2f
        skeletonCanvas.drawText(targetChar, GRID_SIZE / 2f, centerY, skeletonPaint)

        // 2. Allowed Corridor Mask: Slightly wider corridor around the character strokes.
        // Any user ink OUTSIDE this corridor is penalized as Out-Of-Bounds!
        val corridorBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val corridorCanvas = android.graphics.Canvas(corridorBitmap)
        val corridorPaint = Paint(skeletonPaint).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 7f
        }
        corridorCanvas.drawText(targetChar, GRID_SIZE / 2f, centerY, corridorPaint)

        // 3. User Thin Ink Mask (for strict precision check) & User Reach Mask (for skeleton coverage)
        val userThinBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val userThinCanvas = android.graphics.Canvas(userThinBitmap)
        val userThinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val userReachBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val userReachCanvas = android.graphics.Canvas(userReachBitmap)
        val userReachPaint = Paint(userThinPaint).apply {
            strokeWidth = 11f
        }

        for (pts in strokes) {
            if (pts.size == 1) {
                val px = pts[0].x * scaleX
                val py = pts[0].y * scaleY
                userThinCanvas.drawPoint(px, py, userThinPaint)
                userReachCanvas.drawPoint(px, py, userReachPaint)
            } else if (pts.size > 1) {
                val p = android.graphics.Path()
                p.moveTo(pts.first().x * scaleX, pts.first().y * scaleY)
                for (i in 1 until pts.size) {
                    p.lineTo(pts[i].x * scaleX, pts[i].y * scaleY)
                }
                userThinCanvas.drawPath(p, userThinPaint)
                userReachCanvas.drawPath(p, userReachPaint)
            }
        }

        var skeletonPixels = 0
        var coveredSkeletonPixels = 0
        var userThinPixels = 0
        var insideCorridorUserPixels = 0

        // 4-Quadrant coverage tracking so user cannot pass by only drawing half the letter or an 'X'
        val quadRef = IntArray(4)
        val quadCovered = IntArray(4)
        val half = GRID_SIZE / 2

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val qIdx = (if (y >= half) 2 else 0) + (if (x >= half) 1 else 0)
                val isSkel = android.graphics.Color.alpha(skeletonBitmap.getPixel(x, y)) > 50
                val isCorr = android.graphics.Color.alpha(corridorBitmap.getPixel(x, y)) > 40
                val isUserThin = android.graphics.Color.alpha(userThinBitmap.getPixel(x, y)) > 50
                val isUserReach = android.graphics.Color.alpha(userReachBitmap.getPixel(x, y)) > 40

                if (isSkel) {
                    skeletonPixels++
                    quadRef[qIdx]++
                    if (isUserReach) {
                        coveredSkeletonPixels++
                        quadCovered[qIdx]++
                    }
                }
                if (isUserThin) {
                    userThinPixels++
                    if (isCorr) {
                        insideCorridorUserPixels++
                    }
                }
            }
        }

        if (skeletonPixels == 0 || userThinPixels < 20) {
            return TraceEvaluation(12, false, "⚠️ Too short! Trace all strokes of 「$targetChar」.")
        }

        val coverage = coveredSkeletonPixels.toFloat() / skeletonPixels.toFloat()
        val precision = insideCorridorUserPixels.toFloat() / userThinPixels.toFloat()
        val inkRatio = userThinPixels.toFloat() / skeletonPixels.toFloat()

        // Check every quadrant that has significant character ink (>= 12% of the character)
        var worstQuadCoverage = 1f
        for (q in 0..3) {
            if (quadRef[q] >= skeletonPixels * 0.12f) {
                val qCov = quadCovered[q].toFloat() / quadRef[q].toFloat()
                if (qCov < worstQuadCoverage) worstQuadCoverage = qCov
            }
        }

        // Anti-scribble penalty: if user drew way more ink than the character actually has
        val overdrawPenalty = if (inkRatio > 1.75f) ((inkRatio - 1.75f) * 0.45f).coerceAtMost(0.5f) else 0f

        val rawAccuracy = ((coverage * 0.48f + precision * 0.52f - overdrawPenalty) * 100f).roundToInt().coerceIn(5, 99)

        // Strict Pass Conditions:
        // 1. At least 62% of the user's ink must stay INSIDE the character guide corridor (blocks 'X' and random lines!)
        // 2. At least 58% of the character skeleton must be covered
        // 3. Every major quadrant of the character must be at least 42% traced
        // 4. Overall accuracy must be >= 65%
        val passed = precision >= 0.62f &&
            coverage >= 0.58f &&
            worstQuadCoverage >= 0.42f &&
            inkRatio <= 1.85f &&
            rawAccuracy >= 65

        val finalDisplayedAccuracy = if (!passed) min(rawAccuracy, 58) else rawAccuracy

        val feedback = when {
            inkRatio > 1.85f ->
                "❌ $finalDisplayedAccuracy% Accuracy — Too much extra scribbling! Clear and trace cleanly."
            precision < 0.62f ->
                "❌ $finalDisplayedAccuracy% Accuracy — Drawn outside guide lines! Follow 「$targetChar」 carefully."
            coverage < 0.58f || worstQuadCoverage < 0.42f ->
                "⚠️ $finalDisplayedAccuracy% Accuracy — Incomplete! Trace all parts of 「$targetChar」."
            finalDisplayedAccuracy >= 85 ->
                "✨ $finalDisplayedAccuracy% Accuracy — Excellent Trace! (+0.5% Mastery)"
            else ->
                "✅ $finalDisplayedAccuracy% Accuracy — Passed! (+0.5% Mastery)"
        }

        return TraceEvaluation(finalDisplayedAccuracy, passed, feedback)
    }
}

// =========================================================================
// 0.5% STEP MASTERY STORAGE (Preserves old + new progress)
// =========================================================================
private const val MAX_MASTERY_UNITS = 200 // 200 * 0.5% = 100%

private fun loadKanaMasteryMap(prefs: android.content.SharedPreferences): Map<String, Int> {
    val out = mutableMapOf<String, Int>()
    // Migrate any earlier v1 progress if present
    val legacy = prefs.getStringSet("kana_mastery_v1", emptySet()).orEmpty()
    for (entry in legacy) {
        val parts = entry.split(":")
        if (parts.size == 2) {
            parts[1].toIntOrNull()?.let { out[parts[0]] = (it * 20).coerceIn(0, MAX_MASTERY_UNITS) }
        }
    }
    val raw = prefs.getStringSet("kana_mastery_half_pct_v1", emptySet()).orEmpty()
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

private fun charProgressFraction(charStr: String, masteryMap: Map<String, Int>): Float =
    ((masteryMap[charStr] ?: 0).coerceIn(0, MAX_MASTERY_UNITS) / MAX_MASTERY_UNITS.toFloat())

// =========================================================================
// MAIN KANA & KANJI SCREEN
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
// 5-COLUMN KANA GRID WITH PROMINENT GOLDEN PROGRESS BAR ON EVERY LETTER CARD
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
                            masteryUnits = kanaMastery[k.char] ?: 0,
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(94.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(emptySlotColor)
                )
            } else {
                KanaProgressTile(
                    kana = kana,
                    masteryUnits = kanaMastery[kana.char] ?: 0,
                    progressFraction = charProgressFraction(kana.char, kanaMastery),
                    modifier = Modifier.weight(1f),
                    onClick = { onTapChar(kana) }
                )
            }
        }
    }
}

/**
 * Individual Kana Tile with ALWAYS-VISIBLE Golden Progress Bar at the bottom (like Screenshot):
 * - Every single letter card clearly shows the bottom progress bar track and golden indicator.
 * - Any practiced character (`masteryUnits > 0`) lights up with a Golden Border & Golden Text!
 */
@Composable
private fun KanaProgressTile(
    kana: KanaChar,
    masteryUnits: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val hasStartedMastery = masteryUnits > 0
    val goldColor = Color(0xFFFFC800)
    val goldLip = Color(0xFFD9A600)

    val borderCol = when {
        hasStartedMastery -> goldColor
        isDark -> Color(0xFF334654)
        else -> DuoColors.LightBorder
    }
    val lipCol = when {
        hasStartedMastery -> goldLip
        isDark -> Color(0xFF202F3A)
        else -> DuoColors.LightLip
    }
    val cardBg = if (isDark) Color(0xFF16222A) else Color.White
    val charColor = when {
        hasStartedMastery -> goldColor
        isDark -> Color.White
        else -> Color(0xFF0F172A)
    }
    val romajiColor = when {
        hasStartedMastery -> goldColor.copy(alpha = 0.88f)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }
    val trackColor = if (isDark) Color(0xFF2B3C49) else Color(0xFFDCE4EC)

    // Visible gold bar width: starter nub (18%) when unpracticed, growing smoothly up to 100% as practiced
    val barWidthFraction = if (hasStartedMastery) {
        (0.24f + progressFraction * 0.76f).coerceIn(0.24f, 1f)
    } else {
        0.16f
    }
    val barFillColor = if (hasStartedMastery) goldColor else goldColor.copy(alpha = 0.42f)

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(94.dp)
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
                .padding(horizontal = 7.dp, vertical = 8.dp),
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

            // Prominent Golden Progress Bar on Every Single Letter Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(barWidthFraction)
                        .clip(CircleShape)
                        .background(barFillColor)
                )
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
        selectedRow.filterKana(selectedType).ifEmpty { DataRepository.allKana }
    }

    val target = remember(activePool, round) { activePool[round % activePool.size] }
    val options = remember(activePool, target, round) {
        val sameRowWrong = activePool.map { it.romaji }.distinct().filter { it != target.romaji }.shuffled().take(2)
        val basicPool = DataRepository.allKana.filter { it.group == KanaGroup.BASIC }
        val extraWrong = basicPool.map { it.romaji }.distinct()
            .filter { it != target.romaji && it !in sameRowWrong }
            .shuffled()
            .take(3 - sameRowWrong.size)
        (sameRowWrong + extraWrong + target.romaji).shuffled()
    }
    val correctIndex = options.indexOf(target.romaji)
    val targetUnits = kanaMastery[target.char] ?: 0
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

            // Big Character Quiz Card with Clearly Visible Golden Progress Bar
            val barFillFraction = if (targetUnits > 0) (0.24f + targetProgress * 0.76f).coerceIn(0.24f, 1f) else 0.16f
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
                        fontWeight = FontWeight.ExtraBold,
                        color = if (targetUnits > 0) Color(0xFFFFC800) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(if (LocalDarkTheme.current) Color(0xFF2B3C49) else Color(0xFFDCE4EC))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(barFillFraction)
                                .clip(CircleShape)
                                .background(
                                    if (targetUnits > 0) Color(0xFFFFC800)
                                    else Color(0xFFFFC800).copy(alpha = 0.45f)
                                )
                        )
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
// STRICT CHARACTER TRACE DIALOG WITH ACCURACY % CHECK & GOLDEN PROGRESS BAR
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
                // High-contrast Golden Mastery Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (LocalDarkTheme.current) Color(0xFF2B3C49) else Color(0xFFDCE4EC))
                ) {
                    val displayedFraction = (0.18f + progressFraction * 0.77f + if (masteryAwardedForThisChar) 0.05f else 0f).coerceIn(0.18f, 1f)
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
                                false -> DuoColors.CardinalRed
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
                            color = Color(0xFFCBD5E1).copy(alpha = 0.60f),
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
                        false -> DuoColors.CardinalRedBg
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = eval?.feedbackText ?: "✍️ Trace accurately inside the gray guide (65%+ required)",
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
                        text = if (passed) "NEXT IN ROW →" else "TRACE ACCURATELY (65%+)",
                        enabled = passed,
                        onClick = onNextChar,
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Duo3DButton(
                        text = if (passed) "DONE" else "TRACE ACCURATELY (65%+)",
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
