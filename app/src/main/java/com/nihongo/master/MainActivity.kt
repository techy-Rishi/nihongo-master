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
                    primary = Color(0xFFBC002D), // Japanese Crimson
                    secondary = Color(0xFF1E3A8A),
                    tertiary = Color(0xFF047857),
                    background = Color(0xFFF8FAFC),
                    surface = Color.White
                )
            ) {
                NihongoMasterApp(onSpeak = { text -> speakJapanese(text) })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPANESE
            tts?.setSpeechRate(0.9f)
        }
    }

    private fun speakJapanese(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jp_tts")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

enum class AppTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    KANA("Kana", Icons.Default.Edit),
    VOCAB("SRS Deck", Icons.Default.Style),
    GRAMMAR("Grammar", Icons.Default.MenuBook),
    QUIZ("Test", Icons.Default.Quiz)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NihongoMasterApp(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_srs_prefs", Context.MODE_PRIVATE) }

    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var selectedLevel by remember { mutableStateOf<JlptLevel?>(null) }
    var userXp by remember { mutableIntStateOf(prefs.getInt("user_xp", 120)) }
    var masteredSet by remember {
        mutableStateOf(prefs.getStringSet("mastered_ids", emptySet())?.toSet() ?: emptySet())
    }

    fun addXp(points: Int) {
        userXp += points
        prefs.edit().putInt("user_xp", userXp).apply()
    }

    fun toggleMastered(id: String, mastered: Boolean) {
        val updated = if (mastered) masteredSet + id else masteredSet - id
        masteredSet = updated
        prefs.edit().putStringSet("mastered_ids", updated).apply()
        addXp(if (mastered) 15 else 5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("日本語 Master", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ $userXp XP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) }
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
            // Global JLPT Level Filter Bar
            JlptFilterRow(selectedLevel = selectedLevel, onSelect = { selectedLevel = it })

            when (currentTab) {
                AppTab.HOME -> HomeScreen(
                    userXp = userXp,
                    masteredCount = masteredSet.size,
                    totalVocab = DataRepository.vocabList.size,
                    selectedLevel = selectedLevel,
                    onNavigate = { currentTab = it },
                    onSpeak = onSpeak
                )
                AppTab.KANA -> KanaStudioScreen(onSpeak = onSpeak, onPracticeXp = { addXp(5) })
                AppTab.VOCAB -> VocabSrsScreen(
                    selectedLevel = selectedLevel,
                    masteredIds = masteredSet,
                    onGradeCard = { id, mastered -> toggleMastered(id, mastered) },
                    onSpeak = onSpeak
                )
                AppTab.GRAMMAR -> GrammarDojoScreen(
                    selectedLevel = selectedLevel,
                    onSpeak = onSpeak
                )
                AppTab.QUIZ -> QuizArenaScreen(
                    selectedLevel = selectedLevel,
                    onSpeak = onSpeak,
                    onAwardXp = { addXp(it) }
                )
            }
        }
    }
}

@Composable
fun JlptFilterRow(selectedLevel: JlptLevel?, onSelect: (JlptLevel?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { onSelect(null) },
                label = { Text("All (N5–N1)") }
            )
        }
        items(JlptLevel.entries) { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelect(level) },
                label = { Text(level.label, fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Composable
fun HomeScreen(
    userXp: Int,
    masteredCount: Int,
    totalVocab: Int,
    selectedLevel: JlptLevel?,
    onNavigate: (AppTab) -> Unit,
    onSpeak: (String) -> Unit
) {
    val rankTitle = when {
        userXp >= 1000 -> "Shogun (Master N1)"
        userXp >= 500 -> "Samurai (Upper Intermediate)"
        userXp >= 250 -> "Ronin (Elementary N4)"
        else -> "Deshi (Apprentice N5)"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Current Rank: $rankTitle", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("がんばれ！(Keep Going!)", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (masteredCount.toFloat() / totalVocab.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD54F),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SRS Cards Mastered: $masteredCount / $totalVocab",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (selectedLevel != null) {
            item {
                Surface(
                    color = Color(selectedLevel.badgeColor).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${selectedLevel.label}: ${selectedLevel.description}",
                        modifier = Modifier.padding(12.dp),
                        color = Color(selectedLevel.badgeColor),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Text("Study Modules", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickModuleCard(
                    title = "Kana & Stroke Pad",
                    subtitle = "Hiragana, Katakana & Writing Canvas",
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.KANA) }
                )
                QuickModuleCard(
                    title = "Vocab & Kanji SRS",
                    subtitle = "N5–N1 Flashcards + Hindi Hints",
                    color = Color(0xFF047857),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.VOCAB) }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickModuleCard(
                    title = "Grammar Dojo",
                    subtitle = "SOV Syntax, Particles & Keigo",
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.GRAMMAR) }
                )
                QuickModuleCard(
                    title = "JLPT Mock Test",
                    subtitle = "Adaptive Quiz & Instant Explanations",
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppTab.QUIZ) }
                )
            }
        }

        item {
            val dailyWord = DataRepository.vocabList.random()
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
                        Text("Word of the Moment (${dailyWord.level.label})", fontWeight = FontWeight.Bold, color = Color.Gray)
                        IconButton(onClick = { onSpeak(dailyWord.kanji) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("${dailyWord.kanji} 【${dailyWord.kana}】", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${dailyWord.romaji} — ${dailyWord.meaning} (${dailyWord.hindiHint})", fontSize = 15.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(6.dp))
                    Text("Ex: ${dailyWord.exampleJp}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(dailyWord.exampleEn, fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun QuickModuleCard(title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(115.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun KanaStudioScreen(onSpeak: (String) -> Unit, onPracticeXp: () -> Unit) {
    var selectedType by remember { mutableStateOf(KanaType.HIRAGANA) }
    var selectedGroup by remember { mutableStateOf(KanaGroup.GOJUON) }
    var practiceKana by remember { mutableStateOf<KanaChar?>(null) }

    val kanaItems = remember(selectedType, selectedGroup) {
        val base = if (selectedType == KanaType.HIRAGANA) DataRepository.hiraganaList else DataRepository.katakanaList
        base.filter { it.group == selectedGroup }
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
                Text("あ Hiragana")
            }
            Button(
                onClick = { selectedType = KanaType.KATAKANA },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == KanaType.KATAKANA) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            ) {
                Text("ア Katakana")
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(KanaGroup.entries) { grp ->
                FilterChip(
                    selected = selectedGroup == grp,
                    onClick = { selectedGroup = grp },
                    label = { Text(grp.title) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Tap any character for audio & freehand stroke drawing practice:",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(kanaItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.clickable {
                        onSpeak(item.char)
                        practiceKana = item
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.char, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(item.romaji, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (practiceKana != null) {
        KanaDrawingDialog(
            kana = practiceKana!!,
            onSpeak = { onSpeak(practiceKana!!.char) },
            onDismiss = {
                onPracticeXp()
                practiceKana = null
            }
        )
    }
}

@Composable
fun KanaDrawingDialog(kana: KanaChar, onSpeak: () -> Unit, onDismiss: () -> Unit) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showGuide by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Write 「${kana.char}」 (${kana.romaji})", fontWeight = FontWeight.Bold)
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mnemonic: ${kana.hint}", fontSize = 13.sp, color = Color.DarkGray)
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(2.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuide) {
                        Text(
                            text = kana.char,
                            fontSize = 150.sp,
                            color = Color.LightGray.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset -> currentPoints = listOf(offset) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            paths.add(currentPoints)
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw crosshair guidelines
                        drawLine(Color.LightGray, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f)
                        drawLine(Color.LightGray, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2f)

                        val allStrokes = paths + listOf(currentPoints)
                        allStrokes.forEach { pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF1E3A8A),
                                    style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        paths.clear()
                        currentPoints = emptyList()
                    }) {
                        Text("Clear Pad")
                    }
                    OutlinedButton(onClick = { showGuide = !showGuide }) {
                        Text(if (showGuide) "Hide Guide" else "Show Guide")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done (+5 XP)")
            }
        }
    )
}

@Composable
fun VocabSrsScreen(
    selectedLevel: JlptLevel?,
    masteredIds: Set<String>,
    onGradeCard: (String, Boolean) -> Unit,
    onSpeak: (String) -> Unit
) {
    val filteredList = remember(selectedLevel) {
        if (selectedLevel == null) DataRepository.vocabList
        else DataRepository.vocabList.filter { it.level == selectedLevel }
    }

    var currentIndex by remember(filteredList) { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showFurigana by remember { mutableStateOf(true) }

    if (filteredList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards found for this filter.")
        }
        return
    }

    val currentItem = filteredList[currentIndex.coerceIn(0, filteredList.lastIndex)]
    val isMastered = masteredIds.contains(currentItem.id)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Card ${currentIndex + 1} of ${filteredList.size}",
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Furigana", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Switch(checked = showFurigana, onCheckedChange = { showFurigana = it })
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { isFlipped = !isFlipped }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(currentItem.level.badgeColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            currentItem.level.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (isMastered) {
                        Text("✅ Mastered", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.weight(1f))

                if (showFurigana) {
                    Text(currentItem.kana, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(currentItem.kanji, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

                IconButton(onClick = { onSpeak(currentItem.kanji) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Audio", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                }

                AnimatedVisibility(visible = isFlipped) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(currentItem.romaji, fontSize = 16.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(currentItem.meaning, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Hindi Hint: ${currentItem.hindiHint}", fontSize = 15.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).clickable { onSpeak(currentItem.exampleJp) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(currentItem.exampleJp, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(currentItem.exampleRomaji, fontSize = 12.sp, color = Color.Gray)
                                Text(currentItem.exampleEn, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (!isFlipped) {
                    Spacer(Modifier.height(16.dp))
                    Text("Tap card to reveal meaning & example", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onGradeCard(currentItem.id, false)
                    isFlipped = false
                    currentIndex = (currentIndex + 1) % filteredList.size
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Again / Hard")
            }
            Button(
                onClick = {
                    onGradeCard(currentItem.id, true)
                    isFlipped = false
                    currentIndex = (currentIndex + 1) % filteredList.size
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Good / Easy (+15 XP)")
            }
        }
    }
}

@Composable
fun GrammarDojoScreen(selectedLevel: JlptLevel?, onSpeak: (String) -> Unit) {
    val list = remember(selectedLevel) {
        if (selectedLevel == null) DataRepository.grammarList
        else DataRepository.grammarList.filter { it.level == selectedLevel }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { item ->
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
                        Text(item.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Surface(
                            color = Color(item.level.badgeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                item.level.label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Formula: ${item.structure}",
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(item.explanation, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("🇮🇳 Hindi / SOV Parallel: ${item.hindiParallel}", fontSize = 13.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(10.dp))
                    item.examples.forEach { (jp, romaji, en) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .clickable { onSpeak(jp) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(jp, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(romaji, fontSize = 12.sp, color = Color.Gray)
                                Text(en, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuizArenaScreen(
    selectedLevel: JlptLevel?,
    onSpeak: (String) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var questions by remember(selectedLevel) { mutableStateOf(DataRepository.generateQuizQuestions(selectedLevel)) }
    var qIndex by remember(questions) { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var isQuizFinished by remember(questions) { mutableStateOf(false) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions available for this filter.")
        }
        return
    }

    if (isQuizFinished) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉 Test Complete!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text("Score: $score / ${questions.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                questions = DataRepository.generateQuizQuestions(selectedLevel)
                qIndex = 0
                score = 0
                selectedOption = null
                isQuizFinished = false
            }) {
                Text("Start New Mock Test")
            }
        }
        return
    }

    val currentQ = questions[qIndex]

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Question ${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.Bold)
            Text("Score: $score", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
        }

        LinearProgressIndicator(
            progress = { (qIndex + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentQ.subPrompt, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onSpeak(currentQ.audioText) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(currentQ.prompt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        currentQ.options.forEachIndexed { idx, opt ->
            val isSelected = selectedOption == idx
            val isCorrect = idx == currentQ.correctIndex
            val bgColor = when {
                selectedOption == null -> Color.White
                isCorrect -> Color(0xFFD1FAE5)
                isSelected && !isCorrect -> Color(0xFFFEE2E2)
                else -> Color.White
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedOption == null) {
                        selectedOption = idx
                        if (idx == currentQ.correctIndex) {
                            score++
                            onAwardXp(10)
                        }
                        onSpeak(currentQ.audioText)
                    }
            ) {
                Text(
                    text = "${('A' + idx)}. $opt",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                    fontWeight = if (isSelected || (selectedOption != null && isCorrect)) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        if (selectedOption != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (selectedOption == currentQ.correctIndex) "✅ 正解 (Correct! +10 XP)" else "❌ 不正解 (Incorrect)",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedOption == currentQ.correctIndex) Color(0xFF047857) else Color(0xFFC62828)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(currentQ.explanation, fontSize = 13.sp)
                }
            }

            Button(
                onClick = {
                    selectedOption = null
                    if (qIndex + 1 < questions.size) {
                        qIndex++
                    } else {
                        isQuizFinished = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (qIndex + 1 < questions.size) "Next Question →" else "Finish Test")
            }
        }
    }
}
