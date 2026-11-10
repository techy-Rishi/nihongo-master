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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nihongo.master.ui.*
import java.util.Locale

enum class AppSection(val label: String, val hiLabel: String, val icon: ImageVector) {
    HOME("Chapters", "अध्याय", Icons.Default.Home),
    WRITING("あ Kana", "वर्णमाला", Icons.Default.Translate),
    LISTENING("Sentence", "वाक्य अभ्यास", Icons.Default.Extension),
    SPEAKING("Speak", "बोलना", Icons.Default.Mic),
    GRAMMAR("Grammar", "व्याकरण", Icons.Default.AutoStories)
}

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
                "Microphone permission is required for Speaking practice.",
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
                mutableStateOf(if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", true) else systemDark)
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
                "Speech recognition service is not available on this device.",
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
                            "Speech recognition error ($error). Check internet / Google app.",
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
    var xp by remember { mutableIntStateOf(prefs.getInt("xp", 0)) }
    val streak = remember { updateStreak(prefs) }

    // Global App Settings: Language (STRICTLY English OR Hindi) & Romaji ON/OFF
    var appLanguage by remember {
        mutableStateOf(
            runCatching { AppLanguage.valueOf(prefs.getString("app_lang", "ENGLISH") ?: "ENGLISH") }
                .getOrDefault(AppLanguage.ENGLISH)
        )
    }
    var showRomaji by remember {
        mutableStateOf(prefs.getBoolean("show_romaji", true))
    }
    var showGlobalSettings by remember { mutableStateOf(false) }

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

        val nextMastered = when {
            grade == SrsGrade.AGAIN || grade == SrsGrade.HARD -> masteredIds - itemId
            updatedCard.intervalDays >= 7 -> masteredIds + itemId
            else -> masteredIds
        }
        masteredIds = nextMastered
        prefs.edit().putStringSet("mastered", nextMastered).apply()
        addXp(grade.xpReward)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalShowRomaji provides showRomaji
    ) {
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DuoColors.FeatherGreen)
                                        .clickable { section = AppSection.HOME },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("日", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                                }
                                Column {
                                    Text(
                                        text = "Nihongo Master N5",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "${if (appLanguage == AppLanguage.HINDI) "हिन्दी" else "English"} • ${if (showRomaji) "Romaji ON" else "Romaji OFF"}",
                                        fontSize = 11.sp,
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
                                    onClick = { showGlobalSettings = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
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
                                Text(
                                    text = if (appLanguage == AppLanguage.HINDI) sec.hiLabel else sec.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (selected) DuoColors.FeatherGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = section, label = "section_crossfade") { activeSection ->
                    when (activeSection) {
                        AppSection.HOME -> HomeDashboardScreen(
                            xp = xp,
                            streak = streak,
                            level = JlptLevel.N5,
                            srsStates = srsStates,
                            masteredIds = masteredIds,
                            onGradeSrs = ::handleSrsGrade,
                            onNavigate = { section = it },
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.WRITING -> WritingLabScreen(
                            level = JlptLevel.N5,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.LISTENING -> ListeningLabScreen(
                            level = JlptLevel.N5,
                            srsStates = srsStates,
                            onGradeSrs = ::handleSrsGrade,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.SPEAKING -> SpeakingLabScreen(
                            level = JlptLevel.N5,
                            isRecording = isRecording,
                            lastSpokenText = lastSpokenText,
                            onStartListening = onStartListening,
                            onStopListening = onStopListening,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                        AppSection.GRAMMAR -> GrammarLabScreen(
                            level = JlptLevel.N5,
                            onSpeak = onSpeak,
                            onAwardXp = ::addXp
                        )
                    }
                }
            }
        }

        if (showGlobalSettings) {
            DuoLessonSettingsDialog(
                appLanguage = appLanguage,
                onSelectLanguage = { newLang ->
                    appLanguage = newLang
                    prefs.edit().putString("app_lang", newLang.name).apply()
                },
                showRomaji = showRomaji,
                onToggleRomaji = { enabled ->
                    showRomaji = enabled
                    prefs.edit().putBoolean("show_romaji", enabled).apply()
                },
                darkTheme = darkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onDismiss = { showGlobalSettings = false }
            )
        }
    }
}
