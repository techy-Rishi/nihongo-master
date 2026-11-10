package com.nihongo.master

object DataRepository {

    private fun ws(s: String): List<String> = s.trim().split(Regex("\\s+"))

    private fun kanaSet(group: KanaGroup, h: String, k: String, r: String): List<KanaChar> {
        val hs = ws(h)
        val ks = ws(k)
        val rs = ws(r)
        val out = mutableListOf<KanaChar>()
        for (i in rs.indices) {
            out.add(KanaChar(hs[i], rs[i], KanaType.HIRAGANA, group))
            out.add(KanaChar(ks[i], rs[i], KanaType.KATAKANA, group))
        }
        return out
    }

    val allKana: List<KanaChar> by lazy {
        kanaSet(
            KanaGroup.BASIC,
            "あ い う え お か き く け こ さ し す せ そ た ち つ て と な に ぬ ね の は ひ ふ へ ほ ま み む め も や ゆ よ ら り る れ ろ わ を ん",
            "ア イ ウ エ オ カ キ ク ケ コ サ シ ス セ ソ タ チ ツ テ ト ナ ニ ヌ ネ ノ ハ ヒ フ ヘ ホ マ ミ ム メ モ ヤ ユ ヨ ラ リ ル レ ロ ワ ヲ ン",
            "a i u e o ka ki ku ke ko sa shi su se so ta chi tsu te to na ni nu ne no ha hi fu he ho ma mi mu me mo ya yu yo ra ri ru re ro wa wo n"
        ) + kanaSet(
            KanaGroup.VOICED,
            "が ぎ ぐ げ ご ざ じ ず ぜ ぞ だ ぢ づ で ど ば び ぶ べ ぼ ぱ ぴ ぷ ぺ ぽ",
            "ガ ギ グ ゲ ゴ ザ ジ ズ ゼ ゾ ダ ヂ ヅ デ ド バ ビ ブ ベ ボ パ ピ プ ペ ポ",
            "ga gi gu ge go za ji zu ze zo da ji zu de do ba bi bu be bo pa pi pu pe po"
        ) + kanaSet(
            KanaGroup.COMBO,
            "きゃ きゅ きょ しゃ しゅ しょ ちゃ ちゅ ちょ にゃ にゅ にょ ひゃ ひゅ ひょ みゃ みゅ みょ りゃ りゅ りょ ぎゃ ぎゅ ぎょ じゃ じゅ じょ びゃ びゅ びょ ぴゃ ぴゅ ぴょ",
            "キャ キュ キョ シャ シュ ショ チャ チュ チョ ニャ ニュ ニョ ヒャ ヒュ ヒョ ミャ ミュ ミョ リャ リュ リョ ギャ ギュ ギョ ジャ ジュ ジョ ビャ ビュ ビョ ピャ ピュ ピョ",
            "kya kyu kyo sha shu sho cha chu cho nya nyu nyo hya hyu hyo mya myu myo rya ryu ryo gya gyu gyo ja ju jo bya byu byo pya pyu pyo"
        )
    }

    // ---------------------------------------------------------------
    // TOKYO PITCH ACCENT OVERRIDES FOR CORE N5 WORDS
    // ---------------------------------------------------------------
    private val pitchOverrides: Map<String, Int> = mapOf(
        "食べる" to 2, "飲む" to 1, "行く" to 0, "来る" to 1, "見る" to 1,
        "聞く" to 0, "話す" to 2, "読む" to 1, "書く" to 1, "買う" to 0,
        "する" to 0, "勉強する" to 0, "起きる" to 2, "寝る" to 0, "会う" to 1,
        "帰る" to 1, "入る" to 1, "出る" to 1, "待つ" to 1, "座る" to 0,
        "歩く" to 2, "走る" to 2, "働く" to 0, "使う" to 0, "作る" to 2,
        "教える" to 0, "開ける" to 0, "閉める" to 2, "分かる" to 2, "遊ぶ" to 0,
        "泳ぐ" to 2, "乗る" to 0, "洗う" to 0, "歌う" to 0, "飛ぶ" to 0,
        "立つ" to 1, "死ぬ" to 0, "呼ぶ" to 0, "借りる" to 0, "返す" to 1,
        "私" to 0, "先生" to 3, "学生" to 0, "友達" to 0, "家族" to 1,
        "水" to 0, "お茶" to 0, "魚" to 0, "肉" to 2, "駅" to 1,
        "電車" to 0, "学校" to 0, "会社" to 0, "本" to 1, "犬" to 2,
        "猫" to 1, "日本" to 2, "今日" to 1, "明日" to 3, "昨日" to 2,
        "毎日" to 1, "時間" to 0, "天気" to 1, "車" to 0, "名前" to 0,
        "お金" to 0, "家" to 2, "部屋" to 2, "手" to 2, "目" to 1,
        "朝" to 1, "昼" to 2, "夜" to 1, "雨" to 1, "飴" to 0,
        "橋" to 2, "箸" to 1, "山" to 2, "川" to 2, "海" to 1, "店" to 2,
        "大きい" to 3, "小さい" to 3, "新しい" to 4, "古い" to 2,
        "高い" to 2, "安い" to 2, "おいしい" to 0, "楽しい" to 3,
        "忙しい" to 4, "暑い" to 2, "寒い" to 2, "いい" to 1,
        "好き" to 2, "元気" to 1, "きれい" to 1, "静か" to 1,
        "有名" to 0, "上手" to 3, "ありがとう" to 2, "すみません" to 4
    )

    private val wordTypeByCode: Map<String, WordType> by lazy { WordType.entries.associateBy { it.code } }

    private fun parseN5Blocks(blocks: List<String>): List<VocabItem> {
        var globalIdx = 1
        val out = mutableListOf<VocabItem>()
        for (raw in blocks) {
            for (line in raw.trim().lines()) {
                if (line.isBlank()) continue
                val p = line.split("|").map { it.trim() }
                fun f(i: Int) = p.getOrElse(i) { "" }
                val kanji = f(1)
                val explicitPitch = p.getOrNull(9)?.toIntOrNull() ?: pitchOverrides[kanji] ?: -1
                out.add(
                    VocabItem(
                        id = "N5_${globalIdx++}",
                        level = JlptLevel.N5,
                        type = wordTypeByCode[f(0)] ?: WordType.OTHER,
                        kanji = kanji,
                        kana = f(2),
                        romaji = f(3),
                        en = f(4),
                        hi = f(5),
                        exJp = f(6),
                        exRomaji = f(7),
                        exEn = f(8),
                        pitchDropIndex = explicitPitch
                    )
                )
            }
        }
        return out
    }

    // ===============================================================
    // UNIT 1: GREETINGS, POLITE PHRASES & SURVIVAL EXPRESSIONS (52 Words)
    // ===============================================================
    private val n5Unit1Words = """
x|おはよう|おはよう|ohayou|good morning (casual)|सुप्रभात (दोस्त से)|おはよう、今日も元気？|Ohayou, kyou mo genki?|Morning, are you doing well today?
x|おはようございます|おはようございます|ohayou gozaimasu|good morning (polite)|सुप्रभात (आदरपूर्वक)|先生、おはようございます。|Sensei, ohayou gozaimasu.|Good morning, teacher.
x|こんにちは|こんにちは|konnichiwa|hello / good afternoon|नमस्ते / दिन का अभिवादन|こんにちは、いい天気ですね。|Konnichiwa, ii tenki desu ne.|Hello, nice weather isn't it?
x|こんばんは|こんばんは|konbanwa|good evening|शुभ संध्या|こんばんは、お元気ですか。|Konbanwa, ogenki desu ka.|Good evening, how are you?
x|おやすみなさい|おやすみなさい|oyasuminasai|good night|शुभ रात्रि|おやすみなさい、また明日。|Oyasuminasai, mata ashita.|Good night, see you tomorrow.
x|さようなら|さようなら|sayounara|goodbye|अलविदा|先生、さようなら。|Sensei, sayounara.|Goodbye, teacher.
x|じゃあね|じゃあね|jaa ne|see you (casual)|फिर मिलेंगे (दोस्त से)|じゃあね、また来週！|Jaa ne, mata raishuu!|See ya, until next week!
x|また明日|またあした|mata ashita|see you tomorrow|कल फिर मिलेंगे|また明日会いましょう。|Mata ashita aimashou.|Let's meet again tomorrow.
x|はじめまして|はじめまして|hajimemashite|nice to meet you (first time)|पहली बार मिलकर खुशी हुई|はじめまして、リシです。|Hajimemashite, Rishi desu.|Nice to meet you, I am Rishi.
x|よろしくお願いします|よろしくおねがいします|yoroshiku onegaishimasu|pleased to meet you / counting on you|आपसे मिलकर अच्छा लगा / कृपा बनाए रखें|これからよろしくお願いします。|Kore kara yoroshiku onegaishimasu.|Pleased to work with you from now on.
x|ありがとう|ありがとう|arigatou|thank you (casual)|धन्यवाद|手伝ってくれてありがとう。|Tetsudatte kurete arigatou.|Thanks for helping me.
x|ありがとうございます|ありがとうございます|arigatou gozaimasu|thank you very much (polite)|बहुत-बहुत धन्यवाद|いつもありがとうございます。|Itsumo arigatou gozaimasu.|Thank you very much as always.
x|どういたしまして|どういたしまして|dou itashimashite|you're welcome|कोई बात नहीं / आपका स्वागत है|いいえ、どういたしまして。|Iie, dou itashimashite.|No, you're very welcome.
x|すみません|すみません|sumimasen|excuse me / I'm sorry|माफ़ कीजिए / सुनिए|すみません、水をください。|Sumimasen, mizu o kudasai.|Excuse me, water please.
x|ごめんなさい|ごめんなさい|gomennasai|I am sorry|मुझे माफ़ कर दीजिए|遅れてごめんなさい。|Okurete gomennasai.|I am sorry for being late.
x|失礼します|しつれいします|shitsurei shimasu|excuse me (entering/leaving room)|आज्ञा दीजिए / माफ़ कीजिए|お先に失礼します。|Osaki ni shitsurei shimasu.|Excuse me for leaving first.
x|お願いします|おねがいします|onegaishimasu|please (when requesting)|कृपया|これをお願いします。|Kore o onegaishimasu.|This one, please.
x|ください|ください|kudasai|please give me / please do|कृपया दीजिए|パンを二つください。|Pan o futatsu kudasai.|Please give me two breads.
x|いただきます|いただきます|itadakimasu|thank you for the meal (before eating)|भोजन शुरू करने से पहले धन्यवाद|それでは、いただきます！|Sore dewa, itadakimasu!|Well then, let's eat!
x|ごちそうさまでした|ごちそうさまでした|gochisousama deshita|thank you for the meal (after eating)|भोजन के बाद धन्यवाद|おいしかったです。ごちそうさまでした。|Oishikatta desu. Gochisousama deshita.|It was delicious. Thank you for the meal.
x|いってきます|いってきます|ittekimasu|I'm leaving (from home)|मैं घर से निकल रहा हूँ|学校へいってきます。|Gakkou e ittekimasu.|I'm heading off to school.
x|いってらっしゃい|いってらっしゃい|itterasshai|have a safe trip / see you later|ध्यान से जाना (घर से निकलते समय)|気をつけて、いってらっしゃい。|Ki o tsukete, itterasshai.|Take care and have a good day.
x|ただいま|ただいま|tadaima|I'm home / I'm back|मैं घर वापस आ गया हूँ|ただいま帰りました。|Tadaima kaerimashita.|I have just returned home.
x|おかえりなさい|おかえりなさい|okaerinasai|welcome home|घर वापसी पर स्वागत है|おかえりなさい、疲れたでしょう。|Okaerinasai, tsukareta deshou.|Welcome home, you must be tired.
x|いらっしゃいませ|いらっしゃいませ|irasshaimase|welcome (to a shop/restaurant)|दुकान या रेस्टोरेंट में स्वागत है|いらっしゃいませ、何名様ですか。|Irasshaimase, nanmei-sama desu ka.|Welcome, how many people in your party?
x|お邪魔します|おじゃまします|ojama shimasu|excuse me for intruding (entering someone's home)|आपके घर में प्रवेश की आज्ञा चाहता हूँ|こんにちは、お邪魔します。|Konnichiwa, ojama shimasu.|Hello, pardon the intrusion.
x|はい|はい|hai|yes / I understand|हाँ / जी|はい、分かりました。|Hai, wakarimashita.|Yes, I understood.
x|ええ|ええ|ee|yes (conversational)|हाँ (बोलचाल में)|ええ、そうですよ。|Ee, sou desu yo.|Yes, that's right.
x|いいえ|いいえ|iie|no / not at all|नहीं|いいえ、違います。|Iie, chigaimasu.|No, that's wrong.
x|おめでとうございます|おめでとうございます|omedetou gozaimasu|congratulations|बधाई हो|合格おめでとうございます！|Goukaku omedetou gozaimasu!|Congratulations on passing!
x|お大事に|おだいじに|odaiji ni|get well soon / take care of yourself|अपनी सेहत का ध्यान रखें (बीमार व्यक्ति से)|風邪ですか。お大事に。|Kaze desu ka. Odaiji ni.|Is it a cold? Get well soon.
x|気をつけて|きをつけて|ki o tsukete|take care / be careful|सावधान रहें / ध्यान रखें|道に気をつけてください。|Michi ni ki o tsukete kudasai.|Please be careful on the road.
x|お疲れ様です|おつかれさまです|otsukaresama desu|thank you for your hard work|आपकी मेहनत के लिए धन्यवाद|今日も一日お疲れ様でした。|Kyou mo ichinichi otsukaresama deshita.|Thank you for your hard work all day today.
x|頑張ってください|がんばってください|ganbatte kudasai|do your best / good luck|पूरी मेहनत करें / ऑल द बेस्ट|明日の試験、頑張ってください。|Ashita no shiken, ganbatte kudasai.|Good luck on tomorrow's exam.
x|かしこまりました|かしこまりました|kashikomarimashita|certainly (polite staff reply)|जी बिल्कुल, समझ गया|かしこまりました、少々お待ちください。|Kashikomarimashita, shoushou omachi kudasai.|Certainly, please wait a moment.
x|もちろん|もちろん|mochiron|of course|बिल्कुल / बेशक|もちろん手伝いますよ。|Mochiron tetsudaimasu yo.|Of course I will help you.
x|なるほど|なるほど|naruhodo|I see / that makes sense|अच्छा, अब समझ आया|なるほど、そういう意味ですか。|Naruhodo, sou iu imi desu ka.|I see, so that's what it means.
x|本当|ほんとう|hontou|truth / really|सच / वाकई|それは本当ですか。|Sore wa hontou desu ka.|Is that really true?
x|本当ですか|ほんとうですか|hontou desu ka|really? / is that true?|क्या सच में?|えっ、本当ですか！|E, hontou desu ka!|What, really?!
x|そうですね|そうですね|sou desu ne|let me see / that's right, isn't it|हाँ, बात तो सही है / देखते हैं|そうですね、明日にしましょう。|Sou desu ne, ashita ni shimashou.|Let's see, let's do it tomorrow.
x|そうですか|そうですか|sou desu ka|is that so? / I see|अच्छा, ऐसा है क्या?|ああ、そうですか。分かりました。|Aa, sou desu ka. Wakarimashita.|Ah, is that so? I understand.
x|違います|ちがいます|chigaimasu|that's wrong / no it isn't|नहीं, यह अलग/गलत है|いいえ、私のではありません。違います。|Iie, watashi no dewa arimasen. Chigaimasu.|No, it's not mine. That's incorrect.
x|分かりました|わかりました|wakarimashita|I understood / got it|मैं समझ गया|はい、よく分かりました。|Hai, yoku wakarimashita.|Yes, I understood well.
x|分かりません|わかりません|wakarimasen|I don't understand / I don't know|मुझे नहीं पता / समझ नहीं आया|すみません、意味が分かりません。|Sumimasen, imi ga wakarimasen.|Excuse me, I don't understand the meaning.
x|知りません|しりません|shirimasen|I do not know|मुझे जानकारी नहीं है|彼の電話番号は知りません。|Kare no denwa bangou wa shirimasen.|I don't know his phone number.
x|もう一度|もういちど|mou ichido|once more / again|एक बार और|もう一度言ってください。|Mou ichido itte kudasai.|Please say it once more.
x|ゆっくり|ゆっくり|yukkuri|slowly / at ease|धीरे-धीरे / आराम से|ゆっくり話してください。|Yukkuri hanashite kudasai.|Please speak slowly.
x|ちょっと待ってください|ちょっとまってください|chotto matte kudasai|please wait a moment|कृपया थोड़ा इंतज़ार कीजिए|すみません、ちょっと待ってください。|Sumimasen, chotto matte kudasai.|Excuse me, please wait a moment.
x|いいですよ|いいですよ|ii desu yo|sure / go ahead / it's fine|हाँ, ठीक है|ここに座ってもいいですよ。|Koko ni suwatte mo ii desu yo.|You may sit here, sure.
x|だめです|だめです|dame desu|no good / not allowed|मना है / यह ठीक नहीं है|ここで写真を撮ってはだめです。|Koko de shashin o totte wa dame desu.|You must not take photos here.
x|助けて|たすけて|tasukete|help!|बचाओ / मदद करो!|誰か助けてください！|Dareka tasukete kudasai!|Someone please help!
x|乾杯|かんぱい|kanpai|cheers!|चियर्स!|みんなで乾杯しましょう。|Minna de kanpai shimashou.|Let's all make a toast!
"""

    // ===============================================================
    // UNIT 2: PRONOUNS, DEMONSTRATIVES (これ/それ/あれ) & BASICS (52 Words)
    // ===============================================================
    private val n5Unit2Words = """
n|私|わたし|watashi|I / me|मैं|私はインドの学生です。|Watashi wa Indo no gakusei desu.|I am an Indian student.
n|僕|ぼく|boku|I / me (male polite/casual)|मैं (लड़कों के लिए)|僕は日本語が好きです。|Boku wa Nihongo ga suki desu.|I like Japanese.
n|私たち|わたしたち|watashitachi|we / us|हम|私たちは友達です。|Watashitachi wa tomodachi desu.|We are friends.
n|あなた|あなた|anata|you|आप / तुम|あなたは先生ですか。|Anata wa sensei desu ka.|Are you a teacher?
n|あの人|あのひと|ano hito|that person|वह व्यक्ति|あの人は誰ですか。|Ano hito wa dare desu ka.|Who is that person?
n|あの方|あのかた|ano kata|that person (polite)|वे सज्जन (आदरपूर्वक)|あの方は校長先生です。|Ano kata wa kouchou sensei desu.|That person is the principal.
n|彼|かれ|kare|he / boyfriend|वह (लड़का)|彼は私の親友です。|Kare wa watashi no shin'yuu desu.|He is my close friend.
n|彼女|かのじょ|kanojo|she / girlfriend|वह (लड़की)|彼女は東京に住んでいます。|Kanojo wa Toukyou ni sunde imasu.|She lives in Tokyo.
n|みんな|みんな|minna|everyone / all|सभी लोग / सब कुछ|みんなで映画を見ました。|Minna de eiga o mimashita.|Everyone watched the movie together.
n|皆さん|みなさん|minasan|everyone (polite)|आप सभी लोग|皆さん、こんにちは。|Minasan, konnichiwa.|Hello, everyone.
n|自分|じぶん|jibun|oneself / myself|खुद / स्वयं|自分の部屋を掃除します。|Jibun no heya o souji shimasu.|I clean my own room.
n|これ|これ|kore|this one (near speaker)|यह (मेरे पास वाली वस्तु)|これは私の本です。|Kore wa watashi no hon desu.|This is my book.
n|それ|それ|sore|that one (near listener)|वह (आपके पास वाली वस्तु)|それは何ですか。|Sore wa nan desu ka.|What is that near you?
n|あれ|あれ|are|that one over there|वह (दूर वाली वस्तु)|あれは東京タワーです。|Are wa Toukyou tawaa desu.|That over there is Tokyo Tower.
n|どれ|どれ|dore|which one (of three or more)|कौन-सा (तीनों में से)|あなたのかばんはどれですか。|Anata no kaban wa dore desu ka.|Which one is your bag?
x|この|この|kono|this + Noun|यह (संज्ञा के साथ)|この本はとても面白いです。|Kono hon wa totemo omoshiroi desu.|This book is very interesting.
x|その|その|sono|that + Noun (near listener)|वह (आपके पास की चीज़)|その傘を貸してください。|Sono kasa o kashite kudasai.|Please lend me that umbrella.
x|あの|あの|ano|that + Noun (over there)|वह दूर वाली (संज्ञा के साथ)|あの山は富士山です。|Ano yama wa Fujisan desu.|That mountain is Mt. Fuji.
x|どの|どの|dono|which + Noun|कौन-सा (संज्ञा के साथ)|どの車が好きですか。|Dono kuruma ga suki desu ka.|Which car do you like?
n|ここ|ここ|koko|here (this place)|यहाँ|ここに座ってください。|Koko ni suwatte kudasai.|Please sit here.
n|そこ|そこ|soko|there (near listener)|वहाँ (आपके पास)|そこに辞書があります。|Soko ni jisho ga arimasu.|There is a dictionary right there.
n|あそこ|あそこ|asoko|over there (far)|वहाँ दूर|トイレはあそこです。|Toire wa asoko desu.|The restroom is over there.
n|どこ|どこ|doko|where|कहाँ|駅はどこですか。|Eki wa doko desu ka.|Where is the station?
n|こちら|こちら|kochira|this way / this person (polite)|इस तरफ़ / ये सज्जन|どうぞこちらへ。|Douzo kochira e.|Please come this way.
n|そちら|そちら|sochira|that way (near listener)|उस तरफ़ (आपके पास)|そちらの天気はどうですか。|Sochira no tenki wa dou desu ka.|How is the weather over there?
n|あちら|あちら|achira|that way over there|उस तरफ़ (दूर)|出口はあちらです。|Deguchi wa achira desu.|The exit is that way over there.
n|どちら|どちら|dochira|which way / which one (of two) / where (polite)|कौन-सा (दो में से) / किस तरफ़|お国はどちらですか。|Okuni wa dochira desu ka.|Which country are you from?
n|こっち|こっち|kotchi|this way (casual)|इधर (बोलचाल में)|こっちに来て！|Kotchi ni kite!|Come over here!
n|そっち|そっち|sotchi|that way (casual)|उधर (तुम्हारे पास)|そっちはどう？|Sotchi wa dou?|How are things on your end?
n|あっち|あっち|atchi|over there (casual)|उधर दूर|あっちに行こう。|Atchi ni ikou.|Let's go over there.
n|どっち|どっち|dotchi|which one (casual)|दोनों में से कौन-सा|コーヒーと紅茶、どっちがいい？|Koohii to koucha, dotchi ga ii?|Coffee or tea, which do you prefer?
x|こう|こう|kou|like this / in this way|इस तरह से|こうやって書きます。|Kou yatte kakimasu.|You write it like this.
x|そう|そう|sou|like that / so|वैसे / उस तरह|はい、そうです。|Hai, sou desu.|Yes, that is so.
x|ああ|ああ|aa|like that over there|उस तरह से|ああいう本が読みたいです。|Aa iu hon ga yomitai desu.|I want to read a book like that.
x|どう|どう|dou|how / in what way|कैसे / कैसा|日本の生活はどうですか。|Nihon no seikatsu wa dou desu ka.|How is life in Japan?
n|何|なに|nani|what|क्या|何をしていますか。|Nani o shite imasu ka.|What are you doing?
n|誰|だれ|dare|who|कौन|あの人は誰ですか。|Ano hito wa dare desu ka.|Who is that person?
n|どなた|どなた|donata|who (polite)|कौन (आदरपूर्वक)|あの方はどなたですか。|Ano kata wa donata desu ka.|Who is that gentleman/lady?
n|名前|なまえ|namae|name|नाम|お名前は何ですか。|Onamae wa nan desu ka.|What is your name?
n|国|くに|kuni|country / homeland|देश|お国はどちらですか。|Okuni wa dochira desu ka.|Which country are you from?
n|日本|にほん|nihon|Japan|जापान|日本はきれいな国です。|Nihon wa kirei na kuni desu.|Japan is a beautiful country.
n|インド|インド|indo|India|भारत|私はインドから来ました。|Watashi wa Indo kara kimashita.|I came from India.
n|世界|せかい|sekai|world|दुनिया / विश्व|世界を旅行したいです。|Sekai o ryokou shitai desu.|I want to travel the world.
n|言葉|ことば|kotoba|word / language|शब्द / भाषा|新しい言葉を覚えます。|Atarashii kotoba o oboemasu.|I learn new words.
n|日本語|にほんご|nihongo|Japanese language|जापानी भाषा|日本語の勉強は楽しいです。|Nihongo no benkyou wa tanoshii desu.|Studying Japanese is fun.
n|英語|えいご|eigo|English language|अंग्रेज़ी भाषा|英語で話してもいいですか。|Eigo de hanashite mo ii desu ka.|May I speak in English?
n|意味|いみ|imi|meaning|अर्थ / मतलब|この言葉の意味は何ですか。|Kono kotoba no imi wa nan desu ka.|What is the meaning of this word?
n|質問|しつもん|shitsumon|question|सवाल / प्रश्न|質問があります。|Shitsumon ga arimasu.|I have a question.
n|答え|こたえ|kotae|answer|जवाब / उत्तर|正しい答えを選んでください。|Tadashii kotae o erande kudasai.|Please choose the correct answer.
n|例|れい|rei|example|उदाहरण|例を見て書いてください。|Rei o mite kaite kudasai.|Please look at the example and write.
n|問題|もんだい|mondai|problem / exam question|सवाल / समस्या|次の問題を読んでください。|Tsugi no mondai o yonde kudasai.|Please read the next question.
n|宿題|しゅくだい|shukudai|homework|होमवर्क|毎日宿題をします。|Mainichi shukudai o shimasu.|I do homework every day.
"""

    // ===============================================================
    // UNIT 3: PEOPLE, FAMILY (OWN vs OTHERS) & OCCUPATIONS (54 Words)
    // ===============================================================
    private val n5Unit3Words = """
n|人|ひと|hito|person / human|इंसान / व्यक्ति|あそこに人がたくさんいます。|Asoko ni hito ga takusan imasu.|There are many people over there.
n|方|かた|kata|person (polite)|व्यक्ति (आदरपूर्वक)|親切な方ですね。|Shinsetsu na kata desu ne.|What a kind person.
n|男|おとこ|otoko|man / male|पुरुष|男の人が立っています。|Otoko no hito ga tatte imasu.|A man is standing.
n|女|おんな|onna|woman / female|महिला / स्त्री|女の人が話しています。|Onna no hito ga hanashite imasu.|A woman is speaking.
n|男の子|おとこのこ|otokonoko|boy|लड़का|男の子が走っています。|Otokonoko ga hashitte imasu.|A boy is running.
n|女の子|おんなのこ|onnanoko|girl|लड़की|女の子が歌っています。|Onnanoko ga utatte imasu.|A girl is singing.
n|子供|こども|kodomo|child / children|बच्चा / बच्चे|公園に子供がいます。|Kouen ni kodomo ga imasu.|There are children in the park.
n|大人|おとな|otona|adult|वयस्क / बड़े लोग|大人は千円です。|Otona wa sen-en desu.|Adults are 1000 yen.
n|赤ちゃん|あかちゃん|akachan|baby|छोटा शिशु|赤ちゃんが寝ています。|Akachan ga nete imasu.|The baby is sleeping.
n|友達|ともだち|tomodachi|friend|दोस्त / मित्र|友達と一緒に帰ります。|Tomodachi to issho ni kaerimasu.|I go home together with my friend.
n|家族|かぞく|kazoku|family|परिवार|私の家族は四人です。|Watashi no kazoku wa yonin desu.|My family has four people.
n|両親|りょうしん|ryoushin|parents (both)|माता-पिता|両親はインドにいます。|Ryoushin wa Indo ni imasu.|My parents are in India.
n|父|ちち|chichi|my father|मेरे पिताजी (अपने पिता)|父は銀行で働いています。|Chichi wa ginkou de hataraite imasu.|My father works at a bank.
n|お父さん|おとうさん|otousan|father (someone else's / direct)|पिताजी / पापा (दूसरे के)|お父さんはお元気ですか。|Otousan wa ogenki desu ka.|How is your father?
n|母|はは|haha|my mother|मेरी माँ (अपनी माँ)|母の料理が一番好きです。|Haha no ryouri ga ichiban suki desu.|I like my mother's cooking best.
n|お母さん|おかあさん|okaasan|mother (someone else's / direct)|माताजी / मम्मी|お母さん、お腹が空いた。|Okaasan, onaka ga suita.|Mom, I'm hungry.
n|兄|あに|ani|my older brother|मेरा बड़ा भाई|兄は大学生です。|Ani wa daigakusei desu.|My older brother is a university student.
n|お兄さん|おにいさん|oniisan|older brother (polite)|बड़े भैया|お兄さんは東京にいますか。|Oniisan wa Toukyou ni imasu ka.|Is your older brother in Tokyo?
n|姉|あね|ane|my older sister|मेरी बड़ी बहन|姉は病院で働いています。|Ane wa byouin de hataraite imasu.|My older sister works at a hospital.
n|お姉さん|おねえさん|oneesan|older sister (polite)|बड़ी दीदी|お姉さんは優しいですね。|Oneesan wa yasashii desu ne.|Your older sister is kind, isn't she?
n|弟|おとうと|otouto|younger brother|छोटा भाई|弟は高校生です。|Otouto wa koukousei desu.|My younger brother is a high school student.
n|妹|いもうと|imouto|younger sister|छोटी बहन|妹は音楽が好きです。|Imouto wa ongaku ga suki desu.|My younger sister likes music.
n|兄弟|きょうだい|kyoudai|siblings / brothers & sisters|भाई-बहन|兄弟はいますか。|Kyoudai wa imasu ka.|Do you have any siblings?
n|夫|おっと|otto|my husband|मेरे पति|夫は会社員です。|Otto wa kaishain desu.|My husband is an office worker.
n|ご主人|ごしゅじん|goshujin|someone else's husband|आपके पति|ご主人はどちらにいらっしゃいますか。|Goshujin wa dochira ni irasshaimasu ka.|Where is your husband?
n|妻|つま|tsuma|my wife|मेरी पत्नी|妻と買い物に行きます。|Tsuma to kaimono ni ikimasu.|I go shopping with my wife.
n|奥さん|おくさん|okusan|someone else's wife|आपकी पत्नी|奥さんはお元気ですか。|Okusan wa ogenki desu ka.|Is your wife doing well?
n|祖父|そふ|sofu|my grandfather|मेरे दादाजी / नानाजी|祖父は八十歳です。|Sofu wa hachijussai desu.|My grandfather is 80 years old.
n|おじいさん|おじいさん|ojiisan|grandfather / old man|दादाजी / बुज़ुर्ग व्यक्ति|おじいさんは散歩が好きです。|Ojiisan wa sanpo ga suki desu.|Grandpa likes taking walks.
n|祖母|そぼ|sobo|my grandmother|मेरी दादीजी / नानीजी|祖母はとても元気です。|Sobo wa totemo genki desu.|My grandmother is very healthy.
n|おばあさん|おばあさん|obaasan|grandmother / old woman|दादीजी / बुज़ुर्ग महिला|おばあさんが昔話をしました。|Obaasan ga mukashibanashi o shimashita.|Grandma told an old folktale.
n|伯父|おじ|oji|uncle|चाचा / मामा / ताऊ|おじは大阪に住んでいます。|Oji wa Oosaka ni sunde imasu.|My uncle lives in Osaka.
n|伯母|おば|oba|aunt|चाची / मामी / बुआ|おばがケーキを作りました。|Oba ga keeki o tsukurimashita.|My aunt made a cake.
n|先生|せんせい|sensei|teacher / doctor / master|शिक्षक / डॉक्टर|田中先生は日本語の先生です。|Tanaka-sensei wa Nihongo no sensei desu.|Prof. Tanaka is a Japanese teacher.
n|教師|きょうし|kyoushi|teacher (own profession)|अध्यापक (पेशा बताते समय)|私は高校の教師です。|Watashi wa koukou no kyoushi desu.|I am a high school teacher.
n|学生|がくせい|gakusei|student|विद्यार्थी / छात्र|私は大学の学生です。|Watashi wa daigaku no gakusei desu.|I am a university student.
n|留学生|りゅうがくせい|ryuugakusei|international student|विदेशी छात्र|彼はインドからの留学生です。|Kare wa Indo kara no ryuugakusei desu.|He is an international student from India.
n|生徒|せいと|seito|pupil / school student|स्कूली छात्र|教室に生徒がいます。|Kyoushitsu ni seito ga imasu.|There are students in the classroom.
n|会社員|かいしゃいん|kaishain|company employee|कंपनी कर्मचारी|兄は会社員です。|Ani wa kaishain desu.|My older brother is an office worker.
n|社員|しゃいん|shain|employee of X company|कंपनी का स्टाफ़|私はソニーの社員です。|Watashi wa Sonii no shain desu.|I am an employee of Sony.
n|銀行員|ぎんこういん|ginkouin|bank employee|बैंक कर्मचारी|父は銀行員です。|Chichi wa ginkouin desu.|My father is a banker.
n|医者|いしゃ|isha|medical doctor|डॉक्टर|お医者さんに行きます。|Oishasan ni ikimasu.|I go to see the doctor.
n|看護師|かんごし|kangoshi|nurse|नर्स|姉は看護師です。|Ane wa kangoshi desu.|My older sister is a nurse.
n|店員|てんいん|ten'in|shop staff / clerk|दुकान का कर्मचारी|店員さんに聞きました。|Ten'in-san ni kikimashita.|I asked the store clerk.
n|駅員|えきいん|ekiin|station staff|रेलवे स्टेशन कर्मचारी|駅員さんに切符を見せます。|Ekiin-san ni kippu o misemasu.|I show the ticket to the station staff.
n|警官|けいかん|keikan|police officer|पुलिसकर्मी|警官に道を聞きました。|Keikan ni michi o kikimashita.|I asked a police officer for directions.
n|お巡りさん|おまわりさん|omawarisan|police officer (friendly)|पुलिस अंकल / सिपाही|お巡りさんが立っています。|Omawarisan ga tatte imasu.|A friendly policeman is standing there.
n|歌手|かしゅ|kashu|singer|गायक / गायिका|彼女は有名な歌手です。|Kanojo wa yuumei na kashu desu.|She is a famous singer.
n|運転手|うんてんしゅ|untenshu|driver|ड्राइवर|バスの運転手さんです。|Basu no untenshu-san desu.|He is a bus driver.
n|料理人|りょうりにん|ryourinin|chef / cook|रसोइया / शेफ़|彼は日本料理の料理人です。|Kare wa Nihon ryouri no ryourinin desu.|He is a Japanese cuisine chef.
n|エンジニア|エンジニア|enjinia|engineer|इंजीनियर|私はソフトウェアのエンジニアです。|Watashi wa sofutowea no enjinia desu.|I am a software engineer.
n|外国人|がいこくじん|gaikokujin|foreigner|विदेशी नागरिक|京都には外国人が多いです。|Kyouto ni wa gaikokujin ga ooi desu.|There are many foreigners in Kyoto.
n|日本人|にほんじん|nihonjin|Japanese person|जापानी नागरिक|私の友達は日本人です。|Watashi no tomodachi wa Nihonjin desu.|My friend is Japanese.
n|インド人|インドじん|indojin|Indian person|भारतीय नागरिक|私はインド人です。|Watashi wa Indojin desu.|I am Indian.
"""

    // ===============================================================
    // UNIT 4: KATAKANA FOREIGN LOANWORDS PT. 1 — CAFE, FOOD, TECH & MEDIA (58 Words)
    // ===============================================================
    private val n5Unit4Words = """
n|パン|パン|pan|bread|ब्रेड / पाव|毎朝パンを食べます。|Maiasa pan o tabemasu.|I eat bread every morning.
n|コーヒー|コーヒー|koohii|coffee|कॉफ़ी|熱いコーヒーを飲みます。|Atsui koohii o nomimasu.|I drink hot coffee.
n|ジュース|ジュース|juusu|juice|जूस|オレンジジュースをください。|Orenji juusu o kudasai.|Orange juice, please.
n|ミルク|ミルク|miruku|milk|मिल्क / दूध|コーヒーにミルクを入れます。|Koohii ni miruku o iremasu.|I put milk in my coffee.
n|ビール|ビール|biiru|beer|बीयर|冷たいビールを飲みます。|Tsumetai biiru o nomimasu.|I drink cold beer.
n|ワイン|ワイン|wain|wine|वाइन|フランスのワインです。|Furansu no wain desu.|It is French wine.
n|ケーキ|ケーキ|keeki|cake|केक|誕生日にケーキを食べます。|Tanjoubi ni keeki o tabemasu.|I eat cake on my birthday.
n|アイスクリーム|アイスクリーム|aisukuriimu|ice cream|आइसक्रीम|夏はアイスクリームがおいしいです。|Natsu wa aisukuriimu ga oishii desu.|Ice cream is delicious in summer.
n|チョコレート|チョコレート|chokoreeto|chocolate|चॉकलेट|甘いチョコレートが好きです。|Amai chokoreeto ga suki desu.|I like sweet chocolate.
n|カレー|カレー|karee|curry|करी|昼ご飯にカレーを食べました。|Hirugohan ni karee o tabemashita.|I ate curry for lunch.
n|ラーメン|ラーメン|raamen|ramen noodles|रामेन नूडल्स|東京のラーメンは有名です。|Toukyou no raamen wa yuumei desu.|Tokyo ramen is famous.
n|ハンバーガー|ハンバーガー|hanbaagaa|hamburger|बर्गर|ハンバーガーを一つください。|Hanbaagaa o hitotsu kudasai.|One hamburger, please.
n|サンドイッチ|サンドイッチ|sandoitchi|sandwich|सैंडविच|朝はサンドイッチを作ります。|Asa wa sandoitchi o tsukurimasu.|I make sandwiches in the morning.
n|サラダ|サラダ|sarada|salad|सलाद|野菜のサラダを食べます。|Yasai no sarada o tabemasu.|I eat a vegetable salad.
n|スープ|スープ|suupu|soup|सूप|温かいスープを飲みます。|Atatakai suupu o nomimasu.|I drink warm soup.
n|チーズ|チーズ|chiizu|cheese|चीज़|チーズが大好きです。|Chiizu ga daisuki desu.|I love cheese.
n|バター|バター|bataa|butter|मक्खन (बटर)|パンにバターを塗ります。|Pan ni bataa o nurimasu.|I spread butter on bread.
n|ジャム|ジャム|jamu|jam|जैम|いちごのジャムを買いました。|Ichigo no jamu o kaimashita.|I bought strawberry jam.
n|バナナ|バナナ|banana|banana|केला|朝、バナナを食べます。|Asa, banana o tabemasu.|I eat a banana in the morning.
n|オレンジ|オレンジ|orenji|orange|संतरा|オレンジを三つ買いました。|Orenji o mittsu kaimashita.|I bought three oranges.
n|トマト|トマト|tomato|tomato|टमाटर|赤いトマトですね。|Akai tomato desu ne.|That's a red tomato.
n|レモン|レモン|remon|lemon|नींबू|レモンは酸っぱいです。|Remon wa suppai desu.|Lemons are sour.
n|フォーク|フォーク|fooku|fork|काँटा (Fork)|フォークとナイフを使います。|Fooku to naifu o tsukaimasu.|I use a fork and knife.
n|スプーン|スプーン|supuun|spoon|चम्मच (Spoon)|スプーンでカレーを食べます。|Supuun de karee o tabemasu.|I eat curry with a spoon.
n|ナイフ|ナイフ|naifu|knife|चाकू (Knife)|ナイフで肉を切ります。|Naifu de niku o kirimasu.|I cut meat with a knife.
n|コップ|コップ|koppu|cup / glass|गिलास / कप|コップに水を入れます。|Koppu ni mizu o iremasu.|I pour water into a glass.
n|グラス|グラス|gurasu|drinking glass|काँच का गिलास|きれいなグラスですね。|Kirei na gurasu desu ne.|That is a pretty glass.
n|カップ|カップ|kappu|mug / coffee cup|कॉफ़ी कप|コーヒーカップを買いました。|Koohii kappu o kaimashita.|I bought a coffee cup.
n|テーブル|テーブル|teeburu|table|मेज़ / टेबल|テーブルの上に皿があります。|Teeburu no ue ni sara ga arimasu.|There is a plate on the table.
n|メニュー|メニュー|menyuu|menu|मेन्यू कार्ड|すみません、メニューをください。|Sumimasen, menyuu o kudasai.|Excuse me, the menu please.
n|レストラン|レストラン|resutoran|restaurant|रेस्टोरेंट|家族とレストランへ行きます。|Kazoku to resutoran e ikimasu.|I go to a restaurant with my family.
n|カフェ|カフェ|kafe|cafe|कैफ़े|駅前のカフェで休みます。|Ekimae no kafe de yasumimasu.|I rest at a cafe in front of the station.
n|スマホ|スマホ|sumaho|smartphone|स्मार्टफ़ोन|毎日スマホを使います。|Mainichi sumaho o tsukaimasu.|I use a smartphone every day.
n|パソコン|パソコン|pasokon|personal computer / laptop|लैपटॉप / कंप्यूटर|パソコンでレポートを書きます。|Pasokon de repooto o kakimasu.|I write a report on my PC.
n|テレビ|テレビ|terebi|television|टीवी|夜、テレビを見ます。|Yoru, terebi o mimasu.|I watch TV at night.
n|ラジオ|ラジオ|rajio|radio|रेडियो|毎朝ラジオを聞きます。|Maiasa rajio o kikimasu.|I listen to the radio every morning.
n|カメラ|カメラ|kamera|camera|कैमरा|日本のカメラは有名です。|Nihon no kamera wa yuumei desu.|Japanese cameras are famous.
n|ビデオ|ビデオ|bideo|video|वीडियो|ビデオを見ました。|Bideo o mimashita.|I watched a video.
n|インターネット|インターネット|intaanetto|internet|इंटरनेट|インターネットで調べます。|Intaanetto de shirabemasu.|I look it up on the internet.
n|メール|メール|meeru|email / message|ईमेल / मैसेज|先生にメールを送ります。|Sensei ni meeru o okurimasu.|I send an email to the teacher.
n|アプリ|アプリ|apuri|app / application|मोबाइल ऐप|このアプリで日本語を勉強します。|Kono apuri de Nihongo o benkyou shimasu.|I study Japanese with this app.
n|ニュース|ニュース|nyuusu|news|समाचार / न्यूज़|毎朝ニュースを見ます。|Maiasa nyuusu o mimasu.|I watch the news every morning.
n|アニメ|アニメ|anime|anime / animation|एनीमे|日本のアニメが大好きです。|Nihon no anime ga daisuki desu.|I love Japanese anime.
n|ゲーム|ゲーム|geemu|game|गेम|友達とゲームをします。|Tomodachi to geemu o shimasu.|I play games with my friend.
n|スポーツ|スポーツ|supootsu|sports|खेल-कूद|どんなスポーツが好きですか。|Donna supootsu ga suki desu ka.|What kind of sports do you like?
n|サッカー|サッカー|sakkaa|soccer / football|फ़ुटबॉल|週末にサッカーをします。|Shuumatsu ni sakkaa o shimasu.|I play soccer on weekends.
n|テニス|テニス|tenisu|tennis|टेनिस|公園でテニスをします。|Kouen de tenisu o shimasu.|I play tennis in the park.
n|プール|プール|puuru|swimming pool|स्विमिंग पूल|プールで泳ぎます。|Puuru de oyogimasu.|I swim in the pool.
n|ギター|ギター|gitaa|guitar|गिटार|彼はギターが上手です。|Kare wa gitaa ga jouzu desu.|He is good at playing the guitar.
n|ピアノ|ピアノ|piano|piano|पियानो|妹はピアノを弾きます。|Imouto wa piano o hikimasu.|My younger sister plays the piano.
n|ダンス|ダンス|dansu|dance|डांस / नृत्य|ダンスの練習をします。|Dansu no renshuu o shimasu.|I practice dancing.
n|パーティー|パーティー|paatii|party|पार्टी|土曜日にパーティーがあります。|Doyoubi ni paatii ga arimasu.|There is a party on Saturday.
n|プレゼント|プレゼント|purezento|present / gift|उपहार / गिफ्ट|友達にプレゼントをあげました。|Tomodachi ni purezento o agemashita.|I gave a present to my friend.
n|チケット|チケット|chiketto|ticket|टिकट|コンサートのチケットを買いました。|Konsaato no chiketto o kaimashita.|I bought a concert ticket.
n|ノート|ノート|nooto|notebook|नोटबुक / कॉपी|ノートに漢字を書きます。|Nooto ni kanji o kakimasu.|I write kanji in my notebook.
n|ペン|ペン|pen|pen|पेन / कलम|黒いペンで書いてください。|Kuroi pen de kaite kudasai.|Please write with a black pen.
n|ボールペン|ボールペン|boorupen|ballpoint pen|बॉलपेन|ボールペンを貸してください。|Boorupen o kashite kudasai.|Please lend me a ballpoint pen.
n|カレンダー|カレンダー|karendaa|calendar|कैलेंडर|壁にカレンダーを掛けます。|Kabe ni karendaa o kakemasu.|I hang a calendar on the wall.
"""

    // ===============================================================
    // UNIT 5: KATAKANA FOREIGN WORDS PT. 2 — CLOTHES, HOTEL, TRAVEL & HOME (58 Words)
    // ===============================================================
    private val n5Unit5Words = """
n|シャツ|シャツ|shatsu|shirt|शर्ट|白いシャツを着ます。|Shiroi shatsu o kimasu.|I wear a white shirt.
n|Tシャツ|ティーシャツ|tiishatsu|T-shirt|टी-शर्ट|夏はTシャツを着ます。|Natsu wa tiishatsu o kimasu.|I wear a T-shirt in summer.
n|ワイシャツ|ワイシャツ|waishatsu|dress shirt / business shirt|फ़ॉर्मल शर्ट|新しいワイシャツを買いました。|Atarashii waishatsu o kaimashita.|I bought a new dress shirt.
n|コート|コート|kooto|coat|कोट|寒いからコートを着ます。|Samui kara kooto o kimasu.|Since it's cold, I wear a coat.
n|セーター|セーター|seetaa|sweater|स्वेटर|赤いセーターですね。|Akai seetaa desu ne.|That's a red sweater.
n|ジャケット|ジャケット|jaketto|jacket|जैकेट|黒いジャケットがかっこいいです。|Kuroi jaketto ga kakkoii desu.|The black jacket looks cool.
n|スーツ|スーツ|suutsu|suit|सूट|会社へスーツで行きます。|Kaisha e suutsu de ikimasu.|I go to the office in a suit.
n|ズボン|ズボン|zubon|trousers / pants|पैंट / पतलून|青いズボンを履きます。|Aoi zubon o hakimasu.|I put on blue trousers.
n|スカート|スカート|sukaato|skirt|स्कर्ट|彼女はスカートを履いています。|Kanojo wa sukaato o haite imasu.|She is wearing a skirt.
n|ポケット|ポケット|poketto|pocket|जेब (पॉकेट)|ポケットに鍵を入れます。|Poketto ni kagi o iremasu.|I put the key in my pocket.
n|ボタン|ボタン|botan|button|बटन|このボタンを押してください。|Kono botan o oshite kudasai.|Please press this button.
n|ネクタイ|ネクタイ|nekutai|necktie|टाई|父はネクタイをしています。|Chichi wa nekutai o shite imasu.|My father is wearing a necktie.
n|ベルト|ベルト|beruto|belt|बेल्ट|シートベルトを締めてください。|Shiito beruto o shimete kudasai.|Please fasten your seatbelt.
n|スリッパ|スリッパ|surippa|slippers|चप्पल / स्लिपर|部屋でスリッパを履きます。|Heya de surippa o hakimasu.|I wear slippers in the room.
n|ハンカチ|ハンカチ|hankachi|handkerchief|रुमाल|ポケットにハンカチがあります。|Poketto ni hankachi ga arimasu.|There is a handkerchief in my pocket.
n|タオル|タオル|taoru|towel|तौलिया|タオルで手を拭きます。|Taoru de te o fukimasu.|I wipe my hands with a towel.
n|石鹸|せっけん|sekken|soap|साबुन|石鹸で手を洗います。|Sekken de te o araimasu.|I wash my hands with soap.
n|シャワー|シャワー|shawaa|shower|शॉवर|毎朝シャワーを浴びます。|Maiasa shawaa o abimasu.|I take a shower every morning.
n|トイレ|トイレ|toire|toilet / restroom|टॉयलेट / वॉशरूम|すみません、トイレはどこですか。|Sumimasen, toire wa doko desu ka.|Excuse me, where is the restroom?
n|ベッド|ベッド|beddo|bed|बिस्तर / बेड|ベッドで寝ます。|Beddo de nemasu.|I sleep in a bed.
n|ソファ|ソファ|sofa|sofa / couch|सोफ़ा|ソファに座って本を読みます。|Sofa ni suwatte hon o yomimasu.|I sit on the sofa and read a book.
n|エアコン|エアコン|eakon|air conditioner|एसी (Air Conditioner)|暑いのでエアコンをつけます。|Atsui node eakon o tsukemasu.|Because it's hot, I turn on the AC.
n|ストーブ|ストーブ|sutoobu|heater / stove|हीटर|冬はストーブをつけます。|Fuyu wa sutoobu o tsukemasu.|In winter, I turn on the heater.
n|ドア|ドア|doa|door|दरवाज़ा|ドアを開けてください。|Doa o akete kudasai.|Please open the door.
n|エレベーター|エレベーター|erebeetaa|elevator / lift|लिफ़्ट|エレベーターで五階へ行きます。|Erebeetaa de gokai e ikimasu.|I go to the 5th floor by elevator.
n|エスカレーター|エスカレーター|esukareetaa|escalator|एस्केलेटर|エスカレーターに乗ります。|Esukareetaa ni norimasu.|I take the escalator.
n|ホテル|ホテル|hoteru|hotel|होटल|駅の近くのホテルに泊まります。|Eki no chikaku no hoteru ni tomarimasu.|I stay at a hotel near the station.
n|ロビー|ロビー|robii|lobby|लॉबी|ホテルのロビーで待ちます。|Hoteru no robii de machimasu.|I wait in the hotel lobby.
n|アパート|アパート|apaato|apartment building|अपार्टमेंट|小さいアパートに住んでいます。|Chiisai apaato ni sunde imasu.|I live in a small apartment.
n|マンション|マンション|manshon|modern condo / apartment|बहुमंज़िला अपार्टमेंट|新しいマンションに引っ越しました。|Atarashii manshon ni hikkoshimashita.|I moved into a new condo.
n|デパート|デパート|depaato|department store|डिपार्टमेंटल स्टोर / मॉल|日曜日にデパートへ行きます。|Nichiyoubi ni depaato e ikimasu.|I go to the department store on Sunday.
n|スーパー|スーパー|suupaa|supermarket|सुपरमार्केट|スーパーで野菜を買います。|Suupaa de yasai o kaimasu.|I buy vegetables at the supermarket.
n|コンビニ|コンビニ|konbini|convenience store|24x7 कन्वीनियंस स्टोर|コンビニでお弁当を買いました。|Konbini de obentou o kaimashita.|I bought a bento at the convenience store.
n|ビル|ビル|biru|tall building|इमारत / बिल्डिंग|あの高いビルは何ですか。|Ano takai biru wa nan desu ka.|What is that tall building?
n|バス|バス|basu|bus|बस|バスで京都駅へ行きます。|Basu de Kyouto-eki e ikimasu.|I go to Kyoto Station by bus.
n|タクシー|タクシー|takushii|taxi|टैक्सी|タクシーを呼びましょう。|Takushii o yobimashou.|Let's call a taxi.
n|トラック|トラック|torakku|truck|ट्रक|大きいトラックが通ります。|Ookii torakku ga toorimasu.|A big truck passes by.
n|オートバイ|オートバイ|ootobai|motorcycle|मोटरसाइकिल / बाइक|彼はオートバイに乗っています。|Kare wa ootobai ni notte imasu.|He rides a motorcycle.
n|ガソリン|ガソリン|gasorin|gasoline / petrol|पेट्रोल|ガソリンを入れます。|Gasorin o iremasu.|I put in petrol.
n|パスポート|パスポート|pasupooto|passport|पासपोर्ट|パスポートを見せてください。|Pasupooto o misete kudasai.|Please show your passport.
n|スーツケース|スーツケース|suutsukeesu|suitcase|सूटकेस|大きいスーツケースを持っています。|Ookii suutsukeesu o motte imasu.|I am carrying a big suitcase.
n|ガイド|ガイド|gaido|guide / guidebook|गाइड|ガイドさんと一緒に歩きます。|Gaido-san to issho ni arukimasu.|I walk together with the guide.
n|グループ|グループ|guruupu|group|समूह / ग्रुप|四人のグループで作ります。|Yonin no guruupu de tsukurimasu.|We make it in a group of four.
n|クラス|クラス|kurasu|class|क्लास / कक्षा|私のクラスは二十人です。|Watashi no kurasu wa nijuunin desu.|My class has 20 students.
n|テスト|テスト|tesuto|test / quiz|टेस्ट / परीक्षा|明日、漢字のテストがあります。|Ashita, kanji no tesuto ga arimasu.|There is a kanji test tomorrow.
n|ページ|ページ|peeji|page|पन्ना / पेज|十ページを開けてください。|Juppeeji o akete kudasai.|Please open to page 10.
n|コピー|コピー|kopii|copy / photocopy|फ़ोटोकॉपी|この資料をコピーしてください。|Kono shiryou o kopii shite kudasai.|Please photocopy this document.
n|テープ|テープ|teepu|tape / cassette|टेप|テープを聞いて答えてください。|Teepu o kiite kotaete kudasai.|Please listen to the tape and answer.
n|レコード|レコード|rekoodo|record|रिकॉर्ड|古いレコードを聞きます。|Furui rekoodo o kikimasu.|I listen to an old record.
n|フィルム|フィルム|firumu|film|फ़िल्म रोल|カメラにフィルムを入れます。|Kamera ni firumu o iremasu.|I put film in the camera.
n|ペット|ペット|petto|pet|पालतू जानवर|家でペットを飼っています。|Ie de petto o katte imasu.|I keep a pet at home.
n|ポスト|ポスト|posuto|mailbox / postbox|लेटरबॉक्स|ポストに手紙を入れます。|Posuto ni tegami o iremasu.|I put a letter in the mailbox.
n|プール|プール|puuru|pool|स्विमिंग पूल|夏は毎日プールで泳ぎます。|Natsu wa mainichi puuru de oyogimasu.|In summer I swim in the pool every day.
n|グラム|グラム|guramu|gram|ग्राम|肉を三百グラムください。|Niku o sanbyaku guramu kudasai.|Please give me 300 grams of meat.
n|メートル|メートル|meetoru|meter|मीटर|駅まで五百メートルです。|Eki made gohyaku meetoru desu.|It is 500 meters to the station.
n|キロ|キロ|kiro|kilogram / kilometer|किलो / किलोमीटर|りんごを二キロ買いました。|Ringo o nikiro kaimashita.|I bought two kilos of apples.
n|ゼロ|ゼロ|zero|zero (0)|शून्य (0)|ゼロから始めましょう。|Zero kara hajimemashou.|Let's start from zero.
n|パーセント|パーセント|paasento|percent (%)|प्रतिशत (%)|百パーセント準備できました。|Hyaku paasento junbi dekimashita.|I am 100 percent prepared.
"""

    // ===============================================================
    // UNIT 6: NUMBERS (0–10,000), MONEY & ESSENTIAL N5 COUNTERS (56 Words)
    // ===============================================================
    private val n5Unit6Words = """
n|零|れい|rei|zero (0)|शून्य (0)|電話番号の零を読みます。|Denwa bangou no rei o yomimasu.|I read the zero in the phone number.
n|一|いち|ichi|one (1)|एक (1)|一から十まで数えます。|Ichi kara juu made kazoemasu.|I count from one to ten.
n|二|に|ni|two (2)|दो (2)|二月に日本へ行きます。|Nigatsu ni Nihon e ikimasu.|I go to Japan in February.
n|三|さん|san|three (3)|तीन (3)|三時に会いましょう。|Sanji ni aimashou.|Let's meet at three o'clock.
n|四|よん|yon|four (4)|चार (4)|四階に図書館があります。|Yonkai ni toshokan ga arimasu.|There is a library on the 4th floor.
n|五|ご|go|five (5)|पाँच (5)|五分だけ待ってください。|Gofun dake matte kudasai.|Please wait just five minutes.
n|六|ろく|roku|six (6)|छह (6)|毎朝六時に起きます。|Maiasa rokuji ni okimasu.|I wake up at six every morning.
n|七|なな|nana|seven (7)|सात (7)|七時に朝ご飯を食べます。|Shichiji ni asagohan o tabemasu.|I eat breakfast at seven.
n|八|はち|hachi|eight (8)|आठ (8)|八時に家を出ます。|Hachiji ni ie o demasu.|I leave home at eight.
n|九|きゅう|kyuu|nine (9)|नौ (9)|学校は九時から始まります。|Gakkou wa kuji kara hajimarimasu.|School starts at nine.
n|十|じゅう|juu|ten (10)|दस (10)|駅まで歩いて十分です。|Eki made aruite juppun desu.|It is 10 minutes on foot to the station.
n|百|ひゃく|hyaku|one hundred (100)|सौ (100)|このペンは百円です。|Kono pen wa hyaku-en desu.|This pen is 100 yen.
n|三百|さんびゃく|sanbyaku|three hundred (300 - voiced!)|तीन सौ (300)|三百円のノートを買いました。|Sanbyaku-en no nooto o kaimashita.|I bought a 300-yen notebook.
n|六百|ろっぴゃく|roppyaku|six hundred (600 - special!)|छह सौ (600)|これは六百円です。|Kore wa roppyaku-en desu.|This is 600 yen.
n|八百|はっぴゃく|happyaku|eight hundred (800 - special!)|आठ सौ (800)|全部で八百円になります。|Zenbu de happyaku-en ni narimasu.|It comes to 800 yen in total.
n|千|せん|sen|one thousand (1,000)|एक हज़ार (1,000)|千円札を出しました。|Sen-en satsu o dashimashita.|I took out a 1000-yen bill.
n|三千|さんぜん|sanzen|three thousand (3,000 - voiced!)|तीन हज़ार (3,000)|このかばんは三千円です。|Kono kaban wa sanzen-en desu.|This bag is 3000 yen.
n|八千|はっせん|hassen|eight thousand (8,000 - special!)|आठ हज़ार (8,000)|靴を八千円で買いました。|Kutsu o hassen-en de kaimashita.|I bought shoes for 8000 yen.
n|一万|いちまん|ichiman|ten thousand (10,000)|दस हज़ार (10,000)|一万円を両替します。|Ichiman-en o ryougae shimasu.|I exchange 10,000 yen.
n|お金|おかね|okane|money|पैसा / धन|お金が足りません。|Okane ga tarimasen.|I don't have enough money.
n|円|えん|en|Japanese Yen (¥)|येन (जापानी मुद्रा)|百円玉が三つあります。|Hyaku-en dama ga mittsu arimasu.|I have three 100-yen coins.
x|いくら|いくら|ikura|how much (price)|कितने का (कीमत)|すみません、これはいくらですか。|Sumimasen, kore wa ikura desu ka.|Excuse me, how much is this?
x|いくつ|いくつ|ikutsu|how many / how old|कितने (संख्या) / कितनी उम्र|みかんをいくつ買いましたか。|Mikan o ikutsu kaimashita ka.|How many mandarins did you buy?
x|一つ|ひとつ|hitotsu|one thing (general counter)|एक (सामान्य वस्तु)|りんごを一つください。|Ringo o hitotsu kudasai.|Please give me one apple.
x|二つ|ふたつ|futatsu|two things|दो (वस्तुएं)|パンを二つ買いました。|Pan o futatsu kaimashita.|I bought two breads.
x|三つ|みっつ|mittsu|three things|तीन (वस्तुएं)|椅子が三つあります。|Isu ga mittsu arimasu.|There are three chairs.
x|四つ|よっつ|yottsu|four things|चार (वस्तुएं)|卵を四つ使います。|Tamago o yottsu tsukaimasu.|I use four eggs.
x|五つ|いつつ|itsutsu|five things|पाँच (वस्तुएं)|箱が五つ並んでいます。|Hako ga itsutsu narande imasu.|Five boxes are lined up.
x|六つ|むっつ|muttsu|six things|छह (वस्तुएं)|ドーナツを六つ買いました。|Doonatsu o muttsu kaimashita.|I bought six donuts.
x|七つ|ななつ|nanatsu|seven things|सात (वस्तुएं)|テーブルの上に皿が七つあります。|Teeburu no ue ni sara ga nanatsu arimasu.|There are seven plates on the table.
x|八つ|やっつ|yattsu|eight things|आठ (वस्तुएं)|りんごが八つあります。|Ringo ga yattsu arimasu.|There are eight apples.
x|九つ|ここのつ|kokonotsu|nine things|नौ (वस्तुएं)|漢字を九つ書きました。|Kanji o kokonotsu kakimashita.|I wrote nine kanji.
x|十|とお|too|ten things (native counter)|दस (वस्तुएं)|みかんが十あります。|Mikan ga too arimasu.|There are ten mandarins.
n|一人|ひとり|hitori|one person / alone|एक व्यक्ति / अकेले|一人で東京へ行きます。|Hitori de Toukyou e ikimasu.|I go to Tokyo alone.
n|二人|ふたり|futari|two people|दो लोग|友達と二人で食事しました。|Tomodachi to futari de shokuji shimashita.|Two of us, my friend and I, had a meal.
n|三人|さんにん|sannin|three people|तीन लोग|学生が三人います。|Gakusei ga sannin imasu.|There are three students.
n|四人|よにん|yonin|four people (not よんにん!)|चार लोग|家族は四人です。|Kazoku wa yonin desu.|My family has four members.
n|何人|なんにん|nannin|how many people|कितने लोग|クラスに何人いますか。|Kurasu ni nannin imasu ka.|How many people are in the class?
x|一枚|いちまい|ichimai|one flat object (paper/shirt/plate)|एक (कागज़/शर्ट/प्लेट)|紙を一枚ください。|Kami o ichimai kudasai.|Please give me one sheet of paper.
x|一本|いっぽん|ippon|one long cylindrical object (pen/bottle/umbrella)|एक (पेन/बोतल/छाता)|ペンを一本貸してください。|Pen o ippon kashite kudasai.|Please lend me one pen.
x|一台|いちだい|ichidai|one machine or vehicle (car/PC)|एक (गाड़ी/मशीन/कंप्यूटर)|家に車が二台あります。|Ie ni kuruma ga nidai arimasu.|There are two cars at home.
x|一冊|いっさつ|issatsu|one bound book / notebook|एक (किताब/नोटबुक)|本を一冊買いました。|Hon o issatsu kaimashita.|I bought one book.
x|一匹|いっぴき|ippiki|one small animal (dog/cat/fish)|एक (छोटा जानवर)|猫を二匹飼っています。|Neko o nihiki katte imasu.|I have two cats.
x|一階|いっかい|ikkai|first floor / ground floor|पहली मंज़िल|受付は一階にあります。|Uketsuke wa ikkai ni arimasu.|The reception is on the first floor.
x|何階|なんがい|nangai|which floor|कौन-सी मंज़िल|靴売り場は何階ですか。|Kutsu uriba wa nangai desu ka.|Which floor is the shoe department on?
x|一杯|いっぱい|ippai|one cup/glass of drink / full|एक कप/गिलास|水をもう一杯ください。|Mizu o mou ippai kudasai.|Please give me one more glass of water.
x|一番|いちばん|ichiban|number one / the most / best|नंबर एक / सबसे ज़्यादा|果物の中でりんごが一番好きです。|Kudamono no naka de ringo ga ichiban suki desu.|Among fruits, I like apples the most.
x|一回|いっかい|ikkai|one time / once|एक बार|日に三回薬を飲みます。|Hi ni sankai kusuri o nomimasu.|I take medicine three times a day.
x|何歳|なんさい|nansai|how old (age)|कितने साल (उम्र)|弟さんは何歳ですか。|Otouto-san wa nansai desu ka.|How old is your younger brother?
x|二十歳|はたち|hatachi|20 years old (special reading!)|बीस वर्ष (20 साल)|今年、二十歳になります。|Kotoshi, hatachi ni narimasu.|I will turn 20 years old this year.
n|半分|はんぶん|hanbun|half|आधा (Half)|パンを半分食べました。|Pan o hanbun tabemashita.|I ate half the bread.
n|全部|ぜんぶ|zenbu|all / everything|सब कुछ / कुल मिलाकर|全部で千円です。|Zenbu de sen-en desu.|It is 1000 yen in total.
n|番号|ばんごう|bangou|number (phone/room/train)|नंबर / क्रमांक|電話番号を教えてください。|Denwa bangou o oshiete kudasai.|Please tell me your phone number.
n|お釣り|おつり|otsuri|change (money returned)|छुट्टे पैसे (बची हुई रकम)|五百円のお釣りです。|Gohyaku-en no otsuri desu.|Here is 500 yen in change.
n|値段|ねだん|nedan|price|कीमत / दाम|このカメラの値段は高いです。|Kono kamera no nedan wa takai desu.|The price of this camera is high.
n|無料|むりょう|muryou|free of charge|मुफ़्त / निःशुल्क|このアプリは無料です。|Kono apuri wa muryou desu.|This app is free.
"""

    // ===============================================================
    // UNIT 7: CLOCK TIME, DAYS OF WEEK, MONTHS & CALENDAR DATES (58 Words)
    // ===============================================================
    private val n5Unit7Words = """
n|今|いま|ima|now|अभी / इस समय|今、何時ですか。|Ima, nanji desu ka.|What time is it now?
n|時間|じかん|jikan|time / hours|समय / घंटा|あまり時間がありません。|Amari jikan ga arimasen.|I don't have much time.
n|何時|なんじ|nanji|what time|कितने बजे|会議は何時からですか。|Kaigi wa nanji kara desu ka.|What time does the meeting start?
n|一時|いちじ|ichiji|one o'clock (1:00)|एक बजे|一時に昼ご飯を食べます。|Ichiji ni hirugohan o tabemasu.|I eat lunch at 1 o'clock.
n|四時|よじ|yoji|four o'clock (4:00 - not よんじ!)|चार बजे|四時に仕事が終わります。|Yoji ni shigoto ga owarimasu.|Work finishes at 4 o'clock.
n|七時|しちじ|shichiji|seven o'clock (7:00)|सात बजे|毎朝七時に家を出ます。|Maiasa shichiji ni ie o demasu.|I leave home at 7 every morning.
n|九時|くじ|kuji|nine o'clock (9:00 - not きゅうじ!)|नौ बजे|銀行は九時に開きます。|Ginkou wa kuji ni akimasu.|The bank opens at 9 o'clock.
n|半|はん|han|half past (:30)|साढ़े (तीस मिनट)|今、八時半です。|Ima, hachiji han desu.|It is 8:30 right now.
n|分|ふん|fun|minute|मिनट|あと五分で始まります。|Ato gofun de hajimarimasu.|It starts in five minutes.
n|十分|じゅっぷん|juppun|ten minutes|दस मिनट|バスで十分かかります。|Basu de juppun kakarimasu.|It takes 10 minutes by bus.
n|午前|ごぜん|gozen|AM / morning|सुबह (AM)|午前十時に会いましょう。|Gozen juuji ni aimashou.|Let's meet at 10 AM.
n|午後|ごご|gogo|PM / afternoon|दोपहर के बाद (PM)|午後三時に帰ります。|Gogo sanji ni kaerimasu.|I return home at 3 PM.
n|朝|あさ|asa|morning|सुबह|朝はコーヒーを飲みます。|Asa wa koohii o nomimasu.|I drink coffee in the morning.
n|昼|ひる|hiru|noon / daytime|दोपहर|昼は暖かいです。|Hiru wa atatakai desu.|It is warm during the day.
n|夕方|ゆうがた|yuugata|evening (dusk)|शाम|夕方に買い物をします。|Yuugata ni kaimono o shimasu.|I do shopping in the evening.
n|夜|よる|yoru|night|रात|夜は静かに勉強します。|Yoru wa shizuka ni benkyou shimasu.|I study quietly at night.
n|今朝|けさ|kesa|this morning|आज सुबह|今朝は早く起きました。|Kesa wa hayaku okimashita.|I woke up early this morning.
n|今晩|こんばん|konban|tonight / this evening|आज रात|今晩、映画を見ませんか。|Konban, eiga o mimasen ka.|Won't you watch a movie tonight?
n|今日|きょう|kyou|today|आज|今日はとてもいい天気です。|Kyou wa totemo ii tenki desu.|Today the weather is very nice.
n|明日|あした|ashita|tomorrow|कल (आने वाला)|明日、テストがあります。|Ashita, tesuto ga arimasu.|There is a test tomorrow.
n|明後日|あさって|asatte|day after tomorrow|परसों (आने वाला)|明後日、東京へ行きます。|Asatte, Toukyou e ikimasu.|I will go to Tokyo the day after tomorrow.
n|昨日|きのう|kinou|yesterday|कल (बीता हुआ)|昨日は雨でした。|Kinou wa ame deshita.|Yesterday it rained.
n|一昨日|おととい|ototoi|day before yesterday|परसों (बीता हुआ)|一昨日、友達に会いました。|Ototoi, tomodachi ni aimashita.|I met a friend the day before yesterday.
n|毎日|まいにち|mainichi|every day|हर दिन / रोज़|毎日日本語を練習します。|Mainichi Nihongo o renshuu shimasu.|I practice Japanese every day.
n|毎朝|まいあさ|maiasa|every morning|हर सुबह|毎朝公園を走ります。|Maiasa kouen o hashirimasu.|I run in the park every morning.
n|毎晩|まいばん|maiban|every night|हर रात|毎晩本を読んでから寝ます。|Maiban hon o yonde kara nemasu.|Every night I read a book before sleeping.
n|今週|こんしゅう|konshuu|this week|इस हफ़्ते|今週はとても忙しいです。|Konshuu wa totemo isogashii desu.|I am very busy this week.
n|来週|らいしゅう|raishuu|next week|अगले हफ़्ते|来週、旅行に行きます。|Raishuu, ryokou ni ikimasu.|I am going on a trip next week.
n|先週|せんしゅう|senshuu|last week|पिछले हफ़्ते|先週、新しい靴を買いました。|Senshuu, atarashii kutsu o kaimashita.|Last week I bought new shoes.
n|毎週|まいしゅう|maishuu|every week|हर हफ़्ते|毎週日曜日に掃除します。|Maishuu nichiyoubi ni souji shimasu.|I clean every Sunday.
n|週末|しゅうまつ|shuumatsu|weekend|वीकेंड (शनिवार-रविवार)|週末は何をしますか。|Shuumatsu wa nani o shimasu ka.|What will you do on the weekend?
n|月曜日|げつようび|getsuyoubi|Monday|सोमवार|月曜日に会議があります。|Getsuyoubi ni kaigi ga arimasu.|There is a meeting on Monday.
n|火曜日|かようび|kayoubi|Tuesday|मंगलवार|火曜日は図書館が休みです。|Kayoubi wa toshokan ga yasumi desu.|The library is closed on Tuesday.
n|水曜日|すいようび|suiyoubi|Wednesday|बुधवार|水曜日にテストがあります。|Suiyoubi ni tesuto ga arimasu.|There is a test on Wednesday.
n|木曜日|もくようび|mokuyoubi|Thursday|गुरुवार / बृहस्पतिवार|木曜日に友達と会います。|Mokuyoubi ni tomodachi to aimasu.|I meet my friend on Thursday.
n|金曜日|きんようび|kin'youbi|Friday|शुक्रवार|金曜日の夜が好きです。|Kin'youbi no yoru ga suki desu.|I like Friday nights.
n|土曜日|どようび|doyoubi|Saturday|शनिवार|土曜日は映画を見に行きます。|Doyoubi wa eiga o mi ni ikimasu.|On Saturday I go to watch a movie.
n|日曜日|にちようび|nichiyoubi|Sunday|रविवार|日曜日は家で休みます。|Nichiyoubi wa ie de yasumimasu.|On Sunday I rest at home.
n|今月|こんげつ|kongetsu|this month|इस महीने|今月は休みが少ないです。|Kongetsu wa yasumi ga sukunai desu.|There are few holidays this month.
n|来月|らいげつ|raigetsu|next month|अगले महीने|来月、国へ帰ります。|Raigetsu, kuni e kaerimasu.|I will return to my country next month.
n|先月|せんげつ|sengetsu|last month|पिछले महीने|先月、日本へ来ました。|Sengetsu, Nihon e kimashita.|I came to Japan last month.
n|毎月|まいつき|maitsuki|every month|हर महीने|毎月本を二冊読みます。|Maitsuki hon o nisatsu yomimasu.|I read two books every month.
n|今年|ことし|kotoshi|this year|इस साल|今年は日本語を頑張ります。|Kotoshi wa Nihongo o ganbarimasu.|I will work hard at Japanese this year.
n|来年|らいねん|rainen|next year|अगले साल|来年、大学を卒業します。|Rainen, daigaku o sotsugyou shimasu.|I will graduate from university next year.
n|去年|きょねん|kyonen|last year|पिछले साल|去年、京都へ行きました。|Kyonen, Kyouto e ikimashita.|Last year I went to Kyoto.
n|毎年|まいとし|maitoshi|every year|हर साल|毎年桜を見に行きます。|Maitoshi sakura o mi ni ikimasu.|Every year I go to see the cherry blossoms.
n|一日|ついたち|tsuitachi|1st day of the month (special!)|महीने की 1 तारीख|四月一日に始まります。|Shigatsu tsuitachi ni hajimarimasu.|It starts on April 1st.
n|二日|ふつか|futsuka|2nd day of the month / 2 days|2 तारीख / दो दिन|五月二日に会いましょう。|Gogatsu futsuka ni aimashou.|Let's meet on May 2nd.
n|三日|みっか|mikka|3rd day of the month / 3 days|3 तारीख / तीन दिन|三日間ホテルに泊まります。|Mikkakan hoteru ni tomarimasu.|I stay at the hotel for three days.
n|四日|よっか|yokka|4th day of the month / 4 days|4 तारीख / चार दिन|今日は六月四日です。|Kyou wa rokugatsu yokka desu.|Today is June 4th.
n|五日|いつか|itsuka|5th day of the month / 5 days|5 तारीख / पाँच दिन|五日に試験があります。|Itsuka ni shiken ga arimasu.|There is an exam on the 5th.
n|六日|むいか|muika|6th day of the month / 6 days|6 तारीख / छह दिन|六日は私の誕生日です。|Muika wa watashi no tanjoubi desu.|The 6th is my birthday.
n|七日|なのか|nanoka|7th day of the month / 7 days|7 तारीख / सात दिन|七日に帰ります。|Nanoka ni kaerimasu.|I will return on the 7th.
n|八日|ようか|youka|8th day of the month / 8 days|8 तारीख / आठ दिन|八日に映画を見ます。|Youka ni eiga o mimasu.|I will watch a movie on the 8th.
n|九日|ここのか|kokonoka|9th day of the month / 9 days|9 तारीख / नौ दिन|九日まで休みです。|Kokonoka made yasumi desu.|It is a holiday until the 9th.
n|十日|とおか|tooka|10th day of the month / 10 days|10 तारीख / दस दिन|十日に東京へ出発します。|Tooka ni Toukyou e shuppatsu shimasu.|I depart for Tokyo on the 10th.
n|十四日|じゅうよっか|juuyokka|14th day of the month|14 तारीख|今日は二月十四日です。|Kyou wa nigatsu juuyokka desu.|Today is February 14th.
n|二十日|はつか|hatsuka|20th day of the month (special!)|20 तारीख / बीस दिन|二十日に給料が出ます。|Hatsuka ni kyuuryou ga demasu.|Salary is paid on the 20th.
"""

    // ===============================================================
    // UNIT 8: JAPANESE FOOD, DRINKS, FRUITS, VEGETABLES & DINING (50 Words)
    // ===============================================================
    private val n5Unit8Words = """
n|食べ物|たべもの|tabemono|food|खाने की चीज़ / भोजन|日本の食べ物はおいしいです。|Nihon no tabemono wa oishii desu.|Japanese food is delicious.
n|飲み物|のみもの|nomimono|drink / beverage|पीने की चीज़ / पेय|冷たい飲み物をください。|Tsumetai nomimono o kudasai.|Please give me a cold drink.
n|ご飯|ごはん|gohan|cooked rice / meal|चावल / खाना|温かいご飯を食べます。|Atatakai gohan o tabemasu.|I eat warm rice.
n|朝ご飯|あさごはん|asagohan|breakfast|सुबह का नाश्ता|毎日朝ご飯を食べますか。|Mainichi asagohan o tabemasu ka.|Do you eat breakfast every day?
n|昼ご飯|ひるごはん|hirugohan|lunch|दोपहर का खाना|昼ご飯はもう食べましたか。|Hirugohan wa mou tabemashita ka.|Have you eaten lunch already?
n|晩ご飯|ばんごはん|bangohan|dinner / supper|रात का खाना|家族と一緒に晩ご飯を食べます。|Kazoku to issho ni bangohan o tabemasu.|I eat dinner together with my family.
n|お弁当|おべんとう|obentou|boxed lunch (bento)|टिफ़िन / लंच बॉक्स|母がお弁当を作ってくれました。|Haha ga obentou o tsukutte kuremashita.|My mother made a bento lunch for me.
n|料理|りょうり|ryouri|cooking / dish / cuisine|पकवान / खाना बनाना|私はインド料理が好きです。|Watashi wa Indo ryouri ga suki desu.|I like Indian cuisine.
n|水|みず|mizu|water|पानी|冷たい水を飲みます。|Tsumetai mizu o nomimasu.|I drink cold water.
n|お湯|おゆ|oyu|hot water|गर्म पानी|お湯を沸かします。|Oyu o wakashimasu.|I boil hot water.
n|お茶|おちゃ|ocha|green tea / tea|चाय (ग्रीन टी)|どうぞお茶を飲んでください。|Douzo ocha o nonde kudasai.|Please have some green tea.
n|紅茶|こうちゃ|koucha|black tea|काली चाय|レモンを入れた紅茶が好きです。|Remon o ireta koucha ga suki desu.|I like black tea with lemon.
n|牛乳|ぎゅうにゅう|gyuunyuu|cow's milk|गाय का दूध|毎朝牛乳を一杯飲みます。|Maiasa gyuunyuu o ippai nomimasu.|I drink a glass of milk every morning.
n|お酒|おさけ|osake|alcohol / Japanese sake|शराब / साके|私はお酒を飲みません。|Watashi wa osake o nomimasen.|I do not drink alcohol.
n|肉|にく|niku|meat|मांस|スーパーで肉を買います。|Suupaa de niku o kaimasu.|I buy meat at the supermarket.
n|牛肉|ぎゅうにく|gyuuniku|beef|बीफ़|牛肉は食べません。|Gyuuniku wa tabemasen.|I do not eat beef.
n|豚肉|ぶたにく|butaniku|pork|सूअर का मांस|豚肉の料理を作ります。|Butaniku no ryouri o tsukurimasu.|I make a pork dish.
n|鶏肉|とりにく|toriniku|chicken meat|चिकन|鶏肉のカレーが好きです。|Toriniku no karee ga suki desu.|I like chicken curry.
n|魚|さかな|sakana|fish|मछली|日本人は魚をよく食べます。|Nihonjin wa sakana o yoku tabemasu.|Japanese people eat fish often.
n|卵|たまご|tamago|egg|अंडा|朝ご飯に卵を食べます。|Asagohan ni tamago o tabemasu.|I eat an egg for breakfast.
n|野菜|やさい|yasai|vegetable|सब्ज़ी|毎日野菜をたくさん食べます。|Mainichi yasai o takusan tabemasu.|I eat lots of vegetables every day.
n|果物|くだもの|kudamono|fruit|फल|冷蔵庫に果物があります。|Reizouko ni kudamono ga arimasu.|There is fruit in the refrigerator.
n|りんご|りんご|ringo|apple|सेब|赤いりんごを一つ食べました。|Akai ringo o hitotsu tabemashita.|I ate one red apple.
n|みかん|みかん|mikan|mandarin orange|संतरा (नारंगी)|冬はみかんがおいしいです。|Fuyu wa mikan ga oishii desu.|Mandarins are delicious in winter.
n|いちご|いちご|ichigo|strawberry|स्ट्रॉबेरी|甘いイチゴを買いました。|Amai ichigo o kaimashita.|I bought sweet strawberries.
n|すいか|すいか|suika|watermelon|तरबूज़|夏に冷たいすいかを食べます。|Natsu ni tsumetai suika o tabemasu.|I eat cold watermelon in summer.
n|砂糖|さとう|satou|sugar|चीनी / शक्कर|コーヒーに砂糖を入れますか。|Koohii ni satou o iremasu ka.|Do you put sugar in your coffee?
n|塩|しお|shio|salt|नमक|少し塩を入れてください。|Sukoshi shio o irete kudasai.|Please put in a little salt.
n|醤油|しょうゆ|shouyu|soy sauce|सोया सॉस|寿司に醤油をつけます。|Sushi ni shouyu o tsukemasu.|I dip sushi in soy sauce.
n|味噌|みそ|miso|miso paste|मिसो पेस्ट|毎朝味噌汁を飲みます。|Maiasa misoshiru o nomimasu.|I drink miso soup every morning.
n|油|あぶら|abura|cooking oil|तेल|フライパンに油を入れます。|Furaipan ni abura o iremasu.|I put oil in the frying pan.
n|お菓子|おかし|okashi|sweets / snacks / candy|मिठाई / स्नैक्स|子供はお菓子が大好きです。|Kodomo wa okashi ga daisuki desu.|Children love sweets.
n|飴|あめ|ame|candy / toffee (Low-High pitch)|टॉफ़ी|かばんの中に飴があります。|Kaban no naka ni ame ga arimasu.|There is candy inside my bag.|0
n|寿司|すし|sushi|sushi|सुशी|日本で寿司を食べたいです。|Nihon de sushi o tabetai desu.|I want to eat sushi in Japan.
n|天ぷら|てんぷら|tenpura|tempura|टेम्पुरा (जापानी पकौड़े)|えびの天ぷらはおいしいです。|Ebi no tenpura wa oishii desu.|Shrimp tempura is delicious.
n|うどん|うどん|udon|udon thick noodles|उदोन नूडल्स|温かいきつねうどんを食べます。|Atatakai kitsune udon o tabemasu.|I eat warm kitsune udon.
n|そば|そば|soba|buckwheat noodles|सोबा नूडल्स|ざるそばを一つお願いします。|Zaru soba o hitotsu onegaishimasu.|One cold buckwheat noodles, please.
n|おにぎり|おにぎり|onigiri|rice ball|ओनिगिरी (चावल का रोल)|コンビニでおにぎりを買いました。|Konbini de onigiri o kaimashita.|I bought a rice ball at the convenience store.
n|定食|ていしょく|teishoku|set meal|थाली / सेट मील|今日のランチは魚定食です。|Kyou no ranchi wa sakana teishoku desu.|Today's lunch is a fish set meal.
n|箸|はし|hashi|chopsticks (High-Low pitch)|चॉपस्टिक|箸で上手に食べます。|Hashi de jouzu ni tabemasu.|I eat skillfully with chopsticks.|1
n|皿|さら|sara|plate / dish|प्लेट / थाली|白いお皿を五枚買いました。|Shiroi osara o gomai kaimashita.|I bought five white plates.
n|お茶碗|おちゃわん|ochawan|rice bowl|चावल का कटोरा|お茶碗にご飯を盛ります。|Ochawan ni gohan o morimasu.|I serve rice into a rice bowl.
n|食堂|しょくどう|shokudou|cafeteria / dining hall|कैंटीन / भोजनालय|大学の食堂は安くておいしいです。|Daigaku no shokudou wa yasukute oishii desu.|The university cafeteria is cheap and delicious.
n|喫茶店|きっさてん|kissaten|traditional coffee shop|चाय-कॉफ़ी की दुकान|喫茶店で友達と話し合いました。|Kissaten de tomodachi to hanashiaimashita.|I chatted with my friend at a coffee shop.
n|お腹|おなか|onaka|stomach / belly|पेट|お腹が空きましたね。|Onaka ga sukimashita ne.|I've gotten hungry, haven't I?
n|喉|のど|nodo|throat|गला|喉が渇いたから水を飲みます。|Nodo ga kawaita kara mizu o nomimasu.|Because I'm thirsty, I drink water.
n|味|あじ|aji|taste / flavor|स्वाद|このスープはいい味ですね。|Kono suupu wa ii aji desu ne.|This soup has a good flavor.
n|匂い|におい|nioi|smell / scent / aroma|खुशबू / गंध|カレーのいい匂いがします。|Karee no ii nioi ga shimasu.|There is a nice smell of curry.
n|注文|ちゅうもん|chuumon|order (at a restaurant)|ऑर्डर|ご注文はお決まりですか。|Gochuumon wa okimari desu ka.|Have you decided on your order?
n|会計|かいけい|kaikei|bill / check / payment|बिल का भुगतान|すみません、お会計をお願いします。|Sumimasen, okaikei o onegaishimasu.|Excuse me, the check please.
"""
    // ===============================================================
    // UNIT 9: HOUSE, ROOMS, FURNITURE, APPLIANCES & STATIONERY (58 Words)
    // ===============================================================
    private val n5Unit9Words = """
n|家|いえ|ie|house / dwelling|मकान / घर|大きい家に住みたいです。|Ookii ie ni sumitai desu.|I want to live in a big house.|2
n|うち|うち|uchi|home / my place|मेरा घर|日曜日はうちにいます。|Nichiyoubi wa uchi ni imasu.|I am at home on Sunday.
n|部屋|へや|heya|room|कमरा|私の部屋は二階にあります。|Watashi no heya wa nikai ni arimasu.|My room is on the second floor.|2
n|台所|だいどころ|daidokoro|kitchen|रसोईघर (किचन)|母は台所で料理しています。|Haha wa daidokoro de ryouri shite imasu.|My mother is cooking in the kitchen.
n|玄関|げんかん|genkan|front entrance / foyer|घर का मुख्य प्रवेश द्वार|玄関で靴を脱ぎます。|Genkan de kutsu o nugimasu.|I take off my shoes at the entranceway.
n|廊下|ろうか|rouka|corridor / hallway|बरामदा / गलियारा|廊下を走らないでください。|Rouka o hashiranaide kudasai.|Please do not run in the hallway.
n|階段|かいだん|kaidan|stairs / staircase|सीढ़ियाँ|階段を上ります。|Kaidan o noborimasu.|I go up the stairs.
n|お風呂|おふろ|ofuro|bath / bathroom|स्नानघर / बाथटब|夜、お風呂に入ります。|Yoru, ofuro ni hairimasu.|I take a bath at night.
n|庭|にわ|niwa|garden / yard|बगीचा / आँगन|庭にきれいな花が咲いています。|Niwa ni kirei na hana ga saite imasu.|Pretty flowers are blooming in the garden.
n|池|いけ|ike|pond|तालाब|庭の池に魚がいます。|Niwa no ike ni sakana ga imasu.|There are fish in the garden pond.
n|壁|かべ|kabe|wall|दीवार|壁に写真を貼ります。|Kabe ni shashin o harimasu.|I put up a photo on the wall.
n|窓|まど|mado|window|खिड़की|涼しいから窓を開けます。|Suzushii kara mado o akemasu.|Because it is cool, I open the window.
n|戸|と|to|Japanese style sliding door|दरवाज़ा / किवाड़|夜の十時に戸を閉めます。|Yoru no juuji ni to o shimemasu.|I close the door at 10 PM.
n|床|ゆか|yuka|floor|फ़र्श|床をきれいに掃除します。|Yuka o kirei ni souji shimasu.|I clean the floor nicely.
n|屋根|やね|yane|roof|छत|赤い屋根の家が見えます。|Akai yane no ie ga miemasu.|I can see a house with a red roof.
n|机|つくえ|tsukue|desk|मेज़ / डेस्क|机の上に本があります。|Tsukue no ue ni hon ga arimasu.|There is a book on the desk.
n|椅子|いす|isu|chair|कुर्सी|どうぞこの椅子に座ってください。|Douzo kono isu ni suwatte kudasai.|Please sit on this chair.
n|本棚|ほんだな|hondana|bookshelf|किताबों की अलमारी|本棚に辞書を並べます。|Hondana ni jisho o narabemasu.|I line up dictionaries on the bookshelf.
n|冷蔵庫|れいぞうこ|reizouko|refrigerator / fridge|फ़्रिज|冷蔵庫に牛乳が入っています。|Reizouko ni gyuunyuu ga haitte imasu.|There is milk in the refrigerator.
n|洗濯機|せんたくき|sentakuki|washing machine|वॉशिंग मशीन|洗濯機で服を洗います。|Sentakuki de fuku o araimasu.|I wash clothes in the washing machine.
n|掃除機|そうじき|soujiki|vacuum cleaner|वैक्यूम क्लीनर|掃除機で部屋を掃除します。|Soujiki de heya o souji shimasu.|I clean the room with a vacuum cleaner.
n|電気|でんき|denki|electricity / electric light|बिजली / लाइट|部屋の電気をつけます。|Heya no denki o tsukemasu.|I turn on the room light.
n|電話|でんわ|denwa|telephone / phone call|टेलीफ़ोन / फ़ोन कॉल|友達に電話をかけます。|Tomodachi ni denwa o kakemasu.|I make a phone call to my friend.
n|時計|とけい|tokei|clock / watch|घड़ी|壁に時計が掛かっています。|Kabe ni tokei ga kakatte imasu.|A clock is hanging on the wall.
n|花瓶|かびん|kabin|flower vase|फूलदान|テーブルに花瓶を置きます。|Teeburu ni kabin o okimasu.|I place a flower vase on the table.
n|灰皿|はいざら|haizara|ashtray|ऐशट्रे|ここに灰皿があります。|Koko ni haizara ga arimasu.|Here is an ashtray.
n|鍵|かぎ|kagi|key / lock|चाबी / ताला|ドアの鍵を閉めました。|Doa no kagi o shimemashita.|I locked the door.
n|箱|はこ|hako|box|डिब्बा / बॉक्स|大きい箱の中に何がありますか。|Ookii hako no naka ni nani ga arimasu ka.|What is inside the big box?
n|袋|ふくろ|fukuro|bag / sack / pouch|थैली / पन्नी|紙の袋に入れてください。|Kami no fukuro ni irete kudasai.|Please put it in a paper bag.
n|かばん|かばん|kaban|bag / briefcase / backpack|बैग / बस्ता|かばんの中にノートがあります。|Kaban no naka ni nooto ga arimasu.|There is a notebook inside the bag.
n|財布|さいふ|saifu|wallet / purse|बटुआ / पर्स|財布にお金が入っています。|Saifu ni okane ga haitte imasu.|There is money in my wallet.
n|傘|かさ|kasa|umbrella|छाता|雨が降っているから傘をさします。|Ame ga futte iru kara kasa o sashimasu.|Because it is raining, I open an umbrella.
n|靴|くつ|kutsu|shoes|जूते|新しい靴を履きます。|Atarashii kutsu o hakimasu.|I put on new shoes.
n|靴下|くつした|kutsushita|socks|मोज़े|白い靴下を買いました。|Shiroi kutsushita o kaimashita.|I bought white socks.
n|帽子|ぼうし|boushi|hat / cap|टोपी|外は暑いから帽子をかぶります。|Soto wa atsui kara boushi o kaburimasu.|Because it's hot outside, I wear a hat.
n|眼鏡|めがね|megane|eyeglasses|चश्मा|本を読むとき眼鏡をかけます。|Hon o yomu toki megane o kakemasu.|I wear glasses when reading a book.
n|服|ふく|fuku|clothes|कपड़े|デパートで服を買います。|Depaato de fuku o kaimasu.|I buy clothes at the department store.
n|洋服|ようふく|youfuku|Western-style clothes|आधुनिक कपड़े|タンスに洋服をしまいます。|Tansu ni youfuku o shimaimasu.|I put Western clothes into the wardrobe.
n|着物|きもの|kimono|kimono (traditional attire)|किमोनो (पारंपरिक जापानी परिधान)|京都で着物を着ました。|Kyouto de kimono o kimashita.|I wore a kimono in Kyoto.
n|荷物|にもつ|nimotsu|luggage / baggage / parcel|सामान / पार्सल|重い荷物を持ちます。|Omoi nimotsu o mochimasu.|I carry heavy luggage.
n|鉛筆|えんぴつ|enpitsu|pencil|पेंसिल|テストは鉛筆で書いてください。|Tesuto wa enpitsu de kaite kudasai.|Please write the test in pencil.
n|万年筆|まんねんひつ|mannenhitsu|fountain pen|फ़ाउंटेन पेन|父から万年筆をもらいました。|Chichi kara mannenhitsu o moraimashita.|I received a fountain pen from my father.
n|消しゴム|けしごむ|keshigomu|eraser / rubber|रबर (इरेज़र)|消しゴムで字を消します。|Keshigomu de ji o keshimasu.|I erase the letter with an eraser.
n|紙|かみ|kami|paper|कागज़|白い紙に名前を書きます。|Shiroi kami ni namae o kakimasu.|I write my name on white paper.
n|手紙|てがみ|tegami|letter|चिट्ठी / पत्र|母に手紙を書きました。|Haha ni tegami o kakimashita.|I wrote a letter to my mother.
n|はがき|はがき|hagaki|postcard|पोस्टकार्ड|郵便局ではがきを買います。|Yuubinkyoku de hagaki o kaimasu.|I buy postcards at the post office.
n|切手|きって|kitte|postage stamp|डाक टिकट|封筒に切手を貼ります。|Fuutou ni kitte o harimasu.|I stick a stamp on the envelope.
n|封筒|ふうとう|fuutou|envelope|लिफ़ाफ़ा|手紙を封筒に入れます。|Tegami o fuutou ni iremasu.|I put the letter into an envelope.
n|辞書|じしょ|jisho|dictionary|शब्दकोश (डिक्शनरी)|分からない言葉を辞書で引きます。|Wakaranai kotoba o jisho de hikimasu.|I look up unknown words in a dictionary.
n|本|ほん|hon|book|किताब / पुस्तक|図書館で本を借りました。|Toshokan de hon o karimashita.|I borrowed a book from the library.|1
n|新聞|しんぶん|shinbun|newspaper|अख़बार / समाचार-पत्र|毎朝日本の新聞を読みます。|Maiasa Nihon no shinbun o yomimasu.|I read a Japanese newspaper every morning.
n|雑誌|ざっし|zasshi|magazine|पत्रिका / मैगज़ीन|コンビニで雑誌を買いました。|Konbini de zasshi o kaimashita.|I bought a magazine at the convenience store.
n|作文|さくぶん|sakubun|essay / composition|निबंध / लेख|日本語で作文を書きます。|Nihongo de sakubun o kakimasu.|I write an essay in Japanese.
n|絵|え|e|picture / drawing / painting|चित्र / पेंटिंग|彼女は絵を描くのが上手です。|Kanojo wa e o kaku no ga jouzu desu.|She is good at drawing pictures.
n|写真|しゃしん|shashin|photograph|फ़ोटो / तस्वीर|スマホで写真を撮りました。|Sumaho de shashin o torimashita.|I took a photo with my smartphone.
n|漢字|かんじ|kanji|Kanji character|कांजी अक्षर|毎日新しい漢字を覚えます。|Mainichi atarashii kanji o oboemasu.|I memorize new kanji every day.
n|ひらがな|ひらがな|hiragana|Hiragana script|हिरागाना लिपि|ひらがなで名前を書いてください。|Hiragana de namae o kaite kudasai.|Please write your name in Hiragana.
n|カタカナ|カタカナ|katakana|Katakana script|काताकाना लिपि|外国の言葉はカタカナで書きます。|Gaikoku no kotoba wa katakana de kakimasu.|Foreign words are written in Katakana.
"""

    // ===============================================================
    // UNIT 10: TOWN, BUILDINGS, DIRECTIONS & POSITIONS (上/下/前/右) (58 Words)
    // ===============================================================
    private val n5Unit10Words = """
n|町|まち|machi|town / city|शहर / कस्बा|この町はとても静かです。|Kono machi wa totemo shizuka desu.|This town is very quiet.
n|村|むら|mura|village|गाँव|祖父は小さい村に住んでいます。|Sofu wa chiisai mura ni sunde imasu.|My grandfather lives in a small village.
n|道|みち|michi|road / street / way|सड़क / रास्ता|この道を真っ直ぐ行ってください。|Kono michi o massugu itte kudasai.|Please go straight along this road.
n|通り|とおり|toori|avenue / main street|मुख्य सड़क|賑やかな通りを歩きます。|Nigiyaka na toori o arukimasu.|I walk down the lively street.
n|角|かど|kado|corner (of a street/desk)|मोड़ / कोना|次の角を右へ曲がってください。|Tsugi no kado o migi e magatte kudasai.|Please turn right at the next corner.
n|交差点|こうさてん|kousaten|intersection / crossing|चौराहा|交差点で信号を見ます。|Kousaten de shingou o mimasu.|I look at the traffic light at the intersection.
n|信号|しんごう|shingou|traffic light|ट्रैफ़िक सिग्नल|信号が青になりました。|Shingou ga ao ni narimashita.|The traffic light turned green (blue).
n|橋|はし|hashi|bridge (Low-High pitch)|पुल|川の橋を渡ります。|Kawa no hashi o watarimasu.|I cross the river bridge.|2
n|建物|たてもの|tatemo|building / structure|इमारत / भवन|あれは古い建物ですね。|Are wa furui tatemono desu ne.|That is an old building, isn't it?
n|学校|がっこう|gakkou|school|स्कूल / विद्यालय|歩いて学校へ行きます。|Aruite gakkou e ikimasu.|I go to school on foot.|0
n|大学|だいがく|daigaku|university|विश्वविद्यालय / यूनिवर्सिटी|大学で工学を勉強しています。|Daigaku de kougaku o benkyou shite imasu.|I am studying engineering at university.
n|教室|きょうしつ|kyoushitsu|classroom|कक्षा (क्लासरूम)|教室で先生の話を聞きます。|Kyoushitsu de sensei no hanashi o kikimasu.|I listen to the teacher in the classroom.
n|図書館|としょかん|toshokan|library|पुस्तकालय (लाइब्रेरी)|図書館は静かで勉強しやすいです。|Toshokan wa shizuka de benkyou shiyasui desu.|The library is quiet and easy to study in.
n|病院|びょういん|byouin|hospital / clinic|अस्पताल|熱があるから病院へ行きます。|Netsu ga aru kara byouin e ikimasu.|Because I have a fever, I go to the hospital.
n|薬局|やっきょく|yakkyoku|pharmacy / drugstore|दवा की दुकान (मेडिकल स्टोर)|薬局で風邪の薬を買いました。|Yakkyoku de kaze no kusuri o kaimashita.|I bought cold medicine at the pharmacy.
n|銀行|ぎんこう|ginkou|bank|बैंक|駅の前に銀行があります。|Eki no mae ni ginkou ga arimasu.|There is a bank in front of the station.
n|郵便局|ゆうびんきょく|yuubinkyoku|post office|डाकघर (पोस्ट ऑफ़िस)|郵便局で荷物を送ります。|Yuubinkyoku de nimotsu o okurimasu.|I send a package at the post office.
n|大使館|たいしかん|taishikan|embassy|दूतावास (एम्बेसी)|インド大使館は東京にあります。|Indo taishikan wa Toukyou ni arimasu.|The Indian Embassy is in Tokyo.
n|交番|こうばん|kouban|police box|पुलिस चौकी (कोबान)|交番で道を聞きました。|Kouban de michi o kikimashita.|I asked for directions at the police box.
n|公園|こうえん|kouen|park|पार्क / उद्यान|日曜日に公園を散歩します。|Nichiyoubi ni kouen o sanpo shimasu.|I take a walk in the park on Sunday.
n|動物園|どうぶつえん|doubutsuen|zoo|चिड़ियाघर|動物園でパンダを見ました。|Doubutsuen de panda o mimashita.|I saw pandas at the zoo.
n|映画館|えいがかん|eigakan|movie theater / cinema|सिनेमा हॉल|映画館で日本の映画を見ます。|Eigakan de Nihon no eiga o mimasu.|I watch a Japanese movie at the theater.
n|お寺|おてら|otera|Buddhist temple|बौद्ध मंदिर|京都には有名なお寺がたくさんあります。|Kyouto ni wa yuumei na otera ga takusan arimasu.|There are many famous temples in Kyoto.
n|神社|じんじゃ|jinja|Shinto shrine|शिंतो मंदिर (श्राइन)|お正月に神社へ行きます。|Oshougatsu ni jinja e ikimasu.|I go to a shrine on New Year's.
n|店|みせ|mise|shop / store|दुकान|この店は九時に開きます。|Kono mise wa kuji ni akimasu.|This shop opens at 9 o'clock.|2
n|本屋|ほんや|hon'ya|bookstore|किताबों की दुकान|駅前の本屋で雑誌を買いました。|Ekimae no hon'ya de zasshi o kaimashita.|I bought a magazine at the bookstore in front of the station.
n|花屋|はなや|hanaya|flower shop / florist|फूलों की दुकान|花屋できれいな花を買います。|Hanaya de kirei na hana o kaimasu.|I buy pretty flowers at the flower shop.
n|八百屋|やおや|yaoya|greengrocer / vegetable shop|सब्ज़ी की दुकान|八百屋で新鮮な野菜を買います。|Yaoya de shinsen na yasai o kaimasu.|I buy fresh vegetables at the greengrocer.
n|パン屋|パンや|pan'ya|bakery|बेकरी (ब्रेड की दुकान)|パン屋からいい匂いがします。|Pan'ya kara ii nioi ga shimasu.|A nice smell is coming from the bakery.
n|売り場|うりば|uriba|sales floor / counter|काउंटर / डिपार्टमेंट|かばん売り場は何階ですか。|Kaban uriba wa nangai desu ka.|Which floor is the bag counter on?
n|会社|かいしゃ|kaisha|company / corporation|कंपनी / दफ़्तर|父の会社は東京にあります。|Chichi no kaisha wa Toukyou ni arimasu.|My father's company is in Tokyo.|0
n|工場|こうじょう|koujou|factory / plant|कारखाना / फ़ैक्ट्री|兄は車の工場で働いています。|Ani wa kuruma no koujou de hataraite imasu.|My older brother works at a car factory.
n|事務所|じむしょ|jimusho|office|कार्यालय (ऑफ़िस)|事務所に先生がいます。|Jimusho ni sensei ga imasu.|The teacher is in the office.
n|受付|うけつけ|uketsuke|reception desk|रिसेप्शन काउंटर|受付で名前を書いてください。|Uketsuke de namae o kaite kudasai.|Please write your name at the reception desk.
n|入口|いりぐち|iriguchi|entrance|प्रवेश द्वार (Entry)|建物の入口で会いましょう。|Tatemono no iriguchi de aimashou.|Let's meet at the entrance of the building.
n|出口|でぐち|deguchi|exit|निकास द्वार (Exit)|駅の東出口を出ます。|Eki no higashi deguchi o demasu.|I go out of the east exit of the station.
n|場所|ばしょ|basho|place / location|जगह / स्थान|集合の場所を教えてください。|Shuugou no basho o oshiete kudasai.|Please tell me the meeting place.
n|所|ところ|tokoro|place / spot|जगह|ここはとてもいい所ですね。|Koko wa totemo ii tokoro desu ne.|This is a very nice place, isn't it?
n|上|うえ|ue|up / above / on top of|ऊपर|机の上にパソコンがあります。|Tsukue no ue ni pasokon ga arimasu.|There is a laptop on the desk.
n|下|した|shita|down / under / below|नीचे|椅子の下に猫が寝ています。|Isu no shita ni neko ga nete imasu.|A cat is sleeping under the chair.
n|前|まえ|mae|front / before|सामने / के आगे|駅の前にバス停があります。|Eki no mae ni basutei ga arimasu.|There is a bus stop in front of the station.
n|後ろ|うしろ|ushiro|behind / back|पीछे|私の後ろに友達が座っています。|Watashi no ushiro ni tomodachi ga suwatte imasu.|My friend is sitting behind me.
n|右|みぎ|migi|right side|दायाँ (Right)|右の手を挙げてください。|Migi no te o agete kudasai.|Please raise your right hand.
n|左|ひだり|hidari|left side|बायाँ (Left)|銀行の左に郵便局があります。|Ginkou no hidari ni yuubinkyoku ga arimasu.|There is a post office to the left of the bank.
n|中|なか|naka|inside / middle|अंदर / के बीच में|箱の中に何がありますか。|Hako no naka ni nani ga arimasu ka.|What is inside the box?
n|外|そと|soto|outside|बाहर|外は風が強いです。|Soto wa kaze ga tsuyoi desu.|The wind is strong outside.
n|隣|となり|tonari|next to / next door|बगल में / पड़ोस में|スーパーの隣に花屋があります。|Suupaa no tonari ni hanaya ga arimasu.|Next to the supermarket is a flower shop.
n|近く|ちかく|chikaku|near / vicinity|पास में / नज़दीक|家の近くに公園があります。|Ie no chikaku ni kouen ga arimasu.|There is a park near my house.
n|間|あいだ|aida|between / among|दो चीज़ों के बीच में|銀行と本屋の間にカフェがあります。|Ginkou to hon'ya no aida ni kafe ga arimasu.|There is a cafe between the bank and the bookstore.
n|横|よこ|yoko|beside / side / width|बगल में / साइड में|机の横にゴミ箱があります。|Tsukue no yoko ni gomibako ga arimasu.|There is a trash can beside the desk.
n|そば|そば|soba|near / beside|के पास|窓のそばに座ります。|Mado no soba ni suwarimasu.|I sit near the window.
n|向こう|むこう|mukou|over there / opposite side|उस पार / सामने की तरफ़|道の向こうに駅が見えます。|Michi no mukou ni eki ga miemasu.|The station is visible across the street.
n|周り|まわり|mawari|around / surroundings|चारों तरफ़ / आस-पास|池の周りを歩きます。|Ike no mawari o arukimasu.|I walk around the pond.
n|真ん中|まんなか|mannaka|center / middle|बिल्कुल बीच में|部屋の真ん中にテーブルがあります。|Heya no mannaka ni teeburu ga arimasu.|There is a table in the middle of the room.
n|北|きた|kita|north|उत्तर दिशा (North)|北海道は日本の北にあります。|Hokkaidou wa Nihon no kita ni arimasu.|Hokkaido is in the north of Japan.
n|南|みなみ|minami|south|दक्षिण दिशा (South)|南の島へ旅行します。|Minami no shima e ryokou shimasu.|I travel to a southern island.
n|東|ひがし|higashi|east|पूर्व दिशा (East)|太陽は東から昇ります。|Taiyou wa higashi kara noborimasu.|The sun rises from the east.
n|西|にし|nishi|west|पश्चिम दिशा (West)|西の空が赤くなりました。|Nishi no sora ga akaku narimashita.|The western sky turned red.
"""

    // ===============================================================
    // UNIT 11: TRAINS, BUSES, TRAVEL & MOTION VERBS (54 Words)
    // ===============================================================
    private val n5Unit11Words = """
n|駅|えき|eki|train station|रेलवे स्टेशन|東京駅はとても大きいです。|Toukyou-eki wa totemo ookii desu.|Tokyo Station is very big.|1
n|電車|でんしゃ|densha|electric train|ट्रेन / लोकल ट्रेन|毎朝電車で大学へ行きます。|Maiasa densha de daigaku e ikimasu.|I go to university by train every morning.|0
n|地下鉄|ちかてつ|chikatetsu|subway / metro|मेट्रो ट्रेन|東京の地下鉄は便利です。|Toukyou no chikatetsu wa benri desu.|Tokyo's subway is convenient.
n|新幹線|しんかんせん|shinkansen|bullet train (Shinkansen)|बुलेट ट्रेन (शिंकानसेन)|新幹線で京都へ行きました。|Shinkansen de Kyouto e ikimashita.|I went to Kyoto by bullet train.
n|汽車|きしゃ|kisha|train (steam/long-distance)|रेलगाड़ी|汽車で故郷へ帰ります。|Kisha de furusato e kaerimasu.|I return to my hometown by train.
n|車|くるま|kuruma|car / vehicle|कार / गाड़ी|父は車を運転します。|Chichi wa kuruma o unten shimasu.|My father drives a car.|0
n|自動車|じどうしゃ|jidousha|automobile|मोटरगाड़ी|日本の自動車は世界中で有名です。|Nihon no jidousha wa sekaijuu de yuumei desu.|Japanese automobiles are famous worldwide.
n|自転車|じてんしゃ|jitensha|bicycle|साइकिल|駅まで自転車で行きます。|Eki made jitensha de ikimasu.|I go to the station by bicycle.
n|飛行機|ひこうき|hikouki|airplane|हवाई जहाज़|飛行機でインドから日本へ来ました。|Hikouki de Indo kara Nihon e kimashita.|I came from India to Japan by airplane.
n|船|ふね|fune|ship / boat|जहाज़ / नाव|港に大きい船が止まっています。|Minato ni ookii fune ga tomatte imasu.|A big ship is docked at the port.
n|空港|くうこう|kuukou|airport|हवाई अड्डा (एयरपोर्ट)|成田空港に着きました。|Narita kuukou ni tsukimashita.|I arrived at Narita Airport.
n|切符|きっぷ|kippu|ticket (train/bus)|रेल या बस का टिकट|自動販売機で切符を買います。|Jidou hanbaiki de kippu o kaimasu.|I buy a ticket at the vending machine.
n|片道|かたみち|katamichi|one-way trip|एक तरफ़ का (One-way)|片道の切符を一枚ください。|Katamichi no kippu o ichimai kudasai.|One one-way ticket, please.
n|往復|おうふく|oufuku|round trip|आने-जाने का (Round-trip)|京都まで往復の切符を買いました。|Kyouto made oufuku no kippu o kaimashita.|I bought a round-trip ticket to Kyoto.
n|番線|ばんせん|bansen|platform / track number|प्लेटफ़ॉर्म नंबर|山手線は何番線ですか。|Yamanote-sen wa nan-bansen desu ka.|Which platform is the Yamanote Line?
n|次|つぎ|tsugi|next|अगला|次の駅で降ります。|Tsugi no eki de orimasu.|I get off at the next station.
n|旅行|りょこう|ryokou|travel / trip|यात्रा / सफ़र|来月、北海道へ旅行します。|Raigetsu, Hokkaidou e ryokou shimasu.|I will travel to Hokkaido next month.
n|お土産|おみやげ|omiyage|souvenir / gift from trip|यात्रा का उपहार (सोवेनियर)|家族にお土産を買いました。|Kazoku ni omiyage o kaimashita.|I bought souvenirs for my family.
n|地図|ちず|chizu|map|नक़्शा (मैप)|スマホで地図を見ます。|Sumaho de chizu o mimasu.|I look at the map on my smartphone.
v5|行く|いく|iku|to go|जाना|明日、図書館へ行きます。|Ashita, toshokan e ikimasu.|I will go to the library tomorrow.|0
vk|来る|くる|kuru|to come|आना|家に友達が来ます。|Ie ni tomodachi ga kimasu.|My friend is coming to my house.|1
v5|帰る|かえる|kaeru|to return home / go back|घर लौटना / वापस जाना|七時に家へ帰ります。|Shichiji ni ie e kaerimasu.|I return home at 7 o'clock.|1
v1|出かける|でかける|dekakeru|to go out|बाहर घूमने/काम से निकलना|日曜日は家族と出かけます。|Nichiyoubi wa kazoku to dekakemasu.|On Sunday I go out with my family.
v5|乗る|のる|noru|to ride / board (takes に!)|सवार होना / चढ़ना|東京駅で新幹線に乗ります。|Toukyou-eki de shinkansen ni norimasu.|I board the bullet train at Tokyo Station.|0
v1|降りる|おりる|oriru|to get off / descend (takes を!)|उतरना (ट्रेन/बस से)|渋谷駅で電車を降ります。|Shibuya-eki de densha o orimasu.|I get off the train at Shibuya Station.
v1|乗り換える|のりかえる|norikaeru|to transfer (trains/buses)|ट्रेन या बस बदलना|新宿で地下鉄に乗り換えます。|Shinjuku de chikatetsu ni norikaemasu.|I transfer to the subway at Shinjuku.
v5|着く|つく|tsuku|to arrive (takes に!)|पहुँचना|九時に学校に着きました。|Kuji ni gakkou ni tsukimashita.|I arrived at school at 9 o'clock.
vs|出発する|しゅっぱつする|shuppatsu suru|to depart / leave|रवाना होना|バスは八時に出発します。|Basu wa hachiji ni shuppatsu shimasu.|The bus departs at 8 o'clock.
v5|歩く|あるく|aruku|to walk|पैदल चलना|駅から歩いて五分です。|Eki kara aruite gofun desu.|It is five minutes on foot from the station.|2
v5|走る|はしる|hashiru|to run|दौड़ना|時間がないから走ります。|Jikan ga nai kara hashirimasu.|Because there is no time, I run.|2
v5|渡る|わたる|wataru|to cross (a bridge/street)|पार करना (सड़क/पुल)|気をつけて道を渡ってください。|Ki o tsukete michi o watatte kudasai.|Please cross the street carefully.
v5|曲がる|まがる|magaru|to turn (a corner)|मुड़ना|あの角を左へ曲がります。|Ano kado o hidari e magarimasu.|I turn left at that corner.
v5|止まる|とまる|tomaru|to stop / halt|रुकना|ここにタクシーが止まります。|Koko ni takushii ga tomarimasu.|Taxis stop here.
v5|泊まる|とまる|tomaru|to stay overnight|रात भर ठहरना (होटल में)|京都のホテルに二晩泊まります。|Kyouto no hoteru ni futaban tomarimasu.|I stay two nights at a hotel in Kyoto.
v5|通る|とおる|tooru|to pass through / go along|गुज़रना (रास्ते से)|毎日この道を通ります。|Mainichi kono michi o toorimasu.|I pass along this road every day.
v5|入る|はいる|hairu|to enter / go in|अंदर जाना / प्रवेश करना|カフェに入ってお茶を飲みます。|Kafe ni haitte ocha o nomimasu.|I go into a cafe and drink tea.|1
v1|出る|でる|deru|to leave / go out / exit|बाहर निकलना|毎朝八時に家を出ます。|Maiasa hachiji ni ie o demasu.|I leave the house at 8 every morning.|1
v5|登る|のぼる|noboru|to climb (a mountain)|चढ़ना (पहाड़ पर)|夏に富士山に登りました。|Natsu ni Fujisan ni noborimashita.|I climbed Mt. Fuji in summer.
v5|上る|のぼる|noboru|to go up (stairs/slope)|ऊपर जाना (सीढ़ियाँ)|階段を上って二階へ行きます。|Kaidan o nobotte nikai e ikimasu.|I go up the stairs to the 2nd floor.
v5|飛ぶ|とぶ|tobu|to fly / jump|उड़ना / कूदना|空を鳥が飛んでいます。|Sora o tori ga tonde imasu.|Birds are flying in the sky.|0
v5|泳ぐ|およぐ|oyogu|to swim|तैरना|海で楽しく泳ぎました。|Umi de tanoshiku oyogimashita.|I swam happily in the sea.|2
vs|散歩する|さんぽする|sanpo suru|to take a walk / stroll|टहलना / सैर करना|涼しい朝に公園を散歩します。|Suzushii asa ni kouen o sanpo shimasu.|I take a walk in the park on a cool morning.
vs|運転する|うんてんする|unten suru|to drive|गाड़ी चलाना|雨の日は気をつけて運転します。|Ame no hi wa ki o tsukete unten shimasu.|I drive carefully on rainy days.
v5|急ぐ|いそぐ|isogu|to hurry|जल्दी करना|遅れるから急ぎましょう。|Okureru kara isogimashou.|Let's hurry because we'll be late.
v1|遅れる|おくれる|okureru|to be late / delayed|देर होना / लेट होना|電車が五分遅れました。|Densha ga gofun okuremashita.|The train was delayed by five minutes.
v5|間に合う|まにあう|maniau|to be in time for|समय पर पहुँचना|九時の電車に間に合いました。|Kuji no densha ni maniaimashita.|I made it in time for the 9 o'clock train.
v5|送る|おくる|okuru|to send / see someone off|भेजना / छोड़ने जाना|友達を駅まで送りました。|Tomodachi o eki made okurimashita.|I walked my friend to the station.
v1|迎える|むかえる|mukaeru|to pick up / welcome|लेने जाना / स्वागत करना|空港へ両親を迎えに行きます。|Kuukou e ryoushin o mukae ni ikimasu.|I go to the airport to pick up my parents.
v5|動く|うごく|ugoku|to move / operate|हिलना / चलना|電車が動き始めました。|Densha ga ugoki hajimemashita.|The train started moving.
v5|運ぶ|はこぶ|hakobu|to carry / transport|ढोना / ले जाना|部屋へ荷物を運びます。|Heya e nimotsu o hakobimasu.|I carry the luggage to the room.
v5|引く|ひく|hiku|to pull|खींचना|このドアは引いて開けます。|Kono doa wa hiite akemasu.|You pull this door to open it.
v5|押す|おす|osu|to push / press|धक्का देना / दबाना|降車ボタンを押してください。|Kousha botan o oshite kudasai.|Please press the stop button.
v1|気をつける|きをつける|ki o tsukeru|to be careful / pay attention|सावधानी बरतना|車に気をつけて歩いてください。|Kuruma ni ki o tsukete aruite kudasai.|Please walk carefully watching out for cars.
v1|疲れる|つかれる|tsukareru|to get tired|थक जाना|たくさん歩いて疲れました。|Takusan aruite tsukaremashita.|I walked a lot and got tired.
"""

    // ===============================================================
    // UNIT 12: CORE DAILY VERBS PT. 1 — ROUTINE, STUDY & ACTIONS (56 Words)
    // ===============================================================
    private val n5Unit12Words = """
v1|起きる|おきる|okiru|to wake up / get up|उठना / जागना|毎朝六時に起きます。|Maiasa rokuji ni okimasu.|I wake up at 6 every morning.|2
v1|寝る|ねる|neru|to sleep / go to bed|सोना|夜は十一時に寝ます。|Yoru wa juuichiji ni nemasu.|I go to sleep at 11 PM.|0
v1|食べる|たべる|taberu|to eat|खाना|家族と晩ご飯を食べます。|Kazoku to bangohan o tabemasu.|I eat dinner with my family.|2
v5|飲む|のむ|nomu|to drink|पीना|朝、冷たい水を飲みます。|Asa, tsumetai mizu o nomimasu.|I drink cold water in the morning.|1
v1|見る|みる|miru|to see / look / watch|देखना|週末に映画を見ます。|Shuumatsu ni eiga o mimasu.|I watch a movie on the weekend.|1
v5|聞く|きく|kiku|to listen / hear / ask|सुनना / पूछना|日本の音楽を聞きます。|Nihon no ongaku o kikimasu.|I listen to Japanese music.|0
v5|話す|はなす|hanasu|to speak / talk|बोलना / बात करना|友達と日本語で話します。|Tomodachi to Nihongo de hanashimasu.|I speak in Japanese with my friend.|2
v5|言う|いう|iu|to say / tell|कहना / बोलना|日本語で何と言いますか。|Nihongo de nan to iimasu ka.|How do you say it in Japanese?
v5|読む|よむ|yomu|to read|पढ़ना|寝る前に本を読みます。|Neru mae ni hon o yomimasu.|I read a book before going to sleep.|1
v5|書く|かく|kaku|to write / draw|लिखना|ノートにひらがなを書きます。|Nooto ni hiragana o kakimasu.|I write Hiragana in my notebook.|1
v5|買う|かう|kau|to buy|खरीदना|デパートで時計を買いました。|Depaato de tokei o kaimashita.|I bought a watch at the department store.|0
v5|売る|うる|uru|to sell|बेचना|あの店で古い本を売っています。|Ano mise de furui hon o utte imasu.|They sell old books at that shop.
vs|する|する|suru|to do / play|करना|放課後、テニスをします。|Houkago, tenisu o shimasu.|After school, I play tennis.|0
vs|勉強する|べんきょうする|benkyou suru|to study|पढ़ाई करना|図書館で三時間勉強しました。|Toshokan de sanjikan benkyou shimashita.|I studied for three hours at the library.|0
vs|練習する|れんしゅうする|renshuu suru|to practice|अभ्यास (प्रैक्टिस) करना|毎日会話の練習をします。|Mainichi kaiwa no renshuu o shimasu.|I practice conversation every day.
vs|復習する|ふくしゅうする|fukushuu suru|to review lessons|दोहराना (रिवीजन करना)|今日習った漢字を復習します。|Kyou naratta kanji o fukushuu shimasu.|I review the kanji I learned today.
v5|働く|はたらく|hataraku|to work|काम करना / नौकरी करना|月曜日から金曜日まで働きます。|Getsuyoubi kara kin'youbi made hatarakimasu.|I work from Monday to Friday.|0
v5|休む|やすむ|yasumu|to rest / be absent|आराम करना / छुट्टी लेना|風邪をひいたから学校を休みます。|Kaze o hiita kara gakkou o yasumimasu.|Because I caught a cold, I take a day off from school.
v5|作る|つくる|tsukuru|to make / produce / cook|बनाना|日曜日は自分で料理を作ります。|Nichiyoubi wa jibun de ryouri o tsukurimasu.|On Sunday I cook meals by myself.|2
v5|使う|つかう|tsukau|to use|इस्तेमाल करना|パソコンを使って仕事をします。|Pasokon o tsukatte shigoto o shimasu.|I work using a computer.|0
v5|洗う|あらう|arau|to wash|धोना|食事の前に手を洗います。|Shokuji no mae ni te o araimasu.|I wash my hands before meals.|0
vs|掃除する|そうじする|souji suru|to clean / sweep|सफ़ाई करना|毎朝自分の部屋を掃除します。|Maiasa jibun no heya o souji shimasu.|I clean my room every morning.
vs|洗濯する|せんたくする|sentaku suru|to do laundry|कपड़े धोना|天気がいいから洗濯します。|Tenki ga ii kara sentaku shimasu.|Because the weather is good, I do laundry.
vs|買い物する|かいものする|kaimono suru|to do shopping|खरीदारी करना|夕方にスーパーで買い物します。|Yuugata ni suupaa de kaimono shimasu.|I shop at the supermarket in the evening.
vs|料理する|りょうりする|ryouri suru|to cook|खाना पकाना|母と一緒に料理しました。|Haha to issho ni ryouri shimashita.|I cooked together with my mother.
vs|電話する|でんわする|denwa suru|to make a phone call|फ़ोन करना|後で会社に電話します。|Ato de kaisha ni denwa shimasu.|I will call the office later.
vs|質問する|しつもんする|shitsumon suru|to ask a question|सवाल पूछना|分からないとき先生に質問します。|Wakaranai toki sensei ni shitsumon shimasu.|When I don't understand, I ask the teacher.
v1|答える|こたえる|kotaeru|to answer / reply|उत्तर देना|日本語で質問に答えます。|Nihongo de shitsumon ni kotaemasu.|I answer the question in Japanese.
v1|教える|おしえる|oshieru|to teach / tell / inform|सिखाना / बताना|友達に電話番号を教えました。|Tomodachi ni denwa bangou o oshiemashita.|I told my friend my phone number.|0
v5|習う|ならう|narau|to learn (from a person)|सीखना (किसी गुरु/व्यक्ति से)|先生から日本語を習っています。|Sensei kara Nihongo o naratte imasu.|I am learning Japanese from a teacher.
v1|覚える|おぼえる|oboeru|to memorize / remember|याद करना|毎日新しい単語を十個覚えます。|Mainichi atarashii tango o jukko oboemasu.|I memorize 10 new words every day.
v1|忘れる|わすれる|wasureru|to forget / leave behind|भूल जाना|電車に傘を忘れました。|Densha ni kasa o wasuremashita.|I left my umbrella on the train.
v5|分かる|わかる|wakaru|to understand / know|समझना|ひらがなが全部分かります。|Hiragana ga zenbu wakarimasu.|I understand all of Hiragana.|2
v5|知る|しる|shiru|to get to know / know|जानना / पता होना|あの人の名前を知っていますか。|Ano hito no namae o shitte imasu ka.|Do you know that person's name?
v5|思う|おもう|omou|to think / feel|सोचना / लगना|この本はとてもいいと思います。|Kono hon wa totemo ii to omoimasu.|I think this book is very good.
v1|考える|かんがえる|kangaeru|to think / consider|विचार करना / सोचना|もう少し考えさせてください。|Mou sukoshi kangaesasete kudasai.|Please let me think a little more.
v5|始まる|はじまる|hajimaru|to begin / start (intransitive)|शुरू होना|映画は十時から始まります。|Eiga wa juuji kara hajimarimasu.|The movie starts at 10 o'clock.
v1|始める|はじめる|hajimeru|to begin / start (transitive)|शुरू करना|それでは、テストを始めます。|Sore dewa, tesuto o hajimemasu.|Well then, we will start the test.
v5|終わる|おわる|owaru|to end / finish|खत्म होना / पूरा होना|授業は四時に終わります。|Jugyou wa yoji ni owarimasu.|Class finishes at 4 o'clock.
v5|開く|あく|aku|to open (intransitive)|खुलना (अपने आप)|ドアが開きました。|Doa ga akimashita.|The door opened.
v1|開ける|あける|akeru|to open (something)|खोलना|暑いから窓を開けてください。|Atsui kara mado o akete kudasai.|Because it's hot, please open the window.|0
v5|閉まる|しまる|shimaru|to close / be shut (intransitive)|बंद होना|スーパーは九時に閉まります。|Suupaa wa kuji ni shimarimasu.|The supermarket closes at 9 o'clock.
v1|閉める|しめる|shimeru|to close / shut (something)|बंद करना|静かにドアを閉めてください。|Shizuka ni doa o shimete kudasai.|Please close the door quietly.|2
v5|つく|つく|tsuku|to come on (lights/electricity)|लाइट जलना / चालू होना|部屋の電気がついています。|Heya no denki ga tsuite imasu.|The room light is on.
v1|つける|つける|tsukeru|to turn on / switch on / attach|चालू करना (लाइट/टीवी/एसी)|暗いから電気をつけます。|Kurai kara denki o tsukemasu.|Because it is dark, I turn on the light.
v1|消える|きえる|kieru|to go off / disappear|बुझ जाना / गायब होना|急に電気が消えました。|Kyuu ni denki ga kiemashita.|Suddenly the lights went out.
v5|消す|けす|kesu|to turn off / erase|बंद करना (लाइट) / मिटाना|寝る前に電気を消します。|Neru mae ni denki o keshimasu.|I turn off the light before sleeping.
v1|入れる|いれる|ireru|to put in / insert / make tea|अंदर डालना / रखना|かばんに教科書を入れます。|Kaban ni kyoukasho o iremasu.|I put the textbook into my bag.
v5|出す|だす|dasu|to take out / send / hand in|बाहर निकालना / जमा करना|明日までに宿題を出してください。|Ashita made ni shukudai o dashite kudasai.|Please hand in your homework by tomorrow.
v5|置く|おく|oku|to put / place|रखना|荷物をここに置いてください。|Nimotsu o koko ni oite kudasai.|Please place your luggage here.
v5|持つ|もつ|motsu|to hold / carry / possess|पकड़ना / पास होना|重い荷物を持ちましょうか。|Omoi nimotsu o mochimashou ka.|Shall I carry the heavy luggage for you?
v5|取る|とる|toru|to take / pass / get|लेना / पकड़ना|塩を取ってください。|Shio o totte kudasai.|Please pass me the salt.
v5|撮る|とる|toru|to take (a photo / video)|फ़ोटो खींचना|家族の写真を撮りました。|Kazoku no shashin o torimashita.|I took a photo of my family.
v5|切る|きる|kiru|to cut|काटना|ナイフでりんごを切ります。|Naifu de ringo o kirimasu.|I cut an apple with a knife.
v5|貼る|はる|haru|to stick / paste / post|चिपकाना / लगाना|封筒に切手を貼ってください。|Fuutou ni kitte o hatte kudasai.|Please stick a stamp on the envelope.
v5|引く|ひく|hiku|to look up (in dictionary) / catch cold|डिक्शनरी में ढूँढना|分からない漢字を辞書で引きます。|Wakaranai kanji o jisho de hikimasu.|I look up unknown kanji in a dictionary.
"""

    // ===============================================================
    // UNIT 13: CORE DAILY VERBS PT. 2 — EXISTENCE, WEARING, GIVING & LIFE (56 Words)
    // ===============================================================
    private val n5Unit13Words = """
v5|ある|ある|aru|to exist / have (inanimate things)|होना (निर्जीव वस्तु के लिए)|机の上にノートがあります。|Tsukue no ue ni nooto ga arimasu.|There is a notebook on the desk.
v1|いる|いる|iru|to exist / be (living beings)|होना (जीवित इंसान/जानवर के लिए)|庭に犬が二匹います。|Niwa ni inu ga nihiki imasu.|There are two dogs in the yard.
v5|なる|なる|naru|to become|बनना / हो जाना|春になって暖かくなりました。|Haru ni natte atatakaku narimashita.|Spring came and it became warm.
v5|会う|あう|au|to meet (takes に!)|मिलना|駅で友達に会いました。|Eki de tomodachi ni aimashita.|I met a friend at the station.|1
v5|待つ|まつ|matsu|to wait|इंतज़ार करना|ロビーで十分待ちました。|Robii de juppun machimashita.|I waited in the lobby for 10 minutes.|1
v5|座る|すわる|suwaru|to sit (takes に!)|बैठना|前の席に座ってください。|Mae no seki ni suwatte kudasai.|Please sit in the front seat.|0
v5|立つ|たつ|tatsu|to stand|खड़ा होना|名前を呼ばれたら立ってください。|Namae o yobaretara tatte kudasai.|Please stand up when your name is called.|1
v5|遊ぶ|あそぶ|asobu|to play / hang out|खेलना / मौज-मस्ती करना|週末は友達と遊びます。|Shuumatsu wa tomodachi to asobimasu.|I hang out with friends on the weekend.|0
v5|歌う|うたう|utau|to sing|गाना गाना|みんなで日本の歌を歌いました。|Minna de Nihon no uta o utaimashita.|Everyone sang a Japanese song together.|0
v5|弾く|ひく|hiku|to play (piano / guitar)|बजाना (पियानो/गिटार)|姉はピアノを弾くのが上手です。|Ane wa piano o hiku no ga jouzu desu.|My older sister is good at playing the piano.
v5|吹く|ふく|fuku|to blow (wind) / play flute|हवा चलना / बांसुरी बजाना|今日は冷たい風が吹いています。|Kyou wa tsumetai kaze ga fuite imasu.|A cold wind is blowing today.
v5|吸う|すう|suu|to breathe in / smoke|साँस लेना / धूम्रपान करना|ここでタバコを吸ってはいけません。|Koko de tabako o sutte wa ikemasen.|You must not smoke here.
v5|呼ぶ|よぶ|yobu|to call out / invite|बुलाना|店員さんを呼びましょう。|Ten'in-san o yobimashou.|Let's call the waiter/clerk.|0
v5|頼む|たのむ|tanomu|to ask a favor / order|निवेदन करना / ऑर्डर करना|友達に仕事を頼みました。|Tomodachi ni shigoto o tanomimashita.|I asked my friend for a favor with work.
v5|手伝う|てつだう|tetsudau|to help / assist|मदद करना / हाथ बँटाना|母の料理を手伝います。|Haha no ryouri o tetsudaimasu.|I help with my mother's cooking.
v5|貸す|かす|kasu|to lend / rent out|उधार देना|友達に傘を貸しました。|Tomodachi ni kasa o kashimashita.|I lent an umbrella to my friend.
v1|借りる|かりる|kariru|to borrow / rent|उधार लेना|図書館で本を三冊借りました。|Toshokan de hon o sansatsu karimashita.|I borrowed three books from the library.|0
v5|返す|かえす|kaesu|to return (something borrowed)|वापस लौटाना|来週までに本を返します。|Raishuu made ni hon o kaeshimasu.|I will return the book by next week.|1
v1|あげる|あげる|ageru|to give (to someone)|देना|母の日に花をあげました。|Haha no hi ni hana o agemashita.|I gave flowers on Mother's Day.
v5|もらう|もらう|morau|to receive / get|पाना / प्राप्त करना|先生から辞書をもらいました。|Sensei kara jisho o moraimashita.|I received a dictionary from my teacher.
v1|くれる|くれる|kureru|to give (to me)|मुझे देना|友達がプレゼントをくれました。|Tomodachi ga purezento o kuremashita.|My friend gave me a present.
v5|払う|はらう|harau|to pay (money)|पैसे चुकाना / भुगतान करना|レジでお金を払います。|Reji de okane o haraimasu.|I pay the money at the register.
v5|かかる|かかる|kakaru|to take (time or money)|समय या पैसा लगना|家から学校まで三十分かかります。|Ie kara gakkou made sanjuppun kakarimasu.|It takes 30 minutes from home to school.
v1|足りる|たりる|tariru|to be enough / sufficient|पर्याप्त (काफ़ी) होना|千円あれば足ります。|Sen-en areba tarimasu.|If you have 1000 yen, it will be enough.
v1|着る|きる|kiru|to wear / put on (upper body)|पहनना (शर्ट/कोट/किमोनो)|寒いからセーターを着ます。|Samui kara seetaa o kimasu.|Because it is cold, I put on a sweater.
v5|履く|はく|haku|to wear / put on (lower body: shoes/pants)|पहनना (जूते/पैंट/स्कर्ट)|新しい靴を履いて出かけます。|Atarashii kutsu o haite dekakemasu.|I put on new shoes and go out.
v5|かぶる|かぶる|kaburu|to wear / put on (on head: hat/cap)|पहनना (टोपी)|白い帽子をかぶっています。|Shiroi boushi o kabutte imasu.|He is wearing a white cap.
v1|かける|かける|kakeru|to wear (glasses) / hang / call|लगाना (चश्मा) / टाँगना|祖父は眼鏡をかけて新聞を読みます。|Sofu wa megane o kakete shinbun o yomimasu.|Grandfather puts on glasses and reads the newspaper.
v5|脱ぐ|ぬぐ|nugu|to take off (clothes/shoes)|उतारना (कपड़े/जूते)|部屋に入る前に靴を脱ぎます。|Heya ni hairu mae ni kutsu o nugimasu.|I take off my shoes before entering the room.
v1|締める|しめる|shimeru|to tie / fasten (belt/necktie)|बाँधना (टाई/बेल्ट)|青いネクタイを締めます。|Aoi nekutai o shimemasu.|I tie a blue necktie.
v5|住む|すむ|sumu|to live / reside|रहना (निवास करना)|私は東京に住んでいます。|Watashi wa Toukyou ni sunde imasu.|I live in Tokyo.
vs|結婚する|けっこんする|kekkon suru|to get married|शादी करना|姉は来月結婚します。|Ane wa raigetsu kekkon shimasu.|My older sister is getting married next month.
v1|生まれる|うまれる|umareru|to be born|जन्म होना / पैदा होना|私は二〇〇七年に生まれました。|Watashi wa nisen-nananen ni umaremashita.|I was born in 2007.
v5|死ぬ|しぬ|shinu|to die|मरना|庭の木が枯れて死にました。|Niwa no ki ga karete shinimashita.|The tree in the yard withered and died.|0
v5|咲く|さく|saku|to bloom (flowers)|खिलना (फूलों का)|春になると桜が咲きます。|Haru ni naru to sakura ga sakimasu.|When spring comes, cherry blossoms bloom.
v5|降る|ふる|furu|to fall (rain / snow)|बरसना (बारिश या बर्फ़ गिरना)|外は雨が降っています。|Soto wa ame ga futte imasu.|It is raining outside.
v1|晴れる|はれる|hareru|to clear up / be sunny|मौसम साफ़ होना / धूप निकलना|明日はよく晴れるでしょう。|Ashita wa yoku hareru deshou.|Tomorrow will probably be very sunny.
v5|曇る|くもる|kumoru|to become cloudy|बादल छाना|午後から空が曇りました。|Gogo kara sora ga kumorimashita.|The sky became cloudy from the afternoon.
v5|鳴く|なく|naku|to cry / bark / meow (animals)|बोलना/भौंकना (जानवरों या पक्षियों का)|朝、鳥がきれいに鳴いています。|Asa, tori ga kirei ni naite imasu.|Birds are singing prettily in the morning.
v5|泣く|なく|naku|to cry / weep (humans)|रोना (इंसान का)|映画を見て泣きました。|Eiga o mite nakimashita.|I watched the movie and cried.
v5|笑う|わらう|warau|to laugh / smile|हँसना / मुस्कुराना|みんなで楽しく笑いました。|Minna de tanoshiku waraimashita.|Everyone laughed happily together.
v5|怒る|おこる|okoru|to get angry / scold|गुस्सा होना|遅刻して先生に怒られました。|Chikoku shite sensei ni okoraremashita.|I was late and got scolded by the teacher.
v5|困る|こまる|komaru|to be troubled / in difficulty|परेशान होना / मुसीबत में पड़ना|鍵をなくして困りました。|Kagi o nakushite komarimashita.|I lost my key and was in trouble.
v1|浴びる|あびる|abiru|to take (a shower) / bask|नहाना (शॉवर लेना)|運動の後でシャワーを浴びます。|Undou no ato de shawaa o abimasu.|I take a shower after exercising.
v5|磨く|みがく|migaku|to brush (teeth) / polish|ब्रश करना (दाँत) / चमकाना|毎晩歯を磨いてから寝ます。|Maiban ha o migaite kara nemasu.|Every night I brush my teeth before sleeping.
v1|並べる|ならべる|naraberu|to line up / arrange (things)|क्रम से लगाना / सजाना|テーブルにお皿を並べます。|Teeburu ni osara o narabemasu.|I arrange the plates on the table.
v5|並ぶ|ならぶ|narabu|to line up / stand in a queue|कतार (लाइन) में खड़ा होना|店の前に人が並んでいます。|Mise no mae ni hito ga narande imasu.|People are standing in line in front of the shop.
v5|渡す|わたす|watasu|to hand over / give across|सौंपना / हाथ में देना|先生にレポートを渡しました。|Sensei ni repooto o watashimashita.|I handed the report to the teacher.
v5|なくす|なくす|nakusu|to lose (something)|खो देना|どこかで財布をなくしました。|Dokoka de saifu o nakushimashita.|I lost my wallet somewhere.
v1|見せる|みせる|miseru|to show|दिखाना|切符を駅員さんに見せます。|Kippu o ekiin-san ni misemasu.|I show the ticket to the station staff.
v1|調べる|しらべる|shiraberu|to check / look up / investigate|जाँचना / खोजना|電車の時間を調べます。|Densha no jikan o shirabemasu.|I check the train schedule.
v5|選ぶ|えらぶ|erabu|to choose / select|चुनना|好きな色を一つ選んでください。|Suki na iro o hitotsu erande kudasai.|Please choose one color you like.
v5|直す|なおす|naosu|to fix / repair / correct|ठीक करना / सुधारना|壊れた自転車を直します。|Kowareta jitensha o naoshimasu.|I repair the broken bicycle.
v1|決める|きめる|kimeru|to decide|तय करना / फ़ैसला करना|メニューを見て料理を決めます。|Menyuu o mite ryouri o kimemasu.|I look at the menu and decide on a dish.
v1|比べる|くらべる|kuraberu|to compare|तुलना करना|二つの値段を比べます。|Futatsu no nedan o kurabemasu.|I compare the two prices.
v1|集める|あつめる|atsumeru|to collect / gather|इकट्ठा करना|珍しい切手を集めています。|Mezurashii kitte o atsumete imasu.|I collect rare stamps.
"""

    // ===============================================================
    // UNIT 14: ALL N5 い-ADJECTIVES — WEATHER, SIZE, TASTE & FEELINGS (58 Words)
    // ===============================================================
    private val n5Unit14Words = """
adji|大きい|おおきい|ookii|big / large|बड़ा|東京はとても大きい町です。|Toukyou wa totemo ookii machi desu.|Tokyo is a very big city.|3
adji|小さい|ちいさい|chiisai|small / little|छोटा|小さい辞書を買いました。|Chiisai jisho o kaimashita.|I bought a small dictionary.|3
adji|新しい|あたらしい|atarashii|new / fresh|नया|新しいパソコンが欲しいです。|Atarashii pasokon ga hoshii desu.|I want a new laptop.|4
adji|古い|ふるい|furui|old (for things, not people)|पुराना (वस्तुओं के लिए)|京都には古いお寺が多いです。|Kyouto ni wa furui otera ga ooi desu.|There are many old temples in Kyoto.|2
adji|いい|いい|ii|good / nice / fine|अच्छा / बढ़िया|今日はいい天気ですね。|Kyou wa ii tenki desu ne.|It's nice weather today, isn't it?|1
adji|よい|よい|yoi|good (used in conjugations: よかった)|अच्छा (भूतकाल: よかった)|試験が終わってよかったです。|Shiken ga owatte yokatta desu.|I am glad the exam is over.
adji|悪い|わるい|warui|bad / poor|बुरा / ख़राब|今日は少し体の調子が悪いです。|Kyou wa sukoshi karada no choushi ga warui desu.|I feel a bit unwell today.
adji|高い|たかい|takai|expensive / high / tall|महँगा / ऊँचा|富士山は日本一番高い山です。|Fujisan wa Nihon de ichiban takai yama desu.|Mt. Fuji is the highest mountain in Japan.|2
adji|安い|やすい|yasui|cheap / inexpensive|सस्ता|このスーパーの野菜は安いです。|Kono suupaa no yasai wa yasui desu.|The vegetables at this supermarket are cheap.|2
adji|低い|ひくい|hikui|low / short (height)|नीचा / कम ऊँचाई वाला|このテーブルは少し低いです。|Kono teeburu wa sukoshi hikui desu.|This table is a little low.
adji|長い|ながい|nagai|long|लम्बा|長い髪がとてもきれいですね。|Nagai kami ga totemo kirei desu ne.|Your long hair is very pretty.
adji|短い|みじかい|mijikai|short (length / time)|छोटा (लम्बाई या समय में)|冬は昼の時間が短いです。|Fuyu wa hiru no jikan ga mijikai desu.|Daytime hours are short in winter.
adji|重い|おもい|omoi|heavy|भारी|このスーツケースはとても重いです。|Kono suutsukeesu wa totemo omoi desu.|This suitcase is very heavy.
adji|軽い|かるい|karui|light (weight)|हल्का (वज़न में)|軽くて使いやすいかばんです。|Karukute tsukaiyasui kaban desu.|It is a light and easy-to-use bag.
adji|広い|ひろい|hiroi|spacious / wide|चौड़ा / बड़ा और खुला|大学のキャンパスは広いです。|Daigaku no kyanpasu wa hiroi desu.|The university campus is spacious.
adji|狭い|せまい|semai|narrow / cramped (small room)|सँकरा / छोटा (कमरा या गली)|私の部屋は狭いですが快適です。|Watashi no heya wa semai desu ga kaiteki desu.|My room is small, but comfortable.
adji|多い|おおい|ooi|many / numerous|बहुत / अधिक संख्या में|東京は人と車が多いです。|Toukyou wa hito to kuruma ga ooi desu.|Tokyo has many people and cars.
adji|少ない|すくない|sukunai|few / scarce|कम / थोड़ा|今日は雨で人が少ないです。|Kyou wa ame de hito ga sukunai desu.|Because of the rain, there are few people today.
adji|遠い|とおい|tooi|far / distant|दूर|家から駅まで少し遠いです。|Ie kara eki made sukoshi tooi desu.|It is a bit far from my house to the station.
adji|近い|ちかい|chikai|near / close|पास / नज़दीक|コンビニが近くて便利です。|Konbini ga chikakute benri desu.|The convenience store is close and convenient.
adji|速い|はやい|hayai|fast / quick (speed)|तेज़ (गति में)|新幹線はとても速いです。|Shinkansen wa totemo hayai desu.|The bullet train is very fast.
adji|早い|はやい|hayai|early (time)|जल्दी (समय में)|父は毎朝起きるのが早いです。|Chichi wa maiasa okiru no ga hayai desu.|My father wakes up early every morning.
adji|遅い|おそい|osoi|slow / late|धीमा / देर से|夜はバスが遅いです。|Yoru wa basu ga osoi desu.|Buses are slow/late at night.
adji|暑い|あつい|atsui|hot (weather)|गर्म (मौसम)|インドの夏はとても暑いです。|Indo no natsu wa totemo atsui desu.|Summer in India is very hot.|2
adji|寒い|さむい|samui|cold (weather)|ठंडा (मौसम)|今日は昨日より寒いです。|Kyou wa kinou yori samui desu.|Today is colder than yesterday.|2
adji|暖かい|あたたかい|atatakai|warm (weather)|सुहावना गर्म / गुनगुना (मौसम)|春になって暖かくなりました。|Haru ni natte atatakaku narimashita.|Spring came and the weather got warm.
adji|涼しい|すずしい|suzushii|cool / refreshing (weather)|ठंडा और सुहावना (हवा/मौसम)|秋は風が涼しくて気持ちいいです。|Aki wa kaze ga suzushikute kimochi ii desu.|In autumn the breeze is cool and pleasant.
adji|熱い|あつい|atsui|hot (to touch / food & drink)|गर्म (चाय/खाना/छूने में)|熱いお茶をどうぞ。|Atsui ocha o douzo.|Please have some hot green tea.
adji|冷たい|つめたい|tsumetai|cold (to touch / drinks)|ठंडा (पानी/छूने में)|冷たい水を飲みたいです。|Tsumetai mizu o nomitai desu.|I want to drink cold water.
adji|温かい|あたたかい|atatakai|warm (food / drink / heart)|गर्म (भोजन/सूप)|温かいスープがおいしいです。|Atatakai suupu ga oishii desu.|Warm soup is delicious.
adji|おいしい|おいしい|oishii|delicious / tasty|स्वादिष्ट|母が作った料理はおいしいです。|Haha ga tsukutta ryouri wa oishii desu.|The food my mother made is delicious.|0
adji|まずい|まずい|mazui|bad tasting / unappetizing|बेस्वाद / ख़राब स्वाद वाला|この薬は苦くてまずいです。|Kono kusuri wa nigakute mazui desu.|This medicine is bitter and tastes bad.
adji|甘い|あまい|amai|sweet|मीठा|甘いケーキを食べました。|Amai keeki o tabemashita.|I ate a sweet cake.
adji|辛い|からい|karai|spicy / hot|तीखा / चटपटा|辛いカレーが大好きです。|Karai karee ga daisuki desu.|I love spicy curry.
adji|塩辛い|しおからい|shiokarai|salty|नमकीन / तेज़ नमक वाला|この魚は少し塩辛いです。|Kono sakana wa sukoshi shiokarai desu.|This fish is a bit salty.
adji|酸っぱい|すっぱい|suppai|sour|खट्टा|このみかんは少し酸っぱいです。|Kono mikan wa sukoshi suppai desu.|This mandarin is a little sour.
adji|苦い|にがい|nigai|bitter|कड़वा|ブラックコーヒーは苦いです。|Burakku koohii wa nigai desu.|Black coffee is bitter.
adji|楽しい|たのしい|tanoshii|fun / enjoyable|मज़ेदार / आनंददायक|友達との旅行は本当に楽しいです。|Tomodachi to no ryokou wa hontou ni tanoshii desu.|Traveling with friends is truly fun.|3
adji|面白い|おもしろい|omoshiroi|interesting / funny|रोचक / मज़ेदार|この本はとても面白いです。|Kono hon wa totemo omoshiroi desu.|This book is very interesting.
adji|つまらない|つまらない|tsumaranai|boring / dull|उबाऊ / बोरिंग|昨日の映画はつまらなかったです。|Kinou no eiga wa tsumaranakatta desu.|Yesterday's movie was boring.
adji|忙しい|いそがしい|isogashii|busy|व्यस्त (बिज़ी)|今週はテストがあって忙しいです。|Konshuu wa tesuto ga atte isogashii desu.|I have tests this week and am busy.|4
adji|難しい|むずかしい|muzukashii|difficult / hard|कठिन / मुश्किल|漢字の読み方は難しいです。|Kanji no yomikata wa muzukashii desu.|The readings of kanji are difficult.
adji|易しい|やさしい|yasashii|easy / simple|आसान / सरल|この問題は易しいからすぐ分かります。|Kono mondai wa yasashii kara sugu wakarimasu.|Because this problem is easy, I understand it right away.
adji|優しい|やさしい|yasashii|kind / gentle / tender|दयालु / नेक|私の先生はとても優しいです。|Watashi no sensei wa totemo yasashii desu.|My teacher is very kind.
adji|嬉しい|うれしい|ureshii|happy / glad|खुश / प्रसन्न|プレゼントをもらって嬉しいです。|Purezento o moratte ureshii desu.|I am happy to receive a present.
adji|悲しい|かなしい|kanashii|sad / sorrowful|उदास / दुखी|悲しいニュースを聞きました。|Kanashii nyuusu o kikimashita.|I heard sad news.
adji|寂しい|さびしい|sabishii|lonely|अकेला / सूना|一人でいると寂しいです。|Hitori de iru to sabishii desu.|I feel lonely when I am by myself.
adji|怖い|こわい|kowai|scary / afraid|डरावना / डर लगना|暗い夜道は怖いです。|Kurai yomichi wa kowai desu.|A dark night road is scary.
adji|痛い|いたい|itai|painful / hurts / sore|दर्द होना|昨日から頭が痛いです。|Kinou kara atama ga itai desu.|My head has been hurting since yesterday.
adji|眠い|ねむい|nemui|sleepy / drowsy|नींद आना|よく寝なかったから眠いです。|Yoku nenakatta kara nemui desu.|Because I didn't sleep well, I am sleepy.
adji|欲しい|ほしい|hoshii|want / desirable (takes が!)|चाहिए (किसी वस्तु की इच्छा)|新しい自転車が欲しいです。|Atarashii jitensha ga hoshii desu.|I want a new bicycle.
adji|若い|わかい|wakai|young|युवा / जवान|あの先生はまだ若いです。|Ano sensei wa mada wakai desu.|That teacher is still young.
adji|強い|つよい|tsuyoi|strong / powerful|मज़बूत / तेज़|今日は風が強いです。|Kyou wa kaze ga tsuyoi desu.|The wind is strong today.
adji|弱い|よわい|yowai|weak|कमज़ोर|私はお酒に弱いです。|Watashi wa osake ni yowai desu.|I have a low tolerance for alcohol.
adji|明るい|あかるい|akarui|bright / cheerful|रोशन / उजाला / हँसमुख|この部屋は窓が大きくて明るいです。|Kono heya wa mado ga ookikute akarui desu.|This room has big windows and is bright.
adji|暗い|くらい|kurai|dark / gloomy|अँधेरा|外が暗くなりました。|Soto ga kuraku narimashita.|It has gotten dark outside.
adji|太い|ふとい|futoi|thick / fat|मोटा|太いペンで大きく書きます。|Futoi pen de ookiku kakimasu.|I write big with a thick pen.
adji|細い|ほそい|hosoi|thin / slender / narrow|पतला|この道は細くて車が通れません。|Kono michi wa hosokute kuruma ga tooremasen.|This road is narrow so cars cannot pass.
"""

    // ===============================================================
    // UNIT 15: な-ADJECTIVES, NATURE, SEASONS, BODY PARTS & COLORS (60 Words)
    // ===============================================================
    private val n5Unit15Words = """
adjna|好き|すき|suki|like / fond of (takes が!)|पसंद|私は日本のアニメが好きです。|Watashi wa Nihon no anime ga suki desu.|I like Japanese anime.|2
adjna|大好き|だいすき|daisuki|love / like very much (takes が!)|बहुत ज़्यादा पसंद|母のカレーが大好きです。|Haha no karee ga daisuki desu.|I love my mother's curry.
adjna|嫌い|きらい|kirai|dislike (takes が!)|नापसंद|雨の日は嫌いです。|Ame no hi wa kirai desu.|I dislike rainy days.
adjna|上手|じょうず|jouzu|skillful / good at (takes が!)|माहिर / निपुण|彼女は歌が上手です。|Kanojo wa uta ga jouzu desu.|She is good at singing.|3
adjna|下手|へた|heta|unskilled / poor at (takes が!)|कमज़ोर (किसी कला में)|私は絵を描くのが下手です。|Watashi wa e o kaku no ga heta desu.|I am bad at drawing pictures.
adjna|元気|げんき|genki|healthy / energetic / fine|स्वस्थ / चंगा / ऊर्जावान|おじいさんは毎日元気です。|Ojiisan wa mainichi genki desu.|Grandfather is energetic every day.|1
adjna|静か|しずか|shizuka|quiet / peaceful|शांत|静かな公園を歩きます。|Shizuka na kouen o arukimasu.|I walk through a quiet park.|1
adjna|賑やか|にぎやか|nigiyaka|lively / bustling|रौनक भरा / चहल-पहल वाला|渋谷はいつも賑やかな町です。|Shibuya wa itsumo nigiyaka na machi desu.|Shibuya is always a bustling town.
adjna|きれい|きれい|kirei|pretty / beautiful / clean (な-Adj!)|सुंदर / साफ़|京都の紅葉はとてもきれいです。|Kyouto no kouyou wa totemo kirei desu.|Kyoto's autumn leaves are very beautiful.|1
adjna|有名|ゆうめい|yuumei|famous (な-Adj!)|मशहूर / प्रसिद्ध|ここは有名なお寺です。|Koko wa yuumei na otera desu.|This is a famous temple.|0
adjna|親切|しんせつ|shinsetsu|kind / helpful|दयालु / मददगार|駅員さんはとても親切でした。|Ekiin-san wa totemo shinsetsu deshita.|The station attendant was very kind.
adjna|便利|べんり|benri|convenient / handy|सुविधाजनक|スマホはとても便利な道具です。|Sumaho wa totemo benri na dougu desu.|A smartphone is a very convenient tool.
adjna|不便|ふべん|fuben|inconvenient|असुविधाजनक|車がなくて少し不便です。|Kuruma ga nakute sukoshi fuben desu.|It is a bit inconvenient without a car.
adjna|暇|ひま|hima|free time / not busy|खाली (फ़ुर्सत में)|日曜日は暇ですから遊びに来てください。|Nichiyoubi wa hima desu kara asobi ni kite kudasai.|Since I'm free on Sunday, please come hang out.
adjna|大切|たいせつ|taisetsu|important / precious|महत्वपूर्ण / कीमती|健康が一番大切です。|Kenkou ga ichiban taisetsu desu.|Health is the most important thing.
adjna|大変|たいへん|taihen|tough / hard / serious|कठिन / बहुत मेहनत वाला|毎日の仕事は大変ですね。|Mainichi no shigoto wa taihen desu ne.|Daily work is tough, isn't it?
adjna|簡単|かんたん|kantan|simple / easy|सरल / आसान|簡単な料理なら作れます。|Kantan na ryouri nara tsukuremasu.|I can make it if it's a simple dish.
adjna|立派|りっぱ|rippa|splendid / fine / grand|शानदार / भव्य|立派な建物ですね。|Rippa na tatemono desu ne.|What a magnificent building!
adjna|丈夫|じょうぶ|joubu|sturdy / strong / durable|मज़बूत / टिकाऊ|このかばんは丈夫で長持ちします。|Kono kaban wa joubu de nagamochi shimasu.|This bag is sturdy and lasts long.
adjna|安全|あんぜん|anzen|safe / secure|सुरक्षित|日本の夜道は安全です。|Nihon no yomichi wa anzen desu.|Japanese streets at night are safe.
adjna|危険|きけん|kiken|dangerous / risky|ख़तरनाक|ここは危険ですから入らないでください。|Koko wa kiken desu kara hairanaide kudasai.|Because it's dangerous here, please don't enter.
adjna|真っ直ぐ|まっすぐ|massugu|straight ahead|बिल्कुल सीधा|この道を真っ直ぐ行くと駅があります。|Kono michi o massugu iku to eki ga arimasu.|If you go straight down this road, there is a station.
n|空|そら|sora|sky|आसमान / आकाश|青い空に白い雲があります。|Aoi sora ni shiroi kumo ga arimasu.|There are white clouds in the blue sky.
n|太陽|たいよう|taiyou|sun|सूरज / सूर्य|太陽が明るく輝いています。|Taiyou ga akaruku kagayaite imasu.|The sun is shining brightly.
n|月|つき|tsuki|moon|चाँद|今夜は月がとてもきれいです。|Kon'ya wa tsuki ga totemo kirei desu.|The moon is very beautiful tonight.
n|星|ほし|hoshi|star|तारा / सितारे|夜空に星がたくさん見えます。|Yozora ni hoshi ga takusan miemasu.|Many stars can be seen in the night sky.
n|風|かぜ|kaze|wind / breeze|हवा|涼しい風が吹いています。|Suzushii kaze ga fuite imasu.|A cool breeze is blowing.
n|雨|あめ|ame|rain (High-Low pitch)|बारिश / वर्षा|明日は雨が降るでしょう。|Ashita wa ame ga furu deshou.|It will probably rain tomorrow.|1
n|雪|ゆき|yuki|snow|बर्फ़ (स्नो)|北海道では冬に雪がたくさん降ります。|Hokkaidou dewa fuyu ni yuki ga takusan furimasu.|A lot of snow falls in Hokkaido in winter.
n|天気|てんき|tenki|weather|मौसम|明日の天気はどうですか。|Ashita no tenki wa dou desu ka.|How is tomorrow's weather?|1
n|春|はる|haru|spring|वसंत ऋतु (Spring)|春は桜が咲いてきれいです。|Haru wa sakura ga saite kirei desu.|In spring, cherry blossoms bloom and are pretty.
n|夏|なつ|natsu|summer|गर्मी का मौसम (Summer)|日本の夏は暑いです。|Nihon no natsu wa atsui desu.|Japanese summers are hot.
n|秋|あき|aki|autumn / fall|पतझड़ (Autumn)|秋は読書とスポーツの季節です。|Aki wa dokusho to supootsu no kisetsu desu.|Autumn is the season for reading and sports.
n|冬|ふゆ|fuyu|winter|सर्दी का मौसम (Winter)|冬はスキーに行きたいです。|Fuyu wa sukii ni ikitai desu.|I want to go skiing in winter.
n|山|やま|yama|mountain|पहाड़ / पर्वत|遠くに高い山が見えます。|Tooku ni takai yama ga miemasu.|Tall mountains are visible in the distance.|2
n|川|かわ|kawa|river|नदी|きれいな川で魚が泳いでいます。|Kirei na kawa de sakana ga oyoide imasu.|Fish are swimming in the clean river.|2
n|海|うみ|umi|sea / ocean / beach|समुद्र / सागर|夏休みに家族と海へ行きました。|Natsuyasumi ni kazoku to umi e ikimashita.|I went to the beach with my family during summer vacation.|1
n|木|き|ki|tree / wood|पेड़ / लकड़ी|公園に大きい木があります。|Kouen ni ookii ki ga arimasu.|There is a big tree in the park.
n|花|はな|hana|flower|फूल|テーブルに花を飾ります。|Teeburu ni hana o kazarimasu.|I display flowers on the table.
n|犬|いぬ|inu|dog|कुत्ता|犬と公園を走ります。|Inu to kouen o hashirimasu.|I run in the park with my dog.|2
n|猫|ねこ|neko|cat|बिल्ली|屋根の上に猫がいます。|Yane no ue ni neko ga imasu.|There is a cat on the roof.|1
n|鳥|とり|tori|bird|पक्षी / चिड़िया|木の上に小さい鳥がいます。|Ki no ue ni chiisai tori ga imasu.|There is a small bird in the tree.
n|体|からだ|karada|body / health|शरीर / सेहत|野菜は体にいいです。|Yasai wa karada ni ii desu.|Vegetables are good for the body.
n|頭|あたま|atama|head / brain|सिर / दिमाग|彼はとても頭がいいです。|Kare wa totemo atama ga ii desu.|He is very smart.
n|髪|かみ|kami|hair (on head)|बाल|髪を短く切りました。|Kami o mijikaku kirimashita.|I cut my hair short.
n|顔|かお|kao|face|चेहरा|朝起きて顔を洗います。|Asa okite kao o araimasu.|I wake up in the morning and wash my face.
n|目|め|me|eye|आँख|彼女は目が大きいです。|Kanojo wa me ga ookii desu.|She has big eyes.|1
n|耳|みみ|mimi|ear|कान|耳が痛いから病院へ行きます。|Mimi ga itai kara byouin e ikimasu.|Because my ear hurts, I go to the hospital.
n|鼻|はな|hana|nose|नाक|象は鼻が長いです。|Zou wa hana ga nagai desu.|Elephants have long trunks (noses).
n|口|くち|kuchi|mouth|मुँह|大きく口を開けてください。|Ookiku kuchi o akete kudasai.|Please open your mouth wide.
n|歯|は|ha|tooth / teeth|दाँत|食後に歯を磨きます。|Shokugo ni ha o migakimasu.|I brush my teeth after meals.
n|手|て|te|hand / arm|हाथ|冷たい水で手を洗いました。|Tsumetai mizu de te o araimashita.|I washed my hands with cold water.|2
n|足|あし|ashi|foot / leg|पैर / टाँग|たくさん歩いて足が疲れました。|Takusan aruite ashi ga tsukaremashita.|My legs got tired from walking a lot.
n|背|せ|se|height / stature / back|कद / पीठ|兄は背が高いです。|Ani wa se ga takai desu.|My older brother is tall.
adji|赤い|あかい|akai|red|लाल|赤いりんごを買いました。|Akai ringo o kaimashita.|I bought a red apple.
adji|青い|あおい|aoi|blue / green (traffic light)|नीला|今日は青い空がきれいです。|Kyou wa aoi sora ga kirei desu.|The blue sky is pretty today.
adji|白い|しろい|shiroi|white|सफ़ेद|白いシャツを着ています。|Shiroi shatsu o kite imasu.|I am wearing a white shirt.
adji|黒い|くろい|kuroi|black|काला|黒いかばんを持っています。|Kuroi kaban o motte imasu.|I am holding a black bag.
adji|黄色い|きいろい|kiiroi|yellow|पीला|黄色い傘をさしています。|Kiiroi kasa o sashite imasu.|She is holding a yellow umbrella.
n|緑|みどり|midori|green|हरा रंग|公園には緑がたくさんあります。|Kouen ni wa midori ga takusan arimasu.|There is lots of greenery in the park.
"""

    // ===============================================================
    // UNIT 16: ADVERBS, FREQUENCY, CONJUNCTIONS, QUESTIONS & N5 FINAL (58 Words)
    // ===============================================================
    private val n5Unit16Words = """
x|いつも|いつも|itsumo|always|हमेशा|いつも七時に朝ご飯を食べます。|Itsumo shichiji ni asagohan o tabemasu.|I always eat breakfast at 7 o'clock.
x|よく|よく|yoku|often / well|अक्सर / अच्छी तरह|よく図書館へ行きます。|Yoku toshokan e ikimasu.|I often go to the library.
x|時々|ときどき|tokidoki|sometimes|कभी-कभी|時々日本の映画を見ます。|Tokidoki Nihon no eiga o mimasu.|I sometimes watch Japanese movies.
x|あまり|あまり|amari|not very / not much (with negative!)|ज़्यादा नहीं (नकारात्मक वाक्य के साथ)|辛い料理はあまり食べません。|Karai ryouri wa amari tabemasen.|I don't eat spicy food very much.
x|全然|ぜんぜん|zenzen|not at all (with negative!)|बिल्कुल भी नहीं|中国語は全然分かりません。|Chuugokugo wa zenzen wakarimasen.|I don't understand Chinese at all.
x|とても|とても|totemo|very / exceedingly|बहुत|このケーキはとても甘いです。|Kono keeki wa totemo amai desu.|This cake is very sweet.
x|たいへん|たいへん|taihen|very / greatly|अत्यधिक / बहुत|今日はたいへん暑いです。|Kyou wa taihen atsui desu.|It is extremely hot today.
x|少し|すこし|sukoshi|a little / a few|थोड़ा|日本語が少し話せます。|Nihongo ga sukoshi hanasemasu.|I can speak a little Japanese.
x|ちょっと|ちょっと|chotto|a little bit / excuse me|थोड़ा-सा / ज़रा|ちょっとここで待ってください。|Chotto koko de matte kudasai.|Please wait here for a little bit.
x|たくさん|たくさん|takusan|many / a lot|बहुत सारा|写真をたくさん撮りました。|Shashin o takusan torimashita.|I took a lot of photos.
x|大勢|おおぜい|oozei|many people / crowd|बहुत सारे लोग|駅前に人が大勢います。|Ekimae ni hito ga oozei imasu.|There are many people in front of the station.
x|もっと|もっと|motto|more|और ज़्यादा|もっとゆっくり話してください。|Motto yukkuri hanashite kudasai.|Please speak more slowly.
x|もう|もう|mou|already / soon / more|पहले ही (Already) / अब|もう宿題は終わりましたか。|Mou shukudai wa owarimashita ka.|Have you finished your homework already?
x|まだ|まだ|mada|still / not yet|अभी भी / अभी तक नहीं|いいえ、まだ終わっていません。|Iie, mada owatte imasen.|No, I haven't finished yet.
x|すぐ|すぐ|sugu|immediately / right away|तुरंत / अभी|すぐ行きますから待ってください。|Sugu ikimasu kara matte kudasai.|I'm coming right away, so please wait.
x|だんだん|だんだん|dandan|gradually / little by little|धीरे-धीरे|だんだん日本語が上手になりました。|Dandan Nihongo ga jouzu ni narimashita.|My Japanese gradually improved.
x|やっと|やっと|yatto|finally / at last|आख़िरकार|やっとバスが来ました。|Yatto basu ga kimashita.|The bus finally arrived.
x|たぶん|たぶん|tabun|probably / maybe|शायद|明日はたぶん晴れるでしょう。|Ashita wa tabun hareru deshou.|Tomorrow will probably be sunny.
x|きっと|きっと|kitto|surely / undoubtedly|पक्का / निश्चय ही|彼はきっと来ると思います。|Kare wa kitto kuru to omoimasu.|I think he will surely come.
x|はっきり|はっきり|hakkiri|clearly / distinctly|साफ़-साफ़|富士山がはっきり見えます。|Fujisan ga hakkiri miemasu.|Mt. Fuji is clearly visible.
x|一緒に|いっしょに|issho ni|together|एक साथ|一緒に昼ご飯を食べませんか。|Issho ni hirugohan o tabemasen ka.|Won't you eat lunch together with me?
x|別々に|べつべつに|betsubetsu ni|separately|अलग-अलग|お金は別々に払いましょう。|Okane wa betsubetsu ni haraimashou.|Let's pay separately.
x|一人で|ひとりで|hitori de|alone / by oneself|अकेले|一人で電車に乗れます。|Hitori de densha ni noremasu.|I can ride the train by myself.
x|自分で|じぶんで|jibun de|by oneself / on one's own|खुद से / स्वयं|自分のことは自分でします。|Jibun no koto wa jibun de shimasu.|I do my own things by myself.
x|ぐらい|ぐらい|gurai|about / approximately (amount/time)|लगभग (अवधि या मात्रा)|毎日二時間ぐらい勉強します。|Mainichi nijikan gurai benkyou shimasu.|I study for about two hours every day.
x|ごろ|ごろ|goro|around (a specific clock time)|के आस-पास (घड़ी के समय के साथ)|夜十時ごろ寝ます。|Yoru juuji goro nemasu.|I go to sleep around 10 PM.
x|だけ|だけ|dake|only / just|सिर्फ़ / केवल|日曜日だけ休みです。|Nichiyoubi dake yasumi desu.|Only Sunday is a holiday.
x|から|から|kara|from (time/place) / because|से (समय/स्थान) / क्योंकि|九時から五時まで働きます。|Kuji kara goji made hatarakimasu.|I work from 9 to 5.
x|まで|まで|made|until / as far as / to|तक|東京から京都まで新幹線で行きます。|Toukyou kara Kyouto made shinkansen de ikimasu.|I go from Tokyo to Kyoto by bullet train.
x|だから|だから|dakara|so / therefore|इसलिए|雨が降っています。だから行きません。|Ame ga futte imasu. Dakara ikimasen.|It is raining. Therefore, I won't go.
x|ですから|ですから|desukara|therefore / so (polite)|इसलिए (विनम्र)|明日は試験です。ですから今日勉強します。|Ashita wa shiken desu. Desukara kyou benkyou shimasu.|Tomorrow is an exam. Therefore, I study today.
x|でも|でも|demo|but / however|लेकिन / पर|このかばんは高いです。でも、いいかばんです。|Kono kaban wa takai desu. Demo, ii kaban desu.|This bag is expensive. But it's a good bag.
x|しかし|しかし|shikashi|however / but (formal)|हालाँकि / परंतु|難しいです。しかし、楽しいです。|Muzukashii desu. Shikashi, tanoshii desu.|It is difficult. However, it is fun.
x|そして|そして|soshite|and / and then|और / तथा|本を読みました。そして、寝ました。|Hon o yomimashita. Soshite, nemashita.|I read a book. And then I went to sleep.
x|それから|それから|sorekara|after that / and then|उसके बाद / और फिर|シャワーを浴びます。それから、朝ご飯を食べます。|Shawaa o abimasu. Sorekara, asagohan o tabemasu.|I take a shower. After that, I eat breakfast.
x|ところで|ところで|tokorode|by the way|वैसे (बात बदलते समय)|ところで、明日の予定は何ですか。|Tokorode, ashita no yotei wa nan desu ka.|By the way, what are your plans for tomorrow?
x|どうして|どうして|doushite|why / for what reason|क्यों / किस कारण से|どうして昨日来ませんでしたか。|Doushite kinou kimasen deshita ka.|Why didn't you come yesterday?
x|なぜ|なぜ|naze|why (slightly formal)|क्यों|なぜ日本語を勉強していますか。|Naze Nihongo o benkyou shite imasu ka.|Why are you studying Japanese?
x|どうやって|どうやって|dou yatte|how / by what means|कैसे / किस तरीके से|駅までどうやって行きますか。|Eki made dou yatte ikimasu ka.|How do you get to the station?
x|いつ|いつ|itsu|when|कब|誕生日はいつですか。|Tanjoubi wa itsu desu ka.|When is your birthday?
x|どのぐらい|どのぐらい|dono gurai|how long / how far / how much|कितना समय / कितनी दूर|東京までどのぐらいかかりますか。|Toukyou made dono gurai kakarimasu ka.|How long does it take to get to Tokyo?
n|仕事|しごと|shigoto|work / job / occupation|काम / नौकरी|仕事は五時に終わります。|Shigoto wa goji ni owarimasu.|Work finishes at 5 o'clock.
n|生活|せいかつ|seikatsu|daily life / living|जीवन / रहन-सहन|日本の生活に慣れました。|Nihon no seikatsu ni naremashita.|I got used to life in Japan.
n|趣味|しゅみ|shumi|hobby / interest|शौक (हॉबी)|私の趣味は音楽を聞くことです。|Watashi no shumi wa ongaku o kiku koto desu.|My hobby is listening to music.
n|音楽|おんがく|ongaku|music|संगीत|クラシック音楽が好きです。|Kurashikku ongaku ga suki desu.|I like classical music.
n|歌|うた|uta|song|गाना / गीत|日本の歌を歌いましょう。|Nihon no uta o utaimashou.|Let's sing a Japanese song.
n|映画|えいが|eiga|movie / film|फ़िल्म / सिनेमा|週末に友達と映画を見ました。|Shuumatsu ni tomodachi to eiga o mimashita.|I watched a movie with a friend on the weekend.
n|授業|じゅぎょう|jugyou|class / lesson|क्लास / लेक्चर|日本語の授業は九時からです。|Nihongo no jugyou wa kuji kara desu.|Japanese class is from 9 o'clock.
n|試験|しけん|shiken|exam / examination|परीक्षा (एग्ज़ाम)|十二月に日本語の試験があります。|Juunigatsu ni Nihongo no shiken ga arimasu.|There is a Japanese exam in December.
n|休み|やすみ|yasumi|holiday / day off / break|छुट्टी / विश्राम|明日は学校の休みです。|Ashita wa gakkou no yasumi desu.|Tomorrow is a school holiday.
n|夏休み|なつやすみ|natsuyasumi|summer vacation|गर्मी की छुट्टियाँ|夏休みに国へ帰ります。|Natsuyasumi ni kuni e kaerimasu.|I return to my country during summer vacation.
n|誕生日|たんじょうび|tanjoubi|birthday|जन्मदिन|お誕生日おめでとうございます。|Otanjoubi omedetou gozaimasu.|Happy Birthday!
n|約束|やくそく|yakusoku|promise / appointment|वादा / मिलने का समय|今日は友達と約束があります。|Kyou wa tomodachi to yakusoku ga arimasu.|I have an appointment with a friend today.
n|用事|ようじ|youji|errand / things to do|ज़रूरी काम|ちょっと用事があって先に帰ります。|Chotto youji ga atte saki ni kaerimasu.|I have an errand to run, so I'm leaving first.
n|病気|びょうき|byouki|illness / sickness|बीमारी|病気がすっかり治りました。|Byouki ga sukkari naorimashita.|My illness is completely cured.
n|風邪|かぜ|kaze|cold (illness)|सर्दी-ज़ुकाम|風邪をひいて喉が痛いです。|Kaze o hiite nodo ga itai desu.|I caught a cold and my throat hurts.
n|熱|ねつ|netsu|fever / temperature|बुखार|少し熱があるから休みます。|Sukoshi netsu ga aru kara yasumimasu.|Because I have a slight fever, I will rest.
n|薬|くすり|kusuri|medicine|दवा / दवाई|ご飯の後でこの薬を飲んでください。|Gohan no ato de kono kusuri o nonde kudasai.|Please take this medicine after meals.
"""

    val vocab: List<VocabItem> by lazy {
        parseN5Blocks(
            listOf(
                n5Unit1Words, n5Unit2Words, n5Unit3Words, n5Unit4Words,
                n5Unit5Words, n5Unit6Words, n5Unit7Words, n5Unit8Words,
                n5Unit9Words, n5Unit10Words, n5Unit11Words, n5Unit12Words,
                n5Unit13Words, n5Unit14Words, n5Unit15Words, n5Unit16Words
            )
        )
    }

    fun words(level: JlptLevel = JlptLevel.N5): List<VocabItem> = vocab
    // ===============================================================
    // COMPLETE JLPT N5 SENTENCES (50 Essential Sentences Across All 16 Units)
    // ===============================================================
    private val sentenceData = """
N5|はじめまして、よろしくお願いします。|Hajimemashite, yoroshiku onegaishimasu.|Nice to meet you, pleased to make your acquaintance.|आपसे मिलकर खुशी हुई, कृपा बनाए रखें।|はじめまして、よろしくね！|Unit 1: First meeting greeting
N5|私の名前はリシです。|Watashi no namae wa Rishi desu.|My name is Rishi.|मेरा नाम ऋषि है।|私はリシだよ。|Unit 1: Self-introduction
N5|私はインドから来ました。|Watashi wa Indo kara kimashita.|I came from India.|मैं भारत से आया हूँ।|インドから来たよ。|Unit 1: Country of origin
N5|私は大学の学生です。|Watashi wa daigaku no gakusei desu.|I am a university student.|मैं विश्वविद्यालय का छात्र हूँ।|大学の学生だよ。|Unit 2: Identity & の particle
N5|これは日本語の辞書です。|Kore wa Nihongo no jisho desu.|This is a Japanese dictionary.|यह जापानी भाषा का शब्दकोश है।|これ、日本語の辞書だよ。|Unit 2: Demonstrative これ
N5|その傘は誰のですか。|Sono kasa wa dare no desu ka.|Whose umbrella is that?|वह छाता किसका है?|その傘、誰の？|Unit 2: Ownership question
N5|トイレはどこですか。|Toire wa doko desu ka.|Where is the restroom?|टॉयलेट कहाँ है?|トイレどこ？|Unit 2: Asking location
N5|私の家族は四人です。|Watashi no kazoku wa yonin desu.|My family has four people.|मेरे परिवार में चार लोग हैं।|家族は四人だよ。|Unit 3: People counter
N5|父は銀行で働いています。|Chichi wa ginkou de hataraite imasu.|My father works at a bank.|मेरे पिताजी बैंक में काम करते हैं।|父は銀行で働いてるよ。|Unit 3: Occupation & ongoing state
N5|兄は東京に住んでいます。|Ani wa Toukyou ni sunde imasu.|My older brother lives in Tokyo.|मेरा बड़ा भाई टोक्यो में रहता है।|兄は東京に住んでるよ。|Unit 3: Residence with に住んでいます
N5|毎朝パンと卵を食べます。|Maiasa pan to tamago o tabemasu.|I eat bread and eggs every morning.|मैं हर सुबह ब्रेड और अंडा खाता हूँ।|毎朝パンと卵食べるよ。|Unit 4: Katakana food & と
N5|熱いコーヒーを一杯ください。|Atsui koohii o ippai kudasai.|Please give me one cup of hot coffee.|कृपया एक कप गर्म कॉफ़ी दीजिए।|熱いコーヒー一杯ちょうだい。|Unit 4: Ordering at a cafe
N5|毎日スマホでニュースを見ます。|Mainichi sumaho de nyuusu o mimasu.|I watch the news on my smartphone every day.|मैं रोज़ स्मार्टफ़ोन पर समाचार देखता हूँ।|毎日スマホでニュース見るよ。|Unit 4: Tool particle で
N5|デパートで白いシャツを買いました。|Depaato de shiroi shatsu o kaimashita.|I bought a white shirt at the department store.|मैंने डिपार्टमेंटल स्टोर से सफ़ेद शर्ट खरीदी।|デパートで白いシャツ買ったよ。|Unit 5: Past tense shopping
N5|エレベーターで五階へ行きます。|Erebeetaa de gokai e ikimasu.|I go to the fifth floor by elevator.|मैं लिफ़्ट से पाँचवीं मंज़िल पर जाता हूँ।|エレベーターで五階に行くよ。|Unit 5: Katakana & floors
N5|すみません、これはいくらですか。|Sumimasen, kore wa ikura desu ka.|Excuse me, how much is this?|माफ़ कीजिए, यह कितने का है?|これ、いくら？|Unit 6: Asking price
N5|りんごを三つとみかんを五つ買いました。|Ringo o mittsu to mikan o itsutsu kaimashita.|I bought three apples and five mandarins.|मैंने तीन सेब और पाँच संतरे खरीदे।|りんご三つとみかん五つ買ったよ。|Unit 6: Native counters
N5|全部で千八百円になります。|Zenbu de sen-happyaku-en ni narimasu.|It comes to 1,800 yen in total.|कुल मिलाकर 1,800 येन होते हैं।|全部で千八百円だよ。|Unit 6: Total money
N5|今、午前九時半です。|Ima, gozen kuji han desu.|It is 9:30 AM right now.|अभी सुबह के साढ़े नौ बजे हैं।|今、朝の九時半だよ。|Unit 7: Telling clock time
N5|毎朝六時に起きて、十一時に寝ます。|Maiasa rokuji ni okite, juuichiji ni nemasu.|I wake up at 6 every morning and sleep at 11.|मैं हर सुबह 6 बजे उठता हूँ और 11 बजे सोता हूँ।|毎朝六時に起きて十一時に寝るよ。|Unit 7: Daily routine times
N5|銀行は九時から三時までです。|Ginkou wa kuji kara sanji made desu.|The bank is open from 9 to 3.|बैंक 9 बजे से 3 बजे तक खुलता है।|銀行は九時から三時までだよ。|Unit 7: から and まで
N5|来週の日曜日にテストがあります。|Raishuu no nichiyoubi ni tesuto ga arimasu.|There is a test next Sunday.|अगले रविवार को टेस्ट है।|来週の日曜日にテストあるよ。|Unit 7: Days of the week
N5|みそラーメンと冷たい水をください。|Miso raamen to tsumetai mizu o kudasai.|Miso ramen and cold water, please.|मिसो रामेन और ठंडा पानी दीजिए।|みそラーメンとお水ちょうだい。|Unit 8: Restaurant ordering
N5|箸で日本の料理を食べます。|Hashi de Nihon no ryouri o tabemasu.|I eat Japanese food with chopsticks.|मैं चॉपस्टिक से जापानी खाना खाता हूँ।|箸で日本料理食べるよ。|Unit 8: Tool particle で
N5|果物の中でいちごが一番好きです。|Kudamono no naka de ichigo ga ichiban suki desu.|Among fruits, I like strawberries the best.|फलों में मुझे स्ट्रॉबेरी सबसे ज़्यादा पसंद है।|果物の中でいちごが一番好き！|Unit 8: Superlative 一番
N5|机の上に本とノートがあります。|Tsukue no ue ni hon to nooto ga arimasu.|There are a book and a notebook on the desk.|मेज़ के ऊपर किताब और नोटबुक हैं।|机の上に本とノートがあるよ。|Unit 9: Inanimate existence あります
N5|椅子の下に小さい猫がいます。|Isu no shita ni chiisai neko ga imasu.|There is a small cat under the chair.|कुर्सी के नीचे एक छोटी बिल्ली है।|椅子の下に小さい猫がいるよ。|Unit 9: Living existence います
N5|寝る前に部屋の電気を消します。|Neru mae ni heya no denki o keshimasu.|I turn off the room light before sleeping.|सोने से पहले मैं कमरे की लाइट बंद करता हूँ।|寝る前に電気消すよ。|Unit 9: 前に (before doing)
N5|駅の前に大きい銀行があります。|Eki no mae ni ookii ginkou ga arimasu.|There is a big bank in front of the station.|स्टेशन के सामने एक बड़ा बैंक है।|駅の前に大きい銀行があるよ。|Unit 10: Town positions
N5|次の角を右へ曲がってください。|Tsugi no kado o migi e magatte kudasai.|Please turn right at the next corner.|कृपया अगले मोड़ से दाएँ मुड़िए।|次の角を右に曲がってね。|Unit 10: Giving directions
N5|図書館で静かに本を読みます。|Toshokan de shizuka ni hon o yomimasu.|I read books quietly at the library.|मैं पुस्तकालय में शांति से किताब पढ़ता हूँ।|図書館で静かに本読むよ。|Unit 10: Action location で
N5|毎朝電車で大学へ行きます。|Maiasa densha de daigaku e ikimasu.|I go to university by train every morning.|मैं हर सुबह ट्रेन से यूनिवर्सिटी जाता हूँ।|毎朝電車で大学行くよ。|Unit 11: Transport で + Goal へ
N5|東京駅で新幹線に乗り換えます。|Toukyou-eki de shinkansen ni norikaemasu.|I transfer to the bullet train at Tokyo Station.|मैं टोक्यो स्टेशन पर बुलेट ट्रेन बदलता हूँ।|東京駅で新幹線に乗り換えるよ。|Unit 11: Transferring trains
N5|家から駅まで歩いて十分かかります。|Ie kara eki made aruite juppun kakarimasu.|It takes 10 minutes on foot from home to the station.|घर से स्टेशन तक पैदल 10 मिनट लगते हैं।|家から駅まで歩いて十分かかるよ。|Unit 11: Duration かかります
N5|毎日新しい漢字を十個覚えます。|Mainichi atarashii kanji o jukko oboemasu.|I memorize ten new kanji every day.|मैं रोज़ दस नए कांजी अक्षर याद करता हूँ।|毎日新しい漢字を十個覚えるよ。|Unit 12: Study routine
N5|分からない言葉を辞書で調べます。|Wakaranai kotoba o jisho de shirabemasu.|I look up words I don't know in a dictionary.|मैं न समझ आने वाले शब्दों को डिक्शनरी में खोजता हूँ।|分からない言葉を辞書で調べるよ。|Unit 12: Relative clause
N5|暑いから窓を開けてもいいですか。|Atsui kara mado o akete mo ii desu ka.|Because it is hot, may I open the window?|गर्मी है इसलिए क्या मैं खिड़की खोल सकता हूँ?|暑いから窓開けてもいい？|Unit 13: Permission 〜てもいいですか
N5|ここで写真を撮ってはいけません。|Koko de shashin o totte wa ikemasen.|You must not take photos here.|यहाँ फ़ोटो खींचना मना है।|ここで写真撮っちゃダメだよ。|Unit 13: Prohibition 〜てはいけません
N5|今、友達と日本語で話しています。|Ima, tomodachi to Nihongo de hanashite imasu.|Right now, I am speaking in Japanese with my friend.|अभी मैं दोस्त के साथ जापानी में बात कर रहा हूँ।|今、友達と日本語で話してるよ。|Unit 13: Ongoing 〜ています
N5|図書館で借りた本を来週返します。|Toshokan de karita hon o raishuu kaeshimasu.|I will return the book I borrowed from the library next week.|लाइब्रेरी से उधार ली हुई किताब अगले हफ़्ते लौटाऊँगा।|借りた本を来週返すよ。|Unit 13: Borrowing & returning
N5|今日は昨日よりずっと寒いです。|Kyou wa kinou yori zutto samui desu.|Today is much colder than yesterday.|आज कल की तुलना में काफ़ी ठंड है।|今日は昨日よりずっと寒いね。|Unit 14: Comparison より
N5|昨日の映画はとても面白かったです。|Kinou no eiga wa totemo omoshirokatta desu.|Yesterday's movie was very interesting.|कल की फ़िल्म बहुत मज़ेदार थी।|昨日の映画、すごく面白かった！|Unit 14: Past tense い-Adjective
N5|新しいパソコンが欲しいです。|Atarashii pasokon ga hoshii desu.|I want a new laptop.|मुझे एक नया लैपटॉप चाहिए।|新しいパソコン欲しいな。|Unit 14: Desire for object 欲しい
N5|京都は静かできれいな町です。|Kyouto wa shizuka de kirei na machi desu.|Kyoto is a quiet and beautiful city.|क्योटो एक शांत और सुंदर शहर है।|京都は静かできれいな町だね。|Unit 15: Connecting な-Adjectives
N5|彼女はピアノを弾くのが上手です。|Kanojo wa piano o hiku no ga jouzu desu.|She is good at playing the piano.|वह पियानो बजाने में माहिर है।|彼女、ピアノ弾くの上手だね。|Unit 15: Nominalizer のが上手
N5|来月、日本へ旅行に行きたいです。|Raigetsu, Nihon e ryokou ni ikitai desu.|Next month, I want to go on a trip to Japan.|अगले महीने मैं जापान घूमने जाना चाहता हूँ।|来月、日本に旅行に行きたいな。|Unit 15: Desire 〜たいです
N5|もう一度ゆっくり言ってください。|Mou ichido yukkuri itte kudasai.|Please say it slowly once more.|कृपया एक बार और धीरे से बोलिए।|もう一回ゆっくり言って。|Unit 16: Polite request
N5|日本語がだんだん上手になりました。|Nihongo ga dandan jouzu ni narimashita.|My Japanese gradually became better.|मेरी जापानी धीरे-धीरे बेहतर हो गई।|日本語がだんだんうまくなったよ。|Unit 16: Change of state 〜になりました
N5|休みの日は本を読んだり音楽を聞いたりします。|Yasumi no hi wa hon o yondari ongaku o kiitari shimasu.|On holidays, I do things like reading books and listening to music.|छुट्टी के दिन मैं किताबें पढ़ने और संगीत सुनने जैसे काम करता हूँ।|休みの日は本読んだり音楽聞いたりするよ。|Unit 16: 〜たり〜たりします
N5|風邪をひいたから学校を休みました。|Kaze o hiita kara gakkou o yasumimashita.|Because I caught a cold, I was absent from school.|सर्दी-ज़ुकाम होने के कारण मैंने स्कूल से छुट्टी ली।|風邪ひいたから学校休んだよ。|Unit 16: Reason with から
"""

    val allSentences: List<SentenceItem> by lazy {
        sentenceData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            SentenceItem(
                level = JlptLevel.N5,
                jp = p[1],
                romaji = p[2],
                en = p[3],
                hi = p[4],
                casualJp = p.getOrElse(5) { p[1] },
                politeNote = p.getOrElse(6) { "JLPT N5 Essential Pattern" }
            )
        }
    }

    fun sentences(level: JlptLevel = JlptLevel.N5): List<SentenceItem> = allSentences

    // ===============================================================
    // COMPLETE JLPT N5 VISUAL LEGO GRAMMAR (20 Essential N5 Rules)
    // ===============================================================
    private val grammarData = """
N5|1. Word Order: Hindi = Japanese (SOV)|Subject は + Object を + Verb ます|In Japanese, the main action Verb ALWAYS goes at the very end of the sentence—100% identical to Hindi word order!|हिन्दी और जापानी का वाक्य क्रम बिल्कुल एक जैसा है: मैं (私は) + ब्रेड (パンを) + खाता हूँ (食べます)।|私は毎朝パンを食べます。|Watashi wa maiasa pan o tabemasu.|I eat bread every morning.|Mistake: Never put the verb in the middle like English (私は食べますパン ❌ -> 私はパンを食べます ✅).
N5|2. Polite Copula: です / ではありません / でした|Noun A + は + Noun B + です / ではありません|です means 'is / am / are'. Negative is ではありません (or じゃありません), and Past tense is でした (was/were).|です = है/हूँ, ではありません = नहीं है, でした = था/थी।|私は学生です。昨日は休みでした。|Watashi wa gakusei desu. Kinou wa yasumi deshita.|I am a student. Yesterday was a holiday.|Mistake: Never put です directly after an action verb (食べです ❌ -> 食べます ✅).
N5|3. Topic は (wa) vs Subject Focus が (ga)|Known Topic + は / New Focus・Question Word + が|は marks the overall topic ('As for X...'). が spotlights new information, question subjects (誰/何), or existence (あります/います).|は = के बारे में (मैं तो...), が = ही / ने (सवाल पूछने या किसी खास चीज़ पर ज़ोर देने के लिए)।|私はインド人です。誰が来ますか？|Watashi wa Indojin desu. Dare ga kimasu ka?|I am Indian. Who is coming?|Mistake: Question words at the subject position ALWAYS take が, never は (誰は来ますか ❌ -> 誰が来ますか ✅).
N5|4. Demonstratives: これ/それ/あれ vs この/その/あの|これ・それ・あれ + は (Standalone) / この・その・あの + Noun|これ (near me), それ (near you), あれ (far from both) stand alone. この, その, あの MUST be followed immediately by a Noun!|अकेले बोलना हो तो これ/それ/あれ, और अगर साथ में संज्ञा (किताब/पेन) जुड़ी हो तो この/その/あの लगाएँ!|これは私の本です。この本は面白いです。|Kore wa watashi no hon desu. Kono hon wa omoshiroi desu.|This is my book. This book is interesting.|Mistake: これ本 ❌ -> Always この本 ✅ when a noun follows directly!
N5|5. Possessive & Noun Connector: の (no)|Owner Noun + の + Main Noun|Connects two nouns where the first modifies or owns the second—works exactly like Hindi 'का / की / के'.|दो संज्ञाओं को जोड़ने के लिए बीच में 'の' लगाएँ: Watashi no kaban = मेरा (मेरा + का) बैग।|これは日本語の先生のかばんです。|Kore wa Nihongo no sensei no kaban desu.|This is the Japanese teacher's bag.|Mistake: Don't reverse the order—the main item always comes AFTER の.
N5|6. Direct Object Marker: を (o)|Object Noun + を + Transitive Verb ます|Marks the thing that directly receives the physical action (eating, drinking, reading, buying, seeing).|जिस चीज़ पर काम हो रहा है (पानी को पीना, किताब को पढ़ना), उसके तुरंत बाद 'を' लगाएँ।|毎朝、冷たい水を飲みます。|Maiasa, tsumetai mizu o nomimasu.|Every morning, I drink cold water.|Mistake: Verbs of liking/understanding (好き / 分かる) and motion (行く / 来る) do NOT take を!
N5|7. Time & Destination Particle: に (ni)|Specific Clock Time・Date + に / Target Place + に + 行きます|Marks exact clock times/dates (六時に, 日曜日に) and the destination or target point of arrival/boarding.|घड़ी के निश्चित समय (6 बजे = 六時に) और मंज़िल के साथ हमेशा 'に' लगता है।|毎朝六時に起きて、七時に学校に行きます。|Maiasa rokuji ni okite, shichiji ni gakkou ni ikimasu.|I wake up at 6 and go to school at 7.|Mistake: Relative time words like 今日 (today), 明日 (tomorrow), 毎日 (every day) NEVER take に (今日に ❌ -> 今日 ✅)!
N5|8. Action Location & Tool Particle: で (de)|Action Place + で + Verb / Tool・Transport + で + Verb|Marks WHERE an active action takes place (studying at the library) AND the tool/vehicle/language used (by train, with chopsticks).|जहाँ कोई काम/एक्शन हो रहा हो (लाइब्रेरी में = 図書館で) या जिस साधन से काम हो (ट्रेन से = 電車で, चॉपस्टिक से = 箸で), वहाँ 'で' लगाएँ!|電車で図書館へ行って、本を読みます。|Densha de toshokan e itte, hon o yomimasu.|I go to the library by train and read books.|Mistake: 図書館に本を読みます ❌ -> Reading is an active action, so the place MUST take で (図書館で ✅)!
N5|9. Existence Verbs: あります (Things) vs います (Living)|Place + に + Thing が + あります / Place + に + Person・Animal が + います|Use あります for non-living things (books, trees, buildings, money) and います for living, moving beings (people, dogs, cats, fish).|निर्जीव चीज़ों (किताब, गाड़ी) के लिए 'あります' और जीवित प्राणियों (इंसान, कुत्ता, बिल्ली) के लिए 'います' लगाएँ!|机の上に本があります。庭に犬がいます。|Tsukue no ue ni hon ga arimasu. Niwa ni inu ga imasu.|There is a book on the desk. There is a dog in the yard.|Mistake: 犬があります ❌ -> Dogs are alive, so always 犬がいます ✅!
N5|10. Verb 4 Basic Polite Forms (ます / ません / ました / ませんでした)|Verb Stem + ます (Present/Future) / ません (Neg) / ました (Past) / ませんでした (Past Neg)|Japanese polite verbs use these four endings to cover all daily present, future, negative, and past statements.|ます = करता हूँ/करूँगा, ません = नहीं करता, ました = किया, ませんでした = नहीं किया।|昨日勉強しました。今日は遊びません。|Kinou benkyou shimashita. Kyou wa asobimasen.|I studied yesterday. Today I won't play.|Mistake: For past negative, add でした after ません (食べませんでした ✅).
N5|11. い-Adjectives vs な-Adjectives (Noun Modification & Past)|い-Adj + Noun (Past: 〜かった) / な-Adj + な + Noun (Past: 〜でした)|い-Adjectives attach directly to nouns and drop い for かった in past tense. な-Adjectives need な before nouns!|い-विशेषण में अंत का 'い' हटाकर 'かった' (था) और 'くない' (नहीं) बनता है। な-विशेषण के बाद संज्ञा हो तो बीच में 'な' ज़रूर लगाएँ!|昨日は暑かったです。ここは静かな町です。|Kinou wa atsukatta desu. Koko wa shizuka na machi desu.|Yesterday was hot. This is a quiet town.|Mistake: きれい (pretty/clean) and 有名 (famous) end in い in romaji, but they are な-Adjectives (きれいな部屋 ✅)!
N5|12. Likes, Skills & Wants: 好き・上手・分かる・欲しい + が|Item + が + 好きです / 上手です / 分かります / 欲しいです|In Japanese, objects of affection (好き), skill (上手/下手), comprehension (分かる), and desire (欲しい) are marked with が, never を!|पसंद (好き), माहिर (上手), समझना (分かる) और चाहिए (欲しい) के साथ हमेशा 'が (ga)' लगता है, 'を' कभी नहीं!|私は日本の音楽が好きです。新しい車が欲しいです。|Watashi wa Nihon no ongaku ga suki desu. Atarashii kuruma ga hoshii desu.|I like Japanese music. I want a new car.|Mistake: 日本語を分かります ❌ -> Always 日本語が分かります ✅!
N5|13. Expressing Action Desire: 〜たいです (Want to do)|Verb ます-stem + たいです / たくないです|Drop ます from any verb and attach たいです to express what YOU want to do. Conjugates like an い-Adjective!|क्रिया के ます-stem में 'たいです' जोड़ने से 'करना चाहता हूँ' बन जाता है (जैसे: 行き + たい = जाना चाहता हूँ)।|来年、日本へ行きたいです。寿司を食べたいです。|Rainen, Nihon e ikitai desu. Sushi o tabetai desu.|Next year, I want to go to Japan. I want to eat sushi.|Mistake: Don't use 〜たいです for a 3rd person's wish in simple statements.
N5|14. Ongoing Action & Current State: 〜ています|Verb て-form + います|Describes an action happening right now (-ing) OR an ongoing state resulting from a past action (living in Tokyo, being married, knowing).|अभी चल रहा काम (खा रहा हूँ = 食べています) या स्थायी स्थिति (रहता हूँ = 住んでいます)।|今、雨が降っています。姉は東京に住んでいます。|Ima, ame ga futte imasu. Ane wa Toukyou ni sunde imasu.|It is raining right now. My sister lives in Tokyo.|Mistake: To say 'I know', always use 知っています, but for 'I don't know', use plain negative 知りません!
N5|15. Polite Requests & Negative Requests: 〜てください vs 〜ないでください|Verb て-form + ください (Please do) / Verb ない-form + でください (Please don't)|Use て-form + ください to politely ask someone to do something, and ない-form + でください to ask them NOT to do something.|कृपया कीजिए = 〜てください, और कृपया मत कीजिए = 〜ないでください।|ここに名前を書いてください。写真をとらないでください。|Koko ni namae o kaite kudasai. Shashin o toranaide kudasai.|Please write your name here. Please do not take photos.|Mistake: Never use dictionary form before ください (書くください ❌ -> 書いてください ✅).
N5|16. Asking Permission & Prohibition: 〜てもいいですか vs 〜てはいけません|Verb て-form + もいいですか (May I?) / Verb て-form + はいけません (Must not!)|Ask polite permission with 〜てもいいですか, and express strict rules or prohibition with 〜てはいけません.|क्या मैं यह कर सकता हूँ? = 〜てもいいですか। यह करना मना है = 〜てはいけません।|ここに座ってもいいですか。ここでタバコを吸ってはいけません。|Koko ni suwatte mo ii desu ka. Koko de tabako o sutte wa ikemasen.|May I sit here? You must not smoke here.|Mistake: Don't forget to convert the verb to て-form first!
N5|17. Purpose of Going: Verb Stem + に + 行きます / 来ます|Place へ + Verb ます-stem / Action Noun + に + 行きます・来ます・帰ります|Expresses going or coming somewhere IN ORDER TO do an action (going to watch a movie, going to buy clothes).|'कुछ करने के लिए जाना' बताने के लिए क्रिया से ます हटाकर 'に 行きます' लगाएँ (जैसे: 映画を見に行きます = फ़िल्म देखने जाता हूँ)।|デパートへ靴を買いに行きます。|Depaato e kutsu o kai ni ikimasu.|I go to the department store to buy shoes.|Mistake: Drop ます before adding に (買いますに行きます ❌ -> 買いに行きます ✅).
N5|18. Time Sequence: 〜前に (Before) vs 〜てから / 〜後で (After)|Verb Dict Form / Noun の + 前に (Before) / Verb て-form + から (After doing)|Before 前に, the verb is ALWAYS in Dictionary Form (even if the main sentence is past tense!). 〜てから means 'after doing X, I do Y'.|से पहले = Dictionary Verb + 前に (जैसे: 寝る前に = सोने से पहले)। करने के बाद = て-form + から।|寝る前に本を読みます。手を洗ってから食べます。|Neru mae ni hon o yomimasu. Te o aratte kara tabemasu.|I read a book before sleeping. I eat after washing my hands.|Mistake: Never put past tense た-form before 前に (寝た前に ❌ -> 寝る前に ✅).
N5|19. Comparisons & Superlative: A は B より (More than) & 一番 (The Most)|A は + B より + Adj です / Group の中で + X が + 一番 + Adj です|より means 'than' for comparing two items. For 3 or more items, use の中で ... が一番 to state the #1 best/most.|B की तुलना में A = A は B より। सबसे ज़्यादा (नंबर 1) = 一番 (Ichiban)।|新幹線はバスより速いです。日本料理の中で寿司が一番好きです。|Shinkansen wa basu yori hayai desu. Nihon ryouri no naka de sushi ga ichiban suki desu.|The bullet train is faster than the bus. Among Japanese food, I like sushi the best.|Mistake: Remember が before 一番好きです / 一番高いです.
N5|20. Listing Representative Actions & Giving Reasons: 〜たり〜たりする & 〜から|Verb た-form + り + Verb た-form + り + します / Reason Clause + から + Result|Use 〜たり〜たりします to list 2 sample activities among others ('do things like X and Y'). Use から after a clause to mean 'because/so'.|X और Y जैसे काम करना = 〜たり〜たりします (अंत में हमेशा します लगाना ज़रूरी है!)। क्योंकि = 〜から।|日曜日は掃除したり洗濯したりします。雨だから行きません。|Nichiyoubi wa souji shitari sentaku shitari shimasu. Ame dakara ikimasen.|On Sunday I do things like cleaning and laundry. Because it's raining, I won't go.|Mistake: Never forget the final します (or しました for past) at the end of a 〜たり〜たり sentence!
"""

    val allGrammar: List<GrammarPoint> by lazy {
        grammarData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            fun f(i: Int) = p.getOrElse(i) { "" }
            GrammarPoint(
                level = JlptLevel.N5,
                title = f(1),
                structure = f(2),
                explanation = f(3),
                hindiTip = f(4),
                exJp = f(5),
                exRomaji = f(6),
                exEn = f(7),
                commonMistake = f(8)
            )
        }
    }

    fun grammar(level: JlptLevel = JlptLevel.N5): List<GrammarPoint> = allGrammar

    // ===============================================================
    // ALL 103 OFFICIAL JLPT N5 KANJI (Complete Deck with Hindi & Mnemonics)
    // ===============================================================
    private val kanjiData = """
N5|一|イチ、イツ|ひと-つ|One|एक (1)|1|一 (One)|One single horizontal stroke drawn from left to right (एक सीधी रेखा = 1).|一つ (ひとつ), 一人 (ひとり), 一月 (いちがつ)
N5|二|ニ|ふた-つ|Two|दो (2)|2|二 (Two)|Two parallel horizontal lines, top shorter than bottom (दो रेखाएं = 2).|二つ (ふたつ), 二人 (ふたり), 二月 (にがつ)
N5|三|サン|みっ-つ|Three|तीन (3)|3|一 (One)|Three horizontal bars stacked neatly (तीन रेखाएं = 3).|三つ (みっつ), 三人 (さんにん), 三時 (さんじ)
N5|四|シ|よ、よん、よっ-つ|Four|चार (4)|5|囗 (Enclosure)|A window box divided into four corners (चार कोनों वाला डिब्बा = 4).|四つ (よっつ), 四人 (よにん), 四月 (しがつ)
N5|五|ゴ|いつ-つ|Five|पाँच (5)|4|二 (Two)|Five intersections between heaven and earth (पाँच मोड़ वाली आकृति = 5).|五つ (いつつ), 五月 (ごがつ), 五分 (ごふん)
N5|六|ロク|むっ-つ|Six|छह (6)|4|八 (Eight)|A top hat with two legs standing wide underneath (टोपी और दो पैर = 6).|六つ (むっつ), 六時 (ろくじ), 六百 (ろっぴゃく)
N5|七|シチ|なな、なな-つ|Seven|सात (7)|2|一 (One)|A sword slicing cleanly across a horizontal line (उल्टा सात = 7).|七つ (ななつ), 七時 (しちじ), 七月 (しちがつ)
N5|八|ハチ|やっ-つ|Eight|आठ (8)|2|八 (Eight)|Two strokes spreading wide open toward prosperity (दो फैलती रेखाएं = 8).|八つ (やっつ), 八時 (はちじ), 八百 (はっぴゃく)
N5|九|キュウ、ク|ここの-つ|Nine|नौ (9)|2|乙 (Hook)|A muscular arm bending at the elbow with a hook (मुड़ी हुई भुजा = 9).|九つ (ここのつ), 九時 (くじ), 九月 (くがつ)
N5|十|ジュウ、ジッ|とお|Ten|दस (10)|2|十 (Ten)|Two lines crossing at the center: vertical and horizontal (+ का निशान = 10).|十 (とお), 十時 (じゅうじ), 十分 (じゅっぷん)
N5|百|ヒャク|—|Hundred|सौ (100)|6|白 (White)|One (一) atop white (白) — a century milestone (एक सौ = 100).|百円 (ひゃくえん), 三百 (さんびゃく), 八百 (はっぴゃく)
N5|千|セン|ち|Thousand|हज़ार (1,000)|3|十 (Ten)|A slanting stroke atop ten (十) multiplying it to 1,000 (दस के ऊपर रेखा = 1,000).|千円 (せんえん), 三千 (さんぜん), 千葉 (ちば)
N5|万|マン、バン|—|Ten Thousand|दस हज़ार (10,000)|3|一 (One)|A sturdy currency bar representing 10,000 (दस हज़ार = 10,000).|一万円 (いちまんえん), 万年筆 (まんねんひつ)
N5|円|エン|まる-い|Yen, Circle, Round|येन (मुद्रा) / गोल|4|冂 (Border)|A bank teller window enclosing coins (बैंक की खिड़की और येन).|百円 (ひゃくえん), 千円 (せんえん), 円い (まるい)
N5|日|ニチ、ジツ|ひ、-び、-か|Sun, Day|सूर्य / दिन|4|日 (Sun)|A window showing the bright sun in the sky (खिड़की में चमकता सूरज).|日本 (にほん), 毎日 (まいにち), 日曜日 (にちようび)
N5|月|ゲツ、ガツ|つき|Moon, Month|चाँद / महीना|4|月 (Moon)|A crescent moon with two cloud wisps across it (दूज का चाँद).|月曜日 (げつようび), 今月 (こんげつ), 月 (つき)
N5|火|カ|ひ、-び|Fire, Tuesday|आग / मंगलवार|4|火 (Fire)|A blazing campfire with two sparks flying off the sides (जलती आग और चिंगारी).|火曜日 (かようび), 花火 (はなび), 火 (ひ)
N5|水|スイ|みず|Water, Wednesday|पानी / बुधवार|4|水 (Water)|A flowing river current in the center with splashes on both sides (नदी की धारा).|水 (みず), 水曜日 (すいようび), 水泳 (すいえい)
N5|木|モク、ボク|き、こ-|Tree, Wood, Thursday|पेड़ / लकड़ी|4|木 (Tree)|A trunk, horizontal branches, and roots spreading underground (पेड़ और जड़ें).|木曜日 (もくようび), 木 (き), 木材 (もくざい)
N5|金|キン、コン|かね|Gold, Money, Friday|सोना / पैसा|8|金 (Metal)|Gold nuggets buried safely beneath a mountain roof (छत के नीचे दबा सोना).|お金 (おかね), 金曜日 (きんようび), 金 (きん)
N5|土|ド、ト|つち|Soil, Earth, Saturday|मिट्टी / ज़मीन|3|土 (Earth)|A plant sprout (+) rising out of the flat ground layer (ज़मीन से उगता पौधा).|土曜日 (どようび), 土 (つち), 土地 (とち)
N5|曜|ヨウ|—|Day of the Week|सप्ताह का वार|18|日 (Sun)|Sun (日) + feathers (羽) + bird (隹) tracking the 7 days of the week.|日曜日 (にちようび), 月曜日 (げつようび), 何曜日 (なんようび)
N5|年|ネン|とし|Year|साल / वर्ष|6|干 (Dry)|Grain crops harvested once every year (साल भर की फ़सल).|今年 (ことし), 来年 (らいねん), 去年 (きょねん)
N5|時|ジ|とき|Time, Hour|समय / बजे|10|日 (Sun)|The sun (日) moving over a temple (寺) marking each hour.|時間 (じかん), 何時 (なんじ), 時々 (ときどき)
N5|分|フン、ブン|わ-かる、わ-ける|Minute, Divide, Understand|मिनट / समझना / बाँटना|4|刀 (Knife)|Using a knife (刀) to split something in two (八).|五分 (ごふん), 分かる (わかる), 半分 (はんぶん)
N5|半|ハン|なか-ば|Half|आधा / साढ़े|5|十 (Ten)|Splitting a whole right down the center into two equal halves.|八時半 (はちじはん), 半分 (はんぶん), 半年 (はんとし)
N5|今|コン、キン|いま|Now, Present|अभी / वर्तमान|4|人 (Person)|A roof capturing the present moment right now.|今 (いま), 今日 (きょう), 今週 (こんしゅう)
N5|午|ゴ|—|Noon|दोपहर (AM/PM)|4|十 (Ten)|The sun reaching its highest point at 12 noon.|午前 (ごぜん), 午後 (ごご)
N5|前|ゼン|まえ|Before, In Front Of|सामने / से पहले|9|刂 (Knife)|Standing in front of the starting line before moving.|前 (まえ), 午前 (ごぜん), 名前 (なまえ)
N5|後|ゴ、コウ|うし-ろ、あと|After, Behind, Later|पीछे / के बाद|9|彳 (Step)|Walking steps trailing behind someone.|後ろ (うしろ), 午後 (ごご), 後で (あとで)
N5|間|カン、ケン|あいだ、ま|Interval, Between, Space|के बीच में / समय|12|門 (Gate)|The bright sun (日) peeking between two gate doors (門).|時間 (じかん), 間 (あいだ), 一週間 (いっしゅうかん)
N5|毎|マイ|—|Every|हर / प्रत्येक|6|母 (Mother)|Consistent repetition day after day.|毎日 (まいにち), 毎朝 (まいあさ), 毎週 (まいしゅう)
N5|週|シュウ|—|Week|सप्ताह / हफ़्ता|11|辶 (Walk)|Moving along a complete 7-day cycle.|今週 (こんしゅう), 来週 (らいしゅう), 週末 (しゅうまつ)
N5|上|ジョウ|うえ、あ-がる、のぼ-る|Up, Above, On|ऊपर|3|一 (One)|A pointer standing above the baseline (आधार रेखा के ऊपर).|上 (うえ), 上手 (じょうず), 上る (のぼる)
N5|下|カ、ゲ|した、さ-がる、お-りる|Down, Under, Below|नीचे|3|一 (One)|A pointer hanging below the baseline (आधार रेखा के नीचे).|下 (した), 地下鉄 (ちかてつ), 下手 (へた)
N5|中|チュウ|なか|Middle, Inside|अंदर / बीच में|4|丨 (Line)|An arrow piercing dead-center through the middle of a box.|中 (なか), 一日中 (いちにちじゅう), 真ん中 (まんなか)
N5|外|ガイ、ゲ|そと、ほか|Outside, Foreign|बाहर / विदेशी|5|夕 (Evening)|Stepping outside under the evening moon.|外 (そと), 外国人 (がいこくじん), 外国 (がいこく)
N5|右|ウ、ユウ|みぎ|Right|दायाँ (Right)|5|口 (Mouth)|A hand (ナ) reaching for the mouth (口) to eat with the right hand.|右 (みぎ), 右手 (みぎて)
N5|左|サ|ひだり|Left|बायाँ (Left)|5|工 (Work)|A hand (ナ) holding a carpenter's square (工) on the left.|左 (ひだり), 左手 (ひだりて)
N5|北|ホク|きた|North|उत्तर दिशा (North)|5|匕 (Spoon)|Two people sitting back-to-back against the cold northern wind.|北 (きた), 北海道 (ほっかいどう), 北口 (きたぐち)
N5|南|ナン|みなみ|South|दक्षिण दिशा (South)|9|十 (Ten)|Warm southern conservatory filled with plants.|南 (みなみ), 南口 (みなみぐち)
N5|東|トウ|ひがし|East|पूर्व दिशा (East)|8|木 (Tree)|The morning sun (日) rising right behind a tree (木) in the east.|東 (ひがし), 東京 (とうきょう), 東口 (ひがしぐち)
N5|西|セイ、サイ|にし|West|पश्चिम दिशा (West)|6|襾 (Cover)|A bird settling into its nest as the sun sets in the west.|西 (にし), 西口 (にしぐち)
N5|山|サン|やま|Mountain|पहाड़ / पर्वत|3|山 (Mountain)|Three mountain peaks rising toward the sky (तीन पहाड़ की चोटियाँ).|山 (やま), 富士山 (ふじさん)
N5|川|セン|かわ|River|नदी|3|川 (River)|Three smooth streams of water flowing side by side (बहती नदी की तीन धाराएं).|川 (かわ), 小川 (おがわ)
N5|空|クウ|そら、あ-く|Sky, Empty|आसमान / खाली|8|穴 (Cave)|Looking up through an opening (穴) constructed (工) toward the vast sky.|空 (そら), 空港 (くうこう), 空気 (くうき)
N5|天|テン|あま|Heaven, Sky|आकाश / स्वर्ग|4|大 (Big)|The great expanse above a person's (大) head.|天気 (てんき), 天ぷら (てんぷら)
N5|気|キ、ケ|—|Spirit, Air, Mood|ऊर्जा / हवा / मन|6|气 (Steam)|Rising steam and energy filling the air.|天気 (てんき), 元気 (げんき), 気をつける (きをつける)
N5|雨|ウ|あめ|Rain|बारिश / वर्षा|8|雨 (Rain)|Four raindrops falling from a cloud under the sky.|雨 (あめ), 大雨 (おおあめ)
N5|電|デン|—|Electricity|बिजली / विद्युत|13|雨 (Rain)|A lightning bolt flashing down from a rain cloud (雨).|電車 (でんしゃ), 電気 (でんき), 電話 (でんわ)
N5|花|カ|はな|Flower|फूल|7|艹 (Grass)|A plant (艹) transforming (化) into a beautiful blossom.|花 (はな), 花屋 (はなや), 花火 (はなび)
N5|人|ジン、ニン|ひと|Person, Human|इंसान / व्यक्ति|2|人 (Person)|Two legs walking forward supporting each other.|人 (ひと), 日本人 (にほんじん), 三人 (さんにん)
N5|子|シ、ス|こ|Child|बच्चा|3|子 (Child)|A joyful baby with outstretched arms.|子供 (こども), 男の子 (おとこのこ), 女の子 (おんなのこ)
N5|女|ジョ|おんな、め|Woman, Female|महिला / स्त्री|3|女 (Woman)|A graceful person seated in a traditional posture.|女の人 (おんなのひと), 彼女 (かのじょ), 女の子 (おんなのこ)
N5|男|ダン、ナン|おとこ|Man, Male|पुरुष|7|田 (Field)|Applying power (力) in the rice field (田).|男の人 (おとこのひと), 男の子 (おとこのこ)
N5|父|フ|ちち|Father|पिता|4|父 (Father)|Two crossed work tools held firmly by a father.|父 (ちち), お父さん (おとうさん)
N5|母|ボ|はは|Mother|माँ / माता|5|毋 (Mother)|A mother nurturing her children with two dots of care.|母 (はは), お母さん (おかあさん), 母国 (ぼこく)
N5|友|ユウ|とも|Friend|दोस्त / मित्र|4|又 (Hand)|Two hands reaching out to clasp each other in friendship.|友達 (ともだち), 親友 (しんゆう)
N5|先|セン|さき|Ahead, Previous, Teacher|पहले / आगे|6|儿 (Legs)|Walking legs moving ahead to guide others.|先生 (せんせい), 先週 (せんしゅう), 先月 (せんげつ)
N5|生|セイ、ショウ|い-きる、う-まれる|Life, Birth, Student|जीवन / जन्म / विद्यार्थी|5|生 (Life)|A fresh green sprout growing out of the soil.|学生 (がくせい), 先生 (せんせい), 生まれる (うまれる)
N5|名|メイ、ミョウ|な|Name, Famous|नाम / प्रसिद्ध|6|口 (Mouth)|Calling out your name with your mouth (口) in the evening (夕).|名前 (なまえ), 有名 (ゆうめい)
N5|目|モク|め|Eye|आँख|5|目 (Eye)|An eye with an iris and pupil turned vertically.|目 (め), 一枚目 (いちまいめ)
N5|耳|ジ|みみ|Ear|कान|6|耳 (Ear)|The outer curve and inner structure of a human ear.|耳 (みみ)
N5|口|コウ、ク|くち|Mouth, Entrance/Exit|मुँह / द्वार|3|口 (Mouth)|An open mouth ready to speak or eat.|口 (くち), 入口 (いりぐち), 出口 (でぐち)
N5|手|シュ|て|Hand|हाथ|4|手 (Hand)|A palm with fingers spread out.|手 (て), 手紙 (てがみ), 上手 (じょうず)
N5|足|ソク|あし、た-りる|Foot, Leg, Sufficient|पैर / पर्याप्त होना|7|足 (Foot)|A knee (口) and foot striding forward.|足 (あし), 足りる (たりる), 二足 (にそく)
N5|国|コク|くに|Country|देश|8|囗 (Border)|A precious jade jewel (玉) protected inside national borders (囗).|国 (くに), 外国 (がいこく), 中国 (ちゅうごく)
N5|学|ガク|まな-ぶ|Study, Learning|पढ़ाई / सीखना|8|子 (Child)|A child (子) under a school roof learning knowledge.|学校 (がっこう), 学生 (がくせい), 大学 (だいがく)
N5|校|コウ|—|School|स्कूल / विद्यालय|10|木 (Tree)|A wooden (木) building where students cross (交) paths to learn.|学校 (がっこう), 高校 (こうこう), 校長 (こうちょう)
N5|店|テン|みせ|Shop, Store|दुकान|8|广 (Cliff/Roof)|Goods displayed on a stand (占) under a shop awning (广).|店 (みせ), 喫茶店 (きっさてん), 店員 (てんいん)
N5|社|シャ|やしろ|Company, Shrine|कंपनी / मंदिर|7|礻 (Altar)|People gathering around an altar (礻) on the earth (土).|会社 (かいしゃ), 神社 (じんじゃ), 社員 (しゃいん)
N5|駅|エキ|—|Train Station|रेलवे स्टेशन|14|馬 (Horse)|Where horses (馬) and trains stop along measured routes (尺).|駅 (えき), 東京駅 (とうきょうえき), 駅前 (えきまえ)
N5|道|ドウ|みち|Road, Way, Street|सड़क / रास्ता|12|辶 (Walk)|Stepping forward (辶) while looking ahead with your head (首).|道 (みち), 北海道 (ほっかいどう), 水道 (すいどう)
N5|車|シャ|くるま|Car, Wheeled Vehicle|गाड़ी / कार|7|車 (Cart)|Top-down view of a carriage with an axle and two wheels.|車 (くるま), 電車 (でんしゃ), 自転車 (じてんしゃ)
N5|語|ゴ|かた-る|Language, Word|भाषा / शब्द|14|言 (Speech)|Words (言) spoken by five (五) mouths (口) conversing.|日本語 (にほんご), 英語 (えいご), 物語 (ものがたり)
N5|本|ホン|もと|Book, Origin, Counter|किताब / मूल|5|木 (Tree)|A mark at the root of a tree (木) — paper books come from trees.|本 (ほん), 日本 (にほん), 本屋 (ほんや)
N5|休|キュウ|やす-む|Rest, Holiday|आराम करना / छुट्टी|6|人 (Person)|A person (亻) leaning against a shady tree (木) to rest.|休む (やすむ), 休み (やすみ), 夏休み (なつやすみ)
N5|行|コウ、ギョウ|い-く、おこな-う|Go, Line, Bank|जाना / बैंक|6|行 (Go)|Footsteps walking through a four-way street intersection.|行く (いく), 銀行 (ぎんこう), 旅行 (りょこう)
N5|来|ライ|く-る|Come, Next|आना / अगला|7|木 (Tree)|Someone coming toward you through a field of wheat.|来る (くる), 来週 (らいしゅう), 来年 (らいねん)
N5|帰|キ|かえ-る|Return Home|घर लौटना|10|巾 (Cloth)|Sweeping up with a broom and returning home.|帰る (かえる), 帰り (かえり)
N5|食|ショク|た-べる|Eat, Food|खाना / भोजन|9|食 (Eat)|A good (良) meal served warm under a roof.|食べる (たべる), 食べ物 (たべもの), 食堂 (しょくどう)
N5|飲|イン|の-む|Drink|पीना|12|食 (Food)|Food/mouth radical on the left with a person yawning/sipping (欠) on the right.|飲む (のむ), 飲み物 (のみもの)
N5|見|ケン|み-る|See, Look, Watch|देखना|7|見 (See)|An eye (目) mounted on two walking legs looking around.|見る (みる), 見せる (みせる), 花見 (はなみ)
N5|聞|ブン、モン|き-く|Hear, Listen, Ask|सुनना / पूछना|14|耳 (Ear)|An ear (耳) listening attentively at the gate (門).|聞く (きく), 新聞 (しんぶん)
N5|読|ドク|よ-む|Read|पढ़ना|14|言 (Speech)|Speaking out (言) the words sold (売) in a book.|読む (よむ), 読書 (どくしょ)
N5|書|ショ|か-く|Write, Book|लिखना|10|日 (Sun)|A hand holding a brush writing words over paper.|書く (かく), 辞書 (じしょ), 図書館 (としょかん)
N5|話|ワ|はな-す、はなし|Speak, Talk, Story|बोलना / बात|13|言 (Speech)|Words (言) spoken using the tongue (舌).|話す (はなす), 電話 (でんわ), 会話 (かいわ)
N5|買|バイ|か-う|Buy|खरीदना|12|貝 (Shell)|A net (罒) scooping up valuable seashell money (貝) to buy goods.|買う (かう), 買い物 (かいもの)
N5|出|シュツ|で-る、だ-す|Exit, Go Out, Take Out|बाहर निकलना / निकालना|5|凵 (Open Box)|Sprouts pushing up and out of a container.|出る (でる), 出す (だす), 出口 (でぐち)
N5|入|ニュウ|はい-る、い-れる|Enter, Put In|अंदर आना / डालना|2|入 (Enter)|A tent flap parting as someone steps inside.|入る (はいる), 入れる (いれる), 入口 (いりぐち)
N5|立|リツ|た-つ|Stand|खड़ा होना|5|立 (Stand)|A person standing firmly with both feet on the ground line.|立つ (たつ), 立派 (りっぱ)
N5|会|カイ|あ-う|Meet, Society|मिलना / बैठक|6|人 (Person)|People gathering together under one roof.|会う (あう), 会社 (かいしゃ), 会議 (かいぎ)
N5|言|ゲン、ゴン|い-う、こと|Say, Word|कहना / शब्द|7|言 (Speech)|Sound waves rising up from a speaking mouth (口).|言う (いう), 言葉 (ことば)
N5|大|ダイ、タイ|おお-きい|Big, Large, University|बड़ा|3|大 (Big)|A person stretching both arms and legs wide open.|大きい (おおきい), 大学 (だいがく), 大人 (おとな)
N5|小|ショウ|ちい-さい、こ-|Small, Little|छोटा|3|小 (Small)|A tiny center line with two small droplets on the sides.|小さい (ちいさい), 小学校 (しょうがっこう)
N5|新|シン|あたら-しい|New, Fresh|नया|13|斤 (Axe)|Using an axe (斤) on a standing tree (立+木) to cut fresh new timber.|新しい (あたらしい), 新聞 (しんぶん), 新幹線 (しんかんせん)
N5|古|コ|ふる-い|Old|पुराना|5|口 (Mouth)|Stories passed down through ten (十) generations of mouths (口).|古い (ふるい), 中古 (ちゅうこ)
N5|高|コウ|たか-い|High, Tall, Expensive|ऊँचा / महँगा|10|高 (Tall)|A tall multi-story castle tower.|高い (たかい), 高校 (こうこう)
N5|安|アン|やす-い|Cheap, Peaceful, Safe|सस्ता / सुरक्षित|6|宀 (Roof)|A woman (女) resting safely and peacefully under a home roof (宀).|安い (やすい), 安全 (あんぜん), 安心 (あんしん)
N5|多|タ|おお-い|Many, Much|बहुत / अधिक|6|夕 (Evening)|Two evenings (夕+夕) stacked together meaning 'many times'.|多い (おおい), 多分 (たぶん)
N5|少|ショウ|すく-ない、すこ-し|Few, A Little|कम / थोड़ा|4|小 (Small)|Something small (小) sliced with a diagonal stroke to make it even less.|少し (すこし), 少ない (すくない)
N5|長|チョウ|なが-い|Long, Leader|लम्बा / प्रमुख|8|長 (Long)|Long flowing hair held by a hairpin.|長い (ながい), 社長 (しゃちょう), 校長 (こうちょう)
N5|白|ハク|しろ、しろ-い|White|सफ़ेद|5|白 (White)|A bright ray of sunlight above the sun (日).|白い (しろい), 白 (しろ)
"""

    val allKanji: List<KanjiItem> by lazy {
        kanjiData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            KanjiItem(
                level = JlptLevel.N5,
                kanji = p[1],
                onyomi = p[2],
                kunyomi = p[3],
                meaningEn = p[4],
                meaningHi = p[5],
                strokeCount = p[6].toIntOrNull() ?: 4,
                radical = p[7],
                mnemonic = p[8],
                sampleWords = p[9].split(",").map { it.trim() }
            )
        }
    }

    fun kanji(level: JlptLevel = JlptLevel.N5): List<KanjiItem> = allKanji

    // ===============================================================
    // SPOKEN CONTRACTIONS LAB (Beginner-Friendly Native Speech Decoder)
    // ===============================================================
    val spokenContractions: List<SpokenContraction> = listOf(
        SpokenContraction(
            formalForm = "〜ている / 〜ています",
            casualSpoken = "〜てる / 〜てます",
            romaji = "-te iru → -teru",
            explanationHi = "बोलचाल में 'い' (i) अक्सर गायब हो जाता है। जैसे: खा रहा हूँ = 食べている → 食べてる।",
            exFormal = "今、何をしているのですか？",
            exCasual = "今、何してるの？",
            exMeaning = "What are you doing right now?"
        ),
        SpokenContraction(
            formalForm = "〜てしまう / 〜でしまう",
            casualSpoken = "〜ちゃう / 〜じゃう",
            romaji = "-te shimau → -chau",
            explanationHi = "जब कोई काम पूरा हो जाए या गलती से हो जाए (गया/बैठा), तो 〜てしまう छोटा होकर 〜ちゃう बन जाता है।",
            exFormal = "宿題を忘れてしまいました。",
            exCasual = "宿題、忘れちゃった！",
            exMeaning = "I ended up forgetting my homework!"
        ),
        SpokenContraction(
            formalForm = "〜てはいけません",
            casualSpoken = "〜ちゃダメ",
            romaji = "-te wa ikemasen → -cha dame",
            explanationHi = "मनाही बताने वाला लम्बा नियम (〜てはいけません) रोज़मर्रा की बातचीत में सिर्फ '〜ちゃダメ' बोला जाता है।",
            exFormal = "ここで写真を撮ってはいけません。",
            exCasual = "ここで写真撮っちゃダメだよ。",
            exMeaning = "You mustn't take photos here."
        ),
        SpokenContraction(
            formalForm = "〜なければなりません",
            casualSpoken = "〜なきゃ / 〜ないと",
            romaji = "-nakereba narimasen → -nakya",
            explanationHi = "'करना ही पड़ेगा' बोलचाल में बहुत छोटा होकर सिर्फ 〜なきゃ या 〜ないと रह जाता है।",
            exFormal = "もう帰らなければなりません。",
            exCasual = "もう帰らなきゃ！",
            exMeaning = "I have got to go home now!"
        ),
        SpokenContraction(
            formalForm = "〜てください",
            casualSpoken = "〜て / 〜てね",
            romaji = "-te kudasai → -te ne",
            explanationHi = "दोस्तों और परिवार के साथ 'ください' हटाकर सिर्फ て-form बोलने से 'प्लीज़ करो' का अर्थ निकलता है।",
            exFormal = "ちょっと待ってください。",
            exCasual = "ちょっと待ってね！",
            exMeaning = "Wait a second, okay?"
        ),
        SpokenContraction(
            formalForm = "〜ではありません",
            casualSpoken = "〜じゃない",
            romaji = "-dewa arimasen → -ja nai",
            explanationHi = "नकारात्मक वाक्यों में 'ではありません' बोलचाल में 'じゃない (नहीं है)' बन जाता है।",
            exFormal = "これは私のペンではありません。",
            exCasual = "これ、私のペンじゃないよ。",
            exMeaning = "This isn't my pen."
        ),
        SpokenContraction(
            formalForm = "〜らない / 〜るの",
            casualSpoken = "〜んない / 〜んの",
            romaji = "-ranai → -nnai",
            explanationHi = "तेज़ टोक्यो स्पीच में 'ら/る' (ra/ru) की ध्वनि नाक से 'ん' (n) बन जाती है।",
            exFormal = "全然分かりません。",
            exCasual = "全然分かんない。",
            exMeaning = "I don't get it at all."
        )
    )

    // ===============================================================
    // INTERACTIVE N5 SPEAKING & LISTENING ROLEPLAY SCENARIOS
    // ===============================================================
    val roleplayScenarios: List<RoleplayScenario> = listOf(
        RoleplayScenario(
            id = "rp_n5_intro",
            level = JlptLevel.N5,
            title = "1. First Day at Tokyo University",
            setting = "Campus Orientation (पहली मुलाकात और परिचय)",
            politenessLevel = "Polite Student (丁寧語)",
            lines = listOf(
                DialogueLine("Sakura", false, "こんにちは！はじめまして。", "こんにちは はじめまして", "Konnichiwa! Hajimemashite.", "Hello! Nice to meet you.", "नमस्ते! आपसे मिलकर खुशी हुई।"),
                DialogueLine("You", true, "はじめまして。私の名前はリシです。", "はじめまして わたしのなまえはりしです", "Hajimemashite. Watashi no namae wa Rishi desu.", "Nice to meet you. My name is Rishi.", "नमस्ते। मेरा नाम ऋषि है।"),
                DialogueLine("Sakura", false, "お国はどちらですか？", "おくにはどちらですか", "Okuni wa dochira desu ka?", "Which country are you from?", "आप किस देश से हैं?"),
                DialogueLine("You", true, "インドから来ました。よろしくお願いします。", "いんどからきました よろしくおねがいします", "Indo kara kimashita. Yoroshiku onegaishimasu.", "I came from India. Pleased to meet you.", "मैं भारत से आया हूँ। आपसे मिलकर अच्छा लगा।")
            )
        ),
        RoleplayScenario(
            id = "rp_n5_ramen",
            level = JlptLevel.N5,
            title = "2. Ordering at a Tokyo Ramen Shop",
            setting = "Shinjuku Ramen Counter (रेस्टोरेंट में ऑर्डर करते हुए)",
            politenessLevel = "Polite Customer (丁寧語)",
            lines = listOf(
                DialogueLine("Staff", false, "いらっしゃいませ！何名様ですか？", "いらっしゃいませ なんめいさまですか", "Irasshaimase! Nan-mei-sama desu ka?", "Welcome! How many people?", "स्वागत है! आप कितने लोग हैं?"),
                DialogueLine("You", true, "一人です。お願いします。", "ひとりです おねがいします", "Hitori desu. Onegaishimasu.", "Just one person, please.", "मैं अकेला हूँ। कृपया सीट दीजिए।"),
                DialogueLine("Staff", false, "ご注文はお決まりですか？", "ごちゅうもんはおきまりですか", "Go-chuumon wa okimari desu ka?", "Have you decided on your order?", "क्या आपने ऑर्डर तय कर लिया है?"),
                DialogueLine("You", true, "みそラーメンと水をください。", "みそらーめんとみずをください", "Miso raamen to mizu o kudasai.", "Miso ramen and water, please.", "मिसो रामेन और पानी दीजिए।")
            )
        ),
        RoleplayScenario(
            id = "rp_n5_konbini",
            level = JlptLevel.N5,
            title = "3. Shopping at a 7-Eleven Konbini",
            setting = "Tokyo Convenience Store (कन्वीनियंस स्टोर में खरीदारी)",
            politenessLevel = "Daily Shopping (です・ます)",
            lines = listOf(
                DialogueLine("You", true, "すみません、これとこれをください。", "すみません これとこれをください", "Sumimasen, kore to kore o kudasai.", "Excuse me, I'll take this and this, please.", "माफ़ कीजिए, मुझे यह और यह दीजिए।"),
                DialogueLine("Clerk", false, "お弁当は温めますか？", "おべんとうはあたためますか", "Obentou wa atatamemasu ka?", "Would you like your bento warmed up?", "क्या आपका लंच बॉक्स गर्म कर दूँ?"),
                DialogueLine("You", true, "はい、お願いします。袋も一枚ください。", "はい おねがいします ふくろもいちまいください", "Hai, onegaishimasu. Fukuro mo ichimai kudasai.", "Yes, please. Please also give me one bag.", "जी हाँ, कृपया। एक कैरी बैग भी दे दीजिए।"),
                DialogueLine("Clerk", false, "全部で六百五十円になります。", "ぜんぶでろっぴゃくごじゅうえんになります", "Zenbu de roppyaku-gojuu-en ni narimasu.", "That will be 650 yen in total.", "कुल मिलाकर 650 येन हुए।")
            )
        ),
        RoleplayScenario(
            id = "rp_n5_station",
            level = JlptLevel.N5,
            title = "4. Asking Directions & Train Platform",
            setting = "Tokyo JR Station Gate (स्टेशन पर रास्ता और प्लेटफ़ॉर्म पूछना)",
            politenessLevel = "Polite Traveler (丁寧語)",
            lines = listOf(
                DialogueLine("You", true, "すみません、東京駅はどこですか。", "すみません とうきょうえきはどこですか", "Sumimasen, Toukyou-eki wa doko desu ka.", "Excuse me, where is Tokyo Station?", "माफ़ कीजिए, टोक्यो स्टेशन कहाँ है?"),
                DialogueLine("Station Staff", false, "この道を真っ直ぐ行って、右にありますよ。", "このみちをまっすぐいって みぎにありますよ", "Kono michi o massugu itte, migi ni arimasu yo.", "Go straight down this road, and it's on the right.", "इस सड़क पर सीधे जाइए, दाएँ हाथ पर है।"),
                DialogueLine("You", true, "ありがとうございます。京都行きの電車は何番線ですか。", "ありがとうございます きょうといきのでんしゃはなんばんせんですか", "Arigatou gozaimasu. Kyouto-yuki no densha wa nan-bansen desu ka.", "Thank you. Which platform is the train bound for Kyoto?", "धन्यवाद। क्योटो जाने वाली ट्रेन किस प्लेटफ़ॉर्म पर है?"),
                DialogueLine("Station Staff", false, "三番線です。気をつけていってらっしゃい！", "さんばんせんです きをつけていってらっしゃい", "San-bansen desu. Ki o tsukete itterasshai!", "It's Platform 3. Have a safe trip!", "प्लेटफ़ॉर्म 3 पर है। आपकी यात्रा शुभ हो!")
            )
        ),
        RoleplayScenario(
            id = "rp_n5_hospital",
            level = JlptLevel.N5,
            title = "5. At a Clinic / Pharmacy (Feeling Sick)",
            setting = "Town Clinic Reception (तबीयत खराब होने पर डॉक्टर से बात)",
            politenessLevel = "Polite Patient (丁寧語)",
            lines = listOf(
                DialogueLine("Doctor", false, "今日はどうしましたか？", "きょうはどうしましたか", "Kyou wa dou shimashita ka?", "What seems to be the problem today?", "आज आपको क्या तकलीफ़ है?"),
                DialogueLine("You", true, "昨日から頭が痛くて、少し熱があります。", "きのうからあたまがいたくて すこしねつがあります", "Kinou kara atama ga itakute, sukoshi netsu ga arimasu.", "Since yesterday I have a headache and a slight fever.", "कल से मेरे सिर में दर्द है और थोड़ा बुखार है।"),
                DialogueLine("Doctor", false, "風邪ですね。この薬を飲んで、今日はゆっくり休んでください。", "かぜですね このくすりをのんで きょうはゆっくりやすんでください", "Kaze desu ne. Kono kusuri o nonde, kyou wa yukkuri yasunde kudasai.", "It's a cold. Take this medicine and rest well today.", "यह सर्दी-ज़ुकाम है। यह दवा लें और आज आराम करें।"),
                DialogueLine("You", true, "分かりました。ありがとうございます。", "わかりました ありがとうございます", "Wakarimashita. Arigatou gozaimasu.", "I understand. Thank you very much.", "मैं समझ गया। बहुत-बहुत धन्यवाद।")
            )
        )
    )

    // ===============================================================
    // N5 PARTICLE DRILL BANK (20 Contextual Drills for QuizMode.PARTICLE)
    // ===============================================================
    private data class ParticleDrill(
        val level: JlptLevel,
        val sentenceWithBlank: String,
        val options: List<String>,
        val correctParticle: String,
        val explanation: String,
        val fullAudio: String
    )

    private val particleDrills: List<ParticleDrill> = listOf(
        ParticleDrill(JlptLevel.N5, "私 ＿ インドの学生です。", listOf("は", "を", "に", "で"), "は", "は (wa) marks the main topic ('As for me...').", "私はインドの学生です。"),
        ParticleDrill(JlptLevel.N5, "毎朝、冷たい水 ＿ 飲みます。", listOf("を", "に", "で", "へ"), "を", "を (o) marks the direct object of a transitive verb (पानी को पीता हूँ).", "毎朝、冷たい水を飲みます。"),
        ParticleDrill(JlptLevel.N5, "図書館 ＿ 本を読みます。", listOf("で", "に", "を", "へ"), "で", "で (de) marks the location where an active action happens (लाइब्रेरी में पढ़ता हूँ).", "図書館で本を読みます。"),
        ParticleDrill(JlptLevel.N5, "毎朝、六時 ＿ 起きます。", listOf("に", "で", "を", "から"), "に", "に (ni) marks a specific clock time (छह बजे पर).", "毎朝、六時に起きます。"),
        ParticleDrill(JlptLevel.N5, "私は日本語 ＿ 分かります。", listOf("が", "を", "に", "で"), "が", "分かる (understand), 好き (like), and 上手 (skilled) always take が, never を!", "私は日本語が分かります。"),
        ParticleDrill(JlptLevel.N5, "電車 ＿ 大学へ行きます。", listOf("で", "に", "を", "と"), "で", "で (de) marks the means of transportation or tool used (ट्रेन से).", "電車で大学へ行きます。"),
        ParticleDrill(JlptLevel.N5, "机の上 ＿ ノートがあります。", listOf("に", "で", "を", "へ"), "に", "Static existence with あります and います always marks the location with に!", "机の上にノートがあります。"),
        ParticleDrill(JlptLevel.N5, "これは日本語 ＿ 辞書です。", listOf("の", "は", "を", "に"), "の", "の (no) connects two nouns (जापानी भाषा का शब्दकोश).", "これは日本語の辞書です。"),
        ParticleDrill(JlptLevel.N5, "東京駅で新幹線 ＿ 乗ります。", listOf("に", "を", "で", "と"), "に", "乗る (to board/ride) and 入る (to enter) always take target particle に!", "東京駅で新幹線に乗ります。"),
        ParticleDrill(JlptLevel.N5, "渋谷駅で電車 ＿ 降ります。", listOf("を", "に", "で", "から"), "を", "降りる (to get off a vehicle) and 出る (to leave a room) take を!", "渋谷駅で電車を降ります。"),
        ParticleDrill(JlptLevel.N5, "明日、駅で友達 ＿ 会います。", listOf("に", "を", "で", "が"), "に", "会う (to meet someone) marks the person you meet with に (or と).", "明日、駅で友達に会います。"),
        ParticleDrill(JlptLevel.N5, "あそこに誰 ＿ いますか。", listOf("が", "は", "を", "に"), "が", "Question words like 誰 (who) and 何 (what) as the subject ALWAYS take が!", "あそこに誰がいますか。"),
        ParticleDrill(JlptLevel.N5, "銀行は九時 ＿ 三時までです。", listOf("から", "まで", "に", "で"), "から", "から (kara) means 'from' a starting time or place.", "銀行は九時から三時までです。"),
        ParticleDrill(JlptLevel.N5, "箸 ＿ ご飯を食べます。", listOf("で", "に", "を", "と"), "で", "で (de) marks the tool or instrument used to perform an action (चॉपस्टिक से).", "箸でご飯を食べます。"),
        ParticleDrill(JlptLevel.N5, "デパートへ靴を買い ＿ 行きます。", listOf("に", "を", "で", "へ"), "に", "Verb stem + に + 行きます expresses going somewhere for the purpose of doing an action.", "デパートへ靴を買いに行きます。"),
        ParticleDrill(JlptLevel.N5, "新しいパソコン ＿ 欲しいです。", listOf("が", "を", "に", "で"), "が", "欲しい (want an object) is an い-Adjective and always marks the desired item with が!", "新しいパソコンが欲しいです。"),
        ParticleDrill(JlptLevel.N5, "今日は昨日 ＿ 寒いです。", listOf("より", "から", "まで", "ほど"), "より", "より (yori) means 'more than' when comparing two things.", "今日は昨日より寒いです。"),
        ParticleDrill(JlptLevel.N5, "果物の中でりんご ＿ 一番好きです。", listOf("が", "を", "に", "で"), "が", "In superlative (一番) and 好き sentences, the chosen item is marked with が.", "果物の中でりんごが一番好きです。"),
        ParticleDrill(JlptLevel.N5, "土曜日と日曜日 ＿ 休みです。", listOf("だけ", "しか", "より", "まで"), "だけ", "だけ (dake) means 'only / just' with affirmative sentences.", "土曜日と日曜日だけ休みです。"),
        ParticleDrill(JlptLevel.N5, "友達 ＿ 一緒に映画を見ました。", listOf("と", "に", "を", "で"), "と", "と (to) means 'with' when doing an activity together with someone.", "友達と一緒に映画を見ました。")
    )

    // ===============================================================
    // 6-MODE PRO N5 QUIZ GENERATOR
    // ===============================================================
    fun makeQuiz(level: JlptLevel? = JlptLevel.N5, mode: QuizMode, count: Int = 10): List<QuizQuestion> {
        val pool = vocab
        val sentPool = allSentences

        return when (mode) {
            QuizMode.KANA -> {
                val kanaPool = allKana
                kanaPool.shuffled().take(count).map { k ->
                    val wrong = kanaPool.map { it.romaji }.distinct().filter { it != k.romaji }.shuffled().take(3)
                    val opts = (wrong + k.romaji).shuffled()
                    QuizQuestion(
                        instruction = "Identify the sound of this ${if (k.type == KanaType.HIRAGANA) "Hiragana" else "Katakana"} (${k.group.title})",
                        big = k.char,
                        options = opts,
                        correctIndex = opts.indexOf(k.romaji),
                        explanation = "${k.char} is pronounced \"${k.romaji}\".",
                        audio = k.char,
                        hint = k.group.tip
                    )
                }
            }
            QuizMode.MEANING -> pool.shuffled().take(count).map { w ->
                val target = "${w.en} (${w.hi})"
                val wrong = pool.map { "${it.en} (${it.hi})" }.distinct().filter { it != target }.shuffled().take(3)
                val opts = (wrong + target).shuffled()
                QuizQuestion(
                    instruction = "What is the meaning of this JLPT N5 word?",
                    big = w.kanji,
                    options = opts,
                    correctIndex = opts.indexOf(target),
                    explanation = "${w.kanji} (${w.kana} · ${w.romaji}) = ${w.en} · ${w.hi}\nExample: ${w.exJp}",
                    audio = w.kana,
                    hint = "${w.type.label} · Pitch: ${w.pitchType.label}"
                )
            }
            QuizMode.READING -> {
                val kanjiWords = pool.filter { it.kanji != it.kana }.ifEmpty { pool }
                kanjiWords.shuffled().take(count).map { w ->
                    val wrong = kanjiWords.map { it.kana }.distinct().filter { it != w.kana }.shuffled().take(3)
                    val opts = (wrong + w.kana).shuffled()
                    QuizQuestion(
                        instruction = "Select the correct Hiragana reading",
                        big = w.kanji,
                        options = opts,
                        correctIndex = opts.indexOf(w.kana),
                        explanation = "${w.kanji} is read ${w.kana} (${w.romaji}) = ${w.en} (${w.hi})",
                        audio = w.kana,
                        hint = "Meaning: ${w.en} (${w.hi})"
                    )
                }
            }
            QuizMode.LISTEN -> pool.shuffled().take(count).map { w ->
                val target = "${w.en} · ${w.hi}"
                val wrong = pool.map { "${it.en} · ${it.hi}" }.distinct().filter { it != target }.shuffled().take(3)
                val opts = (wrong + target).shuffled()
                QuizQuestion(
                    instruction = "Listen carefully to the native audio and pick the meaning",
                    big = "🔊",
                    options = opts,
                    correctIndex = opts.indexOf(target),
                    explanation = "You heard: ${w.kanji} (${w.kana}) = ${w.en} · ${w.hi}",
                    audio = w.kana,
                    autoPlay = true,
                    hint = "Word class: ${w.type.label}"
                )
            }
            QuizMode.DICTATION -> sentPool.shuffled().take(count).map { s ->
                val wrong = sentPool.map { it.jp }.distinct().filter { it != s.jp }.shuffled().take(3)
                val opts = (wrong + s.jp).shuffled()
                QuizQuestion(
                    instruction = "Dictation Lab: Listen to the sentence and match the exact Japanese script",
                    big = "🎧 🔊",
                    options = opts,
                    correctIndex = opts.indexOf(s.jp),
                    explanation = "${s.jp}\n(${s.romaji})\nMeaning: ${s.en} · ${s.hi}\nCasual Spoken: ${s.casualJp}",
                    audio = s.jp,
                    autoPlay = true,
                    hint = "Hindi hint: ${s.hi}"
                )
            }
            QuizMode.PARTICLE -> {
                particleDrills.shuffled().take(count).map { d ->
                    val opts = d.options.shuffled()
                    QuizQuestion(
                        instruction = "Fill in the blank (＿) with the most natural Japanese particle",
                        big = d.sentenceWithBlank,
                        options = opts,
                        correctIndex = opts.indexOf(d.correctParticle),
                        explanation = "Correct: ${d.correctParticle}\n${d.explanation}\nFull Sentence: ${d.fullAudio}",
                        audio = d.fullAudio,
                        hint = "JLPT N5 Grammar Nuance"
                    )
                }
            }
        }
    }
}
