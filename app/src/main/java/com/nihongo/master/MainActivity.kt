@file:OptIn(ExperimentalMaterial3Api::class)

package com.nihongo.master

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// =========================================================================
// ACTIVITY (owns Text-to-Speech)
// =========================================================================
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

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
                NihongoApp(onSpeak = { text, slow -> speakJapanese(text, slow) })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.JAPANESE)
            ttsReady = result != null &&
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speakJapanese(text: String, slow: Boolean) {
        if (!ttsReady) {
            Toast.makeText(
                this,
                "Japanese voice not available. Install it in Settings > Text-to-speech.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        tts?.setSpeechRate(if (slow) 0.65f else 0.95f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nihongo_tts")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// =========================================================================
// APP SHELL
// =========================================================================
enum class AppSection(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    KANA("Kana", Icons.Default.Brush),
    WORDS("Words", Icons.Default.Style),
    GRAMMAR("Grammar", Icons.Default.MenuBook),
    SENTENCES("Sentences", Icons.Default.Forum),
    QUIZ("Quiz", Icons.Default.EmojiEvents)
}

private val RANKS = listOf(
    0 to "初心者 Beginner",
    200 to "見習い Apprentice",
    600 to "学習者 Learner",
    1500 to "達人 Expert",
    3000 to "名人 Master"
)

private fun rankFor(xp: Int): String = RANKS.last { xp >= it.first }.second

private fun updateStreak(prefs: SharedPreferences): Int {
    val today = System.currentTimeMillis() / 86_400_000L
    val last = prefs.getLong("last_day", -1L)
    val old = prefs.getInt("streak", 0)
    val streak = when (last) {
        today -> old.coerceAtLeast(1)
        today - 1 -> old + 1
        else -> 1
    }
    prefs.edit().putLong("last_day", today).putInt("streak", streak).apply()
    return streak
}

@Composable
fun NihongoApp(onSpeak: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }

    var section by remember { mutableStateOf(AppSection.HOME) }
    var level by remember { mutableStateOf<JlptLevel?>(null) }
    var xp by remember { mutableIntStateOf(prefs.getInt("xp", 0)) }
    val streak = remember { updateStreak(prefs) }
    var mastered by remember {
        mutableStateOf(prefs.getStringSet("mastered", emptySet())?.toSet() ?: emptySet())
    }

    fun addXp(amount: Int) {
        xp += amount
        prefs.edit().putInt("xp", xp).apply()
    }

    fun grade(id: String, known: Boolean) {
        val next = if (known) mastered + id else mastered - id
        mastered = next
        prefs.edit().putStringSet("mastered", next).apply()
        addXp(if (known) 20 else 5)
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
                        Text("日本語 Master", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "🔥 $streak",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Surface(color = Color(0xFFFCE4EC), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "⚡ $xp XP",
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
                        selected = section == sec,
                        onClick = { section = sec },
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
            LevelFilterHeader(selected = level, onSelect = { level = it })

            when (section) {
                AppSection.HOME -> HomeScreen(
                    xp = xp,
                    masteredCount = mastered.size,
                    onNavigate = { section = it },
                    onSpeak = onSpeak
                )
                AppSection.KANA -> KanaScreen(
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
                AppSection.WORDS -> WordsScreen(
                    level = level,
                    masteredIds = mastered,
                    onGrade = { id, known -> grade(id, known) },
                    onSpeak = onSpeak
                )
                AppSection.GRAMMAR -> GrammarScreen(level = level, onSpeak = onSpeak)
                AppSection.SENTENCES -> SentencesScreen(level = level, onSpeak = onSpeak)
                AppSection.QUIZ -> QuizScreen(
                    level = level,
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
            }
        }
    }
}

@Composable
fun LevelFilterHeader(selected: JlptLevel?, onSelect: (JlptLevel?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All (N5–N1)", fontSize = 12.sp) }
            )
        }
        items(JlptLevel.entries) { lvl ->
            FilterChip(
                selected = selected == lvl,
                onClick = { onSelect(lvl) },
                label = { Text(lvl.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
fun LevelBadge(level: JlptLevel, extra: String = "") {
    Surface(color = Color(level.color), shape = RoundedCornerShape(8.dp)) {
        Text(
            if (extra.isEmpty()) level.label else "${level.label} • $extra",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SpeakButtons(text: String, onSpeak: (String, Boolean) -> Unit) {
    Row {
        IconButton(onClick = { onSpeak(text, false) }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { onSpeak(text, true) }) {
            Icon(Icons.Default.SlowMotionVideo, contentDescription = "Play slowly", tint = Color(0xFF1E3A8A))
        }
    }
}

// =========================================================================
// HOME
// =========================================================================
@Composable
fun HomeScreen(
    xp: Int,
    masteredCount: Int,
    onNavigate: (AppSection) -> Unit,
    onSpeak: (String, Boolean) -> Unit
) {
    val total = DataRepository.vocab.size.coerceAtLeast(1)
    val spotlight = remember { DataRepository.vocab.random() }

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
                    Text("CURRENT RANK", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(rankFor(xp), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${DataRepository.allKana.size} kana • ${DataRepository.vocab.size} words • " +
                            "$masteredCount mastered",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (masteredCount.toFloat() / total).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Hindi and Japanese share SOV word order", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The verb goes at the end, and particles come after the noun — just like Hindi postpositions.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SovPart("私は", "मैं", Color(0xFFBC002D))
                        SovPart("毎日", "हर दिन", Color(0xFF1E3A8A))
                        SovPart("日本語を", "जापानी को", Color(0xFF047857))
                        SovPart("勉強します", "पढ़ता हूँ", Color(0xFF6A1B9A))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Kana", "Hiragana & Katakana + tracing", Color(0xFF1E3A8A), Modifier.weight(1f)) {
                    onNavigate(AppSection.KANA)
                }
                DashboardCard("Words", "Flashcards N5–N1", Color(0xFF047857), Modifier.weight(1f)) {
                    onNavigate(AppSection.WORDS)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Grammar", "Patterns + verb conjugation lab", Color(0xFF6A1B9A), Modifier.weight(1f)) {
                    onNavigate(AppSection.GRAMMAR)
                }
                DashboardCard("Quiz", "Meaning, reading, listening", Color(0xFFD97706), Modifier.weight(1f)) {
                    onNavigate(AppSection.QUIZ)
                }
            }
        }

        item {
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
                        Text("Word spotlight", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                        SpeakButtons(text = spotlight.kana, onSpeak = onSpeak)
                    }
                    Text("${spotlight.kanji} 【${spotlight.kana}】", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${spotlight.en} • ${spotlight.hi}",
                        fontSize = 14.sp,
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(spotlight.exJp, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(spotlight.exRomaji, fontSize = 11.sp, color = Color.Gray)
                    Text(spotlight.exEn, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SovPart(jp: String, hi: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(jp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(hi, fontSize = 11.sp)
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
// KANA (grid + tracing pad)
// =========================================================================
@Composable
fun KanaScreen(onSpeak: (String, Boolean) -> Unit, onAwardXp: (Int) -> Unit) {
    var type by remember { mutableStateOf(KanaType.HIRAGANA) }
    var group by remember { mutableStateOf(KanaGroup.BASIC) }
    var active by remember { mutableStateOf<KanaChar?>(null) }

    val kanaList = remember(type, group) {
        DataRepository.allKana.filter { it.type == type && it.group == group }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { type = KanaType.HIRAGANA },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == KanaType.HIRAGANA) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) { Text("あ Hiragana") }
            Button(
                onClick = { type = KanaType.KATAKANA },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == KanaType.KATAKANA) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) { Text("ア Katakana") }
        }

        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(KanaGroup.entries) { grp ->
                FilterChip(
                    selected = group == grp,
                    onClick = { group = grp },
                    label = { Text(grp.title, fontSize = 12.sp) }
                )
            }
        }
        Text(group.tip, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))

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
                        active = item
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.char, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            item.romaji,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    active?.let { k ->
        KanaTraceDialog(
            kana = k,
            onSpeak = { slow -> onSpeak(k.char, slow) },
            onDismiss = { active = null },
            onComplete = { strokeCount ->
                if (strokeCount > 0) onAwardXp(10)
                active = null
            }
        )
    }
}

@Composable
fun KanaTraceDialog(
    kana: KanaChar,
    onSpeak: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onComplete: (Int) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showGuide by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trace 「${kana.char}」 ${kana.romaji}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Row {
                    IconButton(onClick = { onSpeak(false) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onSpeak(true) }) {
                        Icon(Icons.Default.SlowMotionVideo, contentDescription = "Play slowly", tint = Color(0xFF1E3A8A))
                    }
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Strokes drawn: ${strokes.size}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuide) {
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
                    OutlinedButton(onClick = { showGuide = !showGuide }) {
                        Text(if (showGuide) "Hide guide" else "Show guide")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onComplete(strokes.size) }) { Text("Done (+10 XP)") }
        }
    )
}

// =========================================================================
// WORDS (flashcards + conjugation popup)
// =========================================================================
@Composable
fun WordsScreen(
    level: JlptLevel?,
    masteredIds: Set<String>,
    onGrade: (String, Boolean) -> Unit,
    onSpeak: (String, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(level, query) {
        DataRepository.vocab.filter { w ->
            (level == null || w.level == level) &&
                (query.isBlank() ||
                    w.kanji.contains(query) ||
                    w.kana.contains(query) ||
                    w.romaji.contains(query, ignoreCase = true) ||
                    w.en.contains(query, ignoreCase = true) ||
                    w.hi.contains(query))
        }
    }

    var index by remember(filtered) { mutableIntStateOf(0) }
    var flipped by remember(filtered) { mutableStateOf(false) }
    var showKana by remember { mutableStateOf(true) }
    var showConj by remember { mutableStateOf(false) }

    val current: VocabItem? =
        if (filtered.isEmpty()) null else filtered[index.coerceIn(0, filtered.lastIndex)]

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search kanji, romaji, English or Hindi…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching words found.")
            }
        } else {
            val isMastered = masteredIds.contains(current.id)
            val canConjugate = current.type.isVerb

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Card ${index + 1} / ${filtered.size}", fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reading", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Switch(checked = showKana, onCheckedChange = { showKana = it })
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { flipped = !flipped }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LevelBadge(current.level, current.type.label)
                        if (isMastered) {
                            Text("✅ Mastered", color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (showKana) {
                        Text(
                            current.kana,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(current.kanji, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    SpeakButtons(text = current.kana, onSpeak = onSpeak)

                    if (flipped) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(current.en, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                        Text("🇮🇳 ${current.hi}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                        Text(current.romaji, fontSize = 12.sp, color = Color.Gray)

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSpeak(current.exJp, false) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🔊 ${current.exJp}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(current.exRomaji, fontSize = 11.sp, color = Color.Gray)
                                Text(current.exEn, fontSize = 12.sp)
                            }
                        }

                        if (canConjugate) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { showConj = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("⚡ Show verb forms") }
                        }
                    } else {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "👆 Tap the card to reveal meaning, Hindi and example",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onGrade(current.id, false)
                        flipped = false
                        index = (index + 1) % filtered.size
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Again") }

                Button(
                    onClick = {
                        onGrade(current.id, true)
                        flipped = false
                        index = (index + 1) % filtered.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    modifier = Modifier.weight(1f)
                ) { Text("Mastered (+20 XP)") }
            }
        }
    }

    if (showConj && current != null) {
        ConjugationDialog(word = current, onSpeak = onSpeak, onDismiss = { showConj = false })
    }
}

@Composable
fun ConjugationDialog(word: VocabItem, onSpeak: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    val forms = remember(word) { VerbConjugator.forms(word) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${word.kanji} (${word.type.label})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(forms) { f -> ConjFormRow(f, onSpeak) }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ConjFormRow(f: ConjForm, onSpeak: (String, Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .clickable { onSpeak(f.reading, false) }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(f.name, fontSize = 11.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
            Text("${f.jp} 【${f.reading}】", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(f.hint, fontSize = 11.sp, color = Color.DarkGray)
        }
        Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
    }
}

// =========================================================================
// GRAMMAR (+ conjugation lab)
// =========================================================================
@Composable
fun GrammarScreen(level: JlptLevel?, onSpeak: (String, Boolean) -> Unit) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTab == 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) { Text("📖 Grammar") }
            Button(
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTab == 1) Color(0xFF6A1B9A) else Color.Gray
                )
            ) { Text("⚙️ Conjugation") }
        }

        Spacer(Modifier.height(10.dp))

        if (subTab == 0) {
            val points = remember(level) {
                if (level == null) {
                    JlptLevel.entries.flatMap { DataRepository.grammar(it) }
                } else {
                    DataRepository.grammar(level)
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(points) { g -> GrammarCard(g, onSpeak) }
            }
        } else {
            val verbs = remember(level) {
                DataRepository.vocab.filter { it.type.isVerb && (level == null || it.level == level) }
            }
            var selected by remember(verbs) { mutableStateOf(verbs.firstOrNull()) }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(verbs) { v ->
                    FilterChip(
                        selected = selected?.id == v.id,
                        onClick = { selected = v },
                        label = { Text("${v.kanji} (${v.en})") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val verb = selected
            if (verb == null) {
                Text("No verbs for this level.")
            } else {
                val forms = remember(verb) { VerbConjugator.forms(verb) }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forms) { f ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ConjFormRow(f, onSpeak)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarCard(g: GrammarPoint, onSpeak: (String, Boolean) -> Unit) {
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
                Text(g.title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                LevelBadge(g.level)
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Structure: ${g.structure}",
                    modifier = Modifier.padding(10.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(g.explanation, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("🇮🇳 ${g.hindiTip}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .clickable { onSpeak(g.exJp, false) }
                    .padding(10.dp)
            ) {
                Text("🔊 ${g.exJp}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(g.exRomaji, fontSize = 11.sp, color = Color.Gray)
                Text(g.exEn, fontSize = 12.sp)
            }
        }
    }
}

// =========================================================================
// SENTENCES
// =========================================================================
@Composable
fun SentencesScreen(level: JlptLevel?, onSpeak: (String, Boolean) -> Unit) {
    val sentences = remember(level) {
        if (level == null) {
            JlptLevel.entries.flatMap { DataRepository.sentences(it) }
        } else {
            DataRepository.sentences(level)
        }
    }
    var showRomaji by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${sentences.size} sentences — tap to listen", fontSize = 12.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Romaji", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Switch(checked = showRomaji, onCheckedChange = { showRomaji = it })
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sentences) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSpeak(s.jp, false) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔊 ${s.jp}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            LevelBadge(s.level)
                        }
                        if (showRomaji) Text(s.romaji, fontSize = 11.sp, color = Color.Gray)
                        Text(s.en, fontSize = 13.sp)
                        Text("🇮🇳 ${s.hi}", fontSize = 12.sp, color = Color(0xFF047857))
                    }
                }
            }
        }
    }
}

// =========================================================================
// QUIZ
// =========================================================================
private fun multiplierFor(combo: Int): Int = when {
    combo >= 5 -> 3
    combo >= 3 -> 2
    else -> 1
}

@Composable
fun QuizScreen(level: JlptLevel?, onSpeak: (String, Boolean) -> Unit, onAwardXp: (Int) -> Unit) {
    var mode by remember { mutableStateOf(QuizMode.MEANING) }
    var round by remember { mutableIntStateOf(0) }
    val questions = remember(level, mode, round) { DataRepository.makeQuiz(level, mode) }

    var qIndex by remember(questions) { mutableIntStateOf(0) }
    var selected by remember(questions) { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var combo by remember(questions) { mutableIntStateOf(0) }
    var lastGain by remember(questions) { mutableIntStateOf(0) }
    var finished by remember(questions) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(QuizMode.entries) { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.label, fontSize = 12.sp) }
                )
            }
        }
        Text(mode.desc, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))

        when {
            questions.isEmpty() -> {
                Text("No questions available for this filter.")
            }

            finished -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 Round complete!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Score: $score / ${questions.size}",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { round++ }) { Text("Play another round") }
                    }
                }
            }

            else -> {
                val q = questions[qIndex]

                LaunchedEffect(q) {
                    if (q.autoPlay) onSpeak(q.audio, false)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Q ${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.Bold)
                    Text(
                        "🔥 Combo ${combo} (x${multiplierFor(combo)})",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                    Text("Score: $score", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                }

                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                q.instruction,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            SpeakButtons(text = q.audio, onSpeak = onSpeak)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            q.big,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                q.options.forEachIndexed { idx, opt ->
                    val isSelected = selected == idx
                    val isCorrect = idx == q.correctIndex
                    val bg = when {
                        selected == null -> Color.White
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
                            .clickable(enabled = selected == null) {
                                selected = idx
                                if (isCorrect) {
                                    val gain = 10 * multiplierFor(combo)
                                    lastGain = gain
                                    score++
                                    combo++
                                    onAwardXp(gain)
                                } else {
                                    lastGain = 0
                                    combo = 0
                                }
                                onSpeak(q.audio, false)
                            }
                    ) {
                        Text(
                            text = "${'A' + idx}. $opt",
                            modifier = Modifier.padding(14.dp),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected || (selected != null && isCorrect)) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (selected != null) {
                    val correct = selected == q.correctIndex
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (correct) "✅ Correct! (+$lastGain XP)" else "❌ Not quite (combo reset)",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (correct) Color(0xFF047857) else Color(0xFFC62828)
                            )
                            Text(q.explanation, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            selected = null
                            if (qIndex + 1 < questions.size) qIndex++ else finished = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (qIndex + 1 < questions.size) "Next →" else "Finish")
                    }
                }
            }
        }
    }
}
