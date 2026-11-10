package com.nihongo.master.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// ALL KANA ROWS FOR SPECIFIC ROW PRACTICE vs OVERALL PRACTICE
// =========================================================================
private enum class KanaRowFilter(
    val label: String,
    val tipEn: String,
    val tipHi: String,
    val romajiSet: Set<String>?
) {
    VOWELS("A-Row (あ-お)", "5 core vowels: a, i, u, e, o", "5 मूल स्वर: अ (a), इ (i), उ (u), ए (e), ओ (o)", setOf("a", "i", "u", "e", "o")),
    K_ROW("K-Row (か-こ)", "K-consonant row: ka, ki, ku, ke, ko", "क-वर्ग: ka, ki, ku, ke, ko", setOf("ka", "ki", "ku", "ke", "ko")),
    S_ROW("S-Row (さ-そ)", "Note: し is 'shi' (not si)", "ध्यान दें: し = 'shi' (सी नहीं, शी!)", setOf("sa", "shi", "su", "se", "so")),
    T_ROW("T-Row (た-と)", "Note: ち is 'chi' and つ is 'tsu'", "ध्यान दें: ち = 'chi' और つ = 'tsu'", setOf("ta", "chi", "tsu", "te", "to")),
    N_ROW("N-Row (な-の)", "N-consonant row: na, ni, nu, ne, no", "न-वर्ग: na, ni, nu, ne, no", setOf("na", "ni", "nu", "ne", "no")),
    H_ROW("H-Row (は-ほ)", "Note: ふ is pronounced softly as 'fu'", "ध्यान दें: ふ = 'fu' (हल्के होंठों से फू)", setOf("ha", "hi", "fu", "he", "ho")),
    M_ROW("M-Row (ま-も)", "M-consonant row: ma, mi, mu, me, mo", "म-वर्ग: ma, mi, mu, me, mo", setOf("ma", "mi", "mu", "me", "mo")),
    Y_ROW("Y-Row (や-よ)", "3 Y-sounds: ya, yu, yo", "य-वर्ग के 3 अक्षर: ya, yu, yo", setOf("ya", "yu", "yo")),
    R_ROW("R-Row (ら-ろ)", "Japanese R is a light tap between R and L", "र-वर्ग: ra, ri, ru, re, ro", setOf("ra", "ri", "ru", "re", "ro")),
    W_ROW("W/N-Row (わ-ん)", "wa, particle wo (を), and nasal n (ん)", "व, पार्टिकल を (o), और नाक की ध्वनि ん (n)", setOf("wa", "wo", "n")),
    ALL_BASIC("All 46 Basic", "Complete 46 Gojūon characters", "पूरे 46 मूल अक्षर (Gojūon)", null),
    DAKUTEN("Dakuten ゛゜ (25)", "Voiced (g, z, d, b) & p-sounds", "बिंदु (゛) और गोला (゜) वाले 25 अक्षर", null),
    COMBO("Yōon Combo (33)", "Glided sounds with small ゃ, ゅ, ょ", "छोटा ゃ/ゅ/ょ जुड़कर बनने वाले 33 अक्षर", null);

    fun tip(lang: AppLanguage): String = if (lang == AppLanguage.HINDI) tipHi else tipEn

    fun filterKana(type: KanaType): List<KanaChar> {
        return DataRepository.allKana.filter { k ->
            k.type == type && when (this) {
                ALL_BASIC -> k.group == KanaGroup.BASIC
                DAKUTEN -> k.group == KanaGroup.VOICED
                COMBO -> k.group == KanaGroup.COMBO
                else -> k.group == KanaGroup.BASIC && (romajiSet?.contains(k.romaji) == true)
            }
        }
    }
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
    "wa" to "वा", "wo" to "ओ", "n" to "न्"
)

// =========================================================================
// FORGIVING BITMAP OVERLAP TRACING ACCURACY EVALUATOR
// Compares user's finger strokes against the target character shape!
// =========================================================================
private data class TraceEvaluation(
    val accuracyPercent: Int,
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
            return TraceEvaluation(0, false, "Draw on the canvas following the character guide.")
        }

        val scaleX = GRID_SIZE / canvasWidthPx
        val scaleY = GRID_SIZE / canvasHeightPx

        // 1. Render target character onto a 120x120 reference bitmap with a generous tolerance halo
        val refBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val refCanvas = android.graphics.Canvas(refBitmap)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = if (targetChar.length > 1) GRID_SIZE * 0.48f else GRID_SIZE * 0.72f
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 6f // Forgiving tolerance zone around the character
        }
        val fontMetrics = textPaint.fontMetrics
        val centerY = (GRID_SIZE / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        refCanvas.drawText(targetChar, GRID_SIZE / 2f, centerY, textPaint)

        // 2. Render user's drawn strokes onto a 120x120 user bitmap
        val userBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
        val userCanvas = android.graphics.Canvas(userBitmap)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 14f // Generous finger-brush radius for smooth overlap
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
            return TraceEvaluation(15, false, "Keep tracing! Cover the full character shape.")
        }

        val coverage = coveredRefPixels.toFloat() / refPixels.toFloat()     // How much of the character was traced
        val precision = onTrackUserPixels.toFloat() / userPixels.toFloat()   // How close the strokes stayed to the guide

        // Forgiving curve so a reasonable human trace scores 65%–98%
        val rawScore = (coverage * 0.62f + precision * 0.38f) * 125f
        val accuracy = rawScore.roundToInt().coerceIn(10, 99)
        val passed = accuracy >= 55 && coverage >= 0.32f

        val feedback = when {
            accuracy >= 85 -> "✨ $accuracy% Accuracy • Master Calligraphy! (+15 XP & +25% Mastery)"
            passed -> "✅ $accuracy% Accuracy • Good Trace! (+15 XP & +25% Mastery)"
            coverage < 0.32f -> "⚠️ $accuracy% Accuracy • Trace all strokes of 「$targetChar」 completely"
            else -> "🔄 $accuracy% Accuracy • Stay closer to the guide lines and try again"
        }

        return TraceEvaluation(accuracy, passed, feedback)
    }
}

// =========================================================================
// MAIN WRITING & KANA MASTERY SCREEN
// =========================================================================
@Composable
fun WritingLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }
    val appLanguage = LocalAppLanguage.current

    // Persistent Per-Character Mastery Map: char -> 0..4 (0%, 25%, 50%, 75%, 100%)
    var kanaMastery by remember {
        mutableStateOf(loadKanaMasteryMap(prefs))
    }

    fun incrementKanaMastery(charStr: String) {
        val current = kanaMastery[charStr] ?: 0
        val next = (current + 1).coerceAtMost(4)
        val updated = kanaMastery.toMutableMap().apply { put(charStr, next) }
        kanaMastery = updated
        saveKanaMasteryMap(prefs, updated)
    }

    var subTab by remember { mutableIntStateOf(0) } // 0: Kana Dojo & Row Trace, 1: Row & Overall Practice, 2: 103 N5 Kanji

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("あア Kana & Trace", "🎯 Row & Overall Quiz", "✍️ 103 Kanji"),
            selectedIndex = subTab,
            onSelect = { subTab = it },
            activeColor = DuoColors.FeatherGreen,
            activeLipColor = DuoColors.FeatherGreenDark
        )

        Spacer(Modifier.height(12.dp))

        when (subTab) {
            0 -> KanaMasterDojoView(
                appLanguage = appLanguage,
                kanaMastery = kanaMastery,
                onIncrementMastery = ::incrementKanaMastery,
                onStartRowQuiz = { subTab = 1 },
                onSpeak = onSpeak,
                onAwardXp = onAwardXp
            )
            1 -> KanaRowAndOverallPracticeView(
                appLanguage = appLanguage,
                kanaMastery = kanaMastery,
                onIncrementMastery = ::incrementKanaMastery,
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

private fun loadKanaMasteryMap(prefs: android.content.SharedPreferences): Map<String, Int> {
    val raw = prefs.getStringSet("kana_mastery_v1", emptySet()).orEmpty()
    val out = mutableMapOf<String, Int>()
    for (entry in raw) {
        val parts = entry.split(":")
        if (parts.size == 2) {
            parts[1].toIntOrNull()?.let { out[parts[0]] = it }
        }
    }
    return out
}

private fun saveKanaMasteryMap(prefs: android.content.SharedPreferences, map: Map<String, Int>) {
    val set = map.entries.map { "${it.key}:${it.value}" }.toSet()
    prefs.edit().putStringSet("kana_mastery_v1", set).apply()
}

private fun rowMasteryPercent(chars: List<KanaChar>, masteryMap: Map<String, Int>): Int {
    if (chars.isEmpty()) return 0
    val totalPoints = chars.sumOf { (masteryMap[it.char] ?: 0) * 25 }
    return (totalPoints / chars.size).coerceIn(0, 100)
}

// =========================================================================
// TAB 0: KANA DOJO (ROW-BY-ROW TRACE, MASTERY BARS & CONTINUOUS ROW TRACING)
// =========================================================================
@Composable
private fun KanaMasterDojoView(
    appLanguage: AppLanguage,
    kanaMastery: Map<String, Int>,
    onIncrementMastery: (String) -> Unit,
    onStartRowQuiz: () -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(KanaType.HIRAGANA) }
    var rowFilter by remember { mutableStateOf(KanaRowFilter.VOWELS) }
    var tracingQueue by remember { mutableStateOf<List<KanaChar>>(emptyList()) }
    var tracingIndex by remember { mutableIntStateOf(0) }

    val filteredKana = remember(selectedType, rowFilter) {
        rowFilter.filterKana(selectedType)
    }
    val currentRowMastery = remember(filteredKana, kanaMastery) {
        rowMasteryPercent(filteredKana, kanaMastery)
    }
    val overallScriptKana = remember(selectedType) {
        DataRepository.allKana.filter { it.type == selectedType }
    }
    val overallScriptMastery = remember(overallScriptKana, kanaMastery) {
        rowMasteryPercent(overallScriptKana, kanaMastery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Script Toggle (Hiragana vs Katakana) with Overall Script Mastery %
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Duo3DButton(
                    text = "ひらがな Hiragana ($overallScriptMastery%)",
                    onClick = { selectedType = KanaType.HIRAGANA },
                    topColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlue else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlueDark else DuoColors.LightLip,
                    textColor = if (selectedType == KanaType.HIRAGANA) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Duo3DButton(
                    text = "カタカナ Katakana",
                    onClick = { selectedType = KanaType.KATAKANA },
                    topColor = if (selectedType == KanaType.KATAKANA) DuoColors.LilacPurple else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (selectedType == KanaType.KATAKANA) DuoColors.LilacPurpleDark else DuoColors.LightLip,
                    textColor = if (selectedType == KanaType.KATAKANA) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Specific Row Selector Chips (A-Row, K-Row, S-Row, T-Row... with Row Mastery %)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KanaRowFilter.entries) { filter ->
                    val selected = rowFilter == filter
                    val rowPct = rowMasteryPercent(filter.filterKana(selectedType), kanaMastery)
                    FilterChip(
                        selected = selected,
                        onClick = { rowFilter = filter },
                        label = {
                            Text(
                                text = "${filter.label} • $rowPct%",
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

        // 3. Row Mastery Header + "Trace Entire Row" & "Practice Row" Quick Actions
        item {
            Duo3DCard(
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = DuoColors.FeatherGreen
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rowFilter.label} Mastery: $currentRowMastery%",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = rowFilter.tip(appLanguage),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentRowMastery / 100f).coerceIn(0.04f, 1f) },
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Duo3DButton(
                            text = "✍️ Trace Row (${filteredKana.size})",
                            onClick = {
                                tracingQueue = filteredKana
                                tracingIndex = 0
                            },
                            topColor = DuoColors.FeatherGreen,
                            lipColor = DuoColors.FeatherGreenDark,
                            modifier = Modifier.weight(1f)
                        )
                        Duo3DButton(
                            text = "🎯 Quiz Row / All",
                            onClick = onStartRowQuiz,
                            topColor = DuoColors.MacawBlue,
                            lipColor = DuoColors.MacawBlueDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Kana Cards Grid with Per-Character Mastery Stars & 1-Tap Trace
        val chunks = filteredKana.chunked(3)
        items(chunks) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { k ->
                    val levelPts = (kanaMastery[k.char] ?: 0).coerceIn(0, 4)
                    val pct = levelPts * 25
                    val phoneticLabel = if (appLanguage == AppLanguage.HINDI) {
                        val hi = HINDI_ROMAJI_MAP[k.romaji]
                        if (hi != null) "${k.romaji} ($hi)" else k.romaji
                    } else {
                        k.romaji
                    }

                    Duo3DCard(
                        modifier = Modifier.weight(1f),
                        borderColor = if (pct == 100) DuoColors.FeatherGreen else if (selectedType == KanaType.HIRAGANA) DuoColors.MacawBlue.copy(alpha = 0.55f) else DuoColors.LilacPurple.copy(alpha = 0.55f),
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
                                Text("🔊", fontSize = 11.sp)
                                Text(
                                    text = "✍️ Trace",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.FeatherGreenDark,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DuoColors.FeatherGreenBg)
                                        .clickable {
                                            val startIdx = filteredKana.indexOf(k).coerceAtLeast(0)
                                            tracingQueue = filteredKana
                                            tracingIndex = startIdx
                                        }
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
                                text = phoneticLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlueDark
                            )

                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0.08f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = if (pct == 100) DuoColors.FeatherGreen else DuoColors.BeeGold,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (pct == 100) "★ Mastered" else "$pct% Mastery",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pct == 100) DuoColors.FeatherGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
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

    // Continuous Row Tracing Dialog with Live Bitmap Accuracy Check!
    if (tracingQueue.isNotEmpty()) {
        val currentKana = tracingQueue[tracingIndex.coerceIn(0, tracingQueue.lastIndex)]
        val currentMasteryPct = ((kanaMastery[currentKana.char] ?: 0) * 25).coerceIn(0, 100)
        CharacterTraceDialog(
            title = "Trace ${if (currentKana.type == KanaType.HIRAGANA) "Hiragana" else "Katakana"} 「${currentKana.char}」 (${tracingIndex + 1}/${tracingQueue.size})",
            subtitle = "Sound: ${currentKana.romaji}  •  Current Mastery: $currentMasteryPct%",
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

// =========================================================================
// TAB 1: SPECIFIC ROW PRACTICE vs OVERALL PRACTICE (BOOSTS MASTERY EVERY TIME!)
// =========================================================================
@Composable
private fun KanaRowAndOverallPracticeView(
    appLanguage: AppLanguage,
    kanaMastery: Map<String, Int>,
    onIncrementMastery: (String) -> Unit,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    // Mode 0 = Specific Row Practice (A-Row, K-Row, S-Row...), Mode 1 = Overall Practice (Smart Weakest-First!)
    var isOverallMode by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(KanaType.HIRAGANA) }
    var selectedRow by remember { mutableStateOf(KanaRowFilter.VOWELS) }
    var round by remember { mutableIntStateOf(0) }
    var comboStreak by remember { mutableIntStateOf(0) }
    var selectedIdx by remember(round, isOverallMode, selectedRow, selectedType) { mutableStateOf<Int?>(null) }

    val activePool = remember(isOverallMode, selectedRow, selectedType, round) {
        if (isOverallMode) {
            // Overall Practice: prioritizes characters with lowest mastery so weak characters improve fast!
            val allForScript = DataRepository.allKana.filter { it.type == selectedType }
            val sortedByWeakest = allForScript.shuffled().sortedBy { kanaMastery[it.char] ?: 0 }
            sortedByWeakest.take(15).ifEmpty { DataRepository.allKana }
        } else {
            selectedRow.filterKana(selectedType).ifEmpty { DataRepository.allKana }
        }
    }

    val target = remember(activePool, round) { activePool[round % activePool.size] }
    val options = remember(activePool, target, round) {
        // In Specific Row mode, include distractors from the SAME row so you truly master the row!
        val sameRowWrong = activePool.map { it.romaji }.distinct().filter { it != target.romaji }.shuffled().take(2)
        val extraWrong = DataRepository.allKana.map { it.romaji }.distinct()
            .filter { it != target.romaji && it !in sameRowWrong }
            .shuffled()
            .take(3 - sameRowWrong.size)
        (sameRowWrong + extraWrong + target.romaji).shuffled()
    }
    val correctIndex = options.indexOf(target.romaji)
    val targetMasteryPct = ((kanaMastery[target.char] ?: 0) * 25).coerceIn(0, 100)

    LaunchedEffect(target) {
        onSpeak(target.char, ListeningSpeed.NATURAL)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Mode Selector: Specific Row vs Overall Practice
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Duo3DButton(
                    text = "🎯 Specific Row Practice",
                    onClick = { isOverallMode = false; round++ },
                    topColor = if (!isOverallMode) DuoColors.MacawBlue else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (!isOverallMode) DuoColors.MacawBlueDark else DuoColors.LightLip,
                    textColor = if (!isOverallMode) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Duo3DButton(
                    text = "🔥 Overall Practice (All)",
                    onClick = { isOverallMode = true; round++ },
                    topColor = if (isOverallMode) DuoColors.FoxOrange else MaterialTheme.colorScheme.surfaceVariant,
                    lipColor = if (isOverallMode) DuoColors.FoxOrangeDark else DuoColors.LightLip,
                    textColor = if (isOverallMode) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Script Toggle + Row Filter (if in Specific Row mode)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedType == KanaType.HIRAGANA,
                    onClick = { selectedType = KanaType.HIRAGANA; round++ },
                    label = { Text("Hiragana (あ)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedType == KanaType.KATAKANA,
                    onClick = { selectedType = KanaType.KATAKANA; round++ },
                    label = { Text("Katakana (ア)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "🔥 Streak: $comboStreak",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DuoColors.FoxOrangeDark
                )
            }

            if (!isOverallMode) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(KanaRowFilter.entries) { f ->
                        val rowPct = rowMasteryPercent(f.filterKana(selectedType), kanaMastery)
                        FilterChip(
                            selected = selectedRow == f,
                            onClick = { selectedRow = f; round++ },
                            label = { Text("${f.label} • $rowPct%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                        )
                    }
                }
            }
        }

        // 3. Target Kana Prompt Card with Character Mastery Bar
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOverallMode) "OVERALL SMART PRACTICE" else selectedRow.label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.MacawBlueDark
                        )
                        Surface(
                            color = if (targetMasteryPct == 100) DuoColors.FeatherGreenBg else DuoColors.BeeGoldBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Mastery: $targetMasteryPct%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPastel
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = target.char,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "🔊 Tap to hear pronunciation",
                        fontSize = 12.sp,
                        color = DuoColors.MacawBlueDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. 2x2 Answer Choices
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
                            val hi = if (appLanguage == AppLanguage.HINDI) HINDI_ROMAJI_MAP[romaji] else null
                            val isSelected = selectedIdx == idx
                            val isCorrect = idx == correctIndex

                            val bg = when {
                                selectedIdx == null -> MaterialTheme.colorScheme.surface
                                isCorrect -> DuoColors.FeatherGreenBg
                                isSelected -> DuoColors.CardinalRedBg
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val border = when {
                                selectedIdx == null -> DuoColors.MacawBlue.copy(alpha = 0.5f)
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
                                            onIncrementMastery(target.char)
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
                                            text = hi,
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
                val newPct = ((kanaMastery[target.char] ?: 0) * 25).coerceIn(0, 100)
                Duo3DCard(
                    containerColor = if (won) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                    borderColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                    lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (won) "✅ Correct! 「${target.char}」 Mastery is now $newPct% (+10 XP)"
                            else "❌ Correct sound: 「${target.char}」 = ${target.romaji}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = OnPastel
                        )
                        Spacer(Modifier.height(10.dp))
                        Duo3DButton(
                            text = "NEXT CHARACTER →",
                            onClick = { round++ },
                            topColor = if (won) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                            lipColor = if (won) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
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
        modifier = Modifier.fillMaxWidth(),
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
                        text = "✍️ Trace 「${currentKanji.kanji}」 with Accuracy Check (+20 XP)",
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
            subtitle = "${currentKanji.strokeCount} Strokes • ${currentKanji.sampleWords.firstOrNull().orEmpty()}",
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
// SMART CHARACTER TRACE DIALOG WITH LIVE BITMAP ACCURACY CHECK & NEXT IN ROW
// =========================================================================
@Composable
fun CharacterTraceDialog(
    title: String,
    subtitle: String,
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
            Column {
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
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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

                // Live Accuracy Feedback Banner
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
                        text = eval?.feedbackText ?: "✍️ Trace over 「$targetChar」 with your finger to check accuracy",
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
                        text = if (passed) "NEXT IN ROW →" else "TRACE TO UNLOCK NEXT",
                        enabled = passed,
                        onClick = onNextChar,
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Duo3DButton(
                        text = if (passed) "FINISH (+XP)" else "TRACE CHARACTER FIRST",
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
