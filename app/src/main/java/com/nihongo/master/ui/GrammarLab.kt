@file:OptIn(ExperimentalLayoutApi::class)

package com.nihongo.master.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.master.*

private data class ParticleQuickGuide(
    val particle: String,
    val romaji: String,
    val roleEn: String,
    val roleHi: String,
    val visualFormula: String,
    val exampleJp: String,
    val exampleHi: String
)

private val PARTICLE_GUIDES = listOf(
    ParticleQuickGuide("は", "wa", "Topic Marker", "के बारे में / मैं-तो", "[Topic 🟦] + は", "私は学生です。", "मैं (तो) विद्यार्थी हूँ।"),
    ParticleQuickGuide("が", "ga", "Subject Spotlight", "ही / ने (नया फोकस)", "[Subject 🟦] + が", "誰が来ますか？", "कौन आ रहा है?"),
    ParticleQuickGuide("を", "o", "Direct Object", "को (जिस पर काम हो)", "[Object 🟧] + を + [Verb 🟩]", "水を飲みます。", "पानी (को) पीता हूँ।"),
    ParticleQuickGuide("に", "ni", "Target / Time", "को / पर / में (लक्ष्य या समय)", "[Time/Goal 🟧] + に", "六時に学校に行きます。", "6 बजे स्कूल जाता हूँ।"),
    ParticleQuickGuide("で", "de", "Action Place / Tool", "में / से (जहाँ काम हो या साधन)", "[Place/Tool 🟧] + で", "図書館で本を読みます。", "लाइब्रेरी में किताब पढ़ता हूँ।"),
    ParticleQuickGuide("の", "no", "Possessive", "का / की / के", "[Noun A] + の + [Noun B]", "私の本です。", "मेरी किताब है।")
)

@Composable
fun GrammarLabScreen(
    level: JlptLevel?,
    onSpeak: (String, ListeningSpeed) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Lego Grammar & Particles, 1: 10-Form Verb Conjugator
    var selectedParticle by remember { mutableStateOf(PARTICLE_GUIDES.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DuoSegmentedTabs(
            tabs = listOf("🧩 Visual Lego Grammar", "⚙️ Verb Conjugator"),
            selectedIndex = subTab,
            onSelect = { subTab = it },
            activeColor = DuoColors.LilacPurple,
            activeLipColor = DuoColors.LilacPurpleDark
        )

        Spacer(Modifier.height(12.dp))

        if (subTab == 0) {
            val points = remember(level) {
                if (level == null) JlptLevel.entries.flatMap { DataRepository.grammar(it) }
                else DataRepository.grammar(level).ifEmpty { DataRepository.allGrammar }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. COLOR THEORY LEGEND BANNER
                item {
                    Duo3DCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = DuoColors.LilacPurple.copy(alpha = 0.5f),
                        lipColor = DuoColors.LilacPurpleDark
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🎨 Color-Coded Sentence Formula (देखते ही समझें!)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SemanticGrammarChip("🟦 Subject", "कर्ता (मैं/वह)", DuoColors.MacawBlueBg, DuoColors.MacawBlue)
                                SemanticGrammarChip("🟪 Particle", "का/को/में", DuoColors.LilacPurpleBg, DuoColors.LilacPurple)
                                SemanticGrammarChip("🟧 Object", "कर्म/स्थान", DuoColors.FoxOrangeBg, DuoColors.FoxOrange)
                                SemanticGrammarChip("🟩 Verb", "क्रिया (अंत में)", DuoColors.FeatherGreenBg, DuoColors.FeatherGreen)
                            }
                        }
                    }
                }

                // 2. INTERACTIVE 1-TAP PARTICLE DECODER
                item {
                    Duo3DCard(
                        containerColor = DuoColors.LilacPurpleBg,
                        borderColor = DuoColors.LilacPurple,
                        lipColor = DuoColors.LilacPurpleDark
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "⚡ Instant Particle Decoder (Tap any particle)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = OnPastel
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PARTICLE_GUIDES) { pg ->
                                    val isSel = selectedParticle.particle == pg.particle
                                    Duo3DButton(
                                        text = "${pg.particle} (${pg.romaji})",
                                        onClick = {
                                            selectedParticle = pg
                                            onSpeak(pg.exampleJp, ListeningSpeed.NATURAL)
                                        },
                                        topColor = if (isSel) DuoColors.LilacPurple else Color.White,
                                        lipColor = DuoColors.LilacPurpleDark,
                                        textColor = if (isSel) Color.White else OnPastel
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeak(selectedParticle.exampleJp, ListeningSpeed.NATURAL) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selectedParticle.particle} = 🇮🇳 ${selectedParticle.roleHi} (${selectedParticle.roleEn})",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = DuoColors.LilacPurpleDark
                                        )
                                        Text(
                                            text = "🔊 ${selectedParticle.exampleJp}  →  ${selectedParticle.exampleHi}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnPastel
                                        )
                                    }
                                    Text(
                                        text = selectedParticle.visualFormula,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DuoColors.MacawBlueDark
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. LEGO-BLOCK GRAMMAR RULE CARDS
                items(points) { g ->
                    VisualGrammarLegoCard(g = g, onSpeak = onSpeak)
                }
            }
        } else {
            VerbConjugatorLabView(level = level, onSpeak = onSpeak)
        }
    }
}

@Composable
fun VisualGrammarLegoCard(g: GrammarPoint, onSpeak: (String, ListeningSpeed) -> Unit) {
    val blocks = remember(g.structure) {
        g.structure.split(Regex("\\s*[/+→]\\s*")).filter { it.isNotBlank() }
    }

    Duo3DCard(
        borderColor = Color(g.level.color).copy(alpha = 0.6f),
        lipColor = Color(g.level.color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = g.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color(g.level.color),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = g.level.label,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 1. LEGO FORMULA BLOCKS (Visual Instead of Raw String)
            Text(
                text = "🧱 FORMULA BLOCKS",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                blocks.forEachIndexed { idx, part ->
                    val (bg, border) = when {
                        part.contains("Verb") || part.contains("ます") || part.contains("たい") ->
                            DuoColors.FeatherGreenBg to DuoColors.FeatherGreen
                        part.contains("Noun") || part.contains("Topic") || part.contains("Subject") ->
                            DuoColors.MacawBlueBg to DuoColors.MacawBlue
                        part.contains("Place") || part.contains("Object") || part.contains("Target") ->
                            DuoColors.FoxOrangeBg to DuoColors.FoxOrange
                        else -> DuoColors.LilacPurpleBg to DuoColors.LilacPurple
                    }

                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, border)
                    ) {
                        Text(
                            text = part,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = OnPastel
                        )
                    }
                    if (idx < blocks.lastIndex) {
                        Text(
                            text = "➕",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. HINDI LOGIC MIRROR CALLOUT (High-Contrast Green Card)
            Surface(
                color = DuoColors.FeatherGreenBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DuoColors.FeatherGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🇮🇳 हिन्दी ट्रिक (Instant Concept)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoColors.FeatherGreenDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = g.hindiTip,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnPastel
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = g.explanation,
                        fontSize = 12.sp,
                        color = OnPastel.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. COLOR-HIGHLIGHTED AUDIO EXAMPLE CARD
            Duo3DCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = DuoColors.MacawBlue.copy(alpha = 0.5f),
                lipColor = DuoColors.MacawBlueDark,
                onClick = { onSpeak(g.exJp, ListeningSpeed.NATURAL) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = highlightJapaneseParticles(g.exJp),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = g.exRomaji,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DuoColors.MacawBlue
                        )
                        Text(
                            text = "💬 ${g.exEn}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { onSpeak(g.exJp, ListeningSpeed.NATURAL) }) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = DuoColors.MacawBlue
                        )
                    }
                }
            }

            // 4. VISUAL DO vs DON'T MISTAKE ALERT BOX
            if (g.commonMistake.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = DuoColors.CardinalRedBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DuoColors.CardinalRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🚫", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Bacho Is Galti Se (Common Mistake):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.CardinalRedDark
                            )
                            Text(
                                text = g.commonMistake.removePrefix("Mistake:").trim(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPastel
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Colors key Japanese particles inside example sentences so learners visually spot structure! */
@Composable
private fun highlightJapaneseParticles(text: String) = buildAnnotatedString {
    val particleChars = setOf('は', 'が', 'を', 'に', 'で', 'へ', 'と', 'の', 'も')
    for (ch in text) {
        if (ch in particleChars) {
            withStyle(
                SpanStyle(
                    color = DuoColors.LilacPurpleDark,
                    fontWeight = FontWeight.ExtraBold,
                    background = DuoColors.LilacPurpleBg
                )
            ) {
                append(" $ch ")
            }
        } else {
            append(ch)
        }
    }
}

@Composable
fun VerbConjugatorLabView(level: JlptLevel?, onSpeak: (String, ListeningSpeed) -> Unit) {
    val verbs = remember(level) {
        DataRepository.vocab.filter { it.type.isVerb && (level == null || it.level == level) }
            .ifEmpty { DataRepository.vocab.filter { it.type.isVerb } }
    }
    var selectedVerb by remember(verbs) { mutableStateOf(verbs.firstOrNull()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Select a Verb to See Stem + Ending Changes:",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(verbs) { v ->
                val isSel = selectedVerb?.id == v.id
                FilterChip(
                    selected = isSel,
                    onClick = {
                        selectedVerb = v
                        onSpeak(v.kana, ListeningSpeed.NATURAL)
                    },
                    label = {
                        Text(
                            text = "${v.kanji} (${v.hi})",
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

        Spacer(Modifier.height(12.dp))

        val verb = selectedVerb ?: return
        val forms = remember(verb) { VerbConjugator.forms(verb) }
        val stemPrefix = remember(verb) {
            if (verb.kanji.length > 1) verb.kanji.dropLast(1) else ""
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Duo3DCard(
                    containerColor = DuoColors.MacawBlueBg,
                    borderColor = DuoColors.MacawBlue,
                    lipColor = DuoColors.MacawBlueDark,
                    onClick = { onSpeak(verb.kana, ListeningSpeed.NATURAL) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = verb.type.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DuoColors.MacawBlueDark
                            )
                            Text(
                                text = "${verb.kanji} 【${verb.kana}】",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPastel
                            )
                            Text(
                                text = "${verb.en} • 🇮🇳 ${verb.hi}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoColors.FeatherGreenDark
                            )
                        }
                        IconButton(onClick = { onSpeak(verb.kana, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = DuoColors.MacawBlueDark)
                        }
                    }
                }
            }

            items(forms.indices.toList()) { idx ->
                val f = forms[idx]
                val tierColor = when {
                    idx < 4 -> DuoColors.FeatherGreen
                    idx < 7 -> DuoColors.MacawBlue
                    else -> DuoColors.LilacPurple
                }

                Duo3DCard(
                    borderColor = tierColor.copy(alpha = 0.5f),
                    lipColor = tierColor,
                    onClick = { onSpeak(f.reading, ListeningSpeed.NATURAL) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = f.name,
                                fontSize = 11.sp,
                                color = tierColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                            // Highlight Stem vs Changed Ending!
                            Text(
                                text = buildAnnotatedString {
                                    if (stemPrefix.isNotEmpty() && f.jp.startsWith(stemPrefix)) {
                                        append(stemPrefix)
                                        withStyle(SpanStyle(color = tierColor, fontWeight = FontWeight.ExtraBold)) {
                                            append(f.jp.removePrefix(stemPrefix))
                                        }
                                    } else {
                                        withStyle(SpanStyle(color = tierColor, fontWeight = FontWeight.ExtraBold)) {
                                            append(f.jp)
                                        }
                                    }
                                    append("  【${f.reading}】")
                                },
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "🇮🇳 ${f.hint}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { onSpeak(f.reading, ListeningSpeed.NATURAL) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = tierColor)
                        }
                    }
                }
            }
        }
    }
}
