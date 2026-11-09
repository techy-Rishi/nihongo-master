package com.nihongo.master

enum class JlptLevel(val label: String, val badgeColor: Long, val title: String, val xpRequired: Int) {
    N5("N5", 0xFF2E7D32, "Ashigaru (Beginner)", 0),
    N4("N4", 0xFF1565C0, "Samurai (Elementary)", 300),
    N3("N3", 0xFFEF6C00, "Hatamoto (Intermediate)", 800),
    N2("N2", 0xFF6A1B9A, "Daimyo (Pre-Advanced)", 1500),
    N1("N1", 0xFFC62828, "Shogun (Master Fluency)", 3000)
}

enum class KanaType { HIRAGANA, KATAKANA }
enum class KanaGroup(val title: String) {
    GOJUON("Gojūon (Basic 46)"),
    DAKUON("Dakuon/Handakuon (25)"),
    YOON("Yōon Combos (33)"),
    LOOKALIKES("Confusion Traps")
}

data class KanaChar(
    val char: String,
    val romaji: String,
    val type: KanaType,
    val group: KanaGroup,
    val row: String,
    val mnemonic: String
)

enum class PitchType(val label: String, val desc: String) {
    HEIBAN("Heiban (平板 - Flat)", "Starts Low, rises on 2nd mora, stays High even on attached particle."),
    ATAMADAKA("Atamadaka (頭高 - Head-High)", "Starts High on 1st mora, drops sharply on 2nd mora and stays Low."),
    NAKADAKA("Nakadaka (中高 - Mid-High)", "Starts Low, rises in the middle, drops before the final mora."),
    ODAKA("Odaka (尾高 - Tail-High)", "Starts Low, rises on 2nd mora, drops ONLY when a particle is attached.")
}

enum class VerbGroup(val label: String) {
    ICHIDAN("Ichidan (Ru-Verb / Group 2)"),
    GODAN("Godan (U-Verb / Group 1)"),
    IRREGULAR("Irregular (Suru / Kuru / Group 3)"),
    NOT_VERB("Noun / Adjective / Expression")
}

data class VocabItem(
    val id: String,
    val kanji: String,
    val kana: String,
    val romaji: String,
    val meaning: String,
    val hindiHint: String,
    val level: JlptLevel,
    val category: String,
    val verbGroup: VerbGroup = VerbGroup.NOT_VERB,
    val pitchPattern: List<Int>,
    val pitchType: PitchType = PitchType.HEIBAN,
    val radicals: String = "",
    val onyomi: String = "-",
    val kunyomi: String = "-",
    val mnemonic: String = "",
    val exampleJp: String,
    val exampleRomaji: String,
    val exampleEn: String,
    val exampleHi: String
)

data class GrammarPoint(
    val id: String,
    val title: String,
    val level: JlptLevel,
    val structure: String,
    val explanation: String,
    val hindiParallel: String,
    val commonMistake: String,
    val examples: List<ExampleSentence>
)

data class ExampleSentence(
    val jp: String,
    val romaji: String,
    val en: String,
    val hi: String
)

data class ImmersionStory(
    val id: String,
    val titleJp: String,
    val titleEn: String,
    val level: JlptLevel,
    val lines: List<StoryLine>,
    val comprehensionQuestion: String,
    val options: List<String>,
    val correctOption: Int
)

data class StoryLine(
    val speaker: String,
    val jp: String,
    val romaji: String,
    val en: String,
    val hi: String,
    val keyWords: List<Pair<String, String>>
)

data class SentencePuzzle(
    val id: String,
    val level: JlptLevel,
    val englishPrompt: String,
    val hindiPrompt: String,
    val correctTiles: List<String>,
    val distractorTiles: List<String>,
    val grammarNote: String
)

enum class QuizMode(val label: String) {
    MIXED("All-in-One Exam"),
    LISTENING("Audio Listening Only"),
    KANJI_READING("Kanji & Furigana"),
    CONJUGATION("Verb Conjugation Drill"),
    GRAMMAR("Grammar & Particles")
}

data class QuizQuestion(
    val prompt: String,
    val subPrompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val level: JlptLevel,
    val audioText: String,
    val isListeningOnly: Boolean = false
)

data class ConjugationForm(
    val formName: String,
    val japanese: String,
    val reading: String,
    val usageHint: String
)

private data class GodanStems(
    val iStem: String,
    val aStem: String,
    val eStem: String,
    val oStem: String,
    val teForm: String,
    val taForm: String
)

object VerbConjugator {
    fun conjugate(item: VocabItem): List<ConjugationForm> {
        if (item.verbGroup == VerbGroup.NOT_VERB) return emptyList()
        val k = item.kanji
        val r = item.kana

        return when (item.verbGroup) {
            VerbGroup.ICHIDAN -> {
                val stemK = k.removeSuffix("る")
                val stemR = r.removeSuffix("る")
                listOf(
                    ConjugationForm("Dictionary (Plain)", k, r, "Base form / Casual present-future"),
                    ConjugationForm("Polite (Masu)", "${stemK}ます", "${stemR}ます", "Formal daily speech (करता हूँ)"),
                    ConjugationForm("Negative (Nai)", "${stemK}ない", "${stemR}ない", "Plain negative (नहीं करता)"),
                    ConjugationForm("Past Plain (Ta)", "${stemK}た", "${stemR}た", "Plain past (किया / खाया)"),
                    ConjugationForm("Te-Form (Connective)", "${stemK}て", "${stemR}て", "Requests (〜てください), ongoing (〜ている)"),
                    ConjugationForm("Potential (Can do)", "${stemK}られる", "${stemR}られる", "Ability (कर सकता हूँ)"),
                    ConjugationForm("Volitional (Let's)", "${stemK}よう", "${stemR}よう", "Let's do (चलो करते हैं)"),
                    ConjugationForm("Desire (Tai)", "${stemK}たい", "${stemR}たい", "Want to do (करना चाहता हूँ)"),
                    ConjugationForm("Passive (Rareru)", "${stemK}られる", "${stemR}られる", "Done by someone (किया गया)"),
                    ConjugationForm("Causative (Saseru)", "${stemK}させる", "${stemR}させる", "Make/Let someone do (करवाना)"),
                    ConjugationForm("Conditional (Eba)", "${stemK}れば", "${stemR}れば", "If X happens (अगर ऐसा हो)"),
                    ConjugationForm("Must do (Nakereba)", "${stemK}なければならない", "${stemR}なければならない", "Must do (करना ही पड़ेगा)")
                )
            }
            VerbGroup.IRREGULAR -> {
                if (k.endsWith("する")) {
                    val prefixK = k.removeSuffix("する")
                    val prefixR = r.removeSuffix("する")
                    listOf(
                        ConjugationForm("Dictionary (Plain)", "${prefixK}する", "${prefixR}する", "Base form"),
                        ConjugationForm("Polite (Masu)", "${prefixK}します", "${prefixR}します", "Formal polite"),
                        ConjugationForm("Negative (Nai)", "${prefixK}しない", "${prefixR}しない", "Plain negative"),
                        ConjugationForm("Past Plain (Ta)", "${prefixK}した", "${prefixR}した", "Plain past"),
                        ConjugationForm("Te-Form", "${prefixK}して", "${prefixR}して", "Connective / Request"),
                        ConjugationForm("Potential (Can do)", "${prefixK}できる", "${prefixR}できる", "Can do (कर सकता हूँ)"),
                        ConjugationForm("Volitional (Let's)", "${prefixK}しよう", "${prefixR}しよう", "Let's do"),
                        ConjugationForm("Desire (Tai)", "${prefixK}したい", "${prefixR}したい", "Want to do"),
                        ConjugationForm("Passive", "${prefixK}される", "${prefixR}される", "Be done"),
                        ConjugationForm("Causative", "${prefixK}させる", "${prefixR}させる", "Make/let do"),
                        ConjugationForm("Conditional (Eba)", "${prefixK}すれば", "${prefixR}すれば", "If one does"),
                        ConjugationForm("Must do", "${prefixK}しなければならない", "${prefixR}しなければならない", "Must do")
                    )
                } else {
                    listOf(
                        ConjugationForm("Dictionary", "来る", "くる", "To come (आना)"),
                        ConjugationForm("Polite (Masu)", "来ます", "きます", "Formal polite"),
                        ConjugationForm("Negative (Nai)", "来ない", "こない", "Note reading shifts to KO-nai!"),
                        ConjugationForm("Past (Ta)", "来た", "きた", "Came"),
                        ConjugationForm("Te-Form", "来て", "きて", "Come and..."),
                        ConjugationForm("Potential", "来られる", "こられる", "Can come"),
                        ConjugationForm("Volitional", "来よう", "こよう", "Let's come"),
                        ConjugationForm("Conditional", "来れば", "くれば", "If one comes")
                    )
                }
            }
            VerbGroup.GODAN -> {
                val lastChar = k.last()
                val baseK = k.dropLast(1)
                val baseR = r.dropLast(1)
                val stems = when (lastChar) {
                    'う' -> GodanStems("い", "わ", "え", "おう", "って", "った")
                    'つ' -> GodanStems("ち", "た", "て", "とう", "って", "った")
                    'る' -> GodanStems("り", "ら", "れ", "ろう", "って", "った")
                    'む' -> GodanStems("み", "ま", "め", "もう", "んで", "んだ")
                    'ぶ' -> GodanStems("び", "ば", "べ", "ぼう", "んで", "んだ")
                    'ぬ' -> GodanStems("に", "な", "ね", "のう", "んで", "んだ")
                    'く' -> if (k == "行く") GodanStems("き", "か", "け", "こう", "って", "った")
                            else GodanStems("き", "か", "け", "こう", "いて", "いた")
                    'ぐ' -> GodanStems("ぎ", "が", "げ", "ごう", "いで", "いだ")
                    'す' -> GodanStems("し", "さ", "せ", "そう", "して", "した")
                    else -> GodanStems("い", "わ", "え", "おう", "って", "った")
                }
                listOf(
                    ConjugationForm("Dictionary (Plain)", k, r, "Base Godan form"),
                    ConjugationForm("Polite (Masu)", "$baseK${stems.iStem}ます", "$baseR${stems.iStem}ます", "I-stem + masu"),
                    ConjugationForm("Negative (Nai)", "$baseK${stems.aStem}ない", "$baseR${stems.aStem}ない", "A-stem + nai"),
                    ConjugationForm("Past Plain (Ta)", "$baseK${stems.taForm}", "$baseR${stems.taForm}", "Euphonic past"),
                    ConjugationForm("Te-Form", "$baseK${stems.teForm}", "$baseR${stems.teForm}", "Euphonic te-form"),
                    ConjugationForm("Potential (Can do)", "$baseK${stems.eStem}る", "$baseR${stems.eStem}る", "E-stem + ru (कर सकता हूँ)"),
                    ConjugationForm("Volitional (Let's)", "$baseK${stems.oStem}", "$baseR${stems.oStem}", "O-stem + u (चलो करें)"),
                    ConjugationForm("Desire (Tai)", "$baseK${stems.iStem}たい", "$baseR${stems.iStem}たい", "I-stem + tai (करना चाहता हूँ)"),
                    ConjugationForm("Passive", "$baseK${stems.aStem}れる", "$baseR${stems.aStem}れる", "A-stem + reru"),
                    ConjugationForm("Causative", "$baseK${stems.aStem}せる", "$baseR${stems.aStem}せる", "A-stem + seru"),
                    ConjugationForm("Conditional (Eba)", "$baseK${stems.eStem}ば", "$baseR${stems.eStem}ば", "E-stem + ba (अगर करें)"),
                    ConjugationForm("Must do", "$baseK${stems.aStem}なければならない", "$baseR${stems.aStem}なければならない", "Must do")
                )
            }
            else -> emptyList()
        }
    }
}
