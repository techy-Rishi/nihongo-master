package com.nihongo.master

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// 1. JLPT LEVELS & KANA MODELS
// =========================================================================
enum class JlptLevel(
    val label: String,
    val title: String,
    val subtitle: String,
    val color: Long
) {
    N5("N5", "Beginner Foundation", "6-Month Track: Kana, 150+ Words, SOV Grammar & Daily Survival", 0xFF58CC02),
    N4("N4", "Elementary", "Everyday Conversations, Life Experiences, Te-Form & Travel", 0xFF1CB0F6),
    N3("N3", "Intermediate", "Natural Spoken Japanese, Nuances, Opinions & Workplace", 0xFFFF9600),
    N2("N2", "Upper Intermediate", "Business Keigo, News, Technical Reports & Formal Debates", 0xFFCE82FF),
    N1("N1", "Advanced Mastery", "Academic Research, Classical Idioms & Native-Level Fluency", 0xFFFF4B4B)
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
    OTHER("x", "Phrase / Expression", false)
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
// 5. MULTI-MONTH WINDING CURRICULUM ENGINE (N5 TO N1 CHAPTERS & NODES)
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

    // Cohesive, eye-friendly unit palettes inspired by modern language apps
    private val unitPalettes = listOf(
        0xFFFFB800L to 0xFFD69600L, // 1. Warm Amber Gold
        0xFF00CD9CL to 0xFF00A37CL, // 2. Mint Emerald
        0xFF1CB0F6L to 0xFF1487BFL, // 3. Cerulean Sky Blue
        0xFFFF6B6BL to 0xFFD94E4EL, // 4. Coral Rose
        0xFFA568CCL to 0xFF8249A8L, // 5. Royal Amethyst
        0xFF58CC02L to 0xFF439E00L, // 6. Matcha Green
        0xFFFF9600L to 0xFFCC7800L, // 7. Sunset Tangerine
        0xFF3B82F6L to 0xFF2563EBL  // 8. Indigo Ocean
    )

    fun buildUnitsForLevel(level: JlptLevel): List<CurriculumUnit> {
        return when (level) {
            JlptLevel.N5 -> buildN5SixMonthCurriculum()
            JlptLevel.N4 -> buildLevelCurriculum(
                level = JlptLevel.N4,
                sectionNum = 2,
                specs = listOf(
                    Triple("Life Experiences & Hobbies", "अनुभव (〜たことがある) और शौक के बारे में बात करना", "🎎" to "Kyoto Culture"),
                    Triple("Requests, Permissions & Rules", "अनुमति माँगना (〜てもいい) और मनाही (〜てはいけない)", "⛩️" to "Shrine Etiquette"),
                    Triple("Obligations & Daily Duties", "ज़िम्मेदारी बताना (〜なければならない / 〜なきゃ)", "💼" to "Tokyo Routine"),
                    Triple("Giving & Receiving Favors", "मदद लेना और देना (〜てくれる / 〜てもらう)", "🎁" to "Omiyage Gift"),
                    Triple("Opinions, Plans & Weather", "अपनी राय (〜と思います) और अनुमान (〜そうです)", "🗻" to "Mt. Fuji View"),
                    Triple("Travel, Bookings & Directions", "होटल बुकिंग, ट्रेन प्लेटफ़ॉर्म और रास्ता पूछना", "🚅" to "Shinkansen")
                )
            )
            JlptLevel.N3 -> buildLevelCurriculum(
                level = JlptLevel.N3,
                sectionNum = 3,
                specs = listOf(
                    Triple("Unexpected Contrast & Regret", "के बावजूद (〜のに) और उम्मीद के उलट परिणाम", "🎭" to "Kabuki Stage"),
                    Triple("Gratitude vs Blame", "की कृपा से (〜おかげで) और के कारण (〜せいで)", "🌸" to "Sakura Season"),
                    Triple("Hearsay vs Visual Nuance", "सुना है कि vs लगता है कि (〜そうだ के दोनों रूप)", "📰" to "Tokyo News"),
                    Triple("Partial Denial & Exceptions", "ऐसा ज़रूरी नहीं है (〜わけではない)", "🍵" to "Tea Ceremony"),
                    Triple("Tech Office & Daily Standups", "सॉफ़्टवेयर मीटिंग, बग फिक्स और प्रोजेक्ट अपडेट", "💻" to "Shibuya Tech")
                )
            )
            JlptLevel.N2 -> buildLevelCurriculum(
                level = JlptLevel.N2,
                sectionNum = 4,
                specs = listOf(
                    Triple("Compulsion & High Certainty", "मजबूरी (〜ざるを得ない) और पक्का विश्वास (〜に違いない)", "🏯" to "Osaka Castle"),
                    Triple("Business Proposals & Formal Scope", "के संबंध में (〜に関して) और निर्णय लेना", "📊" to "Executive Pitch"),
                    Triple("Keigo Mastery: Sonkeigo & Kenjougo", "बॉस और क्लाइंट के साथ आदरसूचक (尊敬語・謙譲語) भाषा", "🙇" to "Keigo Dojo"),
                    Triple("Contradictions, Trends & Quality", "विरोधाभास (矛盾), रुझान (傾向) और गुणवत्ता", "⚖️" to "Quality Lab")
                )
            )
            JlptLevel.N1 -> buildLevelCurriculum(
                level = JlptLevel.N1,
                sectionNum = 5,
                specs = listOf(
                    Triple("External Pressure & Policy", "परिस्थितियों से बाध्य होना (〜を余儀なくされる)", "🏛️" to "Diet Summit"),
                    Triple("Empirical Data & Objectivity", "तथ्यों और डेटा के अनुरूप (〜に即して)", "🔬" to "Kyoto Research"),
                    Triple("Academic Praise & Breakthroughs", "प्रशंसा के योग्य (〜に値する) और क्रांतिकारी शोध", "🎓" to "Thesis Defense"),
                    Triple("Trial, Error & Optimization", "प्रयास और भूल (試行錯誤) से सिस्टम ऑप्टिमाइज़ेशन", "🚀" to "AI Lab")
                )
            )
        }
    }

    private fun buildN5SixMonthCurriculum(): List<CurriculumUnit> {
        val allWords = DataRepository.words(JlptLevel.N5).ifEmpty { DataRepository.vocab }
        val allSents = DataRepository.sentences(JlptLevel.N5).ifEmpty { DataRepository.allSentences }
        val allGram = DataRepository.grammar(JlptLevel.N5).ifEmpty { DataRepository.allGrammar }
        val basicHiragana = DataRepository.allKana.filter { it.type == KanaType.HIRAGANA && it.group == KanaGroup.BASIC }
        val voicedHiragana = DataRepository.allKana.filter { it.type == KanaType.HIRAGANA && it.group == KanaGroup.VOICED }
        val basicKatakana = DataRepository.allKana.filter { it.type == KanaType.KATAKANA && it.group == KanaGroup.BASIC }

        data class N5UnitBlueprint(
            val unitNum: Int,
            val weeks: String,
            val title: String,
            val subtitleHi: String,
            val mascot: Pair<String, String>,
            val tipHi: String,
            val kanaSlice: List<KanaChar>,
            val vocabSlice: List<VocabItem>,
            val sentSlice: List<SentenceItem>,
            val gramSlice: List<GrammarPoint>
        )

        val greetingsVocab = allWords.filter { it.type == WordType.OTHER }.ifEmpty { allWords.take(12) }
        val foodVocab = allWords.filter {
            it.kanji in setOf("水", "お茶", "牛乳", "ご飯", "パン", "卵", "野菜", "果物", "りんご", "魚", "肉", "ラーメン", "カレー", "食べる", "飲む", "買う")
        }.ifEmpty { allWords.take(14) }
        val numberTimeVocab = allWords.filter {
            it.kanji in setOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千", "今日", "明日", "昨日", "毎日", "朝", "昼", "夜", "今", "時間")
        }.ifEmpty { allWords.drop(10).take(15) }
        val peopleFamilyVocab = allWords.filter {
            it.kanji in setOf("私", "あなた", "彼", "彼女", "先生", "学生", "友達", "家族", "父", "母", "兄", "姉", "名前", "人")
        }.ifEmpty { allWords.drop(20).take(14) }
        val placeTransportVocab = allWords.filter {
            it.kanji in setOf("駅", "電車", "地下鉄", "バス", "自転車", "空港", "学校", "大学", "会社", "病院", "銀行", "図書館", "公園", "コンビニ", "トイレ", "行く", "来る", "帰る", "乗る")
        }.ifEmpty { allWords.drop(30).take(16) }
        val actionVerbsVocab = allWords.filter { it.type.isVerb }.take(18).ifEmpty { allWords.take(15) }
        val adjectivesVocab = allWords.filter { it.type == WordType.ADJI || it.type == WordType.ADJNA }.ifEmpty { allWords.take(16) }
        val katakanaObjectsVocab = allWords.filter {
            it.kanji in setOf("スマホ", "パソコン", "テレビ", "パン", "ラーメン", "カレー", "バス", "コンビニ", "トイレ", "インド", "本", "辞書", "かばん", "傘", "靴", "時計")
        }.ifEmpty { allWords.takeLast(15) }

        val blueprints = listOf(
            N5UnitBlueprint(
                unitNum = 1,
                weeks = "Month 1 • Weeks 1–3",
                title = "Greet people & master Hiragana A–Ko",
                subtitleHi = "नमस्ते, परिचय और पहले 15 हिरागाना अक्षर (あ〜そ)",
                mascot = "🦉" to "Hajimemashite!",
                tipHi = "जापानी में किसी से पहली बार मिलने पर 'Hajimemashite' और अंत में 'Yoroshiku onegaishimasu' कहते हैं।",
                kanaSlice = basicHiragana.take(15),
                vocabSlice = greetingsVocab.take(10) + peopleFamilyVocab.take(4),
                sentSlice = allSents.take(4),
                gramSlice = allGram.take(2)
            ),
            N5UnitBlueprint(
                unitNum = 2,
                weeks = "Month 1 • Weeks 4–6",
                title = "Order food and drinks at a cafe",
                subtitleHi = "खाना-पीना ऑर्डर करना (〜をください) और हिरागाना (た〜ほ)",
                mascot = "🍜" to "Itadakimasu!",
                tipHi = "रेस्टोरेंट में कुछ भी माँगने के लिए चीज़ के नाम के बाद 'を ください (o kudasai)' लगाएँ — जैसे: Mizu o kudasai (पानी दीजिए)।",
                kanaSlice = basicHiragana.drop(15).take(15),
                vocabSlice = foodVocab,
                sentSlice = allSents.filter { it.jp.contains("食べ") || it.jp.contains("ください") || it.jp.contains("いくら") }.ifEmpty { allSents.take(5) },
                gramSlice = allGram.drop(1).take(2)
            ),
            N5UnitBlueprint(
                unitNum = 3,
                weeks = "Month 2 • Weeks 7–9",
                title = "Describe people, family & belongings",
                subtitleHi = "परिवार, दोस्त, 'का/की/के' (の) और हिरागाना (ま〜ん)",
                mascot = "🎎" to "Kazoku & Friends",
                tipHi = "दो संज्ञाओं (Nouns) को जोड़ने के लिए बीच में 'の (no)' लगता है — Watashi no hon (मेरी किताब)।",
                kanaSlice = basicHiragana.drop(30),
                vocabSlice = peopleFamilyVocab,
                sentSlice = allSents.filter { it.jp.contains("私") || it.jp.contains("友達") || it.jp.contains("学生") }.ifEmpty { allSents.take(5) },
                gramSlice = allGram.take(2)
            ),
            N5UnitBlueprint(
                unitNum = 4,
                weeks = "Month 2–3 • Weeks 10–12",
                title = "Count numbers, tell time & schedules",
                subtitleHi = "गिनती (1–1000), घड़ी का समय (に) और दिनचर्या",
                mascot = "⏰" to "Rokuji ni okimasu",
                tipHi = "घड़ी के निश्चित समय (जैसे 6 बजे = 六時) के साथ हमेशा Particle 'に (ni)' लगता है, पर 'आज/कल/हर दिन' के साथ に नहीं लगता!",
                kanaSlice = voicedHiragana.take(15),
                vocabSlice = numberTimeVocab,
                sentSlice = allSents.filter { it.jp.contains("時") || it.jp.contains("毎") || it.jp.contains("明日") }.ifEmpty { allSents.drop(4).take(5) },
                gramSlice = allGram.drop(2).take(2)
            ),
            N5UnitBlueprint(
                unitNum = 5,
                weeks = "Month 3–4 • Weeks 13–15",
                title = "Navigate stations, trains & Tokyo town",
                subtitleHi = "स्टेशन, ट्रेन (で), दिशाएँ और रास्ता पूछना (どこですか)",
                mascot = "🚅" to "Tokyo Station",
                tipHi = "वाहन (Train/Bus) के साथ 'で (de)' और मंज़िल (School/Station) के साथ 'へ (e)' या 'に (ni)' लगता है।",
                kanaSlice = voicedHiragana.drop(15),
                vocabSlice = placeTransportVocab,
                sentSlice = allSents.filter { it.jp.contains("駅") || it.jp.contains("どこ") || it.jp.contains("行きます") }.ifEmpty { allSents.drop(6).take(5) },
                gramSlice = allGram.drop(2).take(2)
            ),
            N5UnitBlueprint(
                unitNum = 6,
                weeks = "Month 4–5 • Weeks 16–18",
                title = "Daily action verbs & ongoing actions",
                subtitleHi = "रोज़मर्रा की क्रियाएं (ます) और अभी चल रहा काम (〜ています)",
                mascot = "⚡" to "Action Master",
                tipHi = "जिस जगह पर कोई सक्रिय काम (पढ़ना, खाना, काम करना) हो रहा हो, उस जगह के बाद हमेशा 'で (de)' लगाएँ!",
                kanaSlice = basicKatakana.take(20),
                vocabSlice = actionVerbsVocab,
                sentSlice = allSents.filter { it.jp.contains("勉強") || it.jp.contains("読みます") || it.jp.contains("話") }.ifEmpty { allSents.takeLast(6) },
                gramSlice = allGram.drop(3).take(2)
            ),
            N5UnitBlueprint(
                unitNum = 7,
                weeks = "Month 5 • Weeks 19–21",
                title = "Express feelings, likes & adjectives",
                subtitleHi = "विशेषण (い vs な), पसंद (好き) और इच्छा (〜たいです)",
                mascot = "🌸" to "Kirei & Oishii",
                tipHi = "पसंद (好き), समझना (分かる) और कुशल होना (上手) के साथ कभी を नहीं लगता — हमेशा 'が (ga)' लगता है!",
                kanaSlice = basicKatakana.drop(20),
                vocabSlice = adjectivesVocab,
                sentSlice = allSents.filter { it.jp.contains("好き") || it.jp.contains("たい") || it.jp.contains("分かります") }.ifEmpty { allSents.take(6) },
                gramSlice = allGram.takeLast(3)
            ),
            N5UnitBlueprint(
                unitNum = 8,
                weeks = "Month 6 • Weeks 22–24",
                title = "Katakana loanwords, shopping & N5 Crown",
                subtitleHi = "विदेशी शब्द (カタカナ), शॉपिंग और संपूर्ण N5 महारत",
                mascot = "👑" to "N5 Samurai",
                tipHi = "अंग्रेज़ी और विदेशी भाषाओं से आए शब्द (जैसे Sumaho, Pasokon, Konbini, Raamen) हमेशा Katakana में लिखे जाते हैं।",
                kanaSlice = basicKatakana.take(25),
                vocabSlice = katakanaObjectsVocab,
                sentSlice = allSents.takeLast(8),
                gramSlice = allGram
            )
        )

        return blueprints.mapIndexed { idx, bp ->
            val (accent, lip) = unitPalettes[idx % unitPalettes.size]
            val uId = "N5_U${bp.unitNum}"
            val vSlice = bp.vocabSlice.ifEmpty { allWords.take(8) }
            val sSlice = bp.sentSlice.ifEmpty { allSents.take(4) }
            val kSlice = bp.kanaSlice.ifEmpty { basicHiragana.take(10) }
            val gPrimary = bp.gramSlice.firstOrNull() ?: allGram.first()

            val nodes = listOf(
                PathNodeSpec(
                    id = "${uId}_N1",
                    unitId = uId,
                    stepNumber = 1,
                    title = "Core Words Pt. 1",
                    subtitleHi = "नए शब्द और उनका सही उच्चारण सीखें",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.take(6),
                    sentenceList = sSlice.take(2),
                    xpReward = 20
                ),
                PathNodeSpec(
                    id = "${uId}_N2",
                    unitId = uId,
                    stepNumber = 2,
                    title = "Kana Recognition",
                    subtitleHi = "अक्षर पहचानें और आवाज़ से मिलाएं",
                    type = PathNodeType.KANA_DOJO,
                    kanaList = kSlice.take(8),
                    vocabList = vSlice.take(4),
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N3",
                    unitId = uId,
                    stepNumber = 3,
                    title = "Core Words Pt. 2",
                    subtitleHi = "वाक्य प्रयोग और हिन्दी अर्थ अभ्यास",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.drop(4).take(6).ifEmpty { vSlice.take(6) },
                    sentenceList = sSlice.drop(1).take(2).ifEmpty { sSlice.take(2) },
                    xpReward = 20
                ),
                PathNodeSpec(
                    id = "${uId}_N4",
                    unitId = uId,
                    stepNumber = 4,
                    title = "Kana Speed Drill",
                    subtitleHi = "तेज़ी से अक्षर और शब्द पढ़ें",
                    type = PathNodeType.KANA_DOJO,
                    kanaList = kSlice.drop(5).take(8).ifEmpty { kSlice.take(8) },
                    vocabList = vSlice.takeLast(4),
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N5",
                    unitId = uId,
                    stepNumber = 5,
                    title = "Bonus Chest",
                    subtitleHi = "रैपिड रिव्यू और बोनस XP इनाम!",
                    type = PathNodeType.CHEST_REWARD,
                    vocabList = vSlice.shuffled().take(5),
                    sentenceList = sSlice.take(2),
                    xpReward = 40
                ),
                PathNodeSpec(
                    id = "${uId}_N6",
                    unitId = uId,
                    stepNumber = 6,
                    title = "Sentence & Grammar",
                    subtitleHi = gPrimary.title,
                    type = PathNodeType.SENTENCE_STUDIO,
                    vocabList = vSlice.take(5),
                    sentenceList = sSlice,
                    grammarPoint = gPrimary,
                    xpReward = 30
                ),
                PathNodeSpec(
                    id = "${uId}_N7",
                    unitId = uId,
                    stepNumber = 7,
                    title = "Unit ${bp.unitNum} Trophy",
                    subtitleHi = "इस अध्याय की अंतिम परीक्षा पास करें!",
                    type = PathNodeType.UNIT_TROPHY,
                    kanaList = kSlice.take(6),
                    vocabList = vSlice.take(8),
                    sentenceList = sSlice,
                    grammarPoint = gPrimary,
                    xpReward = 50
                )
            )

            CurriculumUnit(
                id = uId,
                level = JlptLevel.N5,
                sectionNumber = 1,
                unitNumber = bp.unitNum,
                durationLabel = bp.weeks,
                title = bp.title,
                subtitleHi = bp.subtitleHi,
                accentHex = accent,
                lipHex = lip,
                mascotEmoji = bp.mascot.first,
                mascotCaption = bp.mascot.second,
                guidebookTipHi = bp.tipHi,
                unitVocab = vSlice,
                unitSentences = sSlice,
                unitGrammar = bp.gramSlice.ifEmpty { listOf(gPrimary) },
                unitKana = kSlice,
                nodes = nodes
            )
        }
    }

    private fun buildLevelCurriculum(
        level: JlptLevel,
        sectionNum: Int,
        specs: List<Triple<String, String, Pair<String, String>>>
    ): List<CurriculumUnit> {
        val levelWords = DataRepository.words(level).ifEmpty { DataRepository.vocab }
        val levelSents = DataRepository.sentences(level).ifEmpty { DataRepository.allSentences }
        val levelGram = DataRepository.grammar(level).ifEmpty { DataRepository.allGrammar }
        val comboKana = DataRepository.allKana.filter { it.group == KanaGroup.COMBO }

        val chunkSize = max(5, levelWords.size / specs.size.coerceAtLeast(1))

        return specs.mapIndexed { idx, (title, subHi, mascot) ->
            val (accent, lip) = unitPalettes[(idx + sectionNum) % unitPalettes.size]
            val uId = "${level.label}_U${idx + 1}"
            val vSlice = levelWords.drop(idx * (chunkSize / 2)).take(10).ifEmpty { levelWords.take(8) }
            val sSlice = levelSents.drop(idx * 2).take(4).ifEmpty { levelSents.take(4) }
            val gPoint = levelGram.getOrElse(idx % levelGram.size) { levelGram.first() }

            val nodes = listOf(
                PathNodeSpec(
                    id = "${uId}_N1",
                    unitId = uId,
                    stepNumber = 1,
                    title = "Key Vocabulary",
                    subtitleHi = "इस यूनिट के मुख्य शब्द और पिच एक्सेंट",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.take(5),
                    sentenceList = sSlice.take(2),
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N2",
                    unitId = uId,
                    stepNumber = 2,
                    title = "Grammar & Nuance",
                    subtitleHi = gPoint.title,
                    type = PathNodeType.GRAMMAR_RULE,
                    vocabList = vSlice.take(4),
                    sentenceList = sSlice.take(2),
                    grammarPoint = gPoint,
                    xpReward = 30
                ),
                PathNodeSpec(
                    id = "${uId}_N3",
                    unitId = uId,
                    stepNumber = 3,
                    title = "Usage & Context",
                    subtitleHi = "वाक्यों में शब्दों का सही इस्तेमाल",
                    type = PathNodeType.VOCAB_STAR,
                    vocabList = vSlice.drop(3).take(5).ifEmpty { vSlice.take(5) },
                    sentenceList = sSlice.drop(1).take(2).ifEmpty { sSlice.take(2) },
                    xpReward = 25
                ),
                PathNodeSpec(
                    id = "${uId}_N4",
                    unitId = uId,
                    stepNumber = 4,
                    title = "Bonus Chest",
                    subtitleHi = "स्पीड रिव्यू और बोनस XP",
                    type = PathNodeType.CHEST_REWARD,
                    vocabList = vSlice.shuffled().take(5),
                    sentenceList = sSlice,
                    xpReward = 40
                ),
                PathNodeSpec(
                    id = "${uId}_N5",
                    unitId = uId,
                    stepNumber = 5,
                    title = "Sentence Mastery",
                    subtitleHi = "जापानी वाक्य संरचना और बोलचाल",
                    type = PathNodeType.SENTENCE_STUDIO,
                    vocabList = vSlice,
                    sentenceList = sSlice,
                    grammarPoint = gPoint,
                    xpReward = 35
                ),
                PathNodeSpec(
                    id = "${uId}_N6",
                    unitId = uId,
                    stepNumber = 6,
                    title = "Unit ${idx + 1} Trophy",
                    subtitleHi = "${level.label} यूनिट मास्टरी टेस्ट",
                    type = PathNodeType.UNIT_TROPHY,
                    kanaList = comboKana.take(6),
                    vocabList = vSlice,
                    sentenceList = sSlice,
                    grammarPoint = gPoint,
                    xpReward = 60
                )
            )

            CurriculumUnit(
                id = uId,
                level = level,
                sectionNumber = sectionNum,
                unitNumber = idx + 1,
                durationLabel = "Section $sectionNum • Unit ${idx + 1}",
                title = title,
                subtitleHi = subHi,
                accentHex = accent,
                lipHex = lip,
                mascotEmoji = mascot.first,
                mascotCaption = mascot.second,
                guidebookTipHi = gPoint.hindiTip,
                unitVocab = vSlice,
                unitSentences = sSlice,
                unitGrammar = listOf(gPoint),
                unitKana = comboKana.take(8),
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

    const val NEW_CARDS_PER_DAY = 15

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
// 9. 10-FORM ADVANCED VERB CONJUGATOR (N5 TO N1)
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
            "विनम्र भाषा (करता हूँ / करेंगे) — अजनबियों और ऑफिस के लिए",
            "साधारण नकारात्मक (नहीं करता) — दोस्तों के साथ",
            "वाक्य जोड़ने, कृपया कहने (〜てください) और रहा है (〜ている) के लिए",
            "भूतकाल (किया / गया) — दोस्तों के साथ",
            "अपनी इच्छा बताना (करना चाहता हूँ)",
            "क्षमता बताना (कर सकता हूँ)",
            "सलाह / प्रस्ताव (चलो करते हैं!)",
            "शर्त बताना (अगर ऐसा करें तो...)",
            "कर्मवाच्य (किसी के द्वारा किया जाना)",
            "प्रेरणार्थक (किसी से करवाना या करने की अनुमति देना)"
        )
        return names.indices.map { ConjForm(names[it], jp[it], rd[it], hints[it]) }
    }
}
