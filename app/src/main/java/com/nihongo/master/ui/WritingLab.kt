package com.nihongo.master.ui

import android.graphics.Bitmap
import android.graphics.Paint
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*
import kotlin.math.hypot
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
// STRICT STROKE-COUNT + PER-STROKE TRAJECTORY + 16x16 CELL EVALUATOR
// 1. Enforces official stroke count for every character (e.g. 「あ」 needs 3 strokes).
// 2. Checks EVERY individual stroke: if any stroke strays outside the gray guide
//    (like drawing an 'X'), that stroke turns RED and fails immediately!
// 3. Divides the gray character into a 16x16 grid and requires all segments
//    (including loops like the bottom of 「あ」) to be traced.
// =========================================================================
private data class TraceEvaluation(
    val accuracyPercent: Int,
    val isPassed: Boolean,
    val invalidStrokeIndices: Set<Int>,
    val feedbackText: String
)

private object TraceAccuracyEvaluator {
    private const val BITMAP_SIZE = 160
    private const val CELL_GRID = 16 // 16x16 = 256 fine cells (10x10 px each)

    // Official (minStrokes..maxStrokes) for Hiragana & Katakana
    private val kanaStrokeRanges: Map<String, IntRange> = mapOf(
        // 1-stroke Hiragana
        "く" to 1..1, "し" to 1..1, "つ" to 1..1, "て" to 1..1, "の" to 1..1,
        "ひ" to 1..1, "へ" to 1..1, "ろ" to 1..1, "る" to 1..1, "ん" to 1..1,
        // 2-stroke Hiragana
        "い" to 2..2, "う" to 2..2, "え" to 2..2, "こ" to 2..2, "す" to 2..2,
        "そ" to 1..2, "ち" to 2..2, "と" to 2..2, "ぬ" to 2..2, "ね" to 2..2,
        "め" to 2..2, "ゆ" to 2..2, "よ" to 2..2, "ら" to 2..2, "り" to 1..2,
        "れ" to 2..2, "わ" to 2..2,
        // 3-stroke Hiragana
        "あ" to 3..3, "お" to 3..3, "か" to 3..3, "け" to 3..3, "さ" to 2..3,
        "せ" to 3..3, "に" to 3..3, "は" to 3..3, "ま" to 3..3, "み" to 2..3,
        "む" to 3..3, "も" to 3..3, "や" to 3..3, "を" to 3..3,
        // 4-stroke Hiragana
        "き" to 3..4, "た" to 3..4, "な" to 4..4, "ふ" to 3..4, "ほ" to 4..4,
        // 1-stroke Katakana
        "ノ" to 1..1, "フ" to 1..1, "ヘ" to 1..1, "レ" to 1..1,
        // 2-stroke Katakana
        "ア" to 2..2, "イ" to 2..2, "カ" to 2..2, "ク" to 2..2, "コ" to 2..2,
        "ス" to 2..2, "セ" to 2..2, "ソ" to 2..2, "ト" to 2..2, "ナ" to 2..2,
        "ニ" to 2..2, "ヌ" to 2..2, "ハ" to 2..2, "ヒ" to 2..2, "マ" to 2..2,
        "ム" to 2..2, "メ" to 2..2, "ヤ" to 2..2, "ユ" to 2..2, "ラ" to 2..2,
        "リ" to 2..2, "ル" to 2..2, "ワ" to 2..2, "ン" to 2..2,
        // 3-stroke Katakana
        "ウ" to 3..3, "エ" to 3..3, "オ" to 3..3, "キ" to 3..3, "ケ" to 3..3,
        "サ" to 3..3, "シ" to 3..3, "タ" to 3..3, "チ" to 3..3, "ツ" to 3..3,
        "テ" to 3..3, "モ" to 3..3, "ヨ" to 3..3, "ロ" to 3..3, "ヲ" to 3..3,
        // 4-stroke Katakana
        "ネ" to 4..4, "ホ" to 4..4
    )

    fun expectedStrokeRange(targetChar: String, explicitKanjiStrokes: Int? = null): IntRange {
        if (explicitKanjiStrokes != null && explicitKanjiStrokes > 0) {
            return (explicitKanjiStrokes - 1).coerceAtLeast(1)..(explicitKanjiStrokes + 1)
        }
        kanaStrokeRanges[targetChar]?.let { return it }
        // Dakuten / Handakuten / Combo fallback
        return if (targetChar.length > 1) 3..7 else 2..5
    }

    fun guideTextSizePx(charStr: String, canvasWidthPx: Float): Float =
        if (charStr.length > 1) canvasWidthPx * 0.45f else canvasWidthPx * 0.66f

    fun drawCenteredChar(
        nativeCanvas: android.graphics.Canvas,
        charStr: String,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        paint: Paint
    ) {
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = guideTextSizePx(charStr, canvasWidthPx)
        paint.isFakeBoldText = true
        val fm = paint.fontMetrics
        val centerY = (canvasHeightPx / 2f) - (fm.ascent + fm.descent) / 2f
        nativeCanvas.drawText(charStr, canvasWidthPx / 2f, centerY, paint)
    }

    private fun strokeLengthPx(pts: List<Offset>): Float {
        var len = 0f
        for (i in 1 until pts.size) {
            len += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
        return len
    }

    fun evaluate(
        targetChar: String,
        strokes: List<List<Offset>>,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        explicitKanjiStrokes: Int? = null
    ): TraceEvaluation {
        if (strokes.isEmpty() || canvasWidthPx <= 10f || canvasHeightPx <= 10f) {
            return TraceEvaluation(0, false, emptySet(), "✍️ Trace over 「$targetChar」 following the gray guide.")
        }

        val scaleX = BITMAP_SIZE / canvasWidthPx
        val scaleY = BITMAP_SIZE / canvasHeightPx
        val strokeRange = expectedStrokeRange(targetChar, explicitKanjiStrokes)

        // 1. Exact Guide Bitmap (No inflated fill—matches the gray guide on screen)
        val guideBitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val guideCanvas = android.graphics.Canvas(guideBitmap)
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        drawCenteredChar(guideCanvas, targetChar, BITMAP_SIZE.toFloat(), BITMAP_SIZE.toFloat(), guidePaint)

        // 2. Tight Corridor Bitmap (Only 5px tolerance around the gray guide strokes!)
        val corridorBitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val corridorCanvas = android.graphics.Canvas(corridorBitmap)
        val corridorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        drawCenteredChar(corridorCanvas, targetChar, BITMAP_SIZE.toFloat(), BITMAP_SIZE.toFloat(), corridorPaint)

        // 3. Validate EVERY individual stroke's path & length
        val invalidStrokes = mutableSetOf<Int>()
        var totalSampledPoints = 0
        var totalInCorridorPoints = 0

        val minMeaningfulStrokePx = canvasWidthPx * 0.06f // Ignore tiny accidental taps

        strokes.forEachIndexed { strokeIdx, pts ->
            val lenPx = strokeLengthPx(pts)
            if (lenPx < minMeaningfulStrokePx) {
                invalidStrokes.add(strokeIdx)
                return@forEachIndexed
            }

            var strokePoints = 0
            var strokeValidPoints = 0

            // Sample densely along every segment of this stroke
            for (i in 0 until pts.size) {
                val p1 = pts[i]
                val p2 = if (i + 1 < pts.size) pts[i + 1] else p1
                val dist = hypot(p2.x - p1.x, p2.y - p1.y)
                val steps = (dist / 4f).toInt().coerceAtLeast(1)

                for (s in 0..steps) {
                    val t = s.toFloat() / steps.toFloat()
                    val bx = ((p1.x + (p2.x - p1.x) * t) * scaleX).toInt().coerceIn(0, BITMAP_SIZE - 1)
                    val by = ((p1.y + (p2.y - p1.y) * t) * scaleY).toInt().coerceIn(0, BITMAP_SIZE - 1)

                    strokePoints++
                    totalSampledPoints++

                    val inCorridor = android.graphics.Color.alpha(corridorBitmap.getPixel(bx, by)) > 50
                    if (inCorridor) {
                        strokeValidPoints++
                        totalInCorridorPoints++
                    }
                }
            }

            // If less than 72% of THIS single stroke is inside the gray guide corridor,
            // it means the user drew a line going outside the character (like the 'X' in Screenshot 1)!
            val strokePrecision = if (strokePoints > 0) strokeValidPoints.toFloat() / strokePoints.toFloat() else 0f
            if (strokePrecision < 0.72f) {
                invalidStrokes.add(strokeIdx)
            }
        }

        // 4. Render only VALID user strokes onto `userCoverageBitmap` (with realistic 11f width on 160x160)
        val userCoverageBitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val userCoverageCanvas = android.graphics.Canvas(userCoverageBitmap)
        val coveragePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 11f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEachIndexed { idx, pts ->
            if (idx !in invalidStrokes && pts.size > 1) {
                val p = android.graphics.Path()
                p.moveTo(pts.first().x * scaleX, pts.first().y * scaleY)
                for (i in 1 until pts.size) {
                    p.lineTo(pts[i].x * scaleX, pts[i].y * scaleY)
                }
                userCoverageCanvas.drawPath(p, coveragePaint)
            }
        }

        // 5. Fine 16x16 Cell Coverage Check:
        // Every 10x10px cell of the gray guide that has character pixels MUST be visited!
        val cellSize = BITMAP_SIZE / CELL_GRID
        var requiredCells = 0
        var coveredCells = 0
        var totalGuidePixels = 0
        var coveredGuidePixels = 0

        for (cy in 0 until CELL_GRID) {
            for (cx in 0 until CELL_GRID) {
                var cellGuidePx = 0
                var cellCoveredPx = 0
                val startY = cy * cellSize
                val startX = cx * cellSize

                for (y in startY until (startY + cellSize)) {
                    for (x in startX until (startX + cellSize)) {
                        val isGuide = android.graphics.Color.alpha(guideBitmap.getPixel(x, y)) > 60
                        if (isGuide) {
                            cellGuidePx++
                            totalGuidePixels++
                            val isCovered = android.graphics.Color.alpha(userCoverageBitmap.getPixel(x, y)) > 50
                            if (isCovered) {
                                cellCoveredPx++
                                coveredGuidePixels++
                            }
                        }
                    }
                }

                // If this 10x10 cell contains a real part of the gray guide character
                if (cellGuidePx >= 8) {
                    requiredCells++
                    if (cellCoveredPx >= cellGuidePx * 0.40f) {
                        coveredCells++
                    }
                }
            }
        }

        val cellCoverage = if (requiredCells > 0) coveredCells.toFloat() / requiredCells.toFloat() else 0f
        val pixelCoverage = if (totalGuidePixels > 0) coveredGuidePixels.toFloat() / totalGuidePixels.toFloat() else 0f
        val overallPrecision = if (totalSampledPoints > 0) totalInCorridorPoints.toFloat() / totalSampledPoints.toFloat() else 0f

        val rawAccuracy = ((cellCoverage * 0.50f + overallPrecision * 0.50f) * 100f).roundToInt().coerceIn(5, 99)

        // Check 1: Did any stroke go outside the gray guide?
        if (invalidStrokes.isNotEmpty()) {
            val badNum = invalidStrokes.first() + 1
            val capped = min(rawAccuracy, 48)
            return TraceEvaluation(
                accuracyPercent = capped,
                isPassed = false,
                invalidStrokeIndices = invalidStrokes,
                feedbackText = "❌ $capped% Accuracy — Stroke #$badNum (Red) went outside the gray guide! Tap Undo or Clear."
            )
        }

        // Check 2: Did the user draw fewer strokes than the character has (like 2/3 strokes on 「あ」)?
        if (strokes.size < strokeRange.first) {
            val capped = min(rawAccuracy, 58)
            return TraceEvaluation(
                accuracyPercent = capped,
                isPassed = false,
                invalidStrokeIndices = emptySet(),
                feedbackText = "⏳ Strokes: ${strokes.size} / ${strokeRange.first} — Keep tracing the remaining part of 「$targetChar」!"
            )
        }

        // Check 3: Did the user draw too many strokes?
        if (strokes.size > strokeRange.last + 1) {
            val capped = min(rawAccuracy, 52)
            return TraceEvaluation(
                accuracyPercent = capped,
                isPassed = false,
                invalidStrokeIndices = emptySet(),
                feedbackText = "⚠️ Too many strokes (${strokes.size})! 「$targetChar」 has ${strokeRange.first} strokes. Tap Clear."
            )
        }

        // Check 4: Are all parts/loops of the gray guide covered (Requires >= 75% of all guide cells & >= 70% pixels)?
        if (cellCoverage < 0.75f || pixelCoverage < 0.70f) {
            val capped = min(rawAccuracy, 62)
            return TraceEvaluation(
                accuracyPercent = capped,
                isPassed = false,
                invalidStrokeIndices = emptySet(),
                feedbackText = "⚠️ $capped% Accuracy — Incomplete! Trace the uncovered gray part of 「$targetChar」."
            )
        }

        val finalPassScore = rawAccuracy.coerceIn(72, 99)
        return TraceEvaluation(
            accuracyPercent = finalPassScore,
            isPassed = true,
            invalidStrokeIndices = emptySet(),
            feedbackText = "✅ $finalPassScore% Accuracy — Passed! Progress bar increased."
        )
    }
}

// =========================================================================
// 0.5% STEP MASTERY STORAGE (1 unit = 0.5%, 200 units = 100% Full Gold Card)
// =========================================================================
private const val MAX_MASTERY_UNITS = 200

private fun loadKanaMasteryMap(prefs: android.content.SharedPreferences): Map<String, Int> {
    val raw = prefs.getStringSet("kana_mastery_half_pct_v2", emptySet()).orEmpty()
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
    prefs.edit().putStringSet("kana_mastery_half_pct_v2", set).apply()
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
// 5-COLUMN KANA GRID WITH PROGRESS BAR ON EVERY SINGLE LETTER CARD
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
        val units = kanaMastery[currentKana.char] ?: 0
        val progress = charProgressFraction(currentKana.char, kanaMastery)
        CharacterTraceDialog(
            title = "Trace 「${currentKana.char}」 (${currentKana.romaji})",
            masteryUnits = units,
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
                        .height(92.dp)
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

@Composable
private fun KanaProgressTile(
    kana: KanaChar,
    masteryUnits: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val isFullyMastered = masteryUnits >= MAX_MASTERY_UNITS
    val goldColor = Color(0xFFFFC800)
    val goldLip = Color(0xFFD9A600)

    val borderCol = when {
        isFullyMastered -> goldColor
        isDark -> Color(0xFF334654)
        else -> DuoColors.LightBorder
    }
    val lipCol = when {
        isFullyMastered -> goldLip
        isDark -> Color(0xFF202F3A)
        else -> DuoColors.LightLip
    }
    val cardBg = if (isDark) Color(0xFF16222A) else Color.White
    val charColor = when {
        isFullyMastered -> goldColor
        isDark -> Color.White
        else -> Color(0xFF0F172A)
    }
    val romajiColor = when {
        isFullyMastered -> goldColor.copy(alpha = 0.88f)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }
    val trackColor = if (isDark) Color(0xFF2C3E4C) else Color(0xFFDCE4EC)

    val fillFraction = when {
        masteryUnits <= 0 -> 0f
        isFullyMastered -> 1f
        else -> (0.14f + progressFraction * 0.86f).coerceIn(0.14f, 0.98f)
    }

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(92.dp)
            .clip(shape)
            .background(lipCol)
            .clickable { onClick() }
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(cardBg)
                .border(1.8.dp, borderCol, shape)
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = kana.char,
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = charColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = kana.romaji,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = romajiColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(trackColor)
            ) {
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
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

            val barFillFraction = when {
                targetUnits <= 0 -> 0f
                targetUnits >= MAX_MASTERY_UNITS -> 1f
                else -> (0.14f + targetProgress * 0.86f).coerceIn(0.14f, 0.98f)
            }
            val isFullyMastered = targetUnits >= MAX_MASTERY_UNITS

            Duo3DCard(
                borderColor = if (isFullyMastered) Color(0xFFFFC800) else if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder,
                lipColor = if (isFullyMastered) Color(0xFFD9A600) else if (LocalDarkTheme.current) DuoColors.DarkLip else DuoColors.LightLip,
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
                        color = if (isFullyMastered) Color(0xFFFFC800) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (LocalDarkTheme.current) Color(0xFF2C3E4C) else Color(0xFFDCE4EC))
                    ) {
                        if (barFillFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barFillFraction)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC800))
                            )
                        }
                    }
                }
            }

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
            masteryUnits = targetUnits,
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
            masteryUnits = 20,
            progressFraction = 0.25f,
            targetChar = currentKanji.kanji,
            explicitKanjiStrokes = currentKanji.strokeCount,
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
// STRICT CHARACTER TRACE DIALOG (WITH STROKE COUNTER & RED OUT-OF-BOUNDS STROKES)
// =========================================================================
@Composable
fun CharacterTraceDialog(
    title: String,
    masteryUnits: Int = 0,
    progressFraction: Float,
    targetChar: String,
    explicitKanjiStrokes: Int? = null,
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

    val expectedRange = remember(targetChar, explicitKanjiStrokes) {
        TraceAccuracyEvaluator.expectedStrokeRange(targetChar, explicitKanjiStrokes)
    }

    val guidePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(145, 203, 213, 225)
            style = Paint.Style.FILL
        }
    }

    LaunchedEffect(targetChar) {
        onSpeak()
    }

    fun evaluateNow() {
        val res = TraceAccuracyEvaluator.evaluate(
            targetChar = targetChar,
            strokes = strokes.toList(),
            canvasWidthPx = canvasSize.width.toFloat(),
            canvasHeightPx = canvasSize.height.toFloat(),
            explicitKanjiStrokes = explicitKanjiStrokes
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = "Strokes drawn: ${strokes.size} / ${expectedRange.first}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = DuoColors.MacawBlueDark)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (LocalDarkTheme.current) Color(0xFF2C3E4C) else Color(0xFFDCE4EC))
                ) {
                    val effectiveUnits = masteryUnits + if (masteryAwardedForThisChar) 1 else 0
                    val fill = if (effectiveUnits <= 0) 0f else (0.14f + progressFraction * 0.86f).coerceIn(0.14f, 1f)
                    if (fill > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC800))
                        )
                    }
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

                        if (showGuide) {
                            TraceAccuracyEvaluator.drawCenteredChar(
                                nativeCanvas = drawContext.canvas.nativeCanvas,
                                charStr = targetChar,
                                canvasWidthPx = size.width,
                                canvasHeightPx = size.height,
                                paint = guidePaint
                            )
                        }

                        val invalidSet = evaluation?.invalidStrokeIndices.orEmpty()
                        strokes.forEachIndexed { idx, pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                // Out-of-bounds strokes turn bright RED so the user immediately sees what went wrong!
                                val strokeColor = if (idx in invalidSet) {
                                    DuoColors.CardinalRed
                                } else {
                                    when (idx % 3) {
                                        0 -> DuoColors.MacawBlueDark
                                        1 -> DuoColors.FeatherGreenDark
                                        else -> DuoColors.LilacPurpleDark
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        if (currentStroke.size > 1) {
                            val activePath = Path().apply {
                                moveTo(currentStroke.first().x, currentStroke.first().y)
                                currentStroke.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path = activePath,
                                color = DuoColors.MacawBlue,
                                style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
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
                        text = eval?.feedbackText ?: "✍️ Trace all ${expectedRange.first} strokes inside the gray guide",
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
                        text = if (passed) "NEXT IN ROW →" else "COMPLETE ALL STROKES",
                        enabled = passed,
                        onClick = onNextChar,
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Duo3DButton(
                        text = if (passed) "DONE" else "COMPLETE ALL STROKES",
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
