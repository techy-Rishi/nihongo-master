package com.nihongo.master

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// 0. GLOBAL LANGUAGE PREFERENCE (STRICTLY ENGLISH OR HINDI — NEVER BOTH)
// =========================================================================
enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)")
}

// =========================================================================
// 1. JLPT N5 EXCLUSIVE LEVEL & KANA MODELS
// =========================================================================
enum class JlptLevel(
    val label: String,
    val title: String,
    val subtitle: String,
    val color: Long
) {
    N5("N5", "Complete JLPT N5 (850+ Words • 103 Kanji)", "6-Month Track: Hiragana, Katakana, Foreign Words, 850+ Vocab & Grammar", 0xFF10B981)
}

enum class KanaType { HIRAGANA, KATAKANA }

enum class KanaGroup(val title: String, val tip: String) {
    BASIC("Basic Gojūon (46)", "46 foundational sounds — the first step to reading Japanese."),
    VOICED("Dakuten & Handakuten (25)", "Add ゛ or ゜ to shift consonant vibration (k→g, s→z, h→b/p)."),
    COMBO("Yōon Digraphs (33)", "Small ゃ, ゅ, ょ blends with i-column kana into a single syllable (kya, sha, cha).")
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
    N("n", "Noun", false),
    ADJI("adji", "い-Adjective", false),
    ADJNA("adjna", "な-Adjective", false),
    OTHER("x", "Phrase / Expression", false)
}

enum class PitchPatternType(val label: String, val hiDesc: String) {
    HEIBAN("Heiban (Flat ↗)", "Starts low, stays high without dropping"),
    ATAMADAKA("Atamadaka (High-Drop ↘)", "Starts high on the first mora, then drops immediately"),
    NAKADAKA("Nakadaka (Mid-Peak ↗↘)", "Rises in the middle and drops before the end"),
    ODAKA("Odaka (Tail-Drop ↗)", "Stays high until the end, drops on the following particle")
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

    /** Returns ONLY English or ONLY Hindi based on user's selected language. */
    fun meaning(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) hi.ifBlank { en } else en

    /** Avoids printing "ありがとう 【ありがとう】" when kanji and kana are identical! */
    val headwordWithReading: String
        get() = if (kanji == kana) kanji else "$kanji 【$kana】"
}

data class SentenceItem(
    val level: JlptLevel,
    val jp: String,
    val romaji: String,
    val en: String,
    val hi: String,
    val casualJp: String = "",
    val politeNote: String = "Standard Polite (丁寧語)"
) {
    fun meaning(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) hi.ifBlank { en } else en
}

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
) {
    fun tip(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) hindiTip.ifBlank { explanation } else explanation
}

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
) {
    fun meaning(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) meaningHi.ifBlank { meaningEn } else meaningEn
}

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
) {
    fun meaning(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) hi.ifBlank { en } else en
}

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
    MEANING("Meaning", "Japanese word → Meaning"),
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
// 5. 16-UNIT (6-MONTH) PROGRESSIVE N5 CURRICULUM ENGINE
// =========================================================================
enum class PathNodeType(val badgeIcon: String, val label: String) {
    KANA_DOJO("あ", "Kana Practice"),
    VOCAB_STAR("★", "Core Words"),
    SENTENCE_STUDIO("💬", "Build & Speak"),
    GRAMMAR_RULE("📖", "Grammar Logic"),
    CHEST_REWARD("🎁", "Bonus Review"),
    UNIT_TROPHY("🏆", "Chapter Mastery")
}

data class PathNodeSpec(
    val id: String,
    val unitId: String,
    val stepNumber: Int,
    val title: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val type: PathNodeType,
    val kanaList: List<KanaChar> = emptyList(),
    val vocabList: List<VocabItem> = emptyList(),
    val sentenceList: List<SentenceItem> = emptyList(),
    val grammarPoint: GrammarPoint? = null,
    val xpReward: Int = 25
) {
    fun subtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) subtitleHi else subtitleEn
}

data class CurriculumUnit(
    val id: String,
    val level: JlptLevel,
    val sectionNumber: Int,
    val unitNumber: Int,
    val durationLabel: String,
    val title: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val accentHex: Long,
    val lipHex: Long,
    val mascotEmoji: String,
    val mascotCaption: String,
    val guidebookTipEn: String,
    val guidebookTipHi: String,
    val unitVocab: List<VocabItem>,
    val unitSentences: List<SentenceItem>,
    val unitGrammar: List<GrammarPoint>,
    val unitKana: List<KanaChar>,
    val nodes: List<PathNodeSpec>
) {
    fun subtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) subtitleHi else subtitleEn

    fun guidebookTip(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) guidebookTipHi else guidebookTipEn
}

object CurriculumEngine {

    private val unitPalettes = listOf(
        0xFF10B981L to 0xFF059669L, // Matcha Emerald
        0xFF38BDF8L to 0xFF0284C7L, // Cerulean Sky
        0xFFF59E0BL to 0xFFD97706L, // Yuzu Amber
        0xFFA78BFAL to 0xFF7C3AEDL, // Fuji Violet
        0xFFF43F5EL to 0xFFE11D48L, // Sakura Crimson
        0xFF14B8A6L to 0xFF0D9488L, // Teal Wave
        0xFF6366F1L to 0xFF4F46E5L, // Indigo Night
        0xFFEC4899L to 0xFFDB2777L  // Camellia Pink
    )

    /**
     * Builds sentences directly from the taught `VocabItem` examples (`exJp`, `exRomaji`, `exEn`)
     * so every sentence in a lesson uses the EXACT words taught in that lesson!
     */
    private fun sentencesFromTaughtWords(words: List<VocabItem>): List<SentenceItem> {
        return words
            .filter { it.exJp.isNotBlank() }
            .map { w ->
                SentenceItem(
                    level = JlptLevel.N5,
                    jp = w.exJp,
                    romaji = w.exRomaji,
                    en = w.exEn,
                    hi = "${w.hi} — ${w.exEn}",
                    casualJp = "",
                    politeNote = "Uses word: ${w.headwordWithReading} (${w.en})"
                )
            }
    }

    fun buildUnitsForLevel(level: JlptLevel = JlptLevel.N5): List<CurriculumUnit> {
        val allWords = DataRepository.vocab
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
            val subEn: String,
            val subHi: String,
            val mascot: Pair<String, String>,
            val tipEn: String,
            val tipHi: String,
            val kanaPool: List<KanaChar>
        )

        val metas = listOf(
            UnitMeta(1, 1, "Month 1 • Week 1–2", "Greetings, Polite Phrases & Hiragana あ〜そ", "Daily greetings, manners & first 15 Hiragana", "नमस्ते, शिष्टाचार और पहले 15 हिरागाना अक्षर", "🦉" to "Hajimemashite", "Say 'Hajimemashite' when meeting someone for the first time, and finish with 'Yoroshiku onegaishimasu'.", "जापानी में पहली मुलाकात पर 'Hajimemashite' और अंत में 'Yoroshiku onegaishimasu' कहते हैं।", basicHiragana.take(15)),
            UnitMeta(1, 2, "Month 1 • Week 3–4", "Pronouns, Pointers (これ/それ) & Hiragana た〜ほ", "I/you, this/that/here/there & Hiragana ta–ho", "मैं/आप, यह/वह/यहाँ/वहाँ और हिरागाना (た〜ほ)", "👉" to "Kore & Sore", "Use これ (kore) for items near you, それ (sore) near the listener, and あれ (are) far from both.", "मेरे पास की चीज़ = これ (Kore), सुनने वाले के पास = それ (Sore), और दोनों से दूर = あれ (Are)।", basicHiragana.drop(15).take(15)),
            UnitMeta(1, 3, "Month 2 • Week 5–6", "People, Family, Jobs & Hiragana ま〜ん", "Family members, occupations & remaining Hiragana", "परिवार (पिता/माँ), पेशा और पूरे 46 हिरागाना", "🎎" to "Kazoku", "Use short humble words (chichi/haha) for your own family and polite words (otousan/okaasan) for someone else's family.", "अपने परिवार के लिए चिचि/हाहा (父/母) और दूसरे के परिवार के लिए ओतोउसान/ओकासान कहते हैं।", basicHiragana.drop(30)),
            UnitMeta(1, 4, "Month 2 • Week 7–8", "Katakana Foreign Words Pt. 1: Cafe, Food & Tech", "Coffee, bread, smartphone, PC & media in Katakana", "विदेशी शब्द (カタカナ): कॉफ़ी, ब्रेड, स्मार्टफ़ोन, टीवी, पीसी", "☕" to "Koohii & Pan", "Words borrowed from English (like Sumaho, Pasokon, Koohii, Kamera) are always written in Katakana.", "अंग्रेज़ी से आए सभी आधुनिक शब्द (जैसे Sumaho, Pasokon, Koohii) हमेशा Katakana में लिखे जाते हैं।", basicKatakana.take(25)),
            UnitMeta(2, 5, "Month 2 • Week 9", "Katakana Foreign Words Pt. 2: Clothes, Hotel & Travel", "Shirts, hotels, buses, taxis & long vowels (ー)", "शर्ट, कोट, होटल, टैक्सी, बस और विदेशी शब्द", "🏨" to "Hoteru & Basu", "In Katakana, a long dash 'ー' extends the vowel sound for one full beat, such as タクシー (takushii).", "Katakana में लम्बी आवाज़ के लिए 'ー' (लम्बी रेखा) का प्रयोग होता है, जैसे: タクシー (Takushii)।", basicKatakana.drop(25) + voicedKatakana.take(10)),
            UnitMeta(2, 6, "Month 3 • Week 10–11", "Numbers (0–10,000), Money & Shopping Counters", "Counting yen, asking prices (いくら) & counters", "गिनती, येन (円), कीमत (いくら) और काउंटर", "💴" to "Ikura desu ka", "Use hitotsu, futatsu, mittsu to count general items, and hitori (1 person), futari (2 people) for people.", "सामान गिनने के लिए ひとつ (1), ふたつ (2) और लोगों के लिए ひとり (1), ふたり (2) का प्रयोग करें।", voicedHiragana),
            UnitMeta(2, 7, "Month 3 • Week 12–13", "Clock Time, Days of Week & Calendar Dates", "Telling time (時/分), weekdays & 1st–31st dates", "घड़ी का समय (時・分), वार और महीने की तारीखें", "⏰" to "Nanji desu ka", "Mark specific clock times with particle に (Rokuji ni okimasu = I wake up at 6).", "घड़ी के निश्चित समय के साथ हमेशा Particle 'に' लगता है (जैसे: 六時に起きます)।", comboHiragana.take(18)),
            UnitMeta(2, 8, "Month 3 • Week 14", "Food, Drinks, Fruits & Ordering at a Restaurant", "Japanese dishes, ingredients & ordering with をください", "भोजन, सब्ज़ियाँ, फल और रेस्टोरेंट में ऑर्डर करना", "🍜" to "Mizu o kudasai", "To order anything politely at a shop or restaurant, say: [Item] + を + ください (o kudasai).", "कुछ भी माँगने के लिए: [वस्तु] + を + ください (जैसे: Mizu o kudasai = पानी दीजिए)।", comboHiragana.drop(18)),
            UnitMeta(3, 9, "Month 4 • Week 15–16", "House, Rooms, Furniture & Everyday Objects", "Rooms, appliances, stationery & あります vs います", "घर के कमरे, फ़र्नीचर, स्टेशनरी और रोज़मर्रा का सामान", "🏠" to "Heya & Kagu", "Use あります (arimasu) for non-living things (books, desks) and います (imasu) for living beings (people, cats).", "निर्जीव वस्तुओं के लिए 'あります' और जीवित (इंसान, बिल्ली) के लिए 'います' लगता है!", comboKatakana.take(16)),
            UnitMeta(3, 10, "Month 4 • Week 17–18", "Town, Buildings, Directions & Positions (上/下/前/右)", "landmarks, shops & spatial positions (on/under/front)", "शहर, स्टेशन, बैंक और ऊपर/नीचे/दाएँ/बाएँ", "⛩️" to "Eki no Mae", "Describe locations with [Noun] + の + [Position] + に (e.g., Tsukue no ue ni = on top of the desk).", "स्थान बताने के लिए: Tsukue no ue ni (मेज़ के ऊपर), Eki no mae ni (स्टेशन के सामने)।", comboKatakana.drop(16)),
            UnitMeta(3, 11, "Month 4 • Week 19", "Trains, Buses, Travel & Motion Verbs", "Boarding trains (に乗る), getting off & commuting", "ट्रेन/बस में चढ़ना (乗る), उतरना (降りる) और यात्रा", "🚅" to "Densha de Iku", "Mark your vehicle with で (Densha de ikimasu) and mark boarding a train/bus with に (Densha ni norimasu).", "वाहन से जाने के लिए 'で' और ट्रेन/बस में सवार होने के लिए 'に' (電車に乗ります) लगता है।", basicHiragana.take(15)),
            UnitMeta(4, 12, "Month 5 • Week 20", "Core Daily Verbs Pt. 1: Routine & Study Actions", "Waking, eating, reading, writing & action place で", "उठना, सोना, खाना, पढ़ना, लिखना और बनाना", "⚡" to "Mainichi Verbs", "Mark the place where an active action occurs with で (Toshokan de hon o yomimasu).", "जिस जगह पर कोई एक्शन होता है वहाँ 'で' लगता है: 図書館で本を読みます।", basicKatakana.take(15)),
            UnitMeta(4, 13, "Month 5 • Week 21", "Core Daily Verbs Pt. 2: Te-Form, Ongoing & Giving", "Ongoing actions (〜ています), wearing clothes & favors", "अभी चल रहा काम (〜ています), मदद देना/लेना और उधार", "🤝" to "Te-Form", "Combine verb て-form + ください for polite requests, and て-form + います for ongoing actions.", "Verb के て-form में ください जोड़ने से 'कृपया कीजिए' और います जोड़ने से 'कर रहा हूँ' बनता है।", voicedHiragana.take(15)),
            UnitMeta(4, 14, "Month 5 • Week 22", "い-Adjectives: Weather, Size, Taste & Feelings", "Hot/cold, big/small, delicious & past tense 〜かった", "बड़ा/छोटा, महँगा/सस्ता, गर्म/ठंडा और स्वादिष्ट", "☀️" to "Atsui & Samui", "To form the past tense of an い-adjective, drop the final い and add かった (Oishii → Oishikatta).", "い-Adjective का भूतकाल बनाने के लिए अंतिम い हटाकर 'かった' लगाएँ (Oishii → Oishikatta)।", voicedKatakana.take(15)),
            UnitMeta(4, 15, "Month 6 • Week 23", "な-Adjectives, Nature, Body Parts & Colors", "Likes (好き), quiet/clean (きれい), nature & body", "पसंद (好き), सुंदर (きれい), शांत (静か), शरीर और रंग", "🌸" to "Suki & Kirei", "Place な between a な-adjective and a noun (Shizuka na machi = quiet town, Kirei na heya = clean room).", "な-Adjective के बाद संज्ञा (Noun) हो तो बीच में 'な' ज़रूर लगाएँ (जैसे: きれいな部屋)।", comboHiragana.take(15)),
            UnitMeta(4, 16, "Month 6 • Week 24", "Adverbs, Question Words & Complete N5 Mastery", "Always/often/sometimes, why/how & N5 final review", "क्यों/कैसे, क्रिया-विशेषण (いつも/時々) और N5 फाइनल", "👑" to "N5 Master", "Adverbs like あまり (not much) and 全然 (not at all) always pair with a negative verb at the end!", "あまり (ज़्यादा नहीं) और 全然 (बिल्कुल नहीं) के साथ अंत में हमेशा नकारात्मक क्रिया आती है।", basicHiragana)
        )

        val unitSliceCounts = listOf(52, 52, 54, 58, 58, 56, 58, 50, 58, 58, 54, 56, 56, 58, 60, 58)
        var offset = 0

        return metas.mapIndexed { idx, meta ->
            val (accent, lip) = unitPalettes[idx % unitPalettes.size]
            val uId = "N5_U${meta.unitNum}"
            val count = unitSliceCounts.getOrElse(idx) { 55 }
            val vSlice = allWords.drop(offset).take(count).ifEmpty { allWords.take(40) }
            offset += count

            val gPoint = allGram.getOrElse(idx % allGram.size) { allGram.first() }
            val gSecond = allGram.getOrElse((idx + 1) % allGram.size) { gPoint }

            // Split the ~55 words of this unit into 5 progressive learning sets (10–12 words each)
            val set1 = vSlice.take(11)
            val set2 = vSlice.drop(11).take(11).ifEmpty { set1 }
            val set3 = vSlice.drop(22).take(11).ifEmpty { set1 }
            val set4 = vSlice.drop(33).take(11).ifEmpty { set2 }
            val set5 = vSlice.drop(44).ifEmpty { set3 }

            val unitMatchedSentences = sentencesFromTaughtWords(vSlice.take(12))

            val nodes = listOf(
                PathNodeSpec(
                    id = "${uId}_N1",
                    unitId = uId,
                    stepNumber = 1,
                    title = "Word Set 1 & Sentences",
                    subtitleEn = "Learn ${set1.size} words & build sentences from them",
                    subtitleHi = "${set1.size} नए शब्द सीखें और उन्हीं से वाक्य बनाएं",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = set1,
                    sentenceList = sentencesFromTaughtWords(set1),
                    xpReward = 20
                ),
                PathNodeSpec(
                    id = "${uId}_N2",
                    unitId = uId,
                    stepNumber = 2,
                    title = "Word Set 2 & Kana",
                    subtitleEn = "Learn ${set2.size} words & script recognition",
                    subtitleHi = "${set2.size} नए शब्द और अक्षर पहचान अभ्यास",
                    type = PathNodeType.KANA_DOJO,
                    kanaList = meta.kanaPool.take(10),
                    vocabList = set2,
                    sentenceList = sentencesFromTaughtWords(set2),
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N3",
                    unitId = uId,
                    stepNumber = 3,
                    title = "Word Set 3 & Context",
                    subtitleEn = "Learn ${set3.size} words & contextual sentences",
                    subtitleHi = "${set3.size} नए शब्द और वाक्य प्रयोग",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = set3,
                    sentenceList = sentencesFromTaughtWords(set3),
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N4",
                    unitId = uId,
                    stepNumber = 4,
                    title = "Word Set 4 & Grammar",
                    subtitleEn = gPoint.title,
                    subtitleHi = gPoint.title,
                    type = PathNodeType.GRAMMAR_RULE,
                    vocabList = set4,
                    sentenceList = sentencesFromTaughtWords(set4),
                    grammarPoint = gPoint,
                    xpReward = 30
                ),
                PathNodeSpec(
                    id = "${uId}_N5",
                    unitId = uId,
                    stepNumber = 5,
                    title = "Word Set 5 & Expression",
                    subtitleEn = "Learn final ${set5.size} words of this chapter",
                    subtitleHi = "इस अध्याय के अंतिम ${set5.size} शब्द और वाक्य",
                    type = PathNodeType.SENTENCE_STUDIO,
                    vocabList = set5,
                    sentenceList = sentencesFromTaughtWords(set5),
                    grammarPoint = gSecond,
                    xpReward = 30
                ),
                PathNodeSpec(
                    id = "${uId}_N6",
                    unitId = uId,
                    stepNumber = 6,
                    title = "Chapter Review Chest",
                    subtitleEn = "Review taught words from Sets 1–5",
                    subtitleHi = "सीखे गए शब्दों का तेज़ रिव्यू और बोनस XP",
                    type = PathNodeType.CHEST_REWARD,
                    vocabList = (set1.take(3) + set2.take(3) + set3.take(3) + set4.take(3)),
                    sentenceList = sentencesFromTaughtWords(set1.take(3) + set2.take(3)),
                    xpReward = 45
                ),
                PathNodeSpec(
                    id = "${uId}_N7",
                    unitId = uId,
                    stepNumber = 7,
                    title = "Unit ${meta.unitNum} Mastery Exam",
                    subtitleEn = "Complete chapter test using all taught words",
                    subtitleHi = "सीखे गए सभी शब्दों से अध्याय मास्टरी परीक्षा",
                    type = PathNodeType.UNIT_TROPHY,
                    kanaList = meta.kanaPool.take(8),
                    vocabList = (set1.take(2) + set2.take(2) + set3.take(2) + set4.take(2) + set5.take(2)),
                    sentenceList = unitMatchedSentences,
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
                subtitleEn = meta.subEn,
                subtitleHi = meta.subHi,
                accentHex = accent,
                lipHex = lip,
                mascotEmoji = meta.mascot.first,
                mascotCaption = meta.mascot.second,
                guidebookTipEn = meta.tipEn,
                guidebookTipHi = meta.tipHi,
                unitVocab = vSlice,
                unitSentences = unitMatchedSentences,
                unitGrammar = listOf(gPoint, gSecond).distinct(),
                unitKana = meta.kanaPool,
                nodes = nodes
            )
        }
    }
}

// =========================================================================
// 6. PITCH ACCENT ANALYZER
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
// 7. SM-2 SPACED REPETITION ENGINE
// =========================================================================
enum class SrsGrade(val rating: Int, val label: String, val xpReward: Int, val colorHex: Long) {
    AGAIN(1, "Again", 5, 0xFFF43F5E),
    HARD(3, "Hard", 12, 0xFFF59E0B),
    GOOD(4, "Good", 20, 0xFF10B981),
    EASY(5, "Easy", 30, 0xFF38BDF8)
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
                feedbackTitle = "No Speech Detected",
                feedbackDetail = "Tap the microphone and speak clearly.",
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
            bestScore >= 90 -> "🎉 Excellent Pronunciation! (+15 XP)" to
                "Matched accurately! Check the Pitch Accent graph to refine your intonation."
            bestScore >= 70 -> "✅ Good Job! (Passed +15 XP)" to
                "Recognized! Listen to 1.0x Native audio once more to polish your rhythm."
            bestScore >= 45 -> "⚡ Almost There" to
                "Tap 🐢 0.65x Slow first to hear each mora clearly, then try again."
            else -> "🔄 Keep Practicing" to
                "Heard: \"$spoken\" • Target: \"$targetKanji ($targetKana - $targetRomaji)\""
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
                    s + "ます", s + "ない", s + "て", s + "た", s + "たい",
                    s + "られる", s + "よう", s + "れば", s + "られる", s + "させる"
                )
            }
            WordType.VS -> {
                val s = t.removeSuffix("する")
                listOf(
                    s + "します", s + "しない", s + "して", s + "した", s + "したい",
                    s + "できる", s + "しよう", s + "すれば", s + "される", s + "させる"
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
                val i = stems[0]; val a = stems[1]; val e = stems[2]; val o = stems[3]; val te = stems[4]; val ta = stems[5]
                val neg = if (t == "ある" || t == "有る" || t == "在る") "ない" else base + a + "ない"
                listOf(
                    base + i + "ます", neg, base + te, base + ta, base + i + "たい",
                    base + e + "る", base + o + "う", base + e + "ば", base + a + "れる", base + a + "せる"
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
            "1. Polite (〜ます)", "2. Negative (〜ない)", "3. Te-form (〜て)", "4. Past (〜た)",
            "5. Desire (〜たい)", "6. Potential (Can do)", "7. Volitional (Let's 〜よう)",
            "8. Conditional (If 〜ば)", "9. Passive (〜られる)", "10. Causative (〜させる)"
        )
        val hints = listOf(
            "Polite daily speech", "Plain negative", "Connect actions / requests (〜てください)",
            "Plain past tense", "Personal desire (want to do)", "Ability (can do)",
            "Casual suggestion (let's do)", "Conditional (if)", "Passive voice", "Causative (make/let do)"
        )
        return names.indices.map { ConjForm(names[it], jp[it], rd[it], hints[it]) }
    }
}
