@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.AppLanguage
import com.nihongo.master.DataRepository

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalShowRomaji = compositionLocalOf { true }
val LocalDarkTheme = compositionLocalOf { false }

object DuoColors {
    val FeatherGreen = Color(0xFF10B981)
    val FeatherGreenDark = Color(0xFF059669)
    val FeatherGreenBg = Color(0xFFD1FAE5)

    val MacawBlue = Color(0xFF0EA5E9)
    val MacawBlueDark = Color(0xFF0284C7)
    val MacawBlueBg = Color(0xFFE0F2FE)

    val LilacPurple = Color(0xFF8B5CF6)
    val LilacPurpleDark = Color(0xFF6D28D9)
    val LilacPurpleBg = Color(0xFFEDE9FE)

    val FoxOrange = Color(0xFFF59E0B)
    val FoxOrangeDark = Color(0xFFD97706)
    val FoxOrangeBg = Color(0xFFFEF3C7)

    val CardinalRed = Color(0xFFF43F5E)
    val CardinalRedDark = Color(0xFFE11D48)
    val CardinalRedBg = Color(0xFFFFE4E6)

    val BeeGold = Color(0xFFFBBF24)
    val BeeGoldDark = Color(0xFFD97706)
    val BeeGoldBg = Color(0xFFFEF9C3)

    // Crisp Light Mode Surfaces
    val LightCanvas = Color(0xFFF8FAFC)
    val LightCard = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFCBD5E1)
    val LightLip = Color(0xFF94A3B8)
    val LightEmptySlot = Color(0xFFE2E8F0)
    val LightTrayBg = Color(0xFFF1F5F9)

    // Sleek Dark Mode Surfaces
    val DarkCanvas = Color(0xFF0D131A)
    val DarkCard = Color(0xFF161F2C)
    val DarkBorder = Color(0xFF2D3D52)
    val DarkLip = Color(0xFF1B2636)
    val DarkDisabledBtn = Color(0xFF1E293B)
    val DarkCorrectBanner = Color(0xFF0D2821)
    val DarkWrongBanner = Color(0xFF2C1219)
}

private val LightColors = lightColorScheme(
    primary = DuoColors.FeatherGreenDark,
    onPrimary = Color.White,
    primaryContainer = DuoColors.FeatherGreenBg,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = DuoColors.MacawBlueDark,
    onSecondary = Color.White,
    secondaryContainer = DuoColors.MacawBlueBg,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = DuoColors.LilacPurpleDark,
    onTertiary = Color.White,
    tertiaryContainer = DuoColors.LilacPurpleBg,
    onTertiaryContainer = Color(0xFF4C1D95),
    background = DuoColors.LightCanvas,
    onBackground = Color(0xFF0F172A),
    surface = DuoColors.LightCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColors = darkColorScheme(
    primary = DuoColors.FeatherGreen,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = DuoColors.FeatherGreenBg,
    secondary = DuoColors.MacawBlue,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = DuoColors.MacawBlueBg,
    tertiary = DuoColors.LilacPurple,
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = DuoColors.LilacPurpleBg,
    background = DuoColors.DarkCanvas,
    onBackground = Color(0xFFF1F5F9),
    surface = DuoColors.DarkCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun NihongoMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}

@Composable
fun subTabColors(selected: Boolean): ButtonColors =
    if (selected) ButtonDefaults.buttonColors(
        containerColor = DuoColors.MacawBlueDark,
        contentColor = Color.White
    )
    else ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

val OnPastel = Color(0xFF0F172A)

// =========================================================================
// SMART JAPANESE WORD-AWARE TOKENIZER & ROMAJI CONVERTER
// Prevents splitting words like "おはよう" into "お + は + よう"!
// =========================================================================
object SmartJapaneseTokenizer {

    private val knownPhrasesSorted: List<String> by lazy {
        val vocabWords = DataRepository.vocab.flatMap { listOf(it.kanji, it.kana) }
        val commonPoliteEndings = listOf(
            "おはようございます", "ありがとうございます", "よろしくお願いします",
            "ごちそうさまでした", "いらっしゃいませ", "かしこまりました",
            "おやすみなさい", "いってきます", "いってらっしゃい", "おかえりなさい",
            "おめでとうございます", "お疲れ様でした", "お疲れ様です",
            "ではありませんでした", "ではありません", "じゃありません",
            "ませんでした", "きましょう", "ましょう", "ました", "ません",
            "ています", "ていました", "てください", "ないでください",
            "てもいいですか", "てはいけません", "たいです", "かったです",
            "おはよう", "こんにちは", "こんばんは", "さようなら", "ありがとう",
            "すみません", "ごめんなさい", "お願いします", "いただきます",
            "ただいま", "元気ですか", "元気", "ですか", "ですね", "ですよ", "です", "ます"
        )
        (vocabWords + commonPoliteEndings)
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
    }

    private val twoCharParticles = setOf("から", "まで", "より", "だけ", "ほど", "でも")
    private val oneCharParticles = setOf('は', 'が', 'を', 'に', 'で', 'へ', 'と', 'も', 'の', 'か', 'ね', 'よ')

    fun tokenize(jp: String): List<String> {
        val clean = jp.replace("。", "").replace("？", "").replace("！", "").trim()
        if (clean.isEmpty()) return emptyList()

        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flushBuffer() {
            if (buffer.isNotEmpty()) {
                tokens.add(buffer.toString())
                buffer.clear()
            }
        }

        var i = 0
        while (i < clean.length) {
            val ch = clean[i]
            if (ch == '、' || ch == ' ' || ch == '　') {
                flushBuffer()
                i++
                continue
            }

            val remaining = clean.substring(i)

            // 1. Check if remaining starts with a known multi-char vocabulary word or polite expression
            val matchedWord = knownPhrasesSorted.firstOrNull { remaining.startsWith(it) }
            if (matchedWord != null) {
                flushBuffer()
                tokens.add(matchedWord)
                i += matchedWord.length
                continue
            }

            // 2. Check two-char particle after a flushed token or non-empty buffer
            val twoChar = if (i + 1 < clean.length) clean.substring(i, i + 2) else ""
            if (twoChar in twoCharParticles && (buffer.isNotEmpty() || tokens.isNotEmpty())) {
                flushBuffer()
                tokens.add(twoChar)
                i += 2
                continue
            }

            // 3. Check one-char particle
            if (ch in oneCharParticles && (buffer.isNotEmpty() || tokens.isNotEmpty())) {
                flushBuffer()
                tokens.add(ch.toString())
                i++
                continue
            }

            buffer.append(ch)
            i++
        }
        flushBuffer()
        return tokens.ifEmpty { listOf(clean) }
    }
}

object RomajiHelper {
    private val kanaMap: Map<String, String> by lazy {
        DataRepository.allKana.associate { it.char to it.romaji } + mapOf(
            "ー" to "-", "っ" to "", "ッ" to "",
            "は" to "wa", "へ" to "e", "を" to "o",
            "です" to "desu", "ですか" to "desu ka", "ですね" to "desu ne", "ですよ" to "desu yo",
            "ます" to "masu", "ました" to "mashita", "ません" to "masen",
            "ませんでした" to "masen deshita", "ましょう" to "mashou",
            "ください" to "kudasai", "ています" to "te imasu", "てください" to "te kudasai",
            "たいです" to "tai desu", "さん" to "san", "ちゃん" to "chan", "くん" to "kun"
        )
    }

    private val vocabRomajiMap: Map<String, String> by lazy {
        val out = mutableMapOf<String, String>()
        for (v in DataRepository.vocab) {
            out[v.kanji] = v.romaji
            out[v.kana] = v.romaji
        }
        out
    }

    fun toRomaji(token: String): String {
        val clean = token.trim()
        if (clean.isEmpty()) return ""
        vocabRomajiMap[clean]?.let { return it }
        kanaMap[clean]?.let { return it }

        for (v in DataRepository.vocab) {
            if (v.kanji.length >= 2 && clean.startsWith(v.kanji.dropLast(1))) {
                val suffix = clean.removePrefix(v.kanji.dropLast(1))
                val stemRomaji = v.romaji.dropLast(2)
                return stemRomaji + kanaToRomajiRaw(suffix)
            }
            if (clean.startsWith(v.kanji)) {
                val suffix = clean.removePrefix(v.kanji)
                return v.romaji + kanaToRomajiRaw(suffix)
            }
        }
        return kanaToRomajiRaw(clean)
    }

    private fun kanaToRomajiRaw(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (i + 1 < text.length) {
                val two = text.substring(i, i + 2)
                val match2 = DataRepository.allKana.firstOrNull { it.char == two }?.romaji
                if (match2 != null) {
                    sb.append(match2)
                    i += 2
                    continue
                }
            }
            val one = text[i].toString()
            if (one == "っ" || one == "ッ") {
                if (i + 1 < text.length) {
                    val nextR = DataRepository.allKana.firstOrNull { it.char == text[i + 1].toString() }?.romaji
                    if (!nextR.isNullOrEmpty()) sb.append(nextR.first())
                }
                i++
                continue
            }
            val match1 = DataRepository.allKana.firstOrNull { it.char == one }?.romaji
                ?: if (one == "ー") "-" else ""
            sb.append(match1)
            i++
        }
        return sb.toString()
    }
}

// =========================================================================
// CRISP, HIGH-CONTRAST WORD TILE & SENTENCE DOCK (FIXED FOR LIGHT & DARK MODE)
// =========================================================================

@Composable
fun DuoWordTile(
    text: String,
    showRomaji: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholderSlot: Boolean = false,
    isCorrectHighlight: Boolean = false,
    isWrongHighlight: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(14.dp)
    val romaji = if (showRomaji) RomajiHelper.toRomaji(text) else ""
    val isParticle = text in setOf("は", "が", "を", "に", "で", "へ", "と", "の", "も", "から", "まで", "より", "だけ")

    val slotColor = if (isDark) DuoColors.DarkLip else DuoColors.LightEmptySlot
    val borderColor = when {
        isCorrectHighlight -> DuoColors.FeatherGreen
        isWrongHighlight -> DuoColors.CardinalRed
        isParticle -> DuoColors.LilacPurple
        isDark -> DuoColors.DarkBorder
        else -> DuoColors.LightBorder
    }
    val surfaceColor = when {
        isCorrectHighlight -> if (isDark) Color(0xFF0D2821) else DuoColors.FeatherGreenBg
        isWrongHighlight -> if (isDark) Color(0xFF2C1219) else DuoColors.CardinalRedBg
        isDark -> DuoColors.DarkCard
        else -> Color.White
    }
    val mainTextColor = when {
        isCorrectHighlight && !isDark -> Color(0xFF065F46)
        isWrongHighlight && !isDark -> Color(0xFF9F1239)
        isDark -> Color(0xFFF8FAFC)
        else -> Color(0xFF0F172A)
    }

    if (isPlaceholderSlot) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(slotColor)
                .border(1.5.dp, borderColor.copy(alpha = 0.35f), shape)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = if (showRomaji) 8.dp else 11.dp)
                    .alpha(0f)
            ) {
                if (showRomaji && romaji.isNotBlank()) {
                    Text(text = romaji, fontSize = 11.sp)
                }
                Text(text = text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(borderColor)
                .clickable { onClick() }
                .padding(bottom = 3.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(shape)
                    .background(surfaceColor)
                    .border(1.5.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = if (showRomaji) 7.dp else 11.dp)
            ) {
                if (showRomaji && romaji.isNotBlank()) {
                    Text(
                        text = romaji,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isParticle) DuoColors.LilacPurple else DuoColors.MacawBlueDark
                    )
                    Spacer(Modifier.height(1.dp))
                }
                Text(
                    text = text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = mainTextColor
                )
            }
        }
    }
}

@Composable
fun DuoCharacterAudioPrompt(
    characterEmoji: String,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DuoColors.CardinalRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DuoColors.CardinalRed.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "聴解 • LISTEN & ASSEMBLE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.CardinalRed
                    )
                }
                Text(text = characterEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = DuoColors.MacawBlue.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, DuoColors.MacawBlue),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                        .clickable { onPlayNormal() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Native Speed",
                            tint = DuoColors.MacawBlueDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("1.0x Native", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DuoColors.MacawBlueDark)
                            Text("Tap to play", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(56.dp)
                        .clickable { onPlaySlow() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SlowMotionVideo,
                            contentDescription = "Slow Speed",
                            tint = DuoColors.FoxOrangeDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("0.65x Slow", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text("Clear Mora", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuoCharacterSentencePrompt(
    characterEmoji: String,
    promptText: String,
    subHintText: String = "",
    onSpeakPrompt: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSpeakPrompt() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DuoColors.LilacPurple.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "翻訳 • TRANSLATE TO JAPANESE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.LilacPurple
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Play Audio",
                        tint = DuoColors.MacawBlueDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Listen",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlueDark
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = promptText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subHintText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subHintText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DuoColors.MacawBlueDark
                )
            }
        }
    }
}

@Composable
fun DuoRuledAnswerArea(
    selectedTiles: List<Pair<Int, String>>,
    showRomaji: Boolean,
    checkedResult: Boolean?,
    onRemoveTile: (Pair<Int, String>) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val borderColor = when (checkedResult) {
        true -> DuoColors.FeatherGreen
        false -> DuoColors.CardinalRed
        null -> if (isDark) DuoColors.DarkBorder else DuoColors.LightBorder
    }
    val trayBg = if (isDark) Color(0xFF111923) else DuoColors.LightTrayBg

    Surface(
        color = trayBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 122.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR JAPANESE SENTENCE (Tap tile to remove)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedTiles.isNotEmpty()) {
                    Text(
                        text = "${selectedTiles.size} words",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlueDark
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (selectedTiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap word blocks below to form the sentence",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedTiles.forEach { pair ->
                        DuoWordTile(
                            text = pair.second,
                            showRomaji = showRomaji,
                            isCorrectHighlight = (checkedResult == true),
                            isWrongHighlight = (checkedResult == false),
                            onClick = { onRemoveTile(pair) }
                        )
                    }
                }
            }

            if (showRomaji && selectedTiles.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val fullRomaji = selectedTiles.joinToString(" ") { RomajiHelper.toRomaji(it.second) }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔊 $fullRomaji",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlueDark
                    )
                }
            }
        }
    }
}

@Composable
fun DuoBottomResultBanner(
    isCorrect: Boolean,
    correctAnswerText: String,
    romajiSubtitle: String = "",
    onContinue: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val bannerBg = when {
        isCorrect && isDark -> DuoColors.DarkCorrectBanner
        isCorrect && !isDark -> DuoColors.FeatherGreenBg
        !isCorrect && isDark -> DuoColors.DarkWrongBanner
        else -> DuoColors.CardinalRedBg
    }
    val accentColor = if (isCorrect) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark

    Surface(
        color = bannerBg,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, accentColor, CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCorrect) "合格" else "見直",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCorrect) "正解 • Correct!" else "Target Answer:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Text(
                        text = correctAnswerText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else OnPastel
                    )
                    if (romajiSubtitle.isNotBlank()) {
                        Text(
                            text = romajiSubtitle,
                            fontSize = 12.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.78f) else OnPastel.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Duo3DButton(
                text = "CONTINUE →",
                onClick = onContinue,
                topColor = if (isCorrect) DuoColors.FeatherGreen else DuoColors.CardinalRed,
                lipColor = accentColor,
                textColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DuoLessonSettingsDialog(
    appLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    showRomaji: Boolean,
    onToggleRomaji: (Boolean) -> Unit,
    darkTheme: Boolean? = null,
    onToggleDarkTheme: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⚙️ App & Lesson Settings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meaning & Translation Language", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text(
                        "Show meanings and sentence prompts in your preferred language only:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = appLanguage == lang
                            Surface(
                                color = if (selected) DuoColors.FeatherGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) DuoColors.FeatherGreenDark else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectLanguage(lang) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.label,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (selected) DuoColors.FeatherGreenDark else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Romaji (Pronunciation)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(
                            "Display phonetic reading above Japanese word tiles",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showRomaji, onCheckedChange = onToggleRomaji)
                }

                if (darkTheme != null && onToggleDarkTheme != null) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Theme", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(
                                "Toggle between crisp Light mode and Dark slate mode",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = darkTheme, onCheckedChange = { onToggleDarkTheme() })
                    }
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = "DONE",
                onClick = onDismiss,
                topColor = DuoColors.FeatherGreen,
                lipColor = DuoColors.FeatherGreenDark,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun Duo3DCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = if (LocalDarkTheme.current) DuoColors.DarkBorder else DuoColors.LightBorder,
    lipColor: Color = borderColor,
    cornerRadius: Dp = 18.dp,
    lipHeight: Dp = 3.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(lipColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(bottom = lipHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(1.5.dp, borderColor), shape),
            content = content
        )
    }
}

@Composable
fun Duo3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topColor: Color = DuoColors.FeatherGreen,
    lipColor: Color = DuoColors.FeatherGreenDark,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp
) {
    val isDark = LocalDarkTheme.current
    val actualTop = if (enabled) topColor else if (isDark) DuoColors.DarkDisabledBtn else Color(0xFFE2E8F0)
    val actualLip = if (enabled) lipColor else if (isDark) Color(0xFF151E29) else Color(0xFFCBD5E1)
    val actualText = if (enabled) textColor else Color(0xFF64748B)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(actualLip)
            .clickable(enabled = enabled) { onClick() }
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(actualTop)
                .padding(vertical = 13.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = actualText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DuoSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = DuoColors.MacawBlueDark,
    activeLipColor: Color = DuoColors.MacawBlueDark
) {
    val isDark = LocalDarkTheme.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = selectedIndex == idx
            val top = if (selected) activeColor else MaterialTheme.colorScheme.surface
            val lip = if (selected) activeLipColor else if (isDark) DuoColors.DarkBorder else DuoColors.LightBorder
            val txtColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            val shape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(lip)
                    .clickable { onSelect(idx) }
                    .padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(top)
                        .border(1.dp, lip, shape)
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = txtColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SemanticGrammarChip(
    label: String,
    role: String,
    bgColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(shape)
            .background(borderColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(bottom = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = label, color = OnPastel, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(text = role, color = OnPastel.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}
