package com.nihongo.master

enum class JlptLevel(val label: String, val title: String, val subtitle: String, val color: Long) {
    N5("N5", "Beginner", "Basic words, greetings, simple sentences", 0xFF2E7D32),
    N4("N4", "Elementary", "Everyday conversation and daily life", 0xFF1565C0),
    N3("N3", "Intermediate", "Natural Japanese, opinions and reasons", 0xFFEF6C00),
    N2("N2", "Upper Intermediate", "Work, news and formal Japanese", 0xFF6A1B9A),
    N1("N1", "Advanced", "Academic and native-like expressions", 0xFFC62828)
}

enum class KanaType { HIRAGANA, KATAKANA }

enum class KanaGroup(val title: String, val tip: String) {
    BASIC("Basic (46)", "46 basic sounds. Start with these."),
    VOICED("Voiced (25)", "Add ゛ or ゜ to a basic kana to change its sound."),
    COMBO("Combo (33)", "A small や ゆ ょ blends with the kana before it.")
}

data class KanaChar(
    val char: String,
    val romaji: String,
    val type: KanaType,
    val group: KanaGroup
)

enum class WordType(val code: String, val label: String, val isVerb: Boolean) {
    V1("v1", "Verb (Ru-verb)", true),
    V5("v5", "Verb (U-verb)", true),
    VS("vs", "Verb (Suru-verb)", true),
    VK("vk", "Verb (Kuru)", true),
    N("n", "Noun", false),
    ADJI("adji", "い-Adjective", false),
    ADJNA("adjna", "な-Adjective", false),
    OTHER("x", "Expression / Adverb", false)
}

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
    val exEn: String
)

data class SentenceItem(
    val level: JlptLevel,
    val jp: String,
    val romaji: String,
    val en: String,
    val hi: String
)

data class GrammarPoint(
    val level: JlptLevel,
    val title: String,
    val structure: String,
    val explanation: String,
    val hindiTip: String,
    val exJp: String,
    val exRomaji: String,
    val exEn: String
)

enum class QuizMode(val label: String, val desc: String) {
    MEANING("Meaning", "Japanese word → English meaning"),
    READING("Reading", "Kanji → hiragana reading"),
    LISTEN("Listening", "Listen and pick the meaning"),
    KANA("Kana", "Kana → sound (romaji)")
}

data class QuizQuestion(
    val instruction: String,
    val big: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val audio: String,
    val autoPlay: Boolean = false
)

data class ConjForm(
    val name: String,
    val jp: String,
    val reading: String,
    val hint: String
)

object VerbConjugator {

    // Returns: masu, nai, te, ta, tai, potential
    private fun build(t: String, type: WordType): List<String> {
        return when (type) {
            WordType.V1 -> {
                val s = t.dropLast(1)
                listOf(s + "ます", s + "ない", s + "て", s + "た", s + "たい", s + "られる")
            }
            WordType.VS -> {
                val s = t.removeSuffix("する")
                listOf(s + "します", s + "しない", s + "して", s + "した", s + "したい", s + "できる")
            }
            WordType.VK -> {
                if (t.startsWith("来")) {
                    listOf("来ます", "来ない", "来て", "来た", "来たい", "来られる")
                } else {
                    listOf("きます", "こない", "きて", "きた", "きたい", "こられる")
                }
            }
            WordType.V5 -> {
                val last = t.last()
                val base = t.dropLast(1)
                val (i, a, e, te, ta) = when (last) {
                    'う' -> listOf("い", "わ", "え", "って", "った")
                    'つ' -> listOf("ち", "た", "て", "って", "った")
                    'る' -> listOf("り", "ら", "れ", "って", "った")
                    'む' -> listOf("み", "ま", "め", "んで", "んだ")
                    'ぶ' -> listOf("び", "ば", "べ", "んで", "んだ")
                    'ぬ' -> listOf("に", "な", "ね", "んで", "んだ")
                    'く' -> if (t.endsWith("行く") || t.endsWith("いく")) {
                        listOf("き", "か", "け", "って", "った")
                    } else {
                        listOf("き", "か", "け", "いて", "いた")
                    }
                    'ぐ' -> listOf("ぎ", "が", "げ", "いで", "いだ")
                    'す' -> listOf("し", "さ", "せ", "して", "した")
                    else -> listOf("い", "わ", "え", "って", "った")
                }
                listOf(
                    base + i + "ます",
                    base + a + "ない",
                    base + te,
                    base + ta,
                    base + i + "たい",
                    base + e + "る"
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
            "Polite (〜ます)",
            "Negative (〜ない)",
            "Te-form (〜て)",
            "Past (〜た)",
            "Want to (〜たい)",
            "Can do (potential)"
        )
        val hints = listOf(
            "Formal daily speech",
            "Plain 'not' (नहीं)",
            "Requests and 〜ている",
            "Plain past (किया / गया)",
            "करना चाहता हूँ",
            "कर सकता हूँ"
        )
        return names.indices.map { ConjForm(names[it], jp[it], rd[it], hints[it]) }
    }
}
