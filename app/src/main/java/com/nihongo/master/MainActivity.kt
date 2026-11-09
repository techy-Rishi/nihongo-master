@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nihongo.master.ui.GrammarLabScreen
import com.nihongo.master.ui.HomeDashboardScreen
import com.nihongo.master.ui.ListeningLabScreen
import com.nihongo.master.ui.NihongoMasterTheme
import com.nihongo.master.ui.SpeakingLabScreen
import com.nihongo.master.ui.WritingLabScreen
import java.util.Locale

// =========================================================================
// NAVIGATION SECTIONS
// =========================================================================
enum class AppSection(val label: String, val hiLabel: String, val icon: ImageVector) {
    HOME("Home", "होम & SRS", Icons.Default.Home),
    SPEAKING("Speak", "बोलना", Icons.Default.Mic),
    LISTENING("Listen", "सुनना & शब्द", Icons.Default.Headphones),
    WRITING("Write", "लिखना", Icons.Default.Brush),
    GRAMMAR("Grammar", "व्याकरण", Icons.Default.MenuBook)
}

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
                    onStartListening = { onResult ->
                        requestAndStartListening(onResult)
                    },
                    onStopListening = {
                        stopJapaneseSpeechRecognition()
                    }
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
        lastRecognizedText = "" // clear stale "You said" text
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningState = true
                }

                override fun onBeginningOfSpeech() {
                    isListeningState = true
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListeningState = false
                }

                override fun onError(error: Int) {
                    isListeningState = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> onResult("") // genuinely nothing heard
                        else -> Toast.makeText(
                            this@MainActivity,
                            "Speech recognition error ($error). Internet / Google app check karein aur dobara try karein.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListeningState = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull().orEmpty()
                    lastRecognizedText = spoken
                    onResult(spoken)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        lastRecognizedText = partial
                    }
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
// MAIN APP SHELL
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
    // Default = N5 (saved); "ALL" is stored as null
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

    fun addXp(amount: Int) {
        xp += amount
        prefs.edit().putInt("xp", xp).apply()
    }

    fun handleSrsGrade(itemId: String, grade: SrsGrade) {
        val currentCard = srsStates[itemId] ?: SrsCardState(itemId = itemId)
        val updatedCard = SrsEngine.review(currentCard, grade)
        val nextMap = srsStates.toMutableMap().apply { put(itemId, updatedCard) }
        srsStates = nextMap
        saveSrsMap(prefs, nextMap)

        // "Mastered" = remembered well AND interval already >= 7 days (not just one "Good")
        val nextMastered = when {
            grade == SrsGrade.AGAIN || grade == SrsGrade.HARD -> masteredIds - itemId
            updatedCard.intervalDays >= 7 -> masteredIds + itemId
            else -> masteredIds
        }
        masteredIds = nextMastered
        prefs.edit().putStringSet("mastered", nextMastered).apply()
        addXp(grade.xpReward)
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
                            Text(
                                text = "日本語 Master Pro",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Speaker • Listener • Writer (N5–N1)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "🔥 $streak",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Surface(
                                color = Color(0xFFFCE4EC),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "⚡ $xp XP",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFBC002D)
                                )
                            }
                            IconButton(
                                onClick = onToggleDarkTheme,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary
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
                tonalElevation = 6.dp
            ) {
                AppSection.entries.forEach { sec ->
                    NavigationBarItem(
                        selected = section == sec,
                        onClick = { section = sec },
                        icon = { Icon(sec.icon, contentDescription = sec.label) },
                        label = {
                            Text(
                                text = sec.label,
                                fontSize = 11.sp,
                                fontWeight = if (section == sec) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
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
            LevelFilterHeader(
                selected = selectedLevel,
                onSelect = {
                    selectedLevel = it
                    prefs.edit().putString("level", it?.name ?: "ALL").apply()
                }
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
                            onSpeak = onSpeak
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
                        AppSection.LISTENING -> ListeningLabScreen(
                            level = selectedLevel,
                            srsStates = srsStates,
                            onGradeSrs = ::handleSrsGrade,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.WRITING -> WritingLabScreen(
                            level = selectedLevel,
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

@Composable
fun LevelFilterHeader(selected: JlptLevel?, onSelect: (JlptLevel?) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text("All (N5–N1)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            items(JlptLevel.entries) { lvl ->
                FilterChip(
                    selected = selected == lvl,
                    onClick = { onSelect(lvl) },
                    label = {
                        Text(
                            text = "${lvl.label} · ${lvl.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(lvl.color).copy(alpha = 0.18f),
                        selectedLabelColor = Color(lvl.color)
                    )
                )
            }
        }
    }
}
