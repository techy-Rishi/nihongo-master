@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.DataRepository
import com.nihongo.master.PitchAccentAnalyzer

// =========================================================================
// NEO-TOKYO SUMI-INK & WASHI COLOR SYSTEM (ORIGINAL & ULTRA-CLEAN)
// =========================================================================
object DuoColors {
    // Primary Japanese Accents
    val FeatherGreen = Color(0xFF10B981)       // Matcha Emerald (Primary Action / Passed)
    val FeatherGreenDark = Color(0xFF059669)
    val FeatherGreenBg = Color(0xFFD1FAE5)

    val MacawBlue = Color(0xFF38BDF8)          // Indigo Wave (Audio / Interactive)
    val MacawBlueDark = Color(0xFF0284C7)
    val MacawBlueBg = Color(0xFFE0F2FE)

    val LilacPurple = Color(0xFFA78BFA)        // Fuji Violet (Particles & Grammar)
    val LilacPurpleDark = Color(0xFF7C3AED)
    val LilacPurpleBg = Color(0xFFEDE9FE)

    val FoxOrange = Color(0xFFF59E0B)          // Yuzu Amber (Streak / Highlights)
    val FoxOrangeDark = Color(0xFFD97706)
    val FoxOrangeBg = Color(0xFFFEF3C7)

    val CardinalRed = Color(0xFFF43F5E)        // Sakura Crimson / Hanko Stamp
    val CardinalRedDark = Color(0xFFE11D48)
    val CardinalRedBg = Color(0xFFFFE4E6)

    val BeeGold = Color(0xFFFBBF24)            // Kintsugi Gold
    val BeeGoldDark = Color(0xFFD97706)
    val BeeGoldBg = Color(0xFFFEF9C3)

    // Light Mode: Warm Japanese Washi Paper
    val LightCanvas = Color(0xFFF6F5F2)
    val LightCard = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE2E0D8)
    val LightLip = Color(0xFFD0CEC4)
    val LightEmptySlot = Color(0xFFECEAE4)

    // Dark Mode: Deep Sumi-Ink Night Slate
    val DarkCanvas = Color(0xFF0D131A)
    val DarkCard = Color(0xFF161F2C)
    val DarkBorder = Color(0xFF263345)
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
    onBackground = Color(0xFF18181B),
    surface = DuoColors.LightCard,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFEFEDE8),
    onSurfaceVariant = Color(0xFF64748B)
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
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
// LIVE ROMAJI & PITCH X-RAY CONVERTER
// =========================================================================
object RomajiHelper {
    private val kanaMap: Map<String, String> by lazy {
        DataRepository.allKana.associate { it.char to it.romaji } + mapOf(
            "ー" to "-", "っ" to "", "ッ" to "",
            "は" to "wa", "へ" to "e", "を" to "o",
            "です" to "desu", "ます" to "masu", "ました" to "mashita",
            "ません" to "masen", "ください" to "kudasai", "さん" to "san"
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
// UNIQUE "KOTOBENTO" LESSON UI COMPONENTS (NO DUOLINGO CLONE!)
// =========================================================================

/**
 * 1. Top X-Ray Toggle Pill Bar: Lets the user toggle `[あ/A Romaji]` and `[🇮🇳 Hindi]`
 * directly on the screen with 1 tap (no hidden settings gear required!).
 */
@Composable
fun BentoXRayToggleBar(
    showRomaji: Boolean,
    onToggleRomaji: (Boolean) -> Unit,
    showHindi: Boolean,
    onToggleHindi: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XRayPill(
            label = if (showRomaji) "A Romaji ON" else "A Romaji OFF",
            active = showRomaji,
            activeColor = DuoColors.MacawBlue,
            onClick = { onToggleRomaji(!showRomaji) }
        )
        XRayPill(
            label = if (showHindi) "🇮🇳 Hindi ON" else "🇮🇳 Hindi OFF",
            active = showHindi,
            activeColor = DuoColors.FeatherGreen,
            onClick = { onToggleHindi(!showHindi) }
        )
    }
}

@Composable
private fun XRayPill(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (active) activeColor else Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 2. Magnetic Bento Word Tile:
 * - Shows a subtle top accent indicator for Japanese Particles (`は`, `が`, `を`, `に`, `で`)
 *   so beginners naturally sense sentence grammar without garish colors.
 * - Shows crisp Romaji above the word when X-Ray Romaji is ON.
 * - Leaves a recessed Bento slot when placed into the Sentence Dock.
 */
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
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(12.dp)
    val romaji = if (showRomaji) RomajiHelper.toRomaji(text) else ""
    val isParticle = text in setOf("は", "が", "を", "に", "で", "へ", "と", "の", "も", "から", "まで", "より", "だけ")

    val slotColor = if (isDark) DuoColors.DarkLip else DuoColors.LightEmptySlot
    val borderColor = when {
        isCorrectHighlight -> DuoColors.FeatherGreen
        isWrongHighlight -> DuoColors.CardinalRed
        isParticle -> DuoColors.LilacPurple.copy(alpha = 0.65f)
        isDark -> DuoColors.DarkBorder
        else -> DuoColors.LightBorder
    }
    val surfaceColor = when {
        isCorrectHighlight -> if (isDark) Color(0xFF0D2821) else DuoColors.FeatherGreenBg
        isWrongHighlight -> if (isDark) Color(0xFF2C1219) else DuoColors.CardinalRedBg
        isDark -> DuoColors.DarkCard
        else -> Color.White
    }

    if (isPlaceholderSlot) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(slotColor)
                .border(1.dp, borderColor.copy(alpha = 0.25f), shape)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = if (showRomaji) 8.dp else 11.dp)
                    .alpha(0f)
            ) {
                if (showRomaji && romaji.isNotBlank()) {
                    Text(text = romaji, fontSize = 10.sp)
                }
                Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Surface(
            color = surfaceColor,
            shape = shape,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 2.dp,
            modifier = modifier.clickable { onClick() }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = if (showRomaji) 6.dp else 10.dp)
            ) {
                // Subtle top particle indicator pill
                if (isParticle) {
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(DuoColors.LilacPurple)
                    )
                    Spacer(Modifier.height(2.dp))
                }
                if (showRomaji && romaji.isNotBlank()) {
                    Text(
                        text = romaji,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isParticle) DuoColors.LilacPurple else DuoColors.MacawBlue
                    )
                }
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 3. Neo-Tokyo Acoustic Studio Card (Replaces Duolingo's Cartoon Person + Speech Bubble):
 * Shows a Japanese Category Seal, visual Pitch Mora Waveform, and integrated Dual-Speed Audio Bar.
 */
@Composable
fun DuoCharacterAudioPrompt(
    characterEmoji: String,
    onPlayNormal: () -> Unit,
    onPlaySlow: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DuoColors.CardinalRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DuoColors.CardinalRed.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "聴解 • AUDIO LAB",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoColors.CardinalRed
                        )
                    }
                }
                Text(text = characterEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.height(14.dp))

            // Dual-Speed Studio Pill Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1.0x Native Button
                Surface(
                    color = DuoColors.MacawBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, DuoColors.MacawBlue),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(58.dp)
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
                            tint = DuoColors.MacawBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "1.0x Native",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = DuoColors.MacawBlue
                            )
                            Text(
                                text = "Tap to listen",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 0.65x Clear Mora Speed Button
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(58.dp)
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
                            tint = DuoColors.FoxOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "0.65x Slow",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Clear Mora",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Bento Prompt Card (Replaces Cartoon Person + Speech Bubble for Translation):
 * Clean Japanese Bento Prompt with Meaning, Hindi Sense, and 1-Tap Audio Preview.
 */
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
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
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
                        text = "翻訳 • MEANING PROMPT",
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
                        tint = DuoColors.MacawBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Audio Hint",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlue
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = promptText,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subHintText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subHintText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoColors.FeatherGreen
                )
            }
        }
    }
}

/**
 * 5. Magnetic Bento Sentence Dock (Replaces Duolingo's 3 Notebook Lines):
 * - Snaps selected tiles into a clean recessed Japanese Bento tray.
 * - Automatically displays the combined live Romaji sentence along the bottom bar of the tray!
 */
@Composable
fun DuoRuledAnswerArea(
    selectedTiles: List<Pair<Int, String>>,
    showRomaji: Boolean,
    checkedResult: Boolean?,
    onRemoveTile: (Pair<Int, String>) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = when (checkedResult) {
        true -> DuoColors.FeatherGreen
        false -> DuoColors.CardinalRed
        null -> if (isDark) DuoColors.DarkBorder else DuoColors.LightBorder
    }
    val trayBg = if (isDark) Color(0xFF111923) else Color(0xFFEFEDE8)

    Surface(
        color = trayBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 138.dp)
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
                    text = "BENTO SENTENCE DOCK (Tap tile to remove)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedTiles.isNotEmpty()) {
                    Text(
                        text = "${selectedTiles.size} blocks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlue
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (selectedTiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap Japanese blocks below to assemble your answer",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
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

            // Live Romaji Phonetic Preview Strip at the bottom of the Bento Dock
            if (showRomaji && selectedTiles.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val fullRomaji = selectedTiles.joinToString(" ") { RomajiHelper.toRomaji(it.second) }
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔊 $fullRomaji",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoColors.MacawBlue
                    )
                }
            }
        }
    }
}

/**
 * 6. Japanese Hanko Seal Verification Banner (`合格` Passed / `再確認` Review):
 * Authentic Japanese red/emerald stamp badge instead of Duolingo's "Amazing!" clone sheet.
 */
@Composable
fun DuoBottomResultBanner(
    isCorrect: Boolean,
    correctAnswerText: String,
    romajiSubtitle: String = "",
    onContinue: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bannerBg = when {
        isCorrect && isDark -> DuoColors.DarkCorrectBanner
        isCorrect && !isDark -> DuoColors.FeatherGreenBg
        !isCorrect && isDark -> DuoColors.DarkWrongBanner
        else -> DuoColors.CardinalRedBg
    }
    val accentColor = if (isCorrect) DuoColors.FeatherGreen else DuoColors.CardinalRed
    val lipColor = if (isCorrect) DuoColors.FeatherGreenDark else DuoColors.CardinalRedDark

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
                // Authentic Japanese Hanko Stamp Circle (`合格` or `見直`)
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
                        text = if (isCorrect) "正解 • Seikai (Spot On!)" else "正しい答え • Target Solution",
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
                text = "NEXT STEP →",
                onClick = onContinue,
                topColor = accentColor,
                lipColor = lipColor,
                textColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DuoLessonSettingsDialog(
    showRomaji: Boolean,
    onToggleRomaji: (Boolean) -> Unit,
    showHindiHints: Boolean,
    onToggleHindi: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🔍 X-Ray Script & Hint Settings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Romaji Pronunciation Guide", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Show phonetic reading above every Japanese tile & in the Bento Dock",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showRomaji, onCheckedChange = onToggleRomaji)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hindi Meaning Bridge (🇮🇳)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Show Hindi translation alongside English prompts (SOV word-order match)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showHindiHints, onToggleHindi)
                }
            }
        },
        confirmButton = {
            Duo3DButton(
                text = "SAVE & CLOSE",
                onClick = onDismiss,
                topColor = DuoColors.MacawBlueDark,
                lipColor = DuoColors.MacawBlueDark,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// =========================================================================
// SHARED TACTILE CARDS & BUTTONS
// =========================================================================

@Composable
fun Duo3DCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = if (isSystemInDarkTheme()) DuoColors.DarkBorder else DuoColors.LightBorder,
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
    val isDark = isSystemInDarkTheme()
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = selectedIndex == idx
            val top = if (selected) activeColor else MaterialTheme.colorScheme.surface
            val lip = if (selected) activeLipColor else if (isSystemInDarkTheme()) DuoColors.DarkBorder else DuoColors.LightBorder
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
