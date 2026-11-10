package com.nihongo.master

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// 1. JLPT N5 EXCLUSIVE LEVEL & KANA MODELS
// =========================================================================
enum class JlptLevel(
    val label: String,
    val title: String,
    val subtitle: String,
    val color: Long
) {
    N5("N5", "Complete JLPT N5 (850+ Words • 103 Kanji)", "6-Month Track: Hiragana, Katakana, Foreign Words, 850+ Vocab & Grammar", 0xFF58CC02)
}

enum class KanaType { HIRAGANA, KATAKANA }

enum class KanaGroup(val title: String, val tip: String) {
    BASIC("Basic Gojūon (46)", "46 मूल अक्षर (अ से न् तक) — जापानी पढ़ने की पहली सीढ़ी।"),
    VOICED("Dakuten & Handakuten (25)", "अक्षर पर ゛ (बिंदु) या ゜ (गोला) लगाने से आवाज़ गहरी (k→g, s→z, h→b/p) हो जाती है।"),
    COMBO("Yōon Digraphs (33)", "छोटा ゃ, ゅ, ょ जुड़कर दो अक्षरों को एक मिलाकर (kya, sha, cha) बोलता है।")
}

data class KanaChar(
    val char: String,
    val romaji: String,
    val type: KanaType,
    val group: KanaGroup
)

// =========================================================================
// 2. VOCABULARY, SENTENCES, GRAMMAR & KANJI MODELS
// =========================================================================
enum class WordType(val code: String, val label: String, val isVerb: Boolean) {
    V1("v1", "Ichidan Verb (る-verb)", true),
    V5("v5", "Godan Verb (う-verb)", true),
    VS("vs", "Suru Verb (する)", true),
    VK("vk", "Irregular Verb (来る)", true),
    N("n", "Noun (संज्ञा)", false),
    ADJI("adji", "い-Adjective (विशेषण)", false),
    ADJNA("adjna", "な-Adjective (विशेषण)", false),
    OTHER("x", "Phrase / Adverb / Counter", false)
}

enum class PitchPatternType(val label: String, val hiDesc: String) {
    HEIBAN("Heiban (Flat ↗)", "पहला अक्षर नीचे (Low), बाकी सब ऊपर (High) बिना गिरे"),
    ATAMADAKA("Atamadaka (High-Drop ↘)", "पहला अक्षर ऊँचा (High), उसके तुरंत बाद आवाज़ नीचे गिरती है"),
    NAKADAKA("Nakadaka (Mid-Peak ↗↘)", "बीच में आवाज़ ऊपर जाकर फिर नीचे गिरती है"),
    ODAKA("Odaka (Tail-Drop ↗)", "शब्द के अंत तक ऊँचा, पर अगले Particle (は/が) पर आवाज़ गिरती है")
}

data class PitchMora(
    val mora: String,
    val isHigh: Boolean,
    val hasDropAfter: Boolean = false
)

data class VocabItem(
    val id: String,
    val level: JlptLevel,
    val type: WordType,
    val kanji: String,
    val kana: String,
    val romaji: String,
    val en: String,
    val hi: String,
    val exJp: String,
    val exRomaji: String,
    val exEn: String,
    val pitchDropIndex: Int = -1
) {
    val pitchMoras: List<PitchMora>
        get() = PitchAccentAnalyzer.analyze(kana, pitchDropIndex, type)

    val pitchType: PitchPatternType
        get() = PitchAccentAnalyzer.classifyPattern(kana, pitchDropIndex, type)
}

data class SentenceItem(
    val level: JlptLevel,
    val jp: String,
    val romaji: String,
    val en: String,
    val hi: String,
    val casualJp: String = "",
    val politeNote: String = "Standard Polite (丁寧語 - Teineigo)"
)

data class GrammarPoint(
    val level: JlptLevel,
    val title: String,
    val structure: String,
    val explanation: String,
    val hindiTip: String,
    val exJp: String,
    val exRomaji: String,
    val exEn: String,
    val commonMistake: String = ""
)

data class KanjiItem(
    val kanji: String,
    val level: JlptLevel,
    val onyomi: String,
    val kunyomi: String,
    val meaningEn: String,
    val meaningHi: String,
    val strokeCount: Int,
    val radical: String,
    val mnemonic: String,
    val sampleWords: List<String>
)

// =========================================================================
// 3. PRO SPEAKING, LISTENING & WRITING LAB MODELS
// =========================================================================
enum class ListeningSpeed(val label: String, val rate: Float, val badge: String) {
    SLOW("🐢 0.65x", 0.65f, "Beginner Clear"),
    NATURAL("🎯 1.0x", 0.98f, "Native Speed"),
    FAST("⚡ 1.25x", 1.22f, "Rapid Tokyo")
}

data class SpokenContraction(
    val formalForm: String,
    val casualSpoken: String,
    val romaji: String,
    val explanationHi: String,
    val exFormal: String,
    val exCasual: String,
    val exMeaning: String
)

data class DialogueLine(
    val speaker: String,
    val isUserRole: Boolean,
    val jp: String,
    val kana: String,
    val romaji: String,
    val en: String,
    val hi: String
)

data class RoleplayScenario(
    val id: String,
    val level: JlptLevel,
    val title: String,
    val setting: String,
    val politenessLevel: String,
    val lines: List<DialogueLine>
)

// =========================================================================
// 4. QUIZ & DICTATION MODELS
// =========================================================================
enum class QuizMode(val label: String, val desc: String) {
    MEANING("Meaning", "Japanese word → English & Hindi meaning"),
    READING("Reading", "Kanji → Hiragana reading"),
    LISTEN("Listening", "Audio comprehension without text"),
    KANA("Kana", "Hiragana & Katakana recognition"),
    DICTATION("Dictation", "Listen & match the exact Japanese sentence"),
    PARTICLE("Grammar", "Fill in the right contextual particle (は/が/を/に/で)")
}

data class QuizQuestion(
    val instruction: String,
    val big: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val audio: String,
    val autoPlay: Boolean = false,
    val hint: String = ""
)

// =========================================================================
// 5. 16-UNIT (6-MONTH) COMPLETE N5 WINDING CURRICULUM ENGINE
// =========================================================================
enum class PathNodeType(val badgeIcon: String, val label: String) {
    KANA_DOJO("あ", "Kana Practice"),
    VOCAB_STAR("★", "Core Words"),
    SENTENCE_STUDIO("💬", "Build & Speak"),
    GRAMMAR_RULE("📖", "Grammar Logic"),
    CHEST_REWARD("🎁", "Bonus Chest"),
    UNIT_TROPHY("🏆", "Unit Crown Exam")
}

data class PathNodeSpec(
    val id: String,
    val unitId: String,
    val stepNumber: Int,
    val title: String,
    val subtitleHi: String,
    val type: PathNodeType,
    val kanaList: List<KanaChar> = emptyList(),
    val vocabList: List<VocabItem> = emptyList(),
    val sentenceList: List<SentenceItem> = emptyList(),
    val grammarPoint: GrammarPoint? = null,
    val xpReward: Int = 25
)

data class CurriculumUnit(
    val id: String,
    val level: JlptLevel,
    val sectionNumber: Int,
    val unitNumber: Int,
    val durationLabel: String,
    val title: String,
    val subtitleHi: String,
    val accentHex: Long,
    val lipHex: Long,
    val mascotEmoji: String,
    val mascotCaption: String,
    val guidebookTipHi: String,
    val unitVocab: List<VocabItem>,
    val unitSentences: List<SentenceItem>,
    val unitGrammar: List<GrammarPoint>,
    val unitKana: List<KanaChar>,
    val nodes: List<PathNodeSpec>
)

object CurriculumEngine {

    private val unitPalettes = listOf(
        0xFFFFB800L to 0xFFD69600L, // Amber Gold
        0xFF00CD9CL to 0xFF00A37CL, // Mint Emerald
        0xFF1CB0F6L to 0xFF1487BFL, // Cerulean Sky Blue
        0xFFFF6B6BL to 0xFFD94E4EL, // Coral Rose
        0xFFA568CCL to 0xFF8249A8L, // Royal Amethyst
        0xFF58CC02L to 0xFF439E00L, // Matcha Green
        0xFFFF9600L to 0xFFCC7800L, // Sunset Tangerine
        0xFF3B82F6L to 0xFF2563EBL  // Indigo Ocean
    )

    fun buildUnitsForLevel(level: JlptLevel = JlptLevel.N5): List<CurriculumUnit> {
        val allWords = DataRepository.vocab
        val allSents = DataRepository.allSentences
        val allGram = DataRepository.allGrammar

        val basicHiragana = DataRepository.allKana.filter { it.type == KanaType.HIRAGANA && it.group == KanaGroup.BASIC }
        val voicedHiragana = DataRepository.allKana.filter { it.type == KanaType.HIRAGANA && it.group == KanaGroup.VOICED }
        val comboHiragana = DataRepository.allKana.filter { it.type == KanaType.HIRAGANA && it.group == KanaGroup.COMBO }
        val basicKatakana = DataRepository.allKana.filter { it.type == KanaType.KATAKANA && it.group == KanaGroup.BASIC }
        val voicedKatakana = DataRepository.allKana.filter { it.type == KanaType.KATAKANA && it.group == KanaGroup.VOICED }
        val comboKatakana = DataRepository.allKana.filter { it.type == KanaType.KATAKANA && it.group == KanaGroup.COMBO }

        data class UnitMeta(
            val section: Int,
            val unitNum: Int,
            val timeline: String,
            val title: String,
            val subtitleHi: String,
            val mascot: Pair<String, String>,
            val tipHi: String,
            val kanaPool: List<KanaChar>
        )

        val metas = listOf(
            UnitMeta(1, 1, "Month 1 • Week 1–2", "Greetings, Polite Phrases & Hiragana あ〜そ", "नमस्ते, शिष्टाचार और पहले 15 हिरागाना अक्षर", "🦉" to "Hajimemashite!", "जापानी में पहली मुलाकात पर 'Hajimemashite' और अंत में 'Yoroshiku onegaishimasu' कहते हैं।", basicHiragana.take(15)),
            UnitMeta(1, 2, "Month 1 • Week 3–4", "Pronouns, Demonstratives (これ/それ) & Hiragana た〜ほ", "मैं/आप, यह/वह/यहाँ/वहाँ और हिरागाना (た〜ほ)", "👉" to "Kore & Sore", "मेरे पास की चीज़ = これ (Kore), सुनने वाले के पास = それ (Sore), और दोनों से दूर = あれ (Are)।", basicHiragana.drop(15).take(15)),
            UnitMeta(1, 3, "Month 2 • Week 5–6", "People, Family, Jobs & Hiragana ま〜ん", "परिवार (पिता/माँ), पेशा और पूरे 46 हिरागाना", "🎎" to "Kazoku", "अपने परिवार के लिए चिचि/हाहा (父/母) और दूसरे के परिवार के लिए ओतोउसान/ओकासान कहते हैं।", basicHiragana.drop(30)),
            UnitMeta(1, 4, "Month 2 • Week 7–8", "Katakana Foreign Words Pt. 1: Cafe, Food & Tech", "विदेशी शब्द (カタカナ): कॉफ़ी, ब्रेड, स्मार्टफ़ोन, टीवी, पीसी", "☕" to "Koohii & Pan", "अंग्रेज़ी से आए सभी आधुनिक शब्द (जैसे Sumaho, Pasokon, Koohii, Kora) हमेशा Katakana में लिखे जाते हैं।", basicKatakana.take(25)),
            UnitMeta(2, 5, "Month 2 • Week 9", "Katakana Foreign Words Pt. 2: Clothes, Hotel & Travel", "शर्ट, कोट, होटल, टैक्सी, बस, लिफ्ट और विदेशी शब्द", "🏨" to "Hoteru & Basu", "Katakana में लंम्बी आवाज़ के लिए 'ー' (लम्बी रेखा) का प्रयोग होता है, जैसे: タクシー (Takushii)।", basicKatakana.drop(25) + voicedKatakana.take(10)),
            UnitMeta(2, 6, "Month 3 • Week 10–11", "Numbers (0–10,000), Money & Shopping Counters", "गिनती, येन (円), यह कितने का है (いくら) और काउंटर", "💴" to "Ikura desu ka", "सामान गिनने के लिए ひとつ (1), ふたつ (2), みっつ (3) और लोगों के लिए ひとり (1), ふたり (2) का प्रयोग करें।", voicedHiragana),
            UnitMeta(2, 7, "Month 3 • Week 12–13", "Clock Time, Days of Week & Calendar Dates (1st–31st)", "घड़ी का समय (時・分), वार और महीने की तारीखें", "⏰" to "Nanji desu ka", "महीने की 1 से 10 तारीख (ついたち, ふつか, みっか...) के विशेष उच्चारण N5 परीक्षा में ज़रूर पूछे जाते हैं!", comboHiragana.take(18)),
            UnitMeta(2, 8, "Month 3 • Week 14", "Food, Drinks, Fruits & Ordering at a Restaurant", "भोजन, सब्ज़ियाँ, फल और रेस्टोरेंट में ऑर्डर करना", "🍜" to "Mizu o kudasai", "कुछ भी माँगने के लिए: [वस्तु] + を + ください (जैसे: Raamen o kudasai = रामेन दीजिए)।", comboHiragana.drop(18)),
            UnitMeta(3, 9, "Month 4 • Week 15–16", "House, Rooms, Furniture & Everyday Objects", "घर के कमरे, फ़र्नीचर, स्टेशनरी और रोज़मर्रा का सामान", "🏠" to "Heya & Kagu", "निर्जीव वस्तुओं (किताब, मेज़) के होने के लिए 'あります (arimasu)' और जीवित (इंसान, बिल्ली) के लिए 'います (imasu)' लगता है!", comboKatakana.take(16)),
            UnitMeta(3, 10, "Month 4 • Week 17–18", "Town, Buildings, Directions & Positions (上/下/前/右)", "शहर, स्टेशन, बैंक, अस्पताल और ऊपर/नीचे/दाएँ/बाएँ", "⛩️" to "Eki no Mae", "स्थान बताने के लिए: Tsukue no ue ni (मेज़ के ऊपर), Eki no mae ni (स्टेशन के सामने)।", comboKatakana.drop(16)),
            UnitMeta(3, 11, "Month 4 • Week 19", "Trains, Buses, Travel & Motion Verbs", "ट्रेन/बस में चढ़ना (乗る), उतरना (降りる) और यात्रा", "🚅" to "Densha de Iku", "वाहन से जाने के लिए 'で' (Densha de) और ट्रेन/बस में सवार होने के लिए 'に' (Densha ni norimasu) लगता है।", basicHiragana.take(15)),
            UnitMeta(4, 12, "Month 5 • Week 20", "Core Daily Verbs Pt. 1: Routine & Actions", "उठना, सोना, खाना, पढ़ना, लिखना, खरीदना और बनाना", "⚡" to "Mainichi Verbs", "जिस जगह पर कोई एक्शन होता है वहाँ 'で' लगता है: Toshokan de hon o yomimasu (लाइब्रेरी में किताब पढ़ता हूँ)।", basicKatakana.take(15)),
            UnitMeta(4, 13, "Month 5 • Week 21", "Core Daily Verbs Pt. 2: Te-Form, Ongoing & Giving", "अभी चल रहा काम (〜ています), मदद देना/लेना और उधार", "🤝" to "Te-Form Magic", "Verb के て-form में ください जोड़ने से 'कृपया कीजिए' और います जोड़ने से 'कर रहा हूँ' बनता है।", voicedHiragana.take(15)),
            UnitMeta(4, 14, "Month 5 • Week 22", "い-Adjectives: Weather, Size, Taste & Opposites", "बड़ा/छोटा, महँगा/सस्ता, गर्म/ठंडा, स्वादिष्ट और मज़ेदार", "☀️" to "Atsui & Samui", "い-Adjective का भूतकाल (Past) बनाने के लिए अंतिम い हटाकर 'かった' लगाएँ: Oishii → Oishikatta (स्वादिष्ट था)।", voicedKatakana.take(15)),
            UnitMeta(4, 15, "Month 6 • Week 23", "な-Adjectives, Nature, Body Parts & Colors", "पसंद (好き), सुंदर (きれい), शांत (静か), शरीर और रंग", "🌸" to "Suki & Kirei", "ध्यान रखें: きれい (सुंदर/साफ़) और 有名 (मशहूर) दिखने में い से खत्म होते हैं पर ये な-Adjective हैं!", comboHiragana.take(15)),
            UnitMeta(4, 16, "Month 6 • Week 24", "Adverbs, Question Words & Complete N5 Mastery", "क्यों/कैसे, क्रिया-विशेषण (いつも/時々/ぜんぜん) और N5 फाइनल", "👑" to "N5 Grand Master", "अंतिम पड़ाव! अब आप सभी 850+ N5 शब्द, 103 Kanji और सभी N5 व्याकरण नियमों का अभ्यास कर चुके हैं।", basicHiragana)
        )

        val chunkSize = max(35, allWords.size / metas.size)

        return metas.mapIndexed { idx, meta ->
            val (accent, lip) = unitPalettes[idx % unitPalettes.size]
            val uId = "N5_U${meta.unitNum}"
            val startIdx = (idx * chunkSize).coerceAtMost((allWords.size - 20).coerceAtLeast(0))
            val vSlice = allWords.drop(startIdx).take(chunkSize).ifEmpty { allWords.take(40) }
            val sSlice = allSents.drop((idx * 3) % allSents.size.coerceAtLeast(1)).take(5).ifEmpty { allSents.take(4) }
            val gPoint = allGram.getOrElse(idx % allGram.size) { allGram.first() }
            val gSecond = allGram.getOrElse((idx + 1) % allGram.size) { gPoint }

            val stepChunk = max(6, vSlice.size / 5)

            val nodes = listOf(
                PathNodeSpec(
                    id = "${uId}_N1",
                    unitId = uId,
                    stepNumber = 1,
                    title = "Words Set 1",
                    subtitleHi = "नए शब्द और उनका उच्चारण (Part 1)",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.take(stepChunk),
                    sentenceList = sSlice.take(2),
                    xpReward = 20
                ),
                PathNodeSpec(
                    id = "${uId}_N2",
                    unitId = uId,
                    stepNumber = 2,
                    title = "Kana & Script Drill",
                    subtitleHi = "अक्षर और विदेशी/देशी शब्दों की पहचान",
                    type = PathNodeType.KANA_DOJO,
                    kanaList = meta.kanaPool.take(10),
                    vocabList = vSlice.drop(stepChunk).take(stepChunk).ifEmpty { vSlice.take(8) },
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N3",
                    unitId = uId,
                    stepNumber = 3,
                    title = "Words Set 2",
                    subtitleHi = "नए शब्द और वाक्य प्रयोग (Part 2)",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.drop(stepChunk * 2).take(stepChunk).ifEmpty { vSlice.take(10) },
                    sentenceList = sSlice.drop(1).take(2).ifEmpty { sSlice.take(2) },
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N4",
                    unitId = uId,
                    stepNumber = 4,
                    title = "Grammar Logic",
                    subtitleHi = gPoint.title,
                    type = PathNodeType.GRAMMAR_RULE,
                    vocabList = vSlice.drop(stepChunk * 3).take(stepChunk).ifEmpty { vSlice.takeLast(10) },
                    sentenceList = sSlice,
                    grammarPoint = gPoint,
                    xpReward = 30
                ),
                PathNodeSpec(
                    id = "${uId}_N5",
                    unitId = uId,
                    stepNumber = 5,
                    title = "Bonus Chest",
                    subtitleHi = "तेज़ रिव्यू और बोनस XP खज़ाना!",
                    type = PathNodeType.CHEST_REWARD,
                    vocabList = vSlice.shuffled().take(12),
                    sentenceList = sSlice,
                    xpReward = 45
                ),
                PathNodeSpec(
                    id = "${uId}_N6",
                    unitId = uId,
                    stepNumber = 6,
                    title = "Words Set 3 & Sentences",
                    subtitleHi = "बचे हुए शब्द और वाक्य निर्माण अभ्यास",
                    type = PathNodeType.SENTENCE_STUDIO,
                    vocabList = vSlice.takeLast(stepChunk),
                    sentenceList = sSlice,
                    grammarPoint = gSecond,
                    xpReward = 35
                ),
                PathNodeSpec(
                    id = "${uId}_N7",
                    unitId = uId,
                    stepNumber = 7,
                    title = "Unit ${meta.unitNum} Trophy",
                    subtitleHi = "अध्याय ${meta.unitNum} की मास्टरी परीक्षा पास करें!",
                    type = PathNodeType.UNIT_TROPHY,
                    kanaList = meta.kanaPool.take(8),
                    vocabList = vSlice.shuffled().take(15),
                    sentenceList = sSlice,
                    grammarPoint = gPoint,
                    xpReward = 60
                )
            )

            CurriculumUnit(
                id = uId,
                level = JlptLevel.N5,
                sectionNumber = meta.section,
                unitNumber = meta.unitNum,
                durationLabel = meta.timeline,
                title = meta.title,
                subtitleHi = meta.subtitleHi,
                accentHex = accent,
                lipHex = lip,
                mascotEmoji = meta.mascot.first,
                mascotCaption = meta.mascot.second,
                guidebookTipHi = meta.tipHi,
                unitVocab = vSlice,
                unitSentences = sSlice,
                unitGrammar = listOf(gPoint, gSecond).distinct(),
                unitKana = meta.kanaPool,
                nodes = nodes
            )
        }
    }
}

// =========================================================================
// 6. PITCH ACCENT ANALYZER (MORA SPLITTER & PITCH GRAPH)
// =========================================================================
object PitchAccentAnalyzer {
    private val smallKana = setOf(
        'ゃ', 'ゅ', 'ょ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ',
        'ャ', 'ュ', 'ョ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ'
    )

    fun splitIntoMoras(kana: String): List<String> {
        val clean = kana.trim()
        if (clean.isEmpty()) return emptyList()
        val moras = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val current = clean[i]
            if (i + 1 < clean.length && clean[i + 1] in smallKana) {
                moras.add("${current}${clean[i + 1]}")
                i += 2
            } else {
                moras.add(current.toString())
                i += 1
            }
        }
        return moras
    }

    private fun resolveDropIndex(moras: List<String>, explicitDrop: Int, type: WordType): Int {
        if (explicitDrop >= 0) return explicitDrop
        if (moras.size <= 1) return 1
        return when (type) {
            WordType.V1, WordType.V5 -> if (moras.size >= 3) moras.size - 1 else 1
            WordType.ADJI -> max(1, moras.size - 1)
            WordType.VS, WordType.N, WordType.ADJNA -> 0
            WordType.VK -> 1
            WordType.OTHER -> 1
        }
    }

    fun analyze(kana: String, explicitDrop: Int = -1, type: WordType = WordType.N): List<PitchMora> {
        val moras = splitIntoMoras(kana)
        if (moras.isEmpty()) return emptyList()
        val drop = resolveDropIndex(moras, explicitDrop, type)

        return moras.mapIndexed { idx, mora ->
            val pos = idx + 1
            val isHigh = when (drop) {
                0 -> pos > 1
                1 -> pos == 1
                else -> pos in 2..drop
            }
            PitchMora(
                mora = mora,
                isHigh = isHigh,
                hasDropAfter = (drop > 0 && pos == drop)
            )
        }
    }

    fun classifyPattern(kana: String, explicitDrop: Int = -1, type: WordType = WordType.N): PitchPatternType {
        val moras = splitIntoMoras(kana)
        val drop = resolveDropIndex(moras, explicitDrop, type)
        return when {
            drop == 0 -> PitchPatternType.HEIBAN
            drop == 1 -> PitchPatternType.ATAMADAKA
            drop >= moras.size -> PitchPatternType.ODAKA
            else -> PitchPatternType.NAKADAKA
        }
    }
}

// =========================================================================
// 7. SM-2 SPACED REPETITION ENGINE (SRS MEMORY SYSTEM)
// =========================================================================
enum class SrsGrade(val rating: Int, val label: String, val xpReward: Int, val colorHex: Long) {
    AGAIN(1, "Again (भूल गया)", 5, 0xFFFF4B4B),
    HARD(3, "Hard (कठिन)", 12, 0xFFFF9600),
    GOOD(4, "Good (याद है)", 20, 0xFF58CC02),
    EASY(5, "Easy (आसान)", 30, 0xFF1CB0F6)
}

data class SrsCardState(
    val itemId: String,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val dueEpochDay: Long = 0L,
    val firstSeenDay: Long = -1L
)

object SrsEngine {
    fun currentEpochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    const val NEW_CARDS_PER_DAY = 20

    fun review(card: SrsCardState, grade: SrsGrade, today: Long = currentEpochDay()): SrsCardState {
        val q = grade.rating
        var nextEase = card.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (nextEase < 1.3f) nextEase = 1.3f

        val nextRep: Int
        val nextInterval: Int

        if (q < 3) {
            nextRep = 0
            nextInterval = 1
        } else {
            nextInterval = when (card.repetitions) {
                0 -> 1
                1 -> if (grade == SrsGrade.EASY) 4 else 3
                else -> max(1, (card.intervalDays * nextEase).roundToInt())
            }
            nextRep = card.repetitions + 1
        }

        return card.copy(
            intervalDays = nextInterval,
            repetitions = nextRep,
            easeFactor = nextEase,
            dueEpochDay = if (q < 3) today else today + nextInterval,
            firstSeenDay = if (card.firstSeenDay < 0) today else card.firstSeenDay
        )
    }

    fun serialize(card: SrsCardState): String =
        "${card.itemId}|${card.intervalDays}|${card.repetitions}|${card.easeFactor}|${card.dueEpochDay}|${card.firstSeenDay}"

    fun deserialize(raw: String): SrsCardState? {
        val parts = raw.split("|")
        if (parts.size < 5) return null
        return try {
            SrsCardState(
                itemId = parts[0],
                intervalDays = parts[1].toInt(),
                repetitions = parts[2].toInt(),
                easeFactor = parts[3].toFloat(),
                dueEpochDay = parts[4].toLong(),
                firstSeenDay = parts.getOrNull(5)?.toLong() ?: -1L
            )
        } catch (_: Exception) {
            null
        }
    }
}

// =========================================================================
// 8. SPEECH RECOGNITION & PRONUNCIATION SCORER
// =========================================================================
data class PronunciationResult(
    val scorePercent: Int,
    val recognizedText: String,
    val feedbackTitle: String,
    val feedbackDetail: String,
    val isPassed: Boolean
)

object PronunciationEvaluator {
    private val ignoredPunctuation = setOf('。', '、', '！', '？', ' ', '　', '.', ',', '!', '?', '-', '〜')

    private fun normalize(input: String): String =
        input.filterNot { it in ignoredPunctuation }.lowercase()

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lLen = lhs.length
        val rLen = rhs.length
        if (lLen == 0) return rLen
        if (rLen == 0) return lLen

        var prev = IntArray(rLen + 1) { it }
        var curr = IntArray(rLen + 1)

        for (i in 1..lLen) {
            curr[0] = i
            for (j in 1..rLen) {
                val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[rLen]
    }

    fun evaluate(
        spoken: String,
        targetKanji: String,
        targetKana: String,
        targetRomaji: String = ""
    ): PronunciationResult {
        val cleanSpoken = normalize(spoken)
        if (cleanSpoken.isEmpty()) {
            return PronunciationResult(
                scorePercent = 0,
                recognizedText = "",
                feedbackTitle = "आवाज़ सुनाई नहीं दी (No Speech Detected)",
                feedbackDetail = "Mic button दबाकर साफ़ आवाज़ में दोबारा बोलें।",
                isPassed = false
            )
        }

        val candidates = listOf(
            normalize(targetKanji),
            normalize(targetKana),
            normalize(targetRomaji)
        ).filter { it.isNotEmpty() }

        val bestScore = candidates.maxOfOrNull { target ->
            if (cleanSpoken.contains(target) || target.contains(cleanSpoken)) {
                val ratio = min(cleanSpoken.length, target.length).toFloat() / max(cleanSpoken.length, target.length).coerceAtLeast(1)
                max(85, (ratio * 100f).roundToInt())
            } else {
                val maxLen = max(cleanSpoken.length, target.length)
                if (maxLen == 0) 100
                else {
                    val dist = levenshtein(cleanSpoken, target)
                    ((maxLen - dist).toFloat() / maxLen * 100f).roundToInt().coerceIn(0, 100)
                }
            }
        } ?: 0

        val (title, detail) = when {
            bestScore >= 90 -> "🎉 परफेक्ट! शानदार उच्चारण (+15 XP)" to
                "आपकी आवाज़ बिल्कुल सही पहचानी गई! Pitch ग्राफ़ से उतार-चढ़ाव भी मिलाएँ।"
            bestScore >= 70 -> "✅ बहुत बढ़िया! (Passed +15 XP)" to
                "सही पहचाना गया! और नैचुरल बोलने के लिए 1.0x Native ऑडियो के साथ दोहराएं।"
            bestScore >= 45 -> "⚡ करीब हैं! (Almost There)" to
                "पहले 🐢 0.65x Slow बटन दबाकर हर अक्षर (Mora) को ध्यान से सुनें और फिर बोलें।"
            else -> "🔄 दोबारा कोशिश करें (Keep Practicing)" to
                "सुनाई दिया: \"$spoken\" • लक्ष्य: \"$targetKanji ($targetKana - $targetRomaji)\""
        }

        return PronunciationResult(
            scorePercent = bestScore,
            recognizedText = spoken,
            feedbackTitle = title,
            feedbackDetail = detail,
            isPassed = bestScore >= 70
        )
    }
}

// =========================================================================
// 9. 10-FORM VERB CONJUGATOR FOR N5 VERBS
// =========================================================================
data class ConjForm(
    val name: String,
    val jp: String,
    val reading: String,
    val hint: String
)

object VerbConjugator {

    private fun build(t: String, type: WordType): List<String> {
        return when (type) {
            WordType.V1 -> {
                val s = t.dropLast(1)
                listOf(
                    s + "ます",
                    s + "ない",
                    s + "て",
                    s + "た",
                    s + "たい",
                    s + "られる",
                    s + "よう",
                    s + "れば",
                    s + "られる",
                    s + "させる"
                )
            }
            WordType.VS -> {
                val s = t.removeSuffix("する")
                listOf(
                    s + "します",
                    s + "しない",
                    s + "して",
                    s + "した",
                    s + "したい",
                    s + "できる",
                    s + "しよう",
                    s + "すれば",
                    s + "される",
                    s + "させる"
                )
            }
            WordType.VK -> {
                if (t.startsWith("来")) {
                    listOf("来ます", "来ない", "来て", "来た", "来たい", "来られる", "来よう", "来れば", "来られる", "来させる")
                } else {
                    listOf("きます", "こない", "きて", "きた", "きたい", "こられる", "こよう", "くれば", "こられる", "こさせる")
                }
            }
            WordType.V5 -> {
                val last = t.last()
                val base = t.dropLast(1)
                val stems = when (last) {
                    'う' -> listOf("い", "わ", "え", "お", "って", "った")
                    'つ' -> listOf("ち", "た", "て", "と", "って", "った")
                    'る' -> listOf("り", "ら", "れ", "ろ", "って", "った")
                    'む' -> listOf("み", "ま", "め", "も", "んで", "んだ")
                    'ぶ' -> listOf("び", "ば", "べ", "ぼ", "んで", "んだ")
                    'ぬ' -> listOf("に", "な", "ね", "の", "んで", "んだ")
                    'く' -> if (t.endsWith("行く") || t.endsWith("いく")) {
                        listOf("き", "か", "け", "こ", "って", "った")
                    } else {
                        listOf("き", "か", "け", "こ", "いて", "いた")
                    }
                    'ぐ' -> listOf("ぎ", "が", "げ", "ご", "いで", "いだ")
                    'す' -> listOf("し", "さ", "せ", "そ", "して", "した")
                    else -> listOf("い", "わ", "え", "お", "って", "った")
                }
                val i = stems[0]
                val a = stems[1]
                val e = stems[2]
                val o = stems[3]
                val te = stems[4]
                val ta = stems[5]

                val neg = if (t == "ある" || t == "有る" || t == "在る") "ない" else base + a + "ない"
                listOf(
                    base + i + "ます",
                    neg,
                    base + te,
                    base + ta,
                    base + i + "たい",
                    base + e + "る",
                    base + o + "う",
                    base + e + "ば",
                    base + a + "れる",
                    base + a + "せる"
                )
            }
            else -> emptyList()
        }
    }

    fun forms(w: VocabItem): List<ConjForm> {
        if (!w.type.isVerb) return emptyList()
        val jp = build(w.kanji, w.type)
        val rd = build(w.kana, w.type)
        val names = listOf(
            "1. Polite (〜ます)",
            "2. Negative (〜ない)",
            "3. Te-form (〜て)",
            "4. Past (〜た)",
            "5. Desire (〜たい)",
            "6. Potential (Can do)",
            "7. Volitional (Let's 〜よう)",
            "8. Conditional (If 〜ば)",
            "9. Passive (〜られる)",
            "10. Causative (〜させる)"
        )
        val hints = listOf(
            "विनम्र भाषा (करता हूँ / करेंगे)",
            "साधारण नकारात्मक (नहीं करता)",
            "वाक्य जोड़ने, कृपया कहने (〜てください) और रहा है (〜ている) के लिए",
            "भूतकाल (किया / गया)",
            "अपनी इच्छा बताना (करना चाहता हूँ)",
            "क्षमता बताना (कर सकता हूँ)",
            "सलाह / प्रस्ताव (चलो करते हैं!)",
            "शर्त बताना (अगर ऐसा करें तो...)",
            "कर्मवाच्य (किया जाना)",
            "प्रेरणार्थक (करवाना / करने देना)"
        )
        return names.indices.map { ConjForm(names[it], jp[it], rd[it], hints[it]) }
    }
}
