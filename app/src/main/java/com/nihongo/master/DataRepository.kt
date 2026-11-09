package com.nihongo.master

enum class JlptLevel(val label: String, val badgeColor: Long, val description: String) {
    N5("N5", 0xFF2E7D32, "Beginner: Basic phrases, Hiragana, Katakana & 100 Kanji"),
    N4("N4", 0xFF1565C0, "Elementary: Daily conversations & 300 Kanji"),
    N3("N3", 0xFFEF6C00, "Intermediate: Everyday situations, newspaper headlines"),
    N2("N2", 0xFF6A1B9A, "Pre-Advanced: Business Japanese & complex texts"),
    N1("N1", 0xFFC62828, "Advanced: Native-level academic, literary & formal fluency")
}

enum class KanaType { HIRAGANA, KATAKANA }
enum class KanaGroup(val title: String) {
    GOJUON("Gojūon (Basic 46)"),
    DAKUON("Dakuon & Handakuon (Voiced)"),
    YOON("Yōon (Combo Sounds)")
}

data class KanaChar(
    val char: String,
    val romaji: String,
    val type: KanaType,
    val group: KanaGroup,
    val hint: String
)

data class VocabItem(
    val id: String,
    val kanji: String,
    val kana: String,
    val romaji: String,
    val meaning: String,
    val hindiHint: String,
    val level: JlptLevel,
    val category: String,
    val exampleJp: String,
    val exampleRomaji: String,
    val exampleEn: String
)

data class GrammarPoint(
    val id: String,
    val title: String,
    val level: JlptLevel,
    val structure: String,
    val explanation: String,
    val hindiParallel: String,
    val examples: List<Triple<String, String, String>> // JP, Romaji, EN
)

data class QuizQuestion(
    val prompt: String,
    val subPrompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val level: JlptLevel,
    val audioText: String
)

object DataRepository {

    val hiraganaList: List<KanaChar> = listOf(
        // Gojuon
        KanaChar("あ", "a", KanaType.HIRAGANA, KanaGroup.GOJUON, "Looks like an Apple under a cross"),
        KanaChar("い", "i", KanaType.HIRAGANA, KanaGroup.GOJUON, "Two vertical eels standing side by side"),
        KanaChar("う", "u", KanaType.HIRAGANA, KanaGroup.GOJUON, "U-shaped curve with a roof"),
        KanaChar("え", "e", KanaType.HIRAGANA, KanaGroup.GOJUON, "Exotic bird running"),
        KanaChar("お", "o", KanaType.HIRAGANA, KanaGroup.GOJUON, "Golf ball 'o' on a course"),
        KanaChar("か", "ka", KanaType.HIRAGANA, KanaGroup.GOJUON, "Blade cutting a stick"),
        KanaChar("き", "ki", KanaType.HIRAGANA, KanaGroup.GOJUON, "Looks like an old skeleton Key"),
        KanaChar("く", "ku", KanaType.HIRAGANA, KanaGroup.GOJUON, "Cuckoo bird's open beak"),
        KanaChar("け", "ke", KanaType.HIRAGANA, KanaGroup.GOJUON, "Curved keg barrel"),
        KanaChar("こ", "ko", KanaType.HIRAGANA, KanaGroup.GOJUON, "Two Koi fish swimming parallel"),
        KanaChar("さ", "sa", KanaType.HIRAGANA, KanaGroup.GOJUON, "Signpost pointing left"),
        KanaChar("し", "shi", KanaType.HIRAGANA, KanaGroup.GOJUON, "Fishhook or 'she' has a ponytail"),
        KanaChar("す", "su", KanaType.HIRAGANA, KanaGroup.GOJUON, "Swing looping around"),
        KanaChar("せ", "se", KanaType.HIRAGANA, KanaGroup.GOJUON, "Two people sitting on a bench"),
        KanaChar("そ", "so", KanaType.HIRAGANA, KanaGroup.GOJUON, "Zig-zag sewing stitch"),
        KanaChar("た", "ta", KanaType.HIRAGANA, KanaGroup.GOJUON, "Looks like 't' + 'a'"),
        KanaChar("ち", "chi", KanaType.HIRAGANA, KanaGroup.GOJUON, "Looks like number 5 cheering"),
        KanaChar("つ", "tsu", KanaType.HIRAGANA, KanaGroup.GOJUON, "Big Tsunami wave curling"),
        KanaChar("て", "te", KanaType.HIRAGANA, KanaGroup.GOJUON, "Telescope tilted up"),
        KanaChar("と", "to", KanaType.HIRAGANA, KanaGroup.GOJUON, "Splinter in a Toe"),
        KanaChar("な", "na", KanaType.HIRAGANA, KanaGroup.GOJUON, "Nun kneeling in prayer"),
        KanaChar("に", "ni", KanaType.HIRAGANA, KanaGroup.GOJUON, "Needle and thread"),
        KanaChar("ぬ", "nu", KanaType.HIRAGANA, KanaGroup.GOJUON, "Noodles twisted on chopsticks"),
        KanaChar("ね", "ne", KanaType.HIRAGANA, KanaGroup.GOJUON, "Neko (cat) with a curled tail"),
        KanaChar("の", "no", KanaType.HIRAGANA, KanaGroup.GOJUON, "No-smoking sign loop"),
        KanaChar("は", "ha", KanaType.HIRAGANA, KanaGroup.GOJUON, "Capital H + a"),
        KanaChar("ひ", "hi", KanaType.HIRAGANA, KanaGroup.GOJUON, "Smiling mouth saying 'hee-hee'"),
        KanaChar("ふ", "fu", KanaType.HIRAGANA, KanaGroup.GOJUON, "Mount Fuji slopes"),
        KanaChar("へ", "he", KanaType.HIRAGANA, KanaGroup.GOJUON, "Arrow pointing to Heaven"),
        KanaChar("ほ", "ho", KanaType.HIRAGANA, KanaGroup.GOJUON, "Chimney house (Ho-ho-ho)"),
        KanaChar("ま", "ma", KanaType.HIRAGANA, KanaGroup.GOJUON, "Mast with two sails"),
        KanaChar("み", "mi", KanaType.HIRAGANA, KanaGroup.GOJUON, "Musical note or 21"),
        KanaChar("む", "mu", KanaType.HIRAGANA, KanaGroup.GOJUON, "Cow horn ('Moo')"),
        KanaChar("め", "me", KanaType.HIRAGANA, KanaGroup.GOJUON, "Eye (Me in Japanese)"),
        KanaChar("も", "mo", KanaType.HIRAGANA, KanaGroup.GOJUON, "Fishhook with two worms for 'More' fish"),
        KanaChar("や", "ya", KanaType.HIRAGANA, KanaGroup.GOJUON, "Yak head with horns"),
        KanaChar("ゆ", "yu", KanaType.HIRAGANA, KanaGroup.GOJUON, "U-turn fish"),
        KanaChar("よ", "yo", KanaType.HIRAGANA, KanaGroup.GOJUON, "Yo-yo dangling on string"),
        KanaChar("ら", "ra", KanaType.HIRAGANA, KanaGroup.GOJUON, "Rabbit hopping"),
        KanaChar("り", "ri", KanaType.HIRAGANA, KanaGroup.GOJUON, "River reeds flowing"),
        KanaChar("る", "ru", KanaType.HIRAGANA, KanaGroup.GOJUON, "Route with a loop at the end"),
        KanaChar("れ", "re", KanaType.HIRAGANA, KanaGroup.GOJUON, "Runner stretching leg"),
        KanaChar("ろ", "ro", KanaType.HIRAGANA, KanaGroup.GOJUON, "Road with no loop"),
        KanaChar("わ", "wa", KanaType.HIRAGANA, KanaGroup.GOJUON, "Wasp with a round belly"),
        KanaChar("を", "wo", KanaType.HIRAGANA, KanaGroup.GOJUON, "Object particle 'o'"),
        KanaChar("ん", "n", KanaType.HIRAGANA, KanaGroup.GOJUON, "Looks like cursive 'n'"),
        // Dakuon
        KanaChar("が", "ga", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ka"),
        KanaChar("ぎ", "gi", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ki"),
        KanaChar("ぐ", "gu", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ku"),
        KanaChar("げ", "ge", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ke"),
        KanaChar("ご", "go", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ko"),
        KanaChar("ざ", "za", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Sa"),
        KanaChar("じ", "ji", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Shi"),
        KanaChar("ず", "zu", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Su"),
        KanaChar("ぜ", "ze", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Se"),
        KanaChar("ぞ", "zo", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced So"),
        KanaChar("だ", "da", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ta"),
        KanaChar("で", "de", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Te"),
        KanaChar("ど", "do", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced To"),
        KanaChar("ば", "ba", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ha"),
        KanaChar("び", "bi", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Hi"),
        KanaChar("ぶ", "bu", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Fu"),
        KanaChar("べ", "be", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced He"),
        KanaChar("ぼ", "bo", KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced Ho"),
        KanaChar("ぱ", "pa", KanaType.HIRAGANA, KanaGroup.DAKUON, "Handakuon Ha"),
        KanaChar("ぴ", "pi", KanaType.HIRAGANA, KanaGroup.DAKUON, "Handakuon Hi"),
        KanaChar("ぷ", "pu", KanaType.HIRAGANA, KanaGroup.DAKUON, "Handakuon Fu"),
        KanaChar("ぺ", "pe", KanaType.HIRAGANA, KanaGroup.DAKUON, "Handakuon He"),
        KanaChar("ぽ", "po", KanaType.HIRAGANA, KanaGroup.DAKUON, "Handakuon Ho"),
        // Yoon
        KanaChar("きゃ", "kya", KanaType.HIRAGANA, KanaGroup.YOON, "Ki + small ya"),
        KanaChar("きゅ", "kyu", KanaType.HIRAGANA, KanaGroup.YOON, "Ki + small yu"),
        KanaChar("きょ", "kyo", KanaType.HIRAGANA, KanaGroup.YOON, "Ki + small yo"),
        KanaChar("しゃ", "sha", KanaType.HIRAGANA, KanaGroup.YOON, "Shi + small ya"),
        KanaChar("しゅ", "shu", KanaType.HIRAGANA, KanaGroup.YOON, "Shi + small yu"),
        KanaChar("しょ", "sho", KanaType.HIRAGANA, KanaGroup.YOON, "Shi + small yo"),
        KanaChar("ちゃ", "cha", KanaType.HIRAGANA, KanaGroup.YOON, "Chi + small ya"),
        KanaChar("ちゅ", "chu", KanaType.HIRAGANA, KanaGroup.YOON, "Chi + small yu"),
        KanaChar("ちょ", "cho", KanaType.HIRAGANA, KanaGroup.YOON, "Chi + small yo")
    )

    val katakanaList: List<KanaChar> = listOf(
        KanaChar("ア", "a", KanaType.KATAKANA, KanaGroup.GOJUON, "Angular Axe blade"),
        KanaChar("イ", "i", KanaType.KATAKANA, KanaGroup.GOJUON, "Easel stand"),
        KanaChar("ウ", "u", KanaType.KATAKANA, KanaGroup.GOJUON, "Angular roof"),
        KanaChar("エ", "e", KanaType.KATAKANA, KanaGroup.GOJUON, "Elevator shaft / Engineering beam"),
        KanaChar("オ", "o", KanaType.KATAKANA, KanaGroup.GOJUON, "Opera singer waving arm"),
        KanaChar("カ", "ka", KanaType.KATAKANA, KanaGroup.GOJUON, "Sharp corners of Hiragana Ka"),
        KanaChar("キ", "ki", KanaType.KATAKANA, KanaGroup.GOJUON, "Key teeth without loop"),
        KanaChar("ク", "ku", KanaType.KATAKANA, KanaGroup.GOJUON, "Cook's hat corner"),
        KanaChar("ケ", "ke", KanaType.KATAKANA, KanaGroup.GOJUON, "Sideways K"),
        KanaChar("コ", "ko", KanaType.KATAKANA, KanaGroup.GOJUON, "Two ninety-degree Corners"),
        KanaChar("サ", "sa", KanaType.KATAKANA, KanaGroup.GOJUON, "Sardines on a skewer"),
        KanaChar("シ", "shi", KanaType.KATAKANA, KanaGroup.GOJUON, "Smiling face looking sideways"),
        KanaChar("ス", "su", KanaType.KATAKANA, KanaGroup.GOJUON, "Ski jumper mid-air"),
        KanaChar("セ", "se", KanaType.KATAKANA, KanaGroup.GOJUON, "Angular seat"),
        KanaChar("ソ", "so", KanaType.KATAKANA, KanaGroup.GOJUON, "Two sewing needles dropping down"),
        KanaChar("タ", "ta", KanaType.KATAKANA, KanaGroup.GOJUON, "Taco wedge"),
        KanaChar("チ", "chi", KanaType.KATAKANA, KanaGroup.GOJUON, "Cheerleader with arms out"),
        KanaChar("ツ", "tsu", KanaType.KATAKANA, KanaGroup.GOJUON, "Three water drops falling steeply"),
        KanaChar("テ", "te", KanaType.KATAKANA, KanaGroup.GOJUON, "Telephone pole"),
        KanaChar("ト", "to", KanaType.KATAKANA, KanaGroup.GOJUON, "Totem pole branch"),
        KanaChar("ナ", "na", KanaType.KATAKANA, KanaGroup.GOJUON, "Nutcracker cross"),
        KanaChar("ニ", "ni", KanaType.KATAKANA, KanaGroup.GOJUON, "Two (= Ni in Japanese)"),
        KanaChar("ヌ", "nu", KanaType.KATAKANA, KanaGroup.GOJUON, "Chopsticks picking up noodles"),
        KanaChar("ネ", "ne", KanaType.KATAKANA, KanaGroup.GOJUON, "Necktie knot"),
        KanaChar("ノ", "no", KanaType.KATAKANA, KanaGroup.GOJUON, "Diagonal slash from No sign"),
        KanaChar("ハ", "ha", KanaType.KATAKANA, KanaGroup.GOJUON, "House roof"),
        KanaChar("ヒ", "hi", KanaType.KATAKANA, KanaGroup.GOJUON, "Heel of a boot"),
        KanaChar("フ", "fu", KanaType.KATAKANA, KanaGroup.GOJUON, "Flag cliff"),
        KanaChar("ヘ", "he", KanaType.KATAKANA, KanaGroup.GOJUON, "Mountain peak pointing to Heaven"),
        KanaChar("ホ", "ho", KanaType.KATAKANA, KanaGroup.GOJUON, "Holy cross with flares"),
        KanaChar("マ", "ma", KanaType.KATAKANA, KanaGroup.GOJUON, "Mathematical angle"),
        KanaChar("ミ", "mi", KanaType.KATAKANA, KanaGroup.GOJUON, "Three missile trails"),
        KanaChar("ム", "mu", KanaType.KATAKANA, KanaGroup.GOJUON, "Triangle muscle"),
        KanaChar("メ", "me", KanaType.KATAKANA, KanaGroup.GOJUON, "Sword scar across the eye"),
        KanaChar("モ", "mo", KanaType.KATAKANA, KanaGroup.GOJUON, "Monitor lizard hook"),
        KanaChar("ヤ", "ya", KanaType.KATAKANA, KanaGroup.GOJUON, "Angular yak"),
        KanaChar("ユ", "yu", KanaType.KATAKANA, KanaGroup.GOJUON, "Periscope (#1 U-boat)"),
        KanaChar("ヨ", "yo", KanaType.KATAKANA, KanaGroup.GOJUON, "Backwards E"),
        KanaChar("ラ", "ra", KanaType.KATAKANA, KanaGroup.GOJUON, "Ramen bowl lid"),
        KanaChar("リ", "ri", KanaType.KATAKANA, KanaGroup.GOJUON, "Two straight river reeds"),
        KanaChar("ル", "ru", KanaType.KATAKANA, KanaGroup.GOJUON, "Tree roots"),
        KanaChar("レ", "re", KanaType.KATAKANA, KanaGroup.GOJUON, "Red checkmark"),
        KanaChar("ロ", "ro", KanaType.KATAKANA, KanaGroup.GOJUON, "Square robot mouth"),
        KanaChar("ワ", "wa", KanaType.KATAKANA, KanaGroup.GOJUON, "Wine glass bowl"),
        KanaChar("ヲ", "wo", KanaType.KATAKANA, KanaGroup.GOJUON, "Katakana Wo"),
        KanaChar("ン", "n", KanaType.KATAKANA, KanaGroup.GOJUON, "One-eyed cyclops winking up"),
        // Dakuon & Yoon
        KanaChar("ガ", "ga", KanaType.KATAKANA, KanaGroup.DAKUON, "Voiced Ka"),
        KanaChar("ジ", "ji", KanaType.KATAKANA, KanaGroup.DAKUON, "Voiced Shi"),
        KanaChar("ダ", "da", KanaType.KATAKANA, KanaGroup.DAKUON, "Voiced Ta"),
        KanaChar("バ", "ba", KanaType.KATAKANA, KanaGroup.DAKUON, "Voiced Ha"),
        KanaChar("パ", "pa", KanaType.KATAKANA, KanaGroup.DAKUON, "Handakuon Ha"),
        KanaChar("シャ", "sha", KanaType.KATAKANA, KanaGroup.YOON, "Shi + small Ya"),
        KanaChar("シュ", "shu", KanaType.KATAKANA, KanaGroup.YOON, "Shi + small Yu"),
        KanaChar("ショ", "sho", KanaType.KATAKANA, KanaGroup.YOON, "Shi + small Yo")
    )

    val vocabList: List<VocabItem> = listOf(
        // JLPT N5
        VocabItem("v1", "私", "わたし", "watashi", "I / Me", "मैं (Main)", JlptLevel.N5, "Pronoun", "私は学生です。", "Watashi wa gakusei desu.", "I am a student."),
        VocabItem("v2", "食べる", "たべる", "taberu", "To eat", "खाना (Khaana)", JlptLevel.N5, "Verb (Ichidan)", "毎日りんごを食べます。", "Mainichi ringo o tabemasu.", "I eat an apple every day."),
        VocabItem("v3", "飲む", "のむ", "nomu", "To drink", "पीना (Peena)", JlptLevel.N5, "Verb (Godan)", "水を飲みます。", "Mizu o nomimasu.", "I drink water."),
        VocabItem("v4", "学校", "がっこう", "gakkou", "School", "विद्यालय / स्कूल", JlptLevel.N5, "Noun", "学校へ行きます。", "Gakkou e ikimasu.", "I go to school."),
        VocabItem("v5", "先生", "せんせい", "sensei", "Teacher / Master", "शिक्षक / गुरु", JlptLevel.N5, "Noun", "田中先生は優しいです。", "Tanaka-sensei wa yasashii desu.", "Professor Tanaka is kind."),
        VocabItem("v6", "時間", "じかん", "jikan", "Time / Hours", "समय (Samay)", JlptLevel.N5, "Noun", "時間がありません。", "Jikan ga arimasen.", "There is no time."),
        // JLPT N4
        VocabItem("v7", "約束", "やくそく", "yakusoku", "Promise / Appointment", "वादा (Vaada)", JlptLevel.N4, "Noun / Suru Verb", "友達と約束があります。", "Tomodachi to yakusoku ga arimasu.", "I have an appointment with a friend."),
        VocabItem("v8", "準備", "じゅんび", "junbi", "Preparation", "तैयारी (Taiyaari)", JlptLevel.N4, "Noun / Suru Verb", "旅行の準備をしました。", "Ryokou no junbi o shimashita.", "I prepared for the trip."),
        VocabItem("v9", "経験", "けいけん", "keiken", "Experience", "अनुभव (Anubhav)", JlptLevel.N4, "Noun / Suru Verb", "いい経験になりました。", "Ii keiken ni narimashita.", "It became a good experience."),
        VocabItem("v10", "続ける", "つづける", "tsuzukeru", "To continue", "जारी रखना", JlptLevel.N4, "Verb (Ichidan)", "日本語の勉強を続けます。", "Nihongo no benkyou o tsuzukemasu.", "I will continue studying Japanese."),
        // JLPT N3
        VocabItem("v11", "影響", "えいきょう", "eikyou", "Influence / Effect", "प्रभाव (Prabhaav)", JlptLevel.N3, "Noun / Suru Verb", "天気が農業に影響を与える。", "Tenki ga nougyou ni eikyou o ataeru.", "Weather affects agriculture."),
        VocabItem("v12", "解決", "かいけつ", "kaiketsu", "Resolution / Solution", "समाधान (Samaadhaan)", JlptLevel.N3, "Noun / Suru Verb", "問題を解決する方法を探す。", "Mondai o kaiketsu suru houhou o sagasu.", "Look for a way to solve the problem."),
        VocabItem("v13", "努力", "どりょく", "doryoku", "Effort / Endeavor", "प्रयास / मेहनत", JlptLevel.N3, "Noun / Suru Verb", "彼の努力が実を結んだ。", "Kare no doryoku ga mi o musunda.", "His efforts bore fruit."),
        VocabItem("v14", "偶然", "ぐうぜん", "guuzen", "By chance / Coincidence", "संयोग से (Sanyog se)", JlptLevel.N3, "Adverb / Noun", "駅で偶然彼に会った。", "Eki de guuzen kare ni atta.", "I met him by chance at the station."),
        // JLPT N2
        VocabItem("v15", "妥協", "だきょう", "dakyou", "Compromise", "समझौता (Samjhauta)", JlptLevel.N2, "Noun / Suru Verb", "品質に関しては一切妥協しない。", "Hinshitsu ni kanshite wa issai dakyou shinai.", "We make zero compromises regarding quality."),
        VocabItem("v16", "矛盾", "むじゅん", "mujun", "Contradiction / Inconsistency", "विरोधाभास", JlptLevel.N2, "Noun / Suru Verb", "彼の発言には矛盾がある。", "Kare no hatsugen ni wa mujun ga aru.", "There is a contradiction in his statement."),
        VocabItem("v17", "把握", "はあく", "haaku", "Grasp / Understanding", "पकड़ / समझ", JlptLevel.N2, "Noun / Suru Verb", "現状を正確に把握する必要がある。", "Genjou o seikaku ni haaku suru hitsuyou ga aru.", "It is necessary to accurately grasp the current situation."),
        // JLPT N1
        VocabItem("v18", "普遍的", "ふへんてき", "fuhenteki", "Universal / Omnipresent", "सार्वभौमिक (Saarvabhaumik)", JlptLevel.N1, "Na-Adjective", "それは普遍的な真理だ。", "Sore wa fuhenteki na shinri da.", "That is a universal truth."),
        VocabItem("v19", "画期的", "かっきてき", "kakkiteki", "Groundbreaking / Epoch-making", "क्रांतिकारी / अभूतपूर्व", JlptLevel.N1, "Na-Adjective", "画期的な技術が開発された。", "Kakkiteki na gijutsu ga kaihatsu sareta.", "A groundbreaking technology was developed."),
        VocabItem("v20", "不可欠", "ふかけつ", "fukaketsu", "Indispensable / Essential", "अनिवार्य (Anivaarya)", JlptLevel.N1, "Na-Adjective", "信頼関係はビジネスに不可欠である。", "Shinrai kankei wa bijinesu ni fukaketsu de aru.", "Mutual trust is indispensable in business.")
    )

    val grammarList: List<GrammarPoint> = listOf(
        GrammarPoint(
            id = "g1",
            title = "は (wa) vs が (ga) & SOV Word Order",
            level = JlptLevel.N5,
            structure = "[Topic] は [Object] を [Verb]",
            explanation = "Japanese uses Subject-Object-Verb (SOV) order, identical to Hindi. 'は' (pronounced wa) marks the main topic, while 'を' (o) marks the direct object.",
            hindiParallel = "Exact match with Hindi SOV: 'मैं (私 + は) सेब (りんご + を) खाता हूँ (食べます)।'",
            examples = listOf(
                Triple("私は日本語を勉強します。", "Watashi wa Nihongo o benkyou shimasu.", "I study Japanese."),
                Triple("猫が魚を食べました。", "Neko ga sakana o tabemashita.", "The cat ate the fish.")
            )
        ),
        GrammarPoint(
            id = "g2",
            title = "〜てはいけません (Must Not / Prohibition)",
            level = JlptLevel.N5,
            structure = "Verb (Te-form) + はいけません",
            explanation = "Used to express prohibition or rules ('You must not do X'). In casual speech, this contracts to 〜ちゃダメ (cha dame).",
            hindiParallel = "हमें यह नहीं करना चाहिए / यहाँ मना है (जैसे: यहाँ फोटो खींचना मना है)।",
            examples = listOf(
                Triple("ここで写真を撮ってはいけません。", "Koko de shashin o totte wa ikemasen.", "You must not take photos here.")
            )
        ),
        GrammarPoint(
            id = "g3",
            title = "〜たことがある (Have the experience of doing)",
            level = JlptLevel.N4,
            structure = "Verb (Ta-form / Past Plain) + ことがある",
            explanation = "Describes past life experiences ('I have done X before'). Do not use for something you did just yesterday as a routine.",
            hindiParallel = "मैं पहले कभी यह कर चुका हूँ (जैसे: मैं जापान जा चुका हूँ)।",
            examples = listOf(
                Triple("日本へ行ったことがあります。", "Nihon e itta koto ga arimasu.", "I have been to Japan before."),
                Triple("納豆を食べたことがありますか。", "Nattou o tabeta koto ga arimasu ka.", "Have you ever eaten Natto?")
            )
        ),
        GrammarPoint(
            id = "g4",
            title = "〜おかげで (Thanks to...)",
            level = JlptLevel.N3,
            structure = "Verb/Adj/Noun ( + の) + おかげで",
            explanation = "Expresses gratitude for a positive outcome resulting from someone's help or a favorable condition. (Contrast with 〜せいで for blaming a negative result).",
            hindiParallel = "की वजह से / की कृपा से (सकारात्मक परिणाम के लिए: शिक्षक की कृपा से मैं पास हो गया)।",
            examples = listOf(
                Triple("先生のおかげで、試験に合格しました。", "Sensei no okage de, shiken ni goukaku shimashita.", "Thanks to my teacher, I passed the exam.")
            )
        ),
        GrammarPoint(
            id = "g5",
            title = "〜に違いない (Must be / No doubt that)",
            level = JlptLevel.N2,
            structure = "Plain Form (Noun/Na-Adj drops だ) + に違いない",
            explanation = "Expresses strong conviction or deduction based on evidence ('It must be the case that...').",
            hindiParallel = "निश्चित रूप से / पक्का यही बात होगी।",
            examples = listOf(
                Triple("あの人はプロの選手に違いない。", "Ano hito wa puro no senshu ni chigainai.", "That person must undoubtedly be a pro athlete.")
            )
        ),
        GrammarPoint(
            id = "g6",
            title = "〜を余儀なくされる (To be forced/compelled to)",
            level = JlptLevel.N1,
            structure = "Action Noun + を余儀なくされる",
            explanation = "Formal written expression used in news and business indicating that someone had no choice but to make a difficult decision (e.g., cancellation, withdrawal) due to external circumstances.",
            hindiParallel = "मजबूर होना पड़ा / बाध्य होना पड़ा (जैसे: खराब मौसम के कारण योजना रद्द करने पर बाध्य होना पड़ा)।",
            examples = listOf(
                Triple("悪天候のため、計画の変更を余儀なくされた。", "Akutenkou no tame, keikaku no henkou o yoginaku sareta.", "Due to bad weather, we were forced to change the plan.")
            )
        )
    )

    fun generateQuizQuestions(selectedLevel: JlptLevel?): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()

        // Add Kana Questions if N5 or All
        if (selectedLevel == null || selectedLevel == JlptLevel.N5) {
            questions.add(
                QuizQuestion(
                    prompt = "What is the Romaji reading for the Hiragana 「つ」?",
                    subPrompt = "Kana Recognition • JLPT N5",
                    options = listOf("shi", "tsu", "chi", "su"),
                    correctIndex = 1,
                    explanation = "「つ」 (tsu) looks like a big curling Tsunami wave.",
                    level = JlptLevel.N5,
                    audioText = "つ"
                )
            )
            questions.add(
                QuizQuestion(
                    prompt = "Distinguish Look-Alikes: Which Katakana represents 'shi' (し)?",
                    subPrompt = "Katakana Mastery • JLPT N5",
                    options = listOf("ツ", "シ", "ソ", "ン"),
                    correctIndex = 1,
                    explanation = "「シ」 (shi) has strokes aligned more horizontally left-to-right, whereas 「ツ」 (tsu) drops vertically from the top.",
                    level = JlptLevel.N5,
                    audioText = "シ"
                )
            )
        }

        // Generate Vocab & Kanji Questions dynamically
        val pool = if (selectedLevel == null) vocabList else vocabList.filter { it.level == selectedLevel }
        pool.forEach { item ->
            val distractors = vocabList.filter { it.id != item.id }.shuffled().take(3).map { it.meaning }
            val allOptions = (distractors + item.meaning).shuffled()
            val correctIdx = allOptions.indexOf(item.meaning)

            questions.add(
                QuizQuestion(
                    prompt = "What does 「${item.kanji}」 (${item.kana}) mean?",
                    subPrompt = "Vocabulary & Kanji • ${item.level.label}",
                    options = allOptions,
                    correctIndex = correctIdx,
                    explanation = "${item.kanji} (${item.kana} / ${item.romaji}) means '${item.meaning}' (${item.hindiHint}). Example: ${item.exampleJp}",
                    level = item.level,
                    audioText = item.kanji
                )
            )

            // Also add Kanji Reading question
            val kanaDistractors = vocabList.filter { it.id != item.id }.shuffled().take(3).map { it.kana }
            val kanaOptions = (kanaDistractors + item.kana).shuffled()
            questions.add(
                QuizQuestion(
                    prompt = "Select the correct Furigana reading for 「${item.kanji}」:",
                    subPrompt = "Kanji Reading • ${item.level.label} (${item.meaning})",
                    options = kanaOptions,
                    correctIndex = kanaOptions.indexOf(item.kana),
                    explanation = "「${item.kanji}」 is read as 「${item.kana}」 (${item.romaji}).",
                    level = item.level,
                    audioText = item.kanji
                )
            )
        }

        // Grammar Questions
        val grammarPool = if (selectedLevel == null) grammarList else grammarList.filter { it.level == selectedLevel }
        grammarPool.forEach { g ->
            val ex = g.examples.first()
            questions.add(
                QuizQuestion(
                    prompt = "Which grammar pattern matches: \"${ex.third}\"?",
                    subPrompt = "Grammar Dojo • ${g.level.label}",
                    options = grammarList.map { it.title }.distinct().take(4).let { list ->
                        if (list.contains(g.title)) list.shuffled() else (list.take(3) + g.title).shuffled()
                    },
                    correctIndex = 0, // Will re-index below
                    explanation = "${g.title}: ${g.explanation}\nHindi Parallel: ${g.hindiParallel}",
                    level = g.level,
                    audioText = ex.first
                ).let { q ->
                    q.copy(correctIndex = q.options.indexOf(g.title))
                }
            )
        }

        return questions.shuffled()
    }
}
