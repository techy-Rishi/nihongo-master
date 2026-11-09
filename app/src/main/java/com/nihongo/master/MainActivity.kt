package com.nihongo.master

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFBC002D),
                    secondary = Color(0xFF1E3A8A),
                    tertiary = Color(0xFF047857),
                    background = Color(0xFFF1F5F9),
                    surface = Color.White
                )
            ) {
                NihongoShogunApp(
                    onSpeak = { text, slow -> speakJapanese(text, slow) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPANESE
        }
    }

    private fun speakJapanese(text: String, slow: Boolean = false) {
        tts?.setSpeechRate(if (slow) 0.65f else 0.95f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nihongo_tts")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

enum class AppSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DOJO("Dojo", Icons.Default.Dashboard),
    KANA("208 Kana", Icons.Default.Brush),
    LEXICON("Kanji/SRS", Icons.Default.Style),
    GRAMMAR("Verbs/Grm", Icons.Default.AutoFixHigh),
    IMMERSION("Stories", Icons.Default.Forum),
    ARENA("Arena", Icons.Default.EmojiEvents)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NihongoShogunApp(onSpeak: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_shogun_pro", Context.MODE_PRIVATE) }

    var currentSection by remember { mutableStateOf(AppSection.DOJO) }
    var selectedLevel by remember { mutableStateOf<JlptLevel?>(null) }
    var userXp by remember { mutableIntStateOf(prefs.getInt("xp", 150)) }
    var streakCount by remember { mutableIntStateOf(prefs.getInt("streak", 3)) }
    var masteredSet by remember {
        mutableStateOf(prefs.getStringSet("mastered", emptySet())?.toSet() ?: emptySet())
    }

    fun addXp(amount: Int) {
        userXp += amount
        prefs.edit().putInt("xp", userXp).apply()
    }

    fun markMastered(id: String, mastered: Boolean) {
        val next = if (mastered) masteredSet + id else masteredSet - id
        masteredSet = next
        prefs.edit().putStringSet("mastered", next).apply()
        addXp(if (mastered) 20 else 5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("日本将軍 Shogun Pro", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Kana • Kanji • Pitch • Conjugator • Immersion", fontSize = 10.sp, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "🔥 $streakCount",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Surface(color = Color(0xFFFCE4EC), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "⚡ $userXp XP",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFBC002D)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppSection.entries.forEach { sec ->
                    NavigationBarItem(
                        selected = currentSection == sec,
                        onClick = { currentSection = sec },
                        icon = { Icon(sec.icon, contentDescription = sec.label) },
                        label = { Text(sec.label, fontSize = 10.sp, maxLines = 1) }
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
        ) {
            JlptFilterHeader(selectedLevel = selectedLevel, onSelect = { selectedLevel = it })

            when (currentSection) {
                AppSection.DOJO -> DojoDashboardScreen(
                    userXp = userXp,
                    masteredCount = masteredSet.size,
                    onNavigate = { currentSection = it },
                    onSpeak = onSpeak
                )
                AppSection.KANA -> KanaMasteryScreen(
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
                AppSection.LEXICON -> LexiconSrsScreen(
                    selectedLevel = selectedLevel,
                    masteredIds = masteredSet,
                    onGradeCard = { id, m -> markMastered(id, m) },
                    onSpeak = onSpeak
                )
                AppSection.GRAMMAR -> GrammarAndConjugatorScreen(
                    selectedLevel = selectedLevel,
                    onSpeak = onSpeak
                )
                AppSection.IMMERSION -> ImmersionAndSentenceBuilderScreen(
                    selectedLevel = selectedLevel,
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
                AppSection.ARENA -> ArenaQuizScreen(
                    selectedLevel = selectedLevel,
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
            }
        }
    }
}

@Composable
fun JlptFilterHeader(selectedLevel: JlptLevel?, onSelect: (JlptLevel?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { onSelect(null) },
                label = { Text("All Levels (N5–N1)", fontSize = 12.sp) }
            )
        }
        items(JlptLevel.entries) { lvl ->
            FilterChip(
                selected = selectedLevel == lvl,
                onClick = { onSelect(lvl) },
                label = { Text(lvl.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }
    }
}

// =========================================================================
// TAB 1: DOJO DASHBOARD (Rank, Pitch Accent Preview, SOV Matrix, Modules)
// =========================================================================
@Composable
fun DojoDashboardScreen(
    userXp: Int,
    masteredCount: Int,
    onNavigate: (AppSection) -> Unit,
    onSpeak: (String, Boolean) -> Unit
) {
    val currentRank = JlptLevel.entries.lastOrNull { userXp >= it.xpRequired } ?: JlptLevel.N5
    val totalFormsCount = remember {
        DataRepository.vocabList.sumOf {
            val c = VerbConjugator.conjugate(it).size
            if (c == 0) 1 else c
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT RANK • ${currentRank.label}", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(currentRank.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }
                        Surface(color = Color(currentRank.badgeColor), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                currentRank.label,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Database Active: 216 Kana • ${DataRepository.vocabList.size} Core Kanji Decks • $totalFormsCount Live Conjugations & Drills",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (masteredCount.toFloat() / DataRepository.vocabList.size.coerceAtLeast(1)).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        item {
            Text("Why You Learn 3x Faster (Hindi == Japanese SOV)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "English is SVO (I eat an apple), which scrambles Japanese in your head. Hindi is 100% identical to Japanese SOV word-for-word:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("私は", fontWeight = FontWeight.ExtraBold, color = Color(0xFFBC002D))
                            Text("मैं (Topic)", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("毎日", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                            Text("हर दिन", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("日本語を", fontWeight = FontWeight.ExtraBold, color = Color(0xFF047857))
                            Text("जापानी को", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("勉強します", fontWeight = FontWeight.ExtraBold, color = Color(0xFF6A1B9A))
                            Text("पढ़ता हूँ", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("208 Kana + Pad", "Gojūon, Dakuon, Yōon & Look-alikes", Color(0xFF1E3A8A), Modifier.weight(1f)) {
                    onNavigate(AppSection.KANA)
                }
                DashboardCard("Kanji + Pitch SRS", "Radicals, On/Kun & Pitch Contour", Color(0xFF047857), Modifier.weight(1f)) {
                    onNavigate(AppSection.LEXICON)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("12-Form Conjugator", "Live Verb Engine + Grammar Dojo", Color(0xFF6A1B9A), Modifier.weight(1f)) {
                    onNavigate(AppSection.GRAMMAR)
                }
                DashboardCard("Tile Builder & Stories", "Duolingo Tile Game + Immersion", Color(0xFFD97706), Modifier.weight(1f)) {
                    onNavigate(AppSection.IMMERSION)
                }
            }
        }

        item {
            val spotlight = remember { DataRepository.vocabList.random() }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pitch Accent & Radical Spotlight (${spotlight.level.label})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                        Row {
                            IconButton(onClick = { onSpeak(spotlight.kanji, false) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Normal Audio", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onSpeak(spotlight.kanji, true) }) {
                                Icon(Icons.Default.SlowMotionVideo, contentDescription = "Slow Audio", tint = Color(0xFF1E3A8A))
                            }
                        }
                    }
                    Text("${spotlight.kanji} 【${spotlight.kana}】", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${spotlight.meaning} • ${spotlight.hindiHint}", fontSize = 14.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PitchAccentVisualizer(kana = spotlight.kana, pattern = spotlight.pitchPattern, pitchType = spotlight.pitchType)
                    if (spotlight.radicals.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("🧩 Radicals: ${spotlight.radicals} | 💡 ${spotlight.mnemonic}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(100.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

// =========================================================================
// VISUAL PITCH ACCENT GRAPH COMPONENT (Dogen / OJAD Style)
// =========================================================================
@Composable
fun PitchAccentVisualizer(kana: String, pattern: List<Int>, pitchType: PitchType) {
    val morae = kana.map { it.toString() }
    val safePattern = if (pattern.size >= morae.size) pattern else List(morae.size) { idx -> if (idx == 0) 0 else 1 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text("🎵 Pitch Contour: ${pitchType.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
        Spacer(Modifier.height(6.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            val count = morae.size.coerceAtLeast(1)
            val stepX = size.width / (count + 1)
            val highY = 10.dp.toPx()
            val lowY = 32.dp.toPx()

            val points = morae.indices.map { i ->
                val x = stepX * (i + 1)
                val y = if (safePattern.getOrElse(i) { 0 } == 1) highY else lowY
                Offset(x, y)
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFFBC002D),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 6f
                )
            }

            points.forEach { pt ->
                drawCircle(color = Color(0xFFBC002D), radius = 10f, center = pt)
                drawCircle(color = Color.White, radius = 5f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            morae.forEachIndexed { idx, m ->
                val isHigh = safePattern.getOrElse(idx) { 0 } == 1
                Text(
                    text = "$m(${if (isHigh) "H" else "L"})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHigh) Color(0xFFBC002D) else Color.Gray
                )
            }
        }
    }
}

// =========================================================================
// TAB 2: 208 KANA STUDIO + LOOKALIKE DRILLS + COVERAGE TRACING PAD
// =========================================================================
@Composable
fun KanaMasteryScreen(onSpeak: (String, Boolean) -> Unit, onAwardXp: (Int) -> Unit) {
    var selectedType by remember { mutableStateOf(KanaType.HIRAGANA) }
    var selectedGroup by remember { mutableStateOf(KanaGroup.GOJUON) }
    var activeKana by remember { mutableStateOf<KanaChar?>(null) }

    val kanaList = remember(selectedType, selectedGroup) {
        if (selectedGroup == KanaGroup.LOOKALIKES) {
            DataRepository.allKana.filter { it.group == KanaGroup.LOOKALIKES }
        } else {
            DataRepository.allKana.filter { it.type == selectedType && it.group == selectedGroup }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedType = KanaType.HIRAGANA },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == KanaType.HIRAGANA) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            ) {
                Text("あ Hiragana (104)")
            }
            Button(
                onClick = { selectedType = KanaType.KATAKANA },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == KanaType.KATAKANA) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            ) {
                Text("ア Katakana (104)")
            }
        }

        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(KanaGroup.entries) { grp ->
                FilterChip(
                    selected = selectedGroup == grp,
                    onClick = { selectedGroup = grp },
                    label = { Text(grp.title, fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(kanaList) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.clickable {
                        onSpeak(item.char, false)
                        activeKana = item
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.char, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(item.romaji, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (activeKana != null) {
        KanaStrokeDialog(
            kana = activeKana!!,
            onSpeak = { slow -> onSpeak(activeKana!!.char, slow) },
            onDismiss = {
                onAwardXp(10)
                activeKana = null
            }
        )
    }
}

@Composable
fun KanaStrokeDialog(kana: KanaChar, onSpeak: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showGhost by remember { mutableStateOf(true) }

    val totalPointsDrawn = strokes.sumOf { it.size } + currentStroke.size
    val accuracyScore = (totalPointsDrawn * 2).coerceAtMost(100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trace 「${kana.char}」 (${kana.romaji})", fontWeight = FontWeight.ExtraBold)
                Row {
                    IconButton(onClick = { onSpeak(false) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Normal", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onSpeak(true) }) {
                        Icon(Icons.Default.SlowMotionVideo, contentDescription = "Slow", tint = Color(0xFF1E3A8A))
                    }
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💡 Mnemonic: ${kana.mnemonic}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Strokes Recorded: ${strokes.size} | Ink Practice Score: $accuracyScore%", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGhost) {
                        Text(
                            text = kana.char,
                            fontSize = 145.sp,
                            color = Color.LightGray.copy(alpha = 0.4f),
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
                                    }
                                )
                            }
                    ) {
                        drawLine(Color.LightGray, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawLine(Color.LightGray, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2f)

                        val all = strokes + listOf(currentStroke)
                        all.forEachIndexed { idx, pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                val strokeColor = when (idx % 4) {
                                    0 -> Color(0xFFBC002D)
                                    1 -> Color(0xFF1E3A8A)
                                    2 -> Color(0xFF047857)
                                    else -> Color(0xFF6A1B9A)
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
                    OutlinedButton(onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                    }) { Text("Clear") }
                    OutlinedButton(onClick = { showGhost = !showGhost }) {
                        Text(if (showGhost) "Blind Memory Test" else "Show Guide")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Complete (+10 XP)") }
        }
    )
}

// =========================================================================
// TAB 3: KANJI RADICALS + PITCH ACCENT SRS + LIVE 12-FORM CONJUGATION MODAL
// =========================================================================
@Composable
fun LexiconSrsScreen(
    selectedLevel: JlptLevel?,
    masteredIds: Set<String>,
    onGradeCard: (String, Boolean) -> Unit,
    onSpeak: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(selectedLevel, searchQuery) {
        DataRepository.vocabList.filter { item ->
            (selectedLevel == null || item.level == selectedLevel) &&
                (searchQuery.isBlank() ||
                    item.kanji.contains(searchQuery) ||
                    item.kana.contains(searchQuery) ||
                    item.romaji.contains(searchQuery, ignoreCase = true) ||
                    item.meaning.contains(searchQuery, ignoreCase = true) ||
                    item.hindiHint.contains(searchQuery))
        }
    }

    var currentIndex by remember(filteredList) { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showFurigana by remember { mutableStateOf(true) }
    var showConjugationModal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Kanji, Romaji, English or Hindi...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching vocabulary found.")
            }
            return
        }

        val currentItem = filteredList[currentIndex.coerceIn(0, filteredList.lastIndex)]
        val isMastered = masteredIds.contains(currentItem.id)
        val conjugations = remember(currentItem) { VerbConjugator.conjugate(currentItem) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Card ${currentIndex + 1} / ${filteredList.size}", fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Furigana", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Switch(checked = showFurigana, onCheckedChange = { showFurigana = it })
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { isFlipped = !isFlipped }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(currentItem.level.badgeColor), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "${currentItem.level.label} • ${currentItem.category}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (isMastered) {
                            Text("✅ SRS Mastered", color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (showFurigana) {
                        Text(currentItem.kana, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text(currentItem.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalIconButton(onClick = { onSpeak(currentItem.kanji, false) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Normal Audio")
                        }
                        FilledTonalIconButton(onClick = { onSpeak(currentItem.kanji, true) }) {
                            Icon(Icons.Default.SlowMotionVideo, contentDescription = "Slow Audio")
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = isFlipped) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HorizontalDivider(Modifier.padding(vertical = 10.dp))
                            Text(currentItem.meaning, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                            Text("🇮🇳 Hindi: ${currentItem.hindiHint}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Spacer(Modifier.height(8.dp))

                            PitchAccentVisualizer(
                                kana = currentItem.kana,
                                pattern = currentItem.pitchPattern,
                                pitchType = currentItem.pitchType
                            )

                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🧩 Radicals: ${currentItem.radicals}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("On'yomi: ${currentItem.onyomi} | Kun'yomi: ${currentItem.kunyomi}", fontSize = 12.sp)
                                    Text("💡 Mnemonic: ${currentItem.mnemonic}", fontSize = 12.sp, color = Color(0xFFB45309))
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().clickable { onSpeak(currentItem.exampleJp, false) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🔊 ${currentItem.exampleJp}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(currentItem.exampleRomaji, fontSize = 11.sp, color = Color.Gray)
                                    Text(currentItem.exampleEn, fontSize = 12.sp)
                                    Text(currentItem.exampleHi, fontSize = 12.sp, color = Color(0xFF047857))
                                }
                            }

                            if (conjugations.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { showConjugationModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⚡ View All ${conjugations.size} Verb Conjugations")
                                }
                            }
                        }
                    }

                    if (!isFlipped) {
                        Spacer(Modifier.height(20.dp))
                        Text("👆 Tap Card to Reveal Pitch Graph, Radicals, Hindi Hint & Examples", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    onGradeCard(currentItem.id, false)
                    isFlipped = false
                    currentIndex = (currentIndex + 1) % filteredList.size
                },
                modifier = Modifier.weight(1f)
            ) { Text("Again (1m)") }

            Button(
                onClick = {
                    onGradeCard(currentItem.id, true)
                    isFlipped = false
                    currentIndex = (currentIndex + 1) % filteredList.size
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                modifier = Modifier.weight(1f)
            ) { Text("Mastered (+20 XP)") }
        }
    }

    if (showConjugationModal) {
        val currentItem = filteredList[currentIndex.coerceIn(0, filteredList.lastIndex)]
        val forms = VerbConjugator.conjugate(currentItem)
        AlertDialog(
            onDismissRequest = { showConjugationModal = false },
            title = { Text("${currentItem.kanji} (${currentItem.verbGroup.label})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(forms) { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .clickable { onSpeak(f.japanese, false) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(f.formName, fontSize = 11.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
                                Text("${f.japanese} 【${f.reading}】", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(f.usageHint, fontSize = 11.sp, color = Color.DarkGray)
                            }
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConjugationModal = false }) { Text("Close") }
            }
        )
    }
}

// =========================================================================
// TAB 4: GRAMMAR DOJO & INTERACTIVE VERB CONJUGATION LAB
// =========================================================================
@Composable
fun GrammarAndConjugatorScreen(selectedLevel: JlptLevel?, onSpeak: (String, Boolean) -> Unit) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("📖 Grammar Dojo") }
            Button(
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (subTab == 1) Color(0xFF6A1B9A) else Color.Gray)
            ) { Text("⚙️ Conjugation Lab") }
        }

        Spacer(Modifier.height(10.dp))

        if (subTab == 0) {
            val gList = remember(selectedLevel) {
                if (selectedLevel == null) DataRepository.grammarList
                else DataRepository.grammarList.filter { it.level == selectedLevel }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(gList) { g ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(g.title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                Surface(color = Color(g.level.badgeColor), shape = RoundedCornerShape(8.dp)) {
                                    Text(g.level.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Structure: ${g.structure}", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, color = Color(0xFFB45309), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(g.explanation, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("🇮🇳 Hindi Parallel: ${g.hindiParallel}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("⚠️ Common Trap: ${g.commonMistake}", fontSize = 12.sp, color = Color(0xFFC62828))
                            Spacer(Modifier.height(10.dp))
                            g.examples.forEach { ex ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .clickable { onSpeak(ex.jp, false) }
                                        .padding(10.dp)
                                ) {
                                    Text("🔊 ${ex.jp}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(ex.romaji, fontSize = 11.sp, color = Color.Gray)
                                    Text(ex.en, fontSize = 12.sp)
                                    Text(ex.hi, fontSize = 12.sp, color = Color(0xFF047857))
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        } else {
            val allVerbs = remember(selectedLevel) {
                DataRepository.vocabList.filter {
                    it.verbGroup != VerbGroup.NOT_VERB && (selectedLevel == null || it.level == selectedLevel)
                }
            }
            var selectedVerb by remember(allVerbs) { mutableStateOf(allVerbs.firstOrNull()) }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allVerbs) { v ->
                    FilterChip(
                        selected = selectedVerb?.id == v.id,
                        onClick = { selectedVerb = v },
                        label = { Text("${v.kanji} (${v.meaning})") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            selectedVerb?.let { verb ->
                val forms = VerbConjugator.conjugate(verb)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(forms) { f ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().clickable { onSpeak(f.japanese, false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.formName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                                    Text("${f.japanese} 【${f.reading}】", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(f.usageHint, fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 5: DUOLINGO-KILLER TILE SENTENCE BUILDER + IMMERSION STORIES
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImmersionAndSentenceBuilderScreen(
    selectedLevel: JlptLevel?,
    onSpeak: (String, Boolean) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Sentence Builder, 1 = Immersion Stories

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { mode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (mode == 0) Color(0xFFD97706) else Color.Gray)
            ) { Text("🧩 SOV Sentence Builder") }
            Button(
                onClick = { mode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (mode == 1) Color(0xFF1E3A8A) else Color.Gray)
            ) { Text("📚 Immersion Stories") }
        }

        Spacer(Modifier.height(10.dp))

        if (mode == 0) {
            val puzzles = remember(selectedLevel) {
                val filtered = DataRepository.sentencePuzzles.filter { selectedLevel == null || it.level == selectedLevel }
                filtered.ifEmpty { DataRepository.sentencePuzzles }
            }
            var pIdx by remember(puzzles) { mutableIntStateOf(0) }
            val currentPuzzle = puzzles[pIdx % puzzles.size]

            val allAvailableTiles = remember(currentPuzzle) {
                (currentPuzzle.correctTiles + currentPuzzle.distractorTiles).shuffled()
            }
            val selectedTiles = remember(currentPuzzle) { mutableStateListOf<String>() }
            var checkResult by remember(currentPuzzle) { mutableStateOf<Boolean?>(null) }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SOV Sentence Puzzle ${pIdx + 1} / ${puzzles.size} (${currentPuzzle.level.label})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(currentPuzzle.englishPrompt, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("🇮🇳 Hint: ${currentPuzzle.hindiPrompt}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))

                    // Answer Tray
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        if (selectedTiles.isEmpty()) {
                            Text("Tap word tiles below in Japanese SOV order...", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedTiles.forEach { tile ->
                                    AssistChip(
                                        onClick = {
                                            selectedTiles.remove(tile)
                                            checkResult = null
                                        },
                                        label = { Text(tile, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Word Bank (Includes particle traps!):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allAvailableTiles.forEach { tile ->
                            val isUsed = selectedTiles.contains(tile)
                            ElevatedSuggestionChip(
                                onClick = {
                                    if (!isUsed) {
                                        onSpeak(tile, false)
                                        selectedTiles.add(tile)
                                        checkResult = null
                                    }
                                },
                                enabled = !isUsed,
                                label = { Text(tile, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (checkResult != null) {
                        Surface(
                            color = if (checkResult == true) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    if (checkResult == true) "✅ 完璧 (Perfect SOV Order! +25 XP)" else "❌ Try again! Check your particles.",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (checkResult == true) Color(0xFF047857) else Color(0xFFC62828)
                                )
                                Text(currentPuzzle.grammarNote, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val isCorrect = selectedTiles.toList() == currentPuzzle.correctTiles
                                checkResult = isCorrect
                                if (isCorrect) {
                                    onAwardXp(25)
                                    onSpeak(currentPuzzle.correctTiles.joinToString(""), false)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Check Answer") }

                        OutlinedButton(
                            onClick = {
                                pIdx = (pIdx + 1) % puzzles.size
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Next Puzzle →") }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(DataRepository.stories) { story ->
                    var selectedWordInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${story.titleJp} (${story.level.label})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(story.titleEn, fontSize = 13.sp, color = Color.Gray)
                            Spacer(Modifier.height(10.dp))

                            story.lines.forEach { line ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .clickable { onSpeak(line.jp, false) }
                                        .padding(10.dp)
                                ) {
                                    Text("${line.speaker}: ${line.jp}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(line.romaji, fontSize = 11.sp, color = Color.Gray)
                                    Text(line.en, fontSize = 12.sp)
                                    Text("🇮🇳 ${line.hi}", fontSize = 12.sp, color = Color(0xFF047857))
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(line.keyWords) { kw ->
                                            SuggestionChip(
                                                onClick = {
                                                    selectedWordInfo = kw
                                                    onSpeak(kw.first.substringBefore(" "), false)
                                                },
                                                label = { Text(kw.first, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }

                            selectedWordInfo?.let { (word, def) ->
                                Spacer(Modifier.height(6.dp))
                                Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("🔍 $word = $def", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 6: ARENA (Multi-Mode Exam: Audio Listening, Conjugation, Kanji, Combo XP)
// =========================================================================
@Composable
fun ArenaQuizScreen(
    selectedLevel: JlptLevel?,
    onSpeak: (String, Boolean) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var selectedMode by remember { mutableStateOf(QuizMode.MIXED) }
    var questions by remember(selectedLevel, selectedMode) {
        mutableStateOf(DataRepository.generateQuizQuestions(selectedLevel, selectedMode))
    }
    var qIndex by remember(questions) { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var combo by remember(questions) { mutableIntStateOf(0) }
    var isFinished by remember(questions) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(QuizMode.entries) { m ->
                FilterChip(
                    selected = selectedMode == m,
                    onClick = { selectedMode = m },
                    label = { Text(m.label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (questions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No questions for this filter.")
            }
            return
        }

        if (isFinished) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 Arena Cleared!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Final Score: $score / ${questions.size}", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        questions = DataRepository.generateQuizQuestions(selectedLevel, selectedMode)
                        qIndex = 0
                        score = 0
                        combo = 0
                        selectedOption = null
                        isFinished = false
                    }) {
                        Text("Play Another Round")
                    }
                }
            }
            return
        }

        val currentQ = questions[qIndex]
        val multiplier = when {
            combo >= 5 -> 3
            combo >= 3 -> 2
            else -> 1
        }

        LaunchedEffect(currentQ) {
            if (currentQ.isListeningOnly) {
                onSpeak(currentQ.audioText, false)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Q ${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.Bold)
            Text("🔥 Combo: ${combo}x (${multiplier}x XP)", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
            Text("Score: $score", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
        }

        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentQ.subPrompt, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { onSpeak(currentQ.audioText, false) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onSpeak(currentQ.audioText, true) }) {
                            Icon(Icons.Default.SlowMotionVideo, contentDescription = "Slow Audio", tint = Color(0xFF1E3A8A))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(currentQ.prompt, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        currentQ.options.forEachIndexed { idx, opt ->
            val isSelected = selectedOption == idx
            val isCorrect = idx == currentQ.correctIndex
            val bg = when {
                selectedOption == null -> Color.White
                isCorrect -> Color(0xFFD1FAE5)
                isSelected -> Color(0xFFFEE2E2)
                else -> Color.White
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = selectedOption == null) {
                        selectedOption = idx
                        if (idx == currentQ.correctIndex) {
                            score++
                            combo++
                            onAwardXp(10 * multiplier)
                        } else {
                            combo = 0
                        }
                        onSpeak(currentQ.audioText, false)
                    }
            ) {
                Text(
                    text = "${('A' + idx)}. $opt",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || (selectedOption != null && isCorrect)) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        if (selectedOption != null) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        if (selectedOption == currentQ.correctIndex) "✅ 正解! (+${10 * multiplier} XP)" else "❌ 不正解 (Combo Reset)",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedOption == currentQ.correctIndex) Color(0xFF047857) else Color(0xFFC62828)
                    )
                    Text(currentQ.explanation, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    selectedOption = null
                    if (qIndex + 1 < questions.size) qIndex++ else isFinished = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (qIndex + 1 < questions.size) "Next Challenge →" else "Finish Arena")
            }
        }
    }
}
