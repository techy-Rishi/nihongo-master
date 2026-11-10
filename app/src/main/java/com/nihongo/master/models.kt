package com.nihongo.master

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// 1. JLPT LEVELS & KANA MODELS (Duolingo Color-Theory Harmonized)
// =========================================================================
enum class JlptLevel(
    val label: String,
    val title: String,
    val subtitle: String,
    val color: Long
) {
    N5("N5", "Beginner", "Kana, greetings, numbers & daily survival", 0xFF58CC02),       // Feather Green
    N4("N4", "Elementary", "Everyday conversation and life experiences", 0xFF1CB0F6),    // Macaw Blue
    N3("N3", "Intermediate", "Natural Japanese, opinions and nuances", 0xFFFF9600),      // Fox Orange
    N2("N2", "Upper Intermediate", "Business, news and formal Keigo", 0xFFCE82FF),       // Lilac Purple
    N1("N1", "Advanced", "Academic, literary and native-level fluency", 0xFFFF4B4B)      // Cardinal Red
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
    val pitchDropIndex: Int = -1 // -1 = auto-infer, 0 = Heiban, 1 = Atamadaka, >1 = Nakadaka/Odaka
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
// 5. PITCH ACCENT ANALYZER (MORA SPLITTER & PITCH GRAPH)
// =========================================================================
object PitchAccentAnalyzer {
    private val smallKana = setOf(
        'ゃ', 'ゅ', 'ょ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ',
        'ャ', 'ュ', 'ョ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ'
    )

    /**
     * Splits any Japanese kana word into accurate phonetic moras (拍).
     * Example: "きょうと" -> ["きょ", "う", "と"] (3 moras, not 4 chars)
     */
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
// 6. SM-2 SPACED REPETITION ENGINE (SRS MEMORY SYSTEM)
// =========================================================================
enum class SrsGrade(val rating: Int, val label: String, val xpReward: Int, val colorHex: Long) {
    AGAIN(1, "Again (भूल गया)", 5, 0xFFFF4B4B),   // Cardinal Red
    HARD(3, "Hard (कठिन)", 12, 0xFFFF9600),       // Fox Orange
    GOOD(4, "Good (याद है)", 20, 0xFF58CC02),      // Feather Green
    EASY(5, "Easy (आसान)", 30, 0xFF1CB0F6)        // Macaw Blue
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
    /** Local-timezone day number so the daily queue resets at local midnight. */
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
// 7. SPEECH RECOGNITION & PRONUNCIATION SCORER
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
// 8. 10-FORM ADVANCED VERB CONJUGATOR (N5 TO N1)
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
