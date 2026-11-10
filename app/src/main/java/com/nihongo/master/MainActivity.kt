@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nihongo.master

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nihongo.master.ui.*
import java.util.Locale

// =========================================================================
// NAVIGATION SECTIONS (Ordered like Duolingo: Home -> あ Kana -> Practice)
// =========================================================================
enum class AppSection(val label: String, val hiLabel: String, val icon: ImageVector) {
    HOME("Home", "पाथ & SRS", Icons.Default.Home),
    WRITING("あ Kana", "वर्णमाला & कांजी", Icons.Default.Translate),
    LISTENING("Build", "वाक्य & सुनना", Icons.Default.Extension),
    SPEAKING("Speak", "बोलना", Icons.Default.Mic),
    GRAMMAR("Rules", "व्याकरण", Icons.Default.AutoStories)
}

// =========================================================================
// DUOLINGO-STYLE KANA LESSON MODELS (Beginner Step-by-Step Kana Gates)
// =========================================================================
private data class KanaWordMini(
    val kana: String,
    val romaji: String,
    val meaningEn: String,
    val meaningHi: String,
    val emoji: String,
    val tiles: List<String>
)

private data class KanaLessonGate(
    val id: String,
    val gateNumber: Int,
    val title: String,
    val subtitleHi: String,
    val chars: List<Triple<String, String, String>>, // (Kana, Romaji, Hindi Sound)
    val miniWords: List<KanaWordMini>
)

private val KANA_GATES = listOf(
    KanaLessonGate(
        id = "gate_1",
        gateNumber = 1,
        title = "Gate 1: 5 Magic Vowels (あいうえお)",
        subtitleHi = "जापानी के 5 मूल स्वर और उनसे बनने वाले पहले शब्द",
        chars = listOf(
            Triple("あ", "a", "अ"),
            Triple("い", "i", "इ"),
            Triple("う", "u", "उ"),
            Triple("え", "e", "ए"),
            Triple("お", "o", "ओ")
        ),
        miniWords = listOf(
            KanaWordMini("あい", "ai", "Love", "प्यार", "❤️", listOf("あ", "い", "う", "え")),
            KanaWordMini("いえ", "ie", "House", "घर", "🏠", listOf("い", "え", "あ", "お")),
            KanaWordMini("あお", "ao", "Blue", "नीला", "🔵", listOf("あ", "お", "い", "う")),
            KanaWordMini("うえ", "ue", "Up / Above", "ऊपर", "⬆️", listOf("う", "え", "お", "あ"))
        )
    ),
    KanaLessonGate(
        id = "gate_2",
        gateNumber = 2,
        title = "Gate 2: K-Row (かきくけこ)",
        subtitleHi = "क-वर्ग के अक्षर और रोज़मर्रा के आसान शब्द",
        chars = listOf(
            Triple("か", "ka", "का"),
            Triple("き", "ki", "की"),
            Triple("く", "ku", "कू"),
            Triple("け", "ke", "के"),
            Triple("こ", "ko", "को")
        ),
        miniWords = listOf(
            KanaWordMini("あか", "aka", "Red", "लाल", "🔴", listOf("あ", "か", "き", "こ")),
            KanaWordMini("えき", "eki", "Station", "स्टेशन", "🚉", listOf("え", "き", "く", "け")),
            KanaWordMini("こえ", "koe", "Voice", "आवाज़", "🗣️", listOf("こ", "え", "か", "い")),
            KanaWordMini("あき", "aki", "Autumn", "पतझड़", "🍁", listOf("あ", "き", "く", "こ"))
        )
    ),
    KanaLessonGate(
        id = "gate_3",
        gateNumber = 3,
        title = "Gate 3: S & T Row (さ〜そ / た〜と)",
        subtitleHi = "शि (し), चि (ち), त्सु (つ) और प्रसिद्ध जापानी शब्द",
        chars = listOf(
            Triple("さ", "sa", "सा"),
            Triple("し", "shi", "शी"),
            Triple("す", "su", "सू"),
            Triple("た", "ta", "ता"),
            Triple("ち", "chi", "ची"),
            Triple("つ", "tsu", "त्सु"),
            Triple("て", "te", "ते"),
            Triple("と", "to", "तो")
        ),
        miniWords = listOf(
            KanaWordMini("すし", "sushi", "Sushi", "सुशी", "🍣", listOf("す", "し", "さ", "त")),
            KanaWordMini("かさ", "kasa", "Umbrella", "छाता", "☂️", listOf("か", "さ", "し", "す")),
            KanaWordMini("くつ", "kutsu", "Shoes", "जूते", "👟", listOf("く", "つ", "ち", "て")),
            KanaWordMini("あした", "ashita", "Tomorrow", "कल", "🌅", listOf("あ", "し", "た", "と"))
        )
    ),
    KanaLessonGate(
        id = "gate_4",
        gateNumber = 4,
        title = "Gate 4: N, H & M Row (な〜の / は〜ほ / ま〜も)",
        subtitleHi = "जानवर, प्रकृति और रोज़मर्रा की चीज़ों के नाम",
        chars = listOf(
            Triple("な", "na", "ना"),
            Triple("に", "ni", "नी"),
            Triple("ぬ", "nu", "नू"),
            Triple("ねこ", "neko", "नेको"),
            Triple("は", "ha", "हा"),
            Triple("ふ", "fu", "फू"),
            Triple("ま", "ma", "मा"),
            Triple("み", "mi", "मी")
        ),
        miniWords = listOf(
            KanaWordMini("いぬ", "inu", "Dog", "कुत्ता", "🐕", listOf("い", "ぬ", "ね", "こ")),
            KanaWordMini("ねこ", "neko", "Cat", "बिल्ली", "🐱", listOf("ね", "こ", "い", "ぬ")),
            KanaWordMini("はな", "hana", "Flower", "फूल", "🌸", listOf("は", "な", "ま", "み")),
            KanaWordMini("みみ", "mimi", "Ear", "कान", "👂", listOf("み", "み", "ま", "な"))
        )
    ),
    KanaLessonGate(
        id = "gate_5",
        gateNumber = 5,
        title = "Gate 5: Katakana Loanwords (ア〜コ & カメラ)",
        subtitleHi = "अंग्रेज़ी/विदेशी शब्दों के लिए काताकाना (Katakana) प्रैक्टिस",
        chars = listOf(
            Triple("ア", "a", "अ"),
            Triple("イ", "i", "इ"),
            Triple("ウ", "u", "उ"),
            Triple("エ", "e", "ए"),
            Triple("オ", "o", "ओ"),
            Triple("カ", "ka", "का"),
            Triple("キ", "ki", "की"),
            Triple("コ", "ko", "को")
        ),
        miniWords = listOf(
            KanaWordMini("カメラ", "kamera", "Camera", "कैमरा", "📷", listOf("カ", "メ", "ラ", "コ")),
            KanaWordMini("テレビ", "terebi", "TV", "टीवी", "📺", listOf("テ", "レ", "ビ", "ア")),
            KanaWordMini("パン", "pan", "Bread", "ब्रेड", "🍞", listOf("パ", "ン", "カ", "イ")),
            KanaWordMini("インド", "indo", "India", "भारत", "🇮🇳", listOf("イ", "ン", "ド", "ア"))
        )
    )
)

// =========================================================================
// MAIN ACTIVITY (Owns 3-Speed TTS & Native Japanese SpeechRecognizer)
// =========================================================================
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningState by mutableStateOf(false)
    private var lastRecognizedText by mutableStateOf("")
    private var pendingSpeechCallback: ((String) -> Unit)? = null

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingSpeechCallback?.let { startJapaneseSpeechRecognition(it) }
        } else {
            Toast.makeText(
                this,
                "Speaking Lab के लिए Microphone permission ज़रूरी है।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            var darkTheme by remember {
                mutableStateOf(if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else systemDark)
            }

            NihongoMasterTheme(darkTheme = darkTheme) {
                NihongoApp(
                    darkTheme = darkTheme,
                    onToggleDarkTheme = {
                        darkTheme = !darkTheme
                        prefs.edit().putBoolean("dark_mode", darkTheme).apply()
                    },
                    isRecording = isListeningState,
                    lastSpokenText = lastRecognizedText,
                    onSpeak = { text, speed -> speakJapanese(text, speed) },
                    onStartListening = { onResult -> requestAndStartListening(onResult) },
                    onStopListening = { stopJapaneseSpeechRecognition() }
                )
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

    private fun speakJapanese(text: String, speed: ListeningSpeed = ListeningSpeed.NATURAL) {
        if (!ttsReady) {
            Toast.makeText(
                this,
                "Japanese voice not found. Enable Japanese in Settings > Text-to-speech.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(speed.rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nihongo_tts_${System.currentTimeMillis()}")
    }

    private fun requestAndStartListening(onResult: (String) -> Unit) {
        pendingSpeechCallback = onResult
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startJapaneseSpeechRecognition(onResult)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startJapaneseSpeechRecognition(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition service इस डिवाइस पर उपलब्ध नहीं है।",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        tts?.stop()
        lastRecognizedText = ""
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListeningState = true }
                override fun onBeginningOfSpeech() { isListeningState = true }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningState = false }

                override fun onError(error: Int) {
                    isListeningState = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> onResult("")
                        else -> Toast.makeText(
                            this@MainActivity,
                            "Speech error ($error). Google app / internet check karein.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListeningState = false
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    lastRecognizedText = spoken
                    onResult(spoken)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) lastRecognizedText = partial
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopJapaneseSpeechRecognition() {
        isListeningState = false
        speechRecognizer?.stopListening()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// =========================================================================
// STREAK & SRS STORAGE HELPERS
// =========================================================================
private fun updateStreak(prefs: SharedPreferences): Int {
    val today = SrsEngine.currentEpochDay()
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

private fun loadSrsMap(prefs: SharedPreferences): Map<String, SrsCardState> {
    val rawSet = prefs.getStringSet("srs_cards_v2", emptySet()).orEmpty()
    val out = mutableMapOf<String, SrsCardState>()
    for (entry in rawSet) {
        SrsEngine.deserialize(entry)?.let { out[it.itemId] = it }
    }
    return out
}

private fun saveSrsMap(prefs: SharedPreferences, map: Map<String, SrsCardState>) {
    val serialized = map.values.map { SrsEngine.serialize(it) }.toSet()
    prefs.edit().putStringSet("srs_cards_v2", serialized).apply()
}

// =========================================================================
// MAIN DUOLINGO-STYLE APP SHELL
// =========================================================================
@Composable
fun NihongoApp(
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    isRecording: Boolean,
    lastSpokenText: String,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onStartListening: ((String) -> Unit) -> Unit,
    onStopListening: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nihongo_master", Context.MODE_PRIVATE) }

    var section by remember { mutableStateOf(AppSection.HOME) }
    var selectedLevel by remember {
        mutableStateOf<JlptLevel?>(
            runCatching { JlptLevel.valueOf(prefs.getString("level", "N5") ?: "N5") }.getOrNull()
        )
    }
    var xp by remember { mutableIntStateOf(prefs.getInt("xp", 0)) }
    val streak = remember { updateStreak(prefs) }

    var srsStates by remember { mutableStateOf(loadSrsMap(prefs)) }
    var masteredIds by remember {
        mutableStateOf(prefs.getStringSet("mastered", emptySet())?.toSet() ?: emptySet())
    }
    var completedKanaGates by remember {
        mutableStateOf(prefs.getStringSet("kana_gates_done", emptySet())?.toSet() ?: emptySet())
    }

    // Active Duolingo-style Kana Gate Session (null = not in full-screen lesson)
    var activeKanaGate by remember { mutableStateOf<KanaLessonGate?>(null) }

    fun addXp(amount: Int) {
        xp += amount
        prefs.edit().putInt("xp", xp).apply()
    }

    fun markKanaGateComplete(gateId: String) {
        completedKanaGates = completedKanaGates + gateId
        prefs.edit().putStringSet("kana_gates_done", completedKanaGates).apply()
        addXp(35)
    }

    fun handleSrsGrade(itemId: String, grade: SrsGrade) {
        val currentCard = srsStates[itemId] ?: SrsCardState(itemId = itemId)
        val updatedCard = SrsEngine.review(currentCard, grade)
        val nextMap = srsStates.toMutableMap().apply { put(itemId, updatedCard) }
        srsStates = nextMap
        saveSrsMap(prefs, nextMap)

        val nextMastered = when {
            grade == SrsGrade.AGAIN || grade == SrsGrade.HARD -> masteredIds - itemId
            updatedCard.intervalDays >= 7 -> masteredIds + itemId
            else -> masteredIds
        }
        masteredIds = nextMastered
        prefs.edit().putStringSet("mastered", nextMastered).apply()
        addXp(grade.xpReward)
    }

    // If a Duolingo Kana Gate Lesson is active, render full-screen interactive lesson
    activeKanaGate?.let { gate ->
        DuolingoKanaLessonSession(
            gate = gate,
            onSpeak = onSpeak,
            onClose = { activeKanaGate = null },
            onCompleteGate = {
                markKanaGateComplete(gate.id)
                activeKanaGate = null
            }
        )
        return
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DuoColors.FeatherGreen)
                                    .clickable { section = AppSection.WRITING },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("あ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "Nihongo DuoPro",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Kana • Words • SOV Grammar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DuoColors.MacawBlue
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = DuoColors.FoxOrangeBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "🔥 $streak",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = DuoColors.FoxOrangeDark
                                )
                            }
                            Surface(
                                color = DuoColors.BeeGoldBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "⚡ $xp",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9A6700)
                                )
                            }
                            IconButton(
                                onClick = onToggleDarkTheme,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = DuoColors.MacawBlue
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppSection.entries.forEach { sec ->
                    val selected = section == sec
                    NavigationBarItem(
                        selected = selected,
                        onClick = { section = sec },
                        icon = {
                            Icon(
                                imageVector = sec.icon,
                                contentDescription = sec.label,
                                tint = if (selected) DuoColors.FeatherGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = sec.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (selected) DuoColors.FeatherGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = sec.hiLabel,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Level Filter + Duolingo Kana Quick-Gate Bar
            LevelFilterAndKanaGateHeader(
                selected = selectedLevel,
                completedGatesCount = completedKanaGates.size,
                totalGatesCount = KANA_GATES.size,
                showKanaGatesStrip = (section == AppSection.WRITING || selectedLevel == JlptLevel.N5 || selectedLevel == null),
                completedGates = completedKanaGates,
                onSelectLevel = {
                    selectedLevel = it
                    prefs.edit().putString("level", it?.name ?: "ALL").apply()
                },
                onLaunchKanaGate = { gate -> activeKanaGate = gate }
            )

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = section, label = "section_crossfade") { activeSection ->
                    when (activeSection) {
                        AppSection.HOME -> HomeDashboardScreen(
                            xp = xp,
                            streak = streak,
                            level = selectedLevel,
                            srsStates = srsStates,
                            masteredIds = masteredIds,
                            onGradeSrs = ::handleSrsGrade,
                            onNavigate = { section = it },
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.WRITING -> WritingLabScreen(
                            level = selectedLevel,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.LISTENING -> ListeningLabScreen(
                            level = selectedLevel,
                            srsStates = srsStates,
                            onGradeSrs = ::handleSrsGrade,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.SPEAKING -> SpeakingLabScreen(
                            level = selectedLevel,
                            isRecording = isRecording,
                            lastSpokenText = lastSpokenText,
                            onStartListening = onStartListening,
                            onStopListening = onStopListening,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.GRAMMAR -> GrammarLabScreen(
                            level = selectedLevel,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// TOP LEVEL FILTER + DUOLINGO "LEARN THE KANA" GATE LAUNCHER STRIP
// =========================================================================
@Composable
private fun LevelFilterAndKanaGateHeader(
    selected: JlptLevel?,
    completedGatesCount: Int,
    totalGatesCount: Int,
    showKanaGatesStrip: Boolean,
    completedGates: Set<String>,
    onSelectLevel: (JlptLevel?) -> Unit,
    onLaunchKanaGate: (KanaLessonGate) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { onSelectLevel(null) },
                        label = { Text("All (N5–N1)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                    )
                }
                items(JlptLevel.entries) { lvl ->
                    FilterChip(
                        selected = selected == lvl,
                        onClick = { onSelectLevel(lvl) },
                        label = {
                            Text(
                                text = "${lvl.label} · ${lvl.title}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(lvl.color).copy(alpha = 0.2f),
                            selectedLabelColor = Color(lvl.color)
                        )
                    )
                }
            }

            // Duolingo-Style Beginner Kana Gates Strip
            AnimatedVisibility(visible = showKanaGatesStrip) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DuoColors.FeatherGreenBg.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇯🇵 Duolingo Kana Gates (Tap to Start Interactive Lesson)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.FeatherGreenDark
                        )
                        Text(
                            text = "$completedGatesCount / $totalGatesCount Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnPastel
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(KANA_GATES) { gate ->
                            val isDone = gate.id in completedGates
                            val topCol = if (isDone) DuoColors.BeeGold else DuoColors.FeatherGreen
                            val lipCol = if (isDone) DuoColors.BeeGoldDark else DuoColors.FeatherGreenDark
                            Duo3DButton(
                                text = "${if (isDone) "⭐" else "▶️"} Gate ${gate.gateNumber}: ${gate.chars.take(3).joinToString("") { it.first }}",
                                onClick = { onLaunchKanaGate(gate) },
                                topColor = topCol,
                                lipColor = lipCol,
                                textColor = if (isDone) OnPastel else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// FULL-SCREEN DUOLINGO KANA LESSON ENGINE
// Step 1: Meet the Kana (Audio + Hindi Phonetics)
// Step 2: Audio -> Kana Recognition
// Step 3: Tap the Matching Pairs (Duolingo Pair Match Game!)
// Step 4: Build Beginner Words with Kana Tiles (e.g. す + し = すし)
// =========================================================================
@Composable
private fun DuolingoKanaLessonSession(
    gate: KanaLessonGate,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onClose: () -> Unit,
    onCompleteGate: () -> Unit
) {
    val totalSteps = 4
    var currentStep by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }

    // Step 1 state: Listen & Pick
    var listenRound by remember { mutableIntStateOf(0) }
    val targetListenChar = remember(gate, listenRound) { gate.chars[listenRound % gate.chars.size] }
    val listenOptions = remember(gate, targetListenChar) {
        val wrong = gate.chars.filter { it.first != targetListenChar.first }.shuffled().take(3)
        (wrong + targetListenChar).shuffled()
    }
    var selectedListenOption by remember(listenRound) { mutableStateOf<String?>(null) }

    // Step 2 state: Duolingo Pair Match
    val pairSubset = remember(gate) { gate.chars.take(4) }
    val leftKanaList = remember(pairSubset) { pairSubset.shuffled() }
    val rightRomajiList = remember(pairSubset) { pairSubset.shuffled() }
    var selectedLeftKana by remember { mutableStateOf<String?>(null) }
    var selectedRightRomaji by remember { mutableStateOf<String?>(null) }
    var matchedKanaSet by remember { mutableStateOf(setOf<String>()) }

    // Step 3 state: Beginner Kana Word Builder
    var wordIndex by remember { mutableIntStateOf(0) }
    val currentMiniWord = gate.miniWords[wordIndex.coerceIn(0, gate.miniWords.lastIndex)]
    var builtWordTiles by remember(wordIndex) { mutableStateOf(listOf<String>()) }
    var wordChecked by remember(wordIndex) { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    LinearProgressIndicator(
                        progress = { (currentStep + 1).toFloat() / totalSteps },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(CircleShape),
                        color = DuoColors.FeatherGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Surface(color = DuoColors.CardinalRedBg, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "❤️ $hearts",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = DuoColors.CardinalRedDark
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
                text = gate.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = DuoColors.MacawBlue
            )
            Text(
                text = "🇮🇳 ${gate.subtitleHi}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (currentStep) {
                // STEP 0: MEET THE KANA CARDS WITH INSTANT AUDIO
                0 -> {
                    Duo3DCard(
                        containerColor = DuoColors.BeeGoldBg,
                        borderColor = DuoColors.BeeGold,
                        lipColor = DuoColors.BeeGoldDark
                    ) {
                        Text(
                            text = "👆 Step 1/4: हर अक्षर पर टैप करके उसकी जापानी आवाज़ और हिन्दी उच्चारण सुनें!",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = OnPastel
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        gate.chars.forEach { (kana, romaji, hi) ->
                            Duo3DCard(
                                modifier = Modifier.width(102.dp),
                                borderColor = DuoColors.MacawBlue,
                                lipColor = DuoColors.MacawBlueDark,
                                onClick = { onSpeak(kana, ListeningSpeed.NATURAL) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🔊", fontSize = 12.sp)
                                    Text(kana, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(romaji, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DuoColors.MacawBlue)
                                    Text("🇮🇳 $hi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DuoColors.FeatherGreenDark)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Duo3DButton(
                        text = "I'M READY FOR SOUND QUIZ →",
                        onClick = {
                            currentStep = 1
                            onSpeak(targetListenChar.first, ListeningSpeed.NATURAL)
                        },
                        topColor = DuoColors.FeatherGreen,
                        lipColor = DuoColors.FeatherGreenDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // STEP 1: LISTEN & PICK THE KANA
                1 -> {
                    Duo3DCard(
                        containerColor = DuoColors.MacawBlueBg,
                        borderColor = DuoColors.MacawBlue,
                        lipColor = DuoColors.MacawBlueDark,
                        onClick = { onSpeak(targetListenChar.first, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Step 2/4: What sound do you hear?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlueDark
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("🔊", fontSize = 48.sp)
                            Text(
                                text = "\"${targetListenChar.second}\" (🇮🇳 ${targetListenChar.third})",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPastel
                            )
                            Text("Tap to replay audio", fontSize = 11.sp, color = DuoColors.MacawBlueDark)
                        }
                    }

                    val rows = listenOptions.chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { item ->
                                val isSel = selectedListenOption == item.first
                                val isCorrect = item.first == targetListenChar.first
                                val bg = when {
                                    selectedListenOption == null -> MaterialTheme.colorScheme.surface
                                    isCorrect -> DuoColors.FeatherGreenBg
                                    isSel -> DuoColors.CardinalRedBg
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val border = when {
                                    selectedListenOption == null -> DuoColors.MacawBlue
                                    isCorrect -> DuoColors.FeatherGreen
                                    isSel -> DuoColors.CardinalRed
                                    else -> DuoColors.LightBorder
                                }

                                Duo3DCard(
                                    modifier = Modifier.weight(1f),
                                    containerColor = bg,
                                    borderColor = border,
                                    lipColor = border,
                                    onClick = {
                                        if (selectedListenOption == null) {
                                            selectedListenOption = item.first
                                            onSpeak(item.first, ListeningSpeed.NATURAL)
                                            if (!isCorrect && hearts > 1) hearts--
                                        }
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = item.first,
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selectedListenOption != null && (isCorrect || isSel)) OnPastel else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedListenOption != null) {
                        Duo3DButton(
                            text = if (listenRound < 2) "NEXT SOUND →" else "GO TO PAIR MATCH GAME →",
                            onClick = {
                                if (listenRound < 2) {
                                    listenRound++
                                } else {
                                    currentStep = 2
                                }
                            },
                            topColor = DuoColors.FeatherGreen,
                            lipColor = DuoColors.FeatherGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // STEP 2: DUOLINGO TAP-THE-MATCHING-PAIRS GAME
                2 -> {
                    Duo3DCard(
                        containerColor = DuoColors.LilacPurpleBg,
                        borderColor = DuoColors.LilacPurple,
                        lipColor = DuoColors.LilacPurpleDark
                    ) {
                        Text(
                            text = "🔗 Step 3/4: Tap the Matching Pairs! बाएं से जापानी अक्षर और दाएं से उसकी आवाज़ मिलाएं।",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = OnPastel
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Kana
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            leftKanaList.forEach { (kana, _, _) ->
                                val isMatched = kana in matchedKanaSet
                                val isSelected = selectedLeftKana == kana
                                Duo3DCard(
                                    containerColor = when {
                                        isMatched -> DuoColors.FeatherGreenBg
                                        isSelected -> DuoColors.MacawBlueBg
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    borderColor = when {
                                        isMatched -> DuoColors.FeatherGreen
                                        isSelected -> DuoColors.MacawBlue
                                        else -> DuoColors.LightBorder
                                    },
                                    lipColor = if (isMatched) DuoColors.FeatherGreenDark else DuoColors.MacawBlueDark,
                                    onClick = {
                                        if (!isMatched) {
                                            onSpeak(kana, ListeningSpeed.NATURAL)
                                            selectedLeftKana = kana
                                            if (selectedRightRomaji == kana) {
                                                matchedKanaSet = matchedKanaSet + kana
                                                selectedLeftKana = null
                                                selectedRightRomaji = null
                                            } else if (selectedRightRomaji != null) {
                                                selectedRightRomaji = null
                                            }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isMatched) "✅ $kana" else kana,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isMatched || isSelected) OnPastel else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Right Column: Romaji + Hindi Sound
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rightRomajiList.forEach { (kanaKey, romaji, hi) ->
                                val isMatched = kanaKey in matchedKanaSet
                                val isSelected = selectedRightRomaji == kanaKey
                                Duo3DCard(
                                    containerColor = when {
                                        isMatched -> DuoColors.FeatherGreenBg
                                        isSelected -> DuoColors.MacawBlueBg
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    borderColor = when {
                                        isMatched -> DuoColors.FeatherGreen
                                        isSelected -> DuoColors.MacawBlue
                                        else -> DuoColors.LightBorder
                                    },
                                    lipColor = if (isMatched) DuoColors.FeatherGreenDark else DuoColors.MacawBlueDark,
                                    onClick = {
                                        if (!isMatched) {
                                            selectedRightRomaji = kanaKey
                                            if (selectedLeftKana == kanaKey) {
                                                matchedKanaSet = matchedKanaSet + kanaKey
                                                selectedLeftKana = null
                                                selectedRightRomaji = null
                                            } else if (selectedLeftKana != null) {
                                                selectedLeftKana = null
                                            }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$romaji (🇮🇳 $hi)",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isMatched || isSelected) OnPastel else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchedKanaSet.size == pairSubset.size) {
                        Duo3DButton(
                            text = "🎉 ALL MATCHED! BUILD WORDS →",
                            onClick = { currentStep = 3 },
                            topColor = DuoColors.FeatherGreen,
                            lipColor = DuoColors.FeatherGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // STEP 3: BUILD REAL BEGINNER WORDS USING KANA TILES
                else -> {
                    Duo3DCard(
                        borderColor = DuoColors.FoxOrange,
                        lipColor = DuoColors.FoxOrangeDark,
                        onClick = { onSpeak(currentMiniWord.kana, ListeningSpeed.NATURAL) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Step 4/4: Build this Japanese Word (${wordIndex + 1}/${gate.miniWords.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.FoxOrangeDark
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(currentMiniWord.emoji, fontSize = 44.sp)
                            Text(
                                text = "${currentMiniWord.meaningEn} • 🇮🇳 ${currentMiniWord.meaningHi}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "🔊 Sound Hint: \"${currentMiniWord.romaji}\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoColors.MacawBlue
                            )
                        }
                    }

                    // Answer Box
                    Duo3DCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        borderColor = DuoColors.MacawBlue,
                        lipColor = DuoColors.MacawBlueDark,
                        onClick = {
                            if (wordChecked == null && builtWordTiles.isNotEmpty()) {
                                builtWordTiles = builtWordTiles.dropLast(1)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (builtWordTiles.isEmpty()) {
                                Text(
                                    text = "👇 नीचे के Kana अक्षरों को जोड़कर शब्द बनाएं...",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Text(
                                    text = builtWordTiles.joinToString(""),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoColors.MacawBlue
                                )
                            }
                        }
                    }

                    // Kana Tile Choices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentMiniWord.tiles.forEach { tileChar ->
                            Duo3DCard(
                                modifier = Modifier.weight(1f),
                                borderColor = DuoColors.FeatherGreen,
                                lipColor = DuoColors.FeatherGreenDark,
                                onClick = {
                                    if (wordChecked == null) {
                                        onSpeak(tileChar, ListeningSpeed.NATURAL)
                                        builtWordTiles = builtWordTiles + tileChar
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tileChar, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    if (wordChecked == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Duo3DButton(
                                text = "🔄 Clear",
                                onClick = { builtWordTiles = emptyList() },
                                topColor = MaterialTheme.colorScheme.surfaceVariant,
                                lipColor = DuoColors.LightLip,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.35f)
                            )
                            Duo3DButton(
                                text = "CHECK WORD",
                                onClick = {
                                    val userWord = builtWordTiles.joinToString("")
                                    wordChecked = (userWord == currentMiniWord.kana)
                                    onSpeak(currentMiniWord.kana, ListeningSpeed.NATURAL)
                                },
                                enabled = builtWordTiles.isNotEmpty(),
                                topColor = DuoColors.FeatherGreen,
                                lipColor = DuoColors.FeatherGreenDark,
                                modifier = Modifier.weight(0.65f)
                            )
                        }
                    } else {
                        val passed = wordChecked == true
                        Duo3DCard(
                            containerColor = if (passed) DuoColors.FeatherGreenBg else DuoColors.CardinalRedBg,
                            borderColor = if (passed) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                            lipColor = if (passed) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (passed) "✅ शानदार! ${currentMiniWord.kana} = ${currentMiniWord.meaningHi}"
                                    else "❌ सही शब्द: ${currentMiniWord.kana} (${currentMiniWord.romaji})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = OnPastel
                                )
                                Spacer(Modifier.height(8.dp))
                                Duo3DButton(
                                    text = if (wordIndex + 1 < gate.miniWords.size) "NEXT WORD →" else "🏆 COMPLETE KANA GATE (+35 XP)",
                                    onClick = {
                                        if (wordIndex + 1 < gate.miniWords.size) {
                                            wordIndex++
                                        } else {
                                            onCompleteGate()
                                        }
                                    },
                                    topColor = if (passed) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                                    lipColor = if (passed) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark,
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
