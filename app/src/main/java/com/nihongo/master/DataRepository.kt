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
    // TOKYO PITCH ACCENT OVERRIDES (0 = Heiban, 1 = Atamadaka, >1 = Nakadaka/Odaka)
    // ---------------------------------------------------------------
    private val pitchOverrides: Map<String, Int> = mapOf(
        "食べる" to 2, "飲む" to 1, "行く" to 0, "来る" to 1, "見る" to 1,
        "聞く" to 0, "話す" to 2, "読む" to 1, "書く" to 1, "買う" to 0,
        "する" to 0, "勉強する" to 0, "起きる" to 2, "寝る" to 0, "会う" to 1,
        "帰る" to 1, "入る" to 1, "出る" to 1, "待つ" to 1, "座る" to 0,
        "歩く" to 2, "走る" to 2, "働く" to 0, "使う" to 0, "作る" to 2,
        "教える" to 0, "開ける" to 0, "閉める" to 2, "分かる" to 2, "遊ぶ" to 0,
        "泳ぐ" to 2, "乗る" to 0, "私" to 0, "先生" to 3, "学生" to 0,
        "友達" to 0, "家族" to 1, "水" to 0, "お茶" to 0, "魚" to 0,
        "肉" to 2, "駅" to 1, "電車" to 0, "学校" to 0, "会社" to 0,
        "本" to 1, "犬" to 2, "猫" to 1, "日本" to 2, "今日" to 1,
        "明日" to 3, "昨日" to 2, "毎日" to 1, "時間" to 0, "天気" to 1,
        "車" to 0, "名前" to 0, "お金" to 0, "家" to 2, "部屋" to 2,
        "手" to 2, "目" to 1, "朝" to 1, "雨" to 1, "飴" to 0,
        "橋" to 2, "箸" to 1, "山" to 2, "店" to 2,
        "大きい" to 3, "小さい" to 3, "新しい" to 4, "古い" to 2,
        "高い" to 2, "安い" to 2, "おいしい" to 0, "楽しい" to 3,
        "忙しい" to 4, "暑い" to 2, "寒い" to 2, "いい" to 1,
        "好き" to 2, "元気" to 1, "きれい" to 1, "静か" to 1,
        "有名" to 0, "上手" to 3, "ありがとう" to 2, "すみません" to 4
    )

    // ---------------------------------------------------------------
    // WORDS (type|kanji|kana|romaji|english|hindi|example|example romaji|example english)
    // ---------------------------------------------------------------
    private val wordTypeByCode: Map<String, WordType> by lazy { WordType.entries.associateBy { it.code } }

    private fun parseWords(level: JlptLevel, raw: String): List<VocabItem> {
        return raw.trim().lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
            val p = line.split("|").map { it.trim() }
            fun f(i: Int) = p.getOrElse(i) { "" }
            val kanji = f(1)
            val explicitPitch = p.getOrNull(9)?.toIntOrNull() ?: pitchOverrides[kanji] ?: -1
            VocabItem(
                id = "${level.label}_${idx + 1}",
                level = level,
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
        }
    }

    private val n5Words = """
v1|食べる|たべる|taberu|to eat|खाना|毎朝パンを食べます。|Maiasa pan o tabemasu.|I eat bread every morning.
v5|飲む|のむ|nomu|to drink|पीना|水を飲みます。|Mizu o nomimasu.|I drink water.
v5|行く|いく|iku|to go|जाना|学校へ行きます。|Gakkou e ikimasu.|I go to school.
vk|来る|くる|kuru|to come|आना|友達が来ます。|Tomodachi ga kimasu.|My friend is coming.
v1|見る|みる|miru|to see / watch|देखना|テレビを見ます。|Terebi o mimasu.|I watch TV.
v5|聞く|きく|kiku|to listen / ask|सुनना / पूछना|音楽を聞きます。|Ongaku o kikimasu.|I listen to music.
v5|話す|はなす|hanasu|to speak|बोलना|日本語を話します。|Nihongo o hanashimasu.|I speak Japanese.
v5|読む|よむ|yomu|to read|पढ़ना|本を読みます。|Hon o yomimasu.|I read a book.
v5|書く|かく|kaku|to write|लिखना|名前を書きます。|Namae o kakimasu.|I write my name.
v5|買う|かう|kau|to buy|खरीदना|パンを買います。|Pan o kaimasu.|I buy bread.
vs|する|する|suru|to do|करना|宿題をします。|Shukudai o shimasu.|I do homework.
vs|勉強する|べんきょうする|benkyou suru|to study|पढ़ाई करना|毎日勉強します。|Mainichi benkyou shimasu.|I study every day.
v1|起きる|おきる|okiru|to wake up|उठना|六時に起きます。|Rokuji ni okimasu.|I wake up at six.
v1|寝る|ねる|neru|to sleep|सोना|十一時に寝ます。|Juuichiji ni nemasu.|I sleep at eleven.
v5|会う|あう|au|to meet|मिलना|友達に会います。|Tomodachi ni aimasu.|I meet a friend.
v5|帰る|かえる|kaeru|to return home|घर लौटना|五時に帰ります。|Goji ni kaerimasu.|I go home at five.
v5|入る|はいる|hairu|to enter|अंदर आना|部屋に入ります。|Heya ni hairimasu.|I enter the room.
v1|出る|でる|deru|to go out|बाहर निकलना|八時に家を出ます。|Hachiji ni ie o demasu.|I leave home at eight.
v5|待つ|まつ|matsu|to wait|इंतज़ार करना|駅で待ちます。|Eki de machimasu.|I wait at the station.
v5|座る|すわる|suwaru|to sit|बैठना|ここに座ります。|Koko ni suwarimasu.|I sit here.
v5|歩く|あるく|aruku|to walk|पैदल चलना|公園を歩きます。|Kouen o arukimasu.|I walk in the park.
v5|走る|はしる|hashiru|to run|दौड़ना|朝、走ります。|Asa, hashirimasu.|I run in the morning.
v5|働く|はたらく|hataraku|to work|काम करना|会社で働きます。|Kaisha de hatarakimasu.|I work at a company.
v5|使う|つかう|tsukau|to use|इस्तेमाल करना|スマホを使います。|Sumaho o tsukaimasu.|I use a smartphone.
v5|作る|つくる|tsukuru|to make|बनाना|料理を作ります。|Ryouri o tsukurimasu.|I cook a meal.
v1|教える|おしえる|oshieru|to teach / tell|सिखाना / बताना|先生が教えます。|Sensei ga oshiemasu.|The teacher teaches.
v1|開ける|あける|akeru|to open|खोलना|窓を開けます。|Mado o akemasu.|I open the window.
v1|閉める|しめる|shimeru|to close|बंद करना|ドアを閉めます。|Doa o shimemasu.|I close the door.
v5|分かる|わかる|wakaru|to understand|समझना|日本語が分かります。|Nihongo ga wakarimasu.|I understand Japanese.
v5|遊ぶ|あそぶ|asobu|to play|खेलना / मौज करना|公園で遊びます。|Kouen de asobimasu.|I play in the park.
v5|泳ぐ|およぐ|oyogu|to swim|तैरना|海で泳ぎます。|Umi de oyogimasu.|I swim in the sea.
v5|乗る|のる|noru|to ride|सवार होना|電車に乗ります。|Densha ni norimasu.|I ride the train.
n|私|わたし|watashi|I / me|मैं|私は学生です。|Watashi wa gakusei desu.|I am a student.
n|先生|せんせい|sensei|teacher|शिक्षक|先生は優しいです。|Sensei wa yasashii desu.|The teacher is kind.
n|学生|がくせい|gakusei|student|विद्यार्थी|彼は学生です。|Kare wa gakusei desu.|He is a student.
n|友達|ともだち|tomodachi|friend|दोस्त|友達と遊びます。|Tomodachi to asobimasu.|I play with a friend.
n|家族|かぞく|kazoku|family|परिवार|家族は四人です。|Kazoku wa yonin desu.|My family has four people.
n|水|みず|mizu|water|पानी|水をください。|Mizu o kudasai.|Water, please.
n|お茶|おちゃ|ocha|tea|चाय|お茶を飲みます。|Ocha o nomimasu.|I drink tea.
n|魚|さかな|sakana|fish|मछली|魚が好きです。|Sakana ga suki desu.|I like fish.
n|肉|にく|niku|meat|मांस|肉を食べません。|Niku o tabemasen.|I don't eat meat.
n|駅|えき|eki|station|स्टेशन|駅は近いです。|Eki wa chikai desu.|The station is near.
n|電車|でんしゃ|densha|train|ट्रेन|電車で行きます。|Densha de ikimasu.|I go by train.
n|学校|がっこう|gakkou|school|स्कूल|学校は九時です。|Gakkou wa kuji desu.|School is at nine.
n|会社|かいしゃ|kaisha|company|कंपनी|会社は東京です。|Kaisha wa Toukyou desu.|The company is in Tokyo.
n|本|ほん|hon|book|किताब|この本は面白いです。|Kono hon wa omoshiroi desu.|This book is interesting.
n|犬|いぬ|inu|dog|कुत्ता|犬が走ります。|Inu ga hashirimasu.|The dog runs.
n|猫|ねこ|neko|cat|बिल्ली|猫は可愛いです。|Neko wa kawaii desu.|The cat is cute.
n|日本|にほん|nihon|Japan|जापान|日本へ行きたいです。|Nihon e ikitai desu.|I want to go to Japan.
n|今日|きょう|kyou|today|आज|今日は暑いです。|Kyou wa atsui desu.|Today is hot.
n|明日|あした|ashita|tomorrow|कल (आने वाला)|明日、会いましょう。|Ashita, aimashou.|Let's meet tomorrow.
n|昨日|きのう|kinou|yesterday|कल (बीता हुआ)|昨日は雨でした。|Kinou wa ame deshita.|Yesterday it rained.
n|毎日|まいにち|mainichi|every day|हर दिन|毎日歩きます。|Mainichi arukimasu.|I walk every day.
n|時間|じかん|jikan|time|समय|時間がありません。|Jikan ga arimasen.|I have no time.
n|天気|てんき|tenki|weather|मौसम|今日の天気はいいです。|Kyou no tenki wa ii desu.|Today's weather is good.
n|車|くるま|kuruma|car|गाड़ी|車を買いました。|Kuruma o kaimashita.|I bought a car.
n|名前|なまえ|namae|name|नाम|お名前は何ですか。|Onamae wa nan desu ka.|What is your name?
n|お金|おかね|okane|money|पैसा|お金がありません。|Okane ga arimasen.|I have no money.
n|家|いえ|ie|house / home|घर|大きい家です。|Ookii ie desu.|It is a big house.
n|部屋|へや|heya|room|कमरा|部屋がきれいです。|Heya ga kirei desu.|The room is clean.
n|手|て|te|hand|हाथ|手を洗います。|Te o araimasu.|I wash my hands.
n|目|め|me|eye|आँख|目が痛いです。|Me ga itai desu.|My eyes hurt.
n|朝|あさ|asa|morning|सुबह|朝ご飯を食べます。|Asagohan o tabemasu.|I eat breakfast.
n|雨|あめ|ame|rain (Pitch: High-Low)|बारिश|雨が降っています。|Ame ga futte imasu.|It is raining.|1
n|飴|あめ|ame|candy (Pitch: Low-High)|टॉफ़ी / मिठाई|甘い飴を食べます。|Amai ame o tabemasu.|I eat sweet candy.|0
n|箸|はし|hashi|chopsticks (Pitch: High-Low)|चॉपस्टिक|箸で食べます。|Hashi de tabemasu.|I eat with chopsticks.|1
n|橋|はし|hashi|bridge (Pitch: Low-High)|पुल|橋を渡ります。|Hashi o watarimasu.|I cross the bridge.|2
n|山|やま|yama|mountain|पहाड़|山に登ります。|Yama ni noborimasu.|I climb a mountain.
n|店|みせ|mise|shop|दुकान|店は十時に開きます。|Mise wa juuji ni akimasu.|The shop opens at ten.
adji|大きい|おおきい|ookii|big|बड़ा|大きい犬です。|Ookii inu desu.|It is a big dog.
adji|小さい|ちいさい|chiisai|small|छोटा|小さい猫です。|Chiisai neko desu.|It is a small cat.
adji|新しい|あたらしい|atarashii|new|नया|新しい車です。|Atarashii kuruma desu.|It is a new car.
adji|古い|ふるい|furui|old (things)|पुराना|古い本です。|Furui hon desu.|It is an old book.
adji|高い|たかい|takai|expensive / tall|महँगा / ऊँचा|この店は高いです。|Kono mise wa takai desu.|This shop is expensive.
adji|安い|やすい|yasui|cheap|सस्ता|この本は安いです。|Kono hon wa yasui desu.|This book is cheap.
adji|おいしい|おいしい|oishii|delicious|स्वादिष्ट|魚はおいしいです。|Sakana wa oishii desu.|The fish is delicious.
adji|楽しい|たのしい|tanoshii|fun|मज़ेदार|旅行は楽しいです。|Ryokou wa tanoshii desu.|The trip is fun.
adji|忙しい|いそがしい|isogashii|busy|व्यस्त|今日は忙しいです。|Kyou wa isogashii desu.|I am busy today.
adji|暑い|あつい|atsui|hot (weather)|गर्म (मौसम)|夏は暑いです。|Natsu wa atsui desu.|Summer is hot.
adji|寒い|さむい|samui|cold (weather)|ठंडा (मौसम)|冬は寒いです。|Fuyu wa samui desu.|Winter is cold.
adji|いい|いい|ii|good|अच्छा|いい天気ですね。|Ii tenki desu ne.|Nice weather, isn't it?
adjna|好き|すき|suki|like / favorite|पसंद|猫が好きです。|Neko ga suki desu.|I like cats.
adjna|元気|げんき|genki|healthy / energetic|स्वस्थ / ऊर्जावान|はい、元気です。|Hai, genki desu.|Yes, I'm fine.
adjna|きれい|きれい|kirei|pretty / clean|सुंदर / साफ़|部屋はきれいです。|Heya wa kirei desu.|The room is clean.
adjna|静か|しずか|shizuka|quiet|शांत|図書館は静かです。|Toshokan wa shizuka desu.|The library is quiet.
adjna|有名|ゆうめい|yuumei|famous|मशहूर|有名な店です。|Yuumei na mise desu.|It is a famous shop.
adjna|上手|じょうず|jouzu|skilled|कुशल|彼は上手です。|Kare wa jouzu desu.|He is good at it.
x|ありがとう|ありがとう|arigatou|thank you|धन्यवाद|どうもありがとう。|Doumo arigatou.|Thank you very much.
x|すみません|すみません|sumimasen|excuse me / sorry|माफ़ कीजिए|すみません、駅はどこですか。|Sumimasen, eki wa doko desu ka.|Excuse me, where is the station?
x|おはよう|おはよう|ohayou|good morning|सुप्रभात|おはようございます。|Ohayou gozaimasu.|Good morning.
x|こんにちは|こんにちは|konnichiwa|hello|नमस्ते|こんにちは、元気ですか。|Konnichiwa, genki desu ka.|Hello, how are you?
x|お願いします|おねがいします|onegaishimasu|please|कृपया|水をお願いします。|Mizu o onegaishimasu.|Water, please.
x|とても|とても|totemo|very|बहुत|とても高いです。|Totemo takai desu.|It is very expensive.
x|もう一度|もういちど|mou ichido|once more|एक बार और|もう一度お願いします。|Mou ichido onegaishimasu.|Once more, please.
"""

    private val n4Words = """
v1|続ける|つづける|tsuzukeru|to continue|जारी रखना|勉強を続けます。|Benkyou o tsuzukemasu.|I continue studying.
v1|決める|きめる|kimeru|to decide|तय करना|目標を決めました。|Mokuhyou o kimemashita.|I decided on a goal.
v1|調べる|しらべる|shiraberu|to look up|खोजना / जाँचना|辞書で調べます。|Jisho de shirabemasu.|I look it up in a dictionary.
v5|選ぶ|えらぶ|erabu|to choose|चुनना|好きな色を選びます。|Suki na iro o erabimasu.|I choose a favorite color.
v5|直す|なおす|naosu|to fix|ठीक करना|間違いを直します。|Machigai o naoshimasu.|I correct the mistakes.
vs|約束する|やくそくする|yakusoku suru|to promise|वादा करना|明日会うと約束しました。|Ashita au to yakusoku shimashita.|I promised to meet tomorrow.
vs|準備する|じゅんびする|junbi suru|to prepare|तैयारी करना|試験の準備をします。|Shiken no junbi o shimasu.|I prepare for the exam.
vs|心配する|しんぱいする|shinpai suru|to worry|चिंता करना|母が心配します。|Haha ga shinpai shimasu.|My mother worries.
vs|運転する|うんてんする|unten suru|to drive|गाड़ी चलाना|車を運転します。|Kuruma o unten shimasu.|I drive a car.
vs|旅行する|りょこうする|ryokou suru|to travel|यात्रा करना|日本を旅行します。|Nihon o ryokou shimasu.|I travel around Japan.
v1|覚える|おぼえる|oboeru|to memorize|याद करना|単語を覚えます。|Tango o oboemasu.|I memorize words.
v1|忘れる|わすれる|wasureru|to forget|भूल जाना|宿題を忘れました。|Shukudai o wasuremashita.|I forgot my homework.
v5|送る|おくる|okuru|to send|भेजना|メールを送ります。|Meeru o okurimasu.|I send an email.
v5|思う|おもう|omou|to think|सोचना / लगना|いいと思います。|Ii to omoimasu.|I think it's good.
v5|持つ|もつ|motsu|to hold / have|पकड़ना / रखना|かばんを持ちます。|Kaban o mochimasu.|I hold a bag.
v5|手伝う|てつだう|tetsudau|to help|हाथ बँटाना|母を手伝います。|Haha o tetsudaimasu.|I help my mother.
v5|終わる|おわる|owaru|to end|खत्म होना|授業が終わります。|Jugyou ga owarimasu.|Class ends.
v5|始まる|はじまる|hajimaru|to begin|शुरू होना|映画が始まります。|Eiga ga hajimarimasu.|The movie begins.
n|経験|けいけん|keiken|experience|अनुभव|いい経験でした。|Ii keiken deshita.|It was a good experience.
n|予定|よてい|yotei|plan / schedule|योजना|明日の予定は何ですか。|Ashita no yotei wa nan desu ka.|What are your plans for tomorrow?
n|意味|いみ|imi|meaning|मतलब|この言葉の意味は何ですか。|Kono kotoba no imi wa nan desu ka.|What does this word mean?
n|理由|りゆう|riyuu|reason|कारण|理由を教えてください。|Riyuu o oshiete kudasai.|Please tell me the reason.
n|試験|しけん|shiken|exam|परीक्षा|来週、試験があります。|Raishuu, shiken ga arimasu.|I have an exam next week.
n|会議|かいぎ|kaigi|meeting|बैठक|会議は三時からです。|Kaigi wa sanji kara desu.|The meeting is from three.
n|質問|しつもん|shitsumon|question|सवाल|質問がありますか。|Shitsumon ga arimasu ka.|Do you have any questions?
n|意見|いけん|iken|opinion|राय|あなたの意見を聞きたいです。|Anata no iken o kikitai desu.|I want to hear your opinion.
n|習慣|しゅうかん|shuukan|habit|आदत|早く起きる習慣があります。|Hayaku okiru shuukan ga arimasu.|I have a habit of waking up early.
adji|難しい|むずかしい|muzukashii|difficult|मुश्किल|日本語は難しいです。|Nihongo wa muzukashii desu.|Japanese is difficult.
adji|易しい|やさしい|yasashii|easy|आसान|この問題は易しいです。|Kono mondai wa yasashii desu.|This problem is easy.
adji|危ない|あぶない|abunai|dangerous|ख़तरनाक|この道は危ないです。|Kono michi wa abunai desu.|This road is dangerous.
adji|嬉しい|うれしい|ureshii|happy|खुश|合格して嬉しいです。|Goukaku shite ureshii desu.|I'm happy I passed.
adjna|大切|たいせつ|taisetsu|important|अहम / प्रिय|家族は大切です。|Kazoku wa taisetsu desu.|Family is important.
adjna|大変|たいへん|taihen|tough / serious|कठिन|仕事は大変です。|Shigoto wa taihen desu.|Work is tough.
adjna|便利|べんり|benri|convenient|सुविधाजनक|このアプリは便利です。|Kono apuri wa benri desu.|This app is convenient.
x|たぶん|たぶん|tabun|probably|शायद|たぶん行きます。|Tabun ikimasu.|I'll probably go.
x|必ず|かならず|kanarazu|without fail|ज़रूर|必ず来てください。|Kanarazu kite kudasai.|Please come without fail.
"""

    private val n3Words = """
vs|解決する|かいけつする|kaiketsu suru|to solve|समाधान करना|問題を解決します。|Mondai o kaiketsu shimasu.|I solve the problem.
v1|認める|みとめる|mitomeru|to admit / recognize|स्वीकार करना|ミスを認めます。|Misu o mitomemasu.|I admit the mistake.
v5|守る|まもる|mamoru|to protect / obey|रक्षा करना / पालन करना|ルールを守ります。|Ruuru o mamorimasu.|I follow the rules.
vs|比較する|ひかくする|hikaku suru|to compare|तुलना करना|値段を比較します。|Nedan o hikaku shimasu.|I compare prices.
vs|参加する|さんかする|sanka suru|to participate|हिस्सा लेना|会議に参加します。|Kaigi ni sanka shimasu.|I take part in the meeting.
vs|紹介する|しょうかいする|shoukai suru|to introduce|परिचय कराना|友達を紹介します。|Tomodachi o shoukai shimasu.|I introduce a friend.
vs|説明する|せつめいする|setsumei suru|to explain|समझाना|ルールを説明します。|Ruuru o setsumei shimasu.|I explain the rules.
v1|伝える|つたえる|tsutaeru|to convey|पहुँचाना / बताना|気持ちを伝えます。|Kimochi o tsutaemasu.|I convey my feelings.
v1|集める|あつめる|atsumeru|to collect|इकट्ठा करना|情報を集めます。|Jouhou o atsumemasu.|I collect information.
v5|変わる|かわる|kawaru|to change|बदलना|天気が変わります。|Tenki ga kawarimasu.|The weather changes.
v1|育てる|そだてる|sodateru|to raise / grow|पालना / उगाना|花を育てます。|Hana o sodatemasu.|I grow flowers.
n|影響|えいきょう|eikyou|influence|प्रभाव|天気の影響を受けます。|Tenki no eikyou o ukemasu.|I am affected by the weather.
n|努力|どりょく|doryoku|effort|प्रयास|努力は大切です。|Doryoku wa taisetsu desu.|Effort is important.
n|情報|じょうほう|jouhou|information|जानकारी|情報が足りません。|Jouhou ga tarimasen.|The information is not enough.
n|社会|しゃかい|shakai|society|समाज|社会は変わっています。|Shakai wa kawatte imasu.|Society is changing.
n|文化|ぶんか|bunka|culture|संस्कृति|日本の文化が好きです。|Nihon no bunka ga suki desu.|I like Japanese culture.
n|目的|もくてき|mokuteki|purpose|उद्देश्य|目的は何ですか。|Mokuteki wa nan desu ka.|What is the purpose?
n|結果|けっか|kekka|result|नतीजा|結果を待ちます。|Kekka o machimasu.|I wait for the result.
n|関係|かんけい|kankei|relationship|संबंध|二人は関係があります。|Futari wa kankei ga arimasu.|The two are related.
n|環境|かんきょう|kankyou|environment|पर्यावरण|環境を守りましょう。|Kankyou o mamorimashou.|Let's protect the environment.
adji|厳しい|きびしい|kibishii|strict|सख़्त|先生は厳しいです。|Sensei wa kibishii desu.|The teacher is strict.
adji|珍しい|めずらしい|mezurashii|rare|दुर्लभ|珍しい花ですね。|Mezurashii hana desu ne.|It's a rare flower.
adjna|必要|ひつよう|hitsuyou|necessary|ज़रूरी|お金が必要です。|Okane ga hitsuyou desu.|Money is necessary.
adjna|自然|しぜん|shizen|natural|प्राकृतिक|自然な日本語です。|Shizen na Nihongo desu.|It is natural Japanese.
adjna|正確|せいかく|seikaku|accurate|सटीक|正確な時間を教えて。|Seikaku na jikan o oshiete.|Tell me the exact time.
x|ほとんど|ほとんど|hotondo|almost all|लगभग सारा|ほとんど終わりました。|Hotondo owarimashita.|It's almost finished.
x|実は|じつは|jitsu wa|actually|असल में|実は、日本語が苦手です。|Jitsu wa, Nihongo ga nigate desu.|Actually, I'm not good at Japanese.
"""

    private val n2Words = """
vs|把握する|はあくする|haaku suru|to grasp|पूरी तरह समझना|状況を把握します。|Joukyou o haaku shimasu.|I grasp the situation.
vs|妥協する|だきょうする|dakyou suru|to compromise|समझौता करना|品質は妥協できません。|Hinshitsu wa dakyou dekimasen.|We can't compromise on quality.
vs|貢献する|こうけんする|kouken suru|to contribute|योगदान देना|社会に貢献します。|Shakai ni kouken shimasu.|I contribute to society.
vs|克服する|こくふくする|kokufuku suru|to overcome|काबू पाना|弱点を克服します。|Jakuten o kokufuku shimasu.|I overcome my weakness.
vs|主張する|しゅちょうする|shuchou suru|to insist / assert|ज़ोर देना|自分の意見を主張します。|Jibun no iken o shuchou shimasu.|I assert my opinion.
vs|維持する|いじする|iji suru|to maintain|बनाए रखना|健康を維持します。|Kenkou o iji shimasu.|I maintain my health.
vs|検討する|けんとうする|kentou suru|to consider|विचार करना|案を検討します。|An o kentou shimasu.|I consider the proposal.
v1|抱える|かかえる|kakaeru|to hold / be burdened with|संभालना / बोझ उठाना|問題を抱えています。|Mondai o kakaete imasu.|I am dealing with a problem.
v1|求める|もとめる|motomeru|to seek|माँगना / तलाशना|助けを求めます。|Tasuke o motomemasu.|I ask for help.
v5|招く|まねく|maneku|to invite|आमंत्रित करना|友達を招きます。|Tomodachi o manekimasu.|I invite a friend.
n|矛盾|むじゅん|mujun|contradiction|विरोधाभास|話に矛盾があります。|Hanashi ni mujun ga arimasu.|There is a contradiction in the story.
n|傾向|けいこう|keikou|tendency / trend|रुझान|増える傾向にあります。|Fueru keikou ni arimasu.|There is a trend of increasing.
n|課題|かだい|kadai|task / issue|चुनौती / असाइनमेंट|課題が多いです。|Kadai ga ooi desu.|There are many issues.
n|責任|せきにん|sekinin|responsibility|ज़िम्मेदारी|責任を取ります。|Sekinin o torimasu.|I take responsibility.
n|成果|せいか|seika|achievement|उपलब्धि|努力の成果が出ました。|Doryoku no seika ga demashita.|My effort paid off.
n|条件|じょうけん|jouken|condition|शर्त|条件があります。|Jouken ga arimasu.|There are conditions.
n|状況|じょうきょう|joukyou|situation|हालात|状況は変わりました。|Joukyou wa kawarimashita.|The situation changed.
adji|著しい|いちじるしい|ichijirushii|remarkable|उल्लेखनीय|著しい成長ですね。|Ichijirushii seichou desu ne.|It's remarkable growth.
adji|鋭い|するどい|surudoi|sharp|तीक्ष्ण|鋭い意見です。|Surudoi iken desu.|It's a sharp opinion.
adjna|適切|てきせつ|tekisetsu|appropriate|उचित|適切な方法です。|Tekisetsu na houhou desu.|It's an appropriate method.
adjna|明らか|あきらか|akiraka|obvious|स्पष्ट|結果は明らかです。|Kekka wa akiraka desu.|The result is obvious.
adjna|深刻|しんこく|shinkoku|serious|गंभीर|深刻な問題です。|Shinkoku na mondai desu.|It's a serious problem.
x|あくまで|あくまで|akumade|to the end / only|आख़िर तक / केवल|あくまで例です。|Akumade rei desu.|It's just an example.
x|むしろ|むしろ|mushiro|rather|बल्कि|むしろ便利です。|Mushiro benri desu.|It's rather convenient.
x|大幅に|おおはばに|oohaba ni|drastically|काफ़ी हद तक|大幅に増えました。|Oohaba ni fuemashita.|It increased drastically.
"""

    private val n1Words = """
adjna|不可欠|ふかけつ|fukaketsu|indispensable|अनिवार्य|学習は不可欠です。|Gakushuu wa fukaketsu desu.|Learning is indispensable.
adjna|画期的|かっきてき|kakkiteki|groundbreaking|क्रांतिकारी|画期的な方法です。|Kakkiteki na houhou desu.|It's a groundbreaking method.
adjna|顕著|けんちょ|kencho|striking / marked|स्पष्ट रूप से दिखने वाला|顕著な違いがあります。|Kencho na chigai ga arimasu.|There's a striking difference.
adjna|斬新|ざんしん|zanshin|novel|नवीन|斬新なアイデアです。|Zanshin na aidea desu.|It's a novel idea.
adjna|曖昧|あいまい|aimai|ambiguous|अस्पष्ट|曖昧な答えです。|Aimai na kotae desu.|It's an ambiguous answer.
adji|煩わしい|わずらわしい|wazurawashii|annoying|झंझट भरा|手続きが煩わしいです。|Tetsuzuki ga wazurawashii desu.|The procedure is a hassle.
vs|網羅する|もうらする|moura suru|to cover completely|पूरा कवर करना|全体を網羅します。|Zentai o moura shimasu.|It covers everything.
vs|凌駕する|りょうがする|ryouga suru|to surpass|पीछे छोड़ना|予想を凌駕します。|Yosou o ryouga shimasu.|It surpasses expectations.
vs|促進する|そくしんする|sokushin suru|to promote|बढ़ावा देना|成長を促進します。|Seichou o sokushin shimasu.|It promotes growth.
vs|遂行する|すいこうする|suikou suru|to carry out|पूरा करना|任務を遂行します。|Ninmu o suikou shimasu.|I carry out the mission.
v5|覆す|くつがえす|kutsugaesu|to overturn|पलट देना|常識を覆します。|Joushiki o kutsugaeshimasu.|It overturns common sense.
v1|妨げる|さまたげる|samatageru|to hinder|बाधा डालना|進歩を妨げます。|Shinpo o samatagemasu.|It hinders progress.
n|試行錯誤|しこうさくご|shikou sakugo|trial and error|प्रयास और भूल|試行錯誤を重ねます。|Shikou sakugo o kasanemasu.|I go through trial and error.
n|懸念|けねん|kenen|concern|चिंता / आशंका|懸念が残ります。|Kenen ga nokorimasu.|Concerns remain.
n|概念|がいねん|gainen|concept|अवधारणा|新しい概念を学びます。|Atarashii gainen o manabimasu.|I learn a new concept.
n|姿勢|しせい|shisei|attitude / posture|रवैया / मुद्रा|前向きな姿勢が大切です。|Maemuki na shisei ga taisetsu desu.|A positive attitude is important.
n|妥当性|だとうせい|datousei|validity|वैधता|結果の妥当性を調べます。|Kekka no datousei o shirabemasu.|I examine the validity of the results.
x|あえて|あえて|aete|dare to / deliberately|जानबूझकर|あえて言います。|Aete iimasu.|I'll say it deliberately.
x|依然として|いぜんとして|izen toshite|still|अभी भी|依然として問題です。|Izen toshite mondai desu.|It is still a problem.
x|極めて|きわめて|kiwamete|extremely|अत्यंत|極めて重要です。|Kiwamete juuyou desu.|It is extremely important.
x|概して|がいして|gaishite|generally|आमतौर पर|概して良好です。|Gaishite ryoukou desu.|Overall, it is good.
"""

    val vocab: List<VocabItem> by lazy {
        parseWords(JlptLevel.N5, n5Words) +
            parseWords(JlptLevel.N4, n4Words) +
            parseWords(JlptLevel.N3, n3Words) +
            parseWords(JlptLevel.N2, n2Words) +
            parseWords(JlptLevel.N1, n1Words)
    }

    private val wordsByLevel: Map<JlptLevel, List<VocabItem>> by lazy { vocab.groupBy { it.level } }

    fun words(level: JlptLevel): List<VocabItem> = wordsByLevel[level].orEmpty()

    // ---------------------------------------------------------------
    // SENTENCES (level|polite_jp|romaji|english|hindi|casual_jp|polite_note)
    // ---------------------------------------------------------------
    private val sentenceData = """
N5|はじめまして。|Hajimemashite.|Nice to meet you.|पहली बार मिल रहे हैं, नमस्ते।|はじめまして！よろしくね。|First meeting greeting
N5|私の名前はリシです。|Watashi no namae wa Rishi desu.|My name is Rishi.|मेरा नाम ऋषि है।|俺はリシだよ。 / 私はリシ。|Polite self-introduction vs Casual
N5|インドから来ました。|Indo kara kimashita.|I came from India.|मैं भारत से आया हूँ।|インドから来たよ。|Polite past vs Plain past
N5|これはいくらですか。|Kore wa ikura desu ka.|How much is this?|यह कितने का है?|これ、いくら？|Shopping essential
N5|トイレはどこですか。|Toire wa doko desu ka.|Where is the toilet?|टॉयलेट कहाँ है?|トイレどこ？|Asking location
N5|水をください。|Mizu o kudasai.|Please give me water.|मुझे पानी दीजिए।|お水ちょうだい。|ください (Polite) vs ちょうだい (Casual)
N5|もう一度言ってください。|Mou ichido itte kudasai.|Please say it once more.|कृपया एक बार और बोलिए।|もう一回言って。|Listening clarification
N5|ゆっくり話してください。|Yukkuri hanashite kudasai.|Please speak slowly.|कृपया धीरे बोलिए।|ゆっくり話して。|Essential for beginners
N5|日本語が少し分かります。|Nihongo ga sukoshi wakarimasu.|I understand a little Japanese.|मैं थोड़ी जापानी समझता हूँ।|日本語がちょっと分かるよ。|少し (Polite) vs ちょっと (Spoken)
N5|駅まで歩いて十分です。|Eki made aruite juppun desu.|It's ten minutes on foot to the station.|स्टेशन तक पैदल दस मिनट लगते हैं।|駅まで歩いて十分だよ。|Time & distance expression
N5|毎朝六時に起きます。|Maiasa rokuji ni okimasu.|I wake up at six every morning.|मैं हर सुबह छह बजे उठता हूँ।|毎朝六時に起きるよ。|Daily routine
N5|明日、友達と映画を見ます。|Ashita, tomodachi to eiga o mimasu.|I will watch a movie with a friend tomorrow.|कल दोस्त के साथ फ़िल्म देखूँगा।|明日、友達と映画見るんだ。|〜んだ adds explanatory tone
N4|日本に行ったことがあります。|Nihon ni itta koto ga arimasu.|I have been to Japan.|मैं जापान जा चुका हूँ।|日本に行ったことあるよ。|Past experience pattern
N4|毎日勉強しなければなりません。|Mainichi benkyou shinakereba narimasen.|I have to study every day.|रोज़ पढ़ना ही पड़ेगा।|毎日勉強しなきゃ！|〜なければなりません → 〜なきゃ in speech
N4|この漢字の読み方を教えてください。|Kono kanji no yomikata o oshiete kudasai.|Please tell me how to read this kanji.|इस कांजी को कैसे पढ़ें, बताइए।|この漢字の読み方教えて。|Asking for help
N4|雨が降りそうです。|Ame ga furisou desu.|It looks like it's going to rain.|बारिश होने वाली लगती है।|雨が降りそうだね。|Visual prediction (〜そう)
N4|友達に手伝ってもらいました。|Tomodachi ni tetsudatte moraimashita.|A friend helped me.|दोस्त से मदद मिली।|友達に手伝ってもらった。|Receiving a favor
N4|窓を開けてもいいですか。|Mado o akete mo ii desu ka.|May I open the window?|क्या मैं खिड़की खोल सकता हूँ?|窓を開けてもいい？|Drops です/か with rising intonation
N4|ここで写真を撮ってはいけません。|Koko de shashin o totte wa ikemasen.|You must not take photos here.|यहाँ फोटो खींचना मना है।|ここで写真撮っちゃダメだよ。|〜てはいけません → 〜ちゃダメ
N4|将来、日本で働きたいです。|Shourai, Nihon de hatarakitai desu.|I want to work in Japan in the future.|भविष्य में जापान में काम करना चाहता हूँ।|将来、日本で働きたいな。|Expressing personal dream
N4|時間があれば、手伝います。|Jikan ga areba, tetsudaimasu.|If I have time, I will help.|समय हुआ तो मदद करूँगा।|時間あったら手伝うよ。|Conditional offer
N4|彼は日本語を話すのが上手です。|Kare wa Nihongo o hanasu no ga jouzu desu.|He is good at speaking Japanese.|वह जापानी बोलने में अच्छा है।|彼、日本語話すの上手だね。|Particles often dropped in casual speech
N3|努力すれば、必ず上手になります。|Doryoku sureba, kanarazu jouzu ni narimasu.|If you try, you will surely improve.|मेहनत करो तो ज़रूर बेहतर हो जाओगे।|努力すれば、絶対うまくなるよ。|上手になる → うまくなる in casual speech
N3|彼が来るかどうか分かりません。|Kare ga kuru ka dou ka wakarimasen.|I don't know whether he will come.|पता नहीं वह आएगा या नहीं।|彼が来るかどうか分かんない。|分からない → 分かんない (slurred)
N3|説明を聞いたのに、よく分かりませんでした。|Setsumei o kiita noni, yoku wakarimasen deshita.|Even though I heard the explanation, I didn't understand.|समझाने के बावजूद ठीक से समझ नहीं आया।|説明聞いたのに、よく分かんなかった。|Contrast with のに
N3|先生のおかげで、試験に合格しました。|Sensei no okage de, shiken ni goukaku shimashita.|Thanks to my teacher, I passed the exam.|शिक्षक की वजह से परीक्षा पास हुआ।|先生のおかげで試験に受かったよ！|合格する (formal) → 受かる (spoken)
N3|忙しいにもかかわらず、来てくれました。|Isogashii ni mo kakawarazu, kite kuremashita.|Despite being busy, he came.|व्यस्त होने के बावजूद वह आया।|忙しいのに、来てくれたんだ。|Formal にもかかわらず vs Spoken のに
N3|情報を集めてから、決めます。|Jouhou o atsumete kara, kimemasu.|I will decide after collecting information.|जानकारी जुटाने के बाद तय करूँगा।|情報を集めてから決めるよ。|Sequential action
N3|この問題は簡単というわけではありません。|Kono mondai wa kantan to iu wake dewa arimasen.|This problem isn't necessarily easy.|ऐसा नहीं कि यह समस्या आसान है।|この問題は簡単ってわけじゃないよ。|というわけではない → ってわけじゃない
N3|約束した以上、守らなければなりません。|Yakusoku shita ijou, mamoranakereba narimasen.|Since I promised, I must keep it.|वादा किया है तो निभाना ही होगा।|約束したからには、守らなきゃ。|Strong obligation
N3|天気予報によると、明日は雨だそうです。|Tenki yohou ni yoru to, ashita wa ame da sou desu.|According to the forecast, it will rain tomorrow.|मौसम पूर्वानुमान के अनुसार कल बारिश होगी।|天気予報だと、明日は雨だって。|〜だそうです → 〜だって (hearsay)
N2|状況を把握した上で、判断します。|Joukyou o haaku shita ue de, handan shimasu.|I will judge after grasping the situation.|हालात समझने के बाद फ़ैसला करूँगा।|状況をちゃんと見てから判断するよ。|Business formal vs Daily spoken
N2|品質に関しては、一切妥協できません。|Hinshitsu ni kanshite wa, issai dakyou dekimasen.|We can't compromise at all on quality.|गुणवत्ता पर कोई समझौता नहीं।|品質については絶対妥協できないね。|に関して (formal) vs について (neutral)
N2|努力に応じて、成果が出ます。|Doryoku ni oujite, seika ga demasu.|Results come in proportion to effort.|मेहनत के अनुसार नतीजे मिलते हैं।|頑張った分だけ結果が出るよ。|Proportional outcome
N2|彼の説明は矛盾だらけです。|Kare no setsumei wa mujun darake desu.|His explanation is full of contradictions.|उसकी बातों में विरोधाभास ही विरोधाभास हैं।|彼の説明、矛盾ばっかりだよ。|だらけ vs ばっかり
N2|忙しくても、運動を続けるべきです。|Isogashikute mo, undou o tsuzukeru beki desu.|Even if busy, you should keep exercising.|व्यस्त हों तो भी कसरत जारी रखनी चाहिए।|忙しくても運動は続けた方がいいよ。|〜べきだ (strong) vs 〜た方がいい (advice)
N2|この計画は実現するとは限りません。|Kono keikaku wa jitsugen suru to wa kagirimasen.|This plan won't necessarily come true.|ज़रूरी नहीं कि यह योजना सफल हो।|この計画、うまくいくとは限らないよ。|Partial negation
N2|調査の結果、問題点が明らかになった。|Chousa no kekka, mondaiten ga akiraka ni natta.|As a result of the survey, the problems became clear.|जाँच के नतीजे में समस्याएँ साफ़ हो गईं।|調べてみたら、問題点がはっきりしたよ。|Written report style
N2|彼女は笑顔を見せつつ、話を続けた。|Kanojo wa egao o misetsutsu, hanashi o tsuzuketa.|She continued talking while smiling.|वह मुस्कुराते हुए बात करती रही।|彼女は笑いながら話し続けた。|つつ (literary) vs ながら (spoken)
N1|予算不足のため、計画の縮小を余儀なくされた。|Yosan busoku no tame, keikaku no shukushou o yoginaku sareta.|Due to a budget shortage, we were forced to scale down the plan.|बजट की कमी से योजना छोटी करनी पड़ी।|予算が足りなくて、計画を小さくするしかなかった。|Formal news/executive style
N1|継続的な学習は成長に不可欠である。|Keizokuteki na gakushuu wa seichou ni fukaketsu de aru.|Continuous learning is essential for growth.|लगातार सीखना विकास के लिए अनिवार्य है।|成長するにはコツコツ勉強し続けるしかないね。|Academic essay style (である)
N1|事実に即して判断すべきだ。|Jijitsu ni sokushite handan subeki da.|We should judge based on facts.|तथ्यों के अनुरूप फ़ैसला करना चाहिए।|事実ベースで判断しなきゃね。|Legal/analytical expression
N1|彼の功績は称賛に値する。|Kare no kouseki wa shousan ni atai suru.|His achievements deserve praise.|उसकी उपलब्धियाँ प्रशंसा के योग्य हैं।|彼のやったことは本当にすごいよ。|High-register praise
N1|忙しさのあまり、食事を忘れてしまった。|Isogashisa no amari, shokuji o wasurete shimatta.|I was so busy that I forgot to eat.|इतना व्यस्त था कि खाना भूल गया।|忙しすぎて、ご飯食べるの忘れちゃった。|〜てしまった → 〜ちゃった
N1|試行錯誤を重ねた末、ようやく完成した。|Shikou sakugo o kasaneta sue, youyaku kansei shita.|After repeated trial and error, it was finally completed.|बार-बार प्रयास के बाद आख़िरकार पूरा हुआ।|いろいろ試して、やっと完成したよ！|末 (formal culmination) vs やっと
N1|彼は謝るどころか、逆に怒り出した。|Kare wa ayamaru dokoro ka, gyaku ni okoridashita.|Far from apologizing, he got angry instead.|माफ़ी माँगना तो दूर, उल्टा गुस्सा हो गया।|彼、謝るどころか逆にキレ始めたよ。|Strong contradiction
N1|この問題は一概には言えない。|Kono mondai wa ichigai ni wa ienai.|You can't generalize about this issue.|इस बारे में एकदम से कुछ नहीं कह सकते।|こればっかりは何とも言えないね。|Nuanced academic stance
"""

    private val levelByLabel: Map<String, JlptLevel> by lazy { JlptLevel.entries.associateBy { it.label } }

    val allSentences: List<SentenceItem> by lazy {
        sentenceData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            SentenceItem(
                level = levelByLabel[p[0]] ?: JlptLevel.N5,
                jp = p[1],
                romaji = p[2],
                en = p[3],
                hi = p[4],
                casualJp = p.getOrElse(5) { p[1] },
                politeNote = p.getOrElse(6) { "Standard Polite (丁寧語)" }
            )
        }
    }

    fun sentences(level: JlptLevel): List<SentenceItem> = allSentences.filter { it.level == level }

    // ---------------------------------------------------------------
    // GRAMMAR (level|title|structure|explanation|hindi tip|example|example romaji|example english|commonMistake)
    // ---------------------------------------------------------------
    private val grammarData = """
N5|Particles: は vs が & を に で|Topic は / Subject が / Place で / Target に|は marks the known topic; が spotlights new or specific info. で is where an action happens; に is movement target or static existence.|は = के बारे में (Topic), が = ही/ने (Focus), で = में (क्रिया का स्थान), に = को/में (लक्ष्य या समय)|私は図書館で本を読みます。誰が来ますか？|Watashi wa toshokan de hon o yomimasu. Dare ga kimasu ka?|I read a book at the library. Who is coming?|Mistake: 図書館に本を読みます ❌ -> Action happens AT the library, so always use で (図書館で)! Question words (誰/何) always take が, never は.
N5|Word order (SOV)|Subject – Object – Verb|The verb always goes at the END of the sentence, exactly like Hindi.|हिन्दी और जापानी का वाक्य क्रम 100% समान है: मैं (私-は) सेब (りんご-を) खाता हूँ (食べます)|私はりんごを食べます。|Watashi wa ringo o tabemasu.|I eat an apple.|Mistake: Putting the verb in the middle like English. Keep the main verb strictly at the very end!
N5|です and ます (Polite Style)|Noun + です / Verb stem + ます|です means is / am / are. ます makes a verb polite for strangers, teachers, and coworkers.|です = है / हूँ, ます = विनम्र क्रिया। दोस्तों के साथ इसे हटाकर Plain form बोलते हैं।|私は学生です。毎日勉強します。|Watashi wa gakusei desu. Mainichi benkyou shimasu.|I am a student. I study every day.|Mistake: Never put です directly after a verb stem (食べです ❌) or after an い-adjective past tense.
N5|〜ています (Ongoing Action & State)|Verb て-form + います|Shows an action happening right now OR a resulting state (like being married or living somewhere).|रहा हूँ / रहा है (अभी चल रहा काम या स्थायी स्थिति)|今、日本語を勉強しています。|Ima, Nihongo o benkyou shite imasu.|I am studying Japanese right now.|Mistake: With motion/change verbs like 行く, 来る, 知る, 〜ています means 'is in the state of' (知っています = I know, NOT I am knowing).
N5|〜たいです (Personal Desire)|Verb ます-stem + たいです|Expresses what YOU (first person) want to do. Conjugates like an い-adjective (たくない = don't want to).|करना चाहता हूँ (सिर्फ अपनी इच्छा बताने के लिए)|日本へ行きたいです。|Nihon e ikitai desu.|I want to go to Japan.|Mistake: Don't use 〜たい for a third person's wish without adding と言っています or がっています.
N4|〜てください / 〜てもいいですか|Verb て-form + ください / もいいですか|Ask for a polite favor (Please do) or ask permission (May I do?).|कृपया ... कीजिए / क्या मैं ... कर सकता हूँ?|ここに名前を書いてください。|Koko ni namae o kaite kudasai.|Please write your name here.|Mistake: Using dictionary form + ください (書くください ❌). Always convert to て-form first!
N4|〜なければならない (Obligation)|Verb ない-stem + ければならない|Expresses duty or necessity. In daily conversation, Japanese speakers shorten this to 〜なきゃ or 〜ないと.|करना ही पड़ेगा (बोलचाल में इसे 〜なきゃ कहते हैं)|毎日勉強しなければなりません。|Mainichi benkyou shinakereba narimasen.|I have to study every day.|Mistake: Forgetting to drop the final い from ない before adding ければ.
N4|〜たことがある (Life Experience)|Verb た-form + ことがある|Talk about whether you have ever had an experience in your life.|जीवन में कभी किया है (... चुका हूँ)|日本に行ったことがあります。|Nihon ni itta koto ga arimasu.|I have been to Japan.|Mistake: Don't use 〜たことがある for something you did yesterday or last week as a routine; it's for life experiences.
N4|〜てあげる / くれる / もらう|Verb て-form + あげる / くれる / もらう|Giving and receiving favors. くれる = someone does it FOR ME. もらう = I receive someone's help.|くれる = दूसरे ने मेरे लिए किया, もらう = मैंने दूसरे से मदद ली|友達が日本語を教えてくれました。|Tomodachi ga Nihongo o oshiete kuremashita.|My friend kindly taught me Japanese.|Mistake: Saying 〜てあげる to a superior sounds boastful/rude. Offer help with 〜ましょうか instead.
N4|〜と思います (Opinion & Conjecture)|Plain form + と思います|Soft, natural way to state an opinion or guess in Japanese without sounding overly blunt.|मुझे लगता है कि ... (अपनी राय विनम्रता से रखना)|明日は雨だと思います。|Ashita wa ame da to omoimasu.|I think it will rain tomorrow.|Mistake: Nouns and な-adjectives MUST take だ before と (雨と思います ❌ -> 雨だと思います ✅).
N3|〜のに (Frustrated Contrast)|Plain form + のに (Noun/な-adj + なのに)|Shows unexpected contrast, regret, or frustration ('Even though X, surprisingly Y happened').|... के बावजूद (जब नतीजा उम्मीद के उलट हो)|一生懸命勉強したのに、忘れました。|Isshoukenmei benkyou shita noni, wasuremashita.|Even though I studied hard, I forgot.|Mistake: Nouns need な before のに (雨のに ❌ -> 雨なのに ✅). Also don't use commands/requests after のに.
N3|〜おかげで vs 〜せいで|Plain/Noun の + おかげで (good) / せいで (bad)|おかげで credits a positive outcome ('thanks to'); せいで blames a negative result ('because of').|की कृपा/वजह से (अच्छा परिणाम) vs के कारण (बुरा परिणाम/दोष)|先生のおかげで、試験に合格しました。|Sensei no okage de, shiken ni goukaku shimashita.|Thanks to my teacher, I passed the exam.|Mistake: Using おかげで for a bad result sounds sarcastic! Use せいで for negative outcomes.
N3|〜わけではない (Partial Denial)|Plain form + わけではない|Softly denies an extreme assumption ('It doesn't necessarily mean that...').|ऐसा बिल्कुल नहीं है कि ... (पूरी तरह नकारने के बजाय आंशिक सुधार)|高いものが全部いいわけではない。|Takai mono ga zenbu ii wake dewa nai.|Not everything expensive is necesariamente good.|Mistake: Don't confuse with わけがない, which means 'There is zero chance / Impossible!'.
N3|〜そうだ (Hearsay vs Appearance)|Plain form + そうだ (Hearsay) / Stem + そうだ (Looks like)|Full plain form + そうだ means 'I heard that...'. Verb stem + そうだ means 'It looks like it's about to...'.|सुना है कि (Plain + そうだ) vs लगता है कि होने वाला है (Stem + そうだ)|天気予報によると、明日は雨だそうです。|Tenki yohou ni yoru to, ashita wa ame da sou desu.|According to the forecast, I heard it will rain tomorrow.|Mistake: 雨が降るそうだ (I heard it will rain) vs 雨が降りそうだ (Looking at the sky, it's about to rain!).
N3|〜ために (Purpose & Benefit)|Verb Dictionary Form + ために / Noun の + ために|Expresses clear, intentional purpose ('in order to achieve X').|... के उद्देश्य से / के लिए|N1に合格するために、毎日勉強します。|N1 ni goukaku suru tame ni, mainichi benkyou shimasu.|I study every day in order to pass N1.|Mistake: Use Dictionary Form (する), not ます-stem, before ために.
N2|〜ざるを得ない (Compelled To)|Verb ない-stem + ざるを得ない (する → せざるを得ない)|Formal expression meaning you have no choice but to do something despite reluctance.|मजबूरी में करना ही पड़ना (कोई दूसरा चारा न होना)|事情により、計画を変更せざるを得ません。|Jijou ni yori, keikaku o henkou sezaru o emasen.|Due to circumstances, we have no choice but to change the plan.|Mistake: する becomes せざるを得ない (NOT しざるを得ない), and 来る becomes こざるを得ない.
N2|〜に違いない (High Certainty)|Plain form + に違いない|Strong conviction based on intuition or evidence ('It must definitely be the case that...').|पक्का यही बात होगी / कोई शक नहीं|彼はもう家に帰ったに違いない。|Kare wa mou ie ni kaetta ni chigainai.|He must have already gone home.|Mistake: Unlike と思います, Nouns/な-adjectives do NOT take だ before に違いない (本当だに違いない ❌ -> 本当に違いない ✅).
N2|〜に関して (Formal 'Regarding')|Noun + に関して / に関しては / に関する + Noun|Business and academic equivalent of について ('concerning / with regard to').|... के संबंध में (औपचारिक और बिज़नेस जापानी)|品質に関しては一切妥協できない。|Hinshitsu ni kanshite wa issai dakyou dekinai.|We cannot compromise at all regarding quality.|Mistake: When modifying a noun directly, use に関する + Noun (この問題に関する質問).
N2|〜つつ (Simultaneous / Literary Contrast)|Verb ます-stem + つつ (も)|Literary counterpart to ながら. With も (つつも), it means 'while knowing X, still doing Y'.|करते हुए / जानते हुए भी (साहित्यिक और औपचारिक)|体に悪いと知りつつも、夜更かしをしてしまう。|Karada ni warui to shiritsutsu mo, yofukashi o shite shimau.|While knowing it's bad for my health, I end up staying up late.|Mistake: Attach つつ to the ます-stem (知りつつ), never the dictionary form.
N2|Keigo Mastery: 尊敬語 vs 謙譲語|お + Stem + になる (Respect) vs お + Stem + する (Humble)|Sonkeigo (尊敬語) elevates the listener/client; Kenjougo (謙譲語) humbles your own actions toward them.|आदरसूचक (बॉस/ग्राहक के लिए: お〜になる) vs विनम्र (अपने कार्य के लिए: お〜する)|先生はもうお帰りになりました。私がお持ちします。|Sensei wa mou okaeri ni narimashita. Watashi ga omochi shimasu.|The teacher has gone home. I will humbly carry that for you.|Mistake: Never use お〜になる for yourself, and never use お〜する for what your boss/client does!
N1|〜を余儀なくされる (Forced by Circumstances)|Noun + を余儀なくされる|High-level formal structure used in news and business when external pressure forces a difficult decision.|परिस्थितियों के कारण बाध्य होना पड़ना|予算不足のため、計画の縮小を余儀なくされた。|Yosan busoku no tame, keikaku no shukushou o yoginaku sareta.|Due to budget shortages, we were forced to scale back the plan.|Mistake: Only takes action/change Nouns + を (中止、変更、縮小、辞任).
N1|〜に即して (Strictly In Accordance With)|Noun + に即して / に即した + Noun|Adhering closely to facts, data, rules, or reality without bias.|तथ्यों या नियमों के सख़्त अनुरूप|事実に即して客観的に判断すべきだ。|Jijitsu ni sokushite kyakkanteki ni handan subeki da.|We should judge objectively based on the facts.|Mistake: Used with abstract standards like 事実 (facts), 現実 (reality), 規則 (rules), 法律 (law).
N1|〜ずにはいられない (Uncontrollable Urge)|Verb ない-stem + ずにはいられない (する → せずには)|When an emotion or natural urge is so strong you cannot stop yourself.|किए बिना खुद को रोक न पाना|その映画を見ると、泣かずにはいられない。|Sono eiga o miru to, nakazu ni wa irarenai.|Whenever I watch that movie, I can't help crying.|Mistake: Remember する → せずにはいられない.
N1|〜ものの (Formal Concession)|Plain form + ものの|Formal written 'although/even though', often used when acknowledging a fact before stating a limitation.|हालाँकि ... फिर भी (औपचारिक लेखन)|車を買ったものの、めったに運転していない。|Kuruma o katta mono no, metta ni unten shite inai.|Although I bought a car, I rarely drive it.|Mistake: Nouns take であるものの (学生であるものの), while な-adjectives take なものの.
N1|〜に値する (Worthy Of)|Noun / Verb Dictionary Form + に値する|Indicates that something genuinely merits praise, attention, or criticism.|... के पूर्णतः योग्य होना|彼の長年の研究は称賛に値する。|Kare no naganen no kenkyuu wa shousan ni atai suru.|His many years of research are worthy of praise.|Mistake: Negative form is 〜に値しない (not even worth mentioning/doing).
"""

    val allGrammar: List<GrammarPoint> by lazy {
        grammarData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            fun f(i: Int) = p.getOrElse(i) { "" }
            GrammarPoint(
                level = levelByLabel[f(0)] ?: JlptLevel.N5,
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

    fun grammar(level: JlptLevel): List<GrammarPoint> = allGrammar.filter { it.level == level }

    // ---------------------------------------------------------------
    // KANJI WRITING & STROKE ORDER DECK (level|kanji|onyomi|kunyomi|en|hi|strokes|radical|mnemonic|samples)
    // ---------------------------------------------------------------
    private val kanjiData = """
N5|日|ニチ、ジツ|ひ、-び、-か|Sun, Day|सूर्य / दिन|4|日 (Sun)|A window showing the bright sun in the sky (खिड़की से दिखता सूरज).|日本 (にほん), 毎日 (まいにち), 今日 (きょう)
N5|月|ゲツ、ガツ|つき|Moon, Month|चाँद / महीना|4|月 (Moon)|A crescent moon with two horizontal clouds across it (दूज का चाँद).|月曜日 (げつようび), 今月 (こんげつ), つき (Moon)
N5|水|スイ|みず|Water|पानी|4|水 (Water)|A flowing river current in the center with splashes on both sides (बहती नदी की धारा).|水 (みず), 水曜日 (すいようび), 水泳 (すいえい)
N5|火|カ|ひ、-び|Fire|आग|4|火 (Fire)|A campfire with two sparks flying off the sides (जलती हुई आग और चिंगारी).|火曜日 (かようび), 花火 (はなび), 火 (ひ)
N5|木|モク、ボク|き、こ-|Tree, Wood|पेड़ / लकड़ी|4|木 (Tree)|A trunk, horizontal branches, and roots spreading underground (तना, शाखाएं और जड़ें).|木曜日 (もくようび), 木 (き), 木材 (もくざい)
N5|金|キン、コン|かね、かな-|Gold, Money, Metal|सोना / पैसा|8|金 (Metal)|Gold nuggets buried safely underneath a mountain roof (छत के नीचे दबा सोना).|お金 (おかね), 金曜日 (きんようび), 金メダル (Gold medal)
N5|土|ド、ト|つち|Soil, Earth, Ground|मिट्टी / ज़मीन|3|土 (Earth)|A plant sprout rising up out of the flat ground layer (ज़मीन से निकलता पौधा).|土曜日 (どようび), 土 (つち), 土地 (とち)
N5|人|ジン、ニン|ひと|Person, Human|इंसान / व्यक्ति|2|人 (Person)|Two legs walking forward supporting each other (दो पैरों पर चलता इंसान).|日本人 (にほんじん), 人 (ひと), 三人 (さんにん)
N5|食|ショク|た-べる、く-う|Eat, Food|खाना / भोजन|9|食 (Eat)|A good (良) meal served under a roof (छत के नीचे अच्छा भोजन).|食べる (たべる), 食事 (しょくじ), 朝食 (ちょうしょく)
N5|語|ゴ|かた-る|Language, Word|भाषा / शब्द|14|言 (Speech)|Words (言) spoken by five (五) mouths (口) conversing together.|日本語 (にほんご), 英語 (えいご), 物語 (ものがたり)
N4|強|キョウ、ゴウ|つよ-い、し-いる|Strong, Force|मज़बूत / शक्तिशाली|11|弓 (Bow)|A heavy bow (弓) that gives giant strength.|勉強 (べんきょう), 強い (つよい), 強調 (きょうちょう)
N4|験|ケン|ため-す|Test, Examine, Verify|परीक्षा / परख|18|馬 (Horse)|Testing a swift horse (馬) thoroughly before a journey.|試験 (しけん), 経験 (けいけん), 実験 (じっけん)
N4|思|シ|おも-う|Think, Feel|सोचना / विचार करना|9|心 (Heart)|Your brain/field (田) working above your heart (心).|思う (おもう), 思い出 (おもいで), 思想 (しそう)
N4|意|イ|—|Idea, Mind, Meaning|अर्थ / इरादा|13|心 (Heart)|The sound (音) resonating inside the heart (心).|意味 (いみ), 意見 (いけん), 注意 (ちゅうい)
N4|動|ドウ|うご-く、うご-かす|Move, Motion|हिलना / गति|11|力 (Power)|Applying heavy (重) power (力) to make something move.|運動 (うんどう), 動く (うごく), 自動 (じどう)
N3|努|ド|つと-める|Toil, Effort, Endeavor|प्रयास / मेहनत|7|力 (Power)|A person (奴) exerting full power (力) to succeed.|努力 (どりょく), 努める (つとめる)
N3|解|カイ、ゲ|と-く、と-ける|Untie, Solve, Understand|सुलझाना / हल करना|13|角 (Horn)|Using a knife (刀) to separate a bull's horn (角) — untangling a hard problem.|解決 (かいけつ), 理解 (りかい), 解く (とく)
N3|情|ジョウ|なさ-け|Emotion, Condition, Info|भावना / जानकारी|11|忄 (Heart)|A pure blue/green (青) feeling in the heart (忄).|情報 (じょうほう), 感情 (かんじょう), 事情 (じじょう)
N3|環|カン|わ|Ring, Surround, Environment|चक्र / पर्यावरण|17|王 (Jewel)|A jade ring encircling and protecting the world around us.|環境 (かんきょう), 循環 (じゅんかん)
N2|把|ハ|と-る|Grasp, Hold, Bundle|पकड़ना / समझना|7|扌 (Hand)|Using the hand (扌) to firmly grip the target (巴).|把握 (はあく), 把手 (とって)
N2|妥|ダ|—|Gentle, Compromise, Appropriate|समझौता / उचित|7|女 (Woman)|A hand/claw (爫) calming down a situation peacefully (女).|妥協 (だきょう), 妥当 (だとう)
N2|矛|ム|ほこ|Halberd, Spear, Contradiction|भाला / विरोधाभास|5|矛 (Halberd)|An ancient spear from the story of the unstoppable spear vs. unbreakable shield (矛盾).|矛盾 (むじゅん)
N2|貢|コウ|みつ-ぐ|Tribute, Contribute|योगदान देना|10|貝 (Shell/Money)|Work/craft (工) combined with valuable shells/treasure (貝).|貢献 (こうけん), 年貢 (ねんぐ)
N1|網|モウ|あみ|Net, Network, Cover All|जाल / पूरा कवर करना|14|糸 (Thread)|Fine threads (糸) woven into a comprehensive net (罔).|網羅 (もうら), 通信網 (つうしんもう)
N1|凌|リョウ|しの-ぐ|Endure, Surpass, Outdo|पार करना / पीछे छोड़ना|10|冫 (Ice)|Rising above icy obstacles (冫) to reach a high peak (夌).|凌駕 (りょうが), 凌ぐ (しのぐ)
N1|曖|アイ|—|Dark, Unclear, Ambiguous|अस्पष्ट / धुँधला|17|日 (Sun)|The sun (日) hidden behind clouds of love/emotion (愛), making vision blurry.|曖昧 (あいまい)
N1|懸|ケン|か-ける、か-かる|Suspend, Hang, Worry|लटकना / चिंता करना|20|心 (Heart)|A state/prefecture (県) hanging anxiously over the heart (心).|懸念 (けねん), 一生懸命 (いっしょうけんめい)
"""

    val allKanji: List<KanjiItem> by lazy {
        kanjiData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            KanjiItem(
                level = levelByLabel[p[0]] ?: JlptLevel.N5,
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

    fun kanji(level: JlptLevel): List<KanjiItem> = allKanji.filter { it.level == level }

    // ---------------------------------------------------------------
    // SPOKEN CONTRACTIONS LAB (Real Native Listening Decoder)
    // ---------------------------------------------------------------
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
            formalForm = "〜なければならない",
            casualSpoken = "〜なきゃ / 〜ないと",
            romaji = "-nakereba naranai → -nakya",
            explanationHi = "लम्बा 'करना ही पड़ेगा' फॉर्म बोलचाल में बहुत छोटा होकर सिर्फ 〜なきゃ या 〜ないと रह जाता है।",
            exFormal = "もう帰らなければなりません。",
            exCasual = "もう帰らなきゃ！",
            exMeaning = "I have got to go home now!"
        ),
        SpokenContraction(
            formalForm = "〜ておく / 〜でおく",
            casualSpoken = "〜とく / 〜どく",
            romaji = "-te oku → -toku",
            explanationHi = "भविष्य की तैयारी के लिए कोई काम पहले से करके रखना (कर रखूँगा)।",
            exFormal = "資料を読んでおきます。",
            exCasual = "資料、読んどくね。",
            exMeaning = "I'll read the document in advance."
        ),
        SpokenContraction(
            formalForm = "〜では / 〜ては",
            casualSpoken = "〜じゃ / 〜ちゃ",
            romaji = "-de wa → -ja / -te wa → -cha",
            explanationHi = "मनाही या नेगेटिव वाक्यों में 'ては' → 'ちゃ' और 'では' → 'じゃ' बन जाता है।",
            exFormal = "ここで寝てはいけません。",
            exCasual = "ここで寝ちゃダメだよ。",
            exMeaning = "You mustn't sleep here."
        ),
        SpokenContraction(
            formalForm = "〜という / 〜のだ",
            casualSpoken = "〜って / 〜んだ",
            romaji = "-to iu → -tte / -no da → -nda",
            explanationHi = "किसी बात को कोट करने या कारण बताने के लिए 'という' सिर्फ 'って' बन जाता है।",
            exFormal = "明日は休みだということです。",
            exCasual = "明日は休みだって！",
            exMeaning = "They say tomorrow is a holiday!"
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

    // ---------------------------------------------------------------
    // INTERACTIVE SPEAKING & LISTENING ROLEPLAY SCENARIOS
    // ---------------------------------------------------------------
    val roleplayScenarios: List<RoleplayScenario> = listOf(
        RoleplayScenario(
            id = "rp_n5_ramen",
            level = JlptLevel.N5,
            title = "Ordering at a Tokyo Ramen Shop",
            setting = "Shinjuku Ramen Counter (दिनेश/ऋषि रेस्टोरेंट में ऑर्डर करते हुए)",
            politenessLevel = "Polite Customer (丁寧語)",
            lines = listOf(
                DialogueLine("Staff", false, "いらっしゃいませ！何名様ですか？", "いらっしゃいませ なにめいさまですか", "Irasshaimase! Nan-mei-sama desu ka?", "Welcome! How many people?", "स्वागत है! आप कितने लोग हैं?"),
                DialogueLine("You", true, "一人です。お願いします。", "ひとりです おねがいします", "Hitori desu. Onegaishimasu.", "Just one person, please.", "मैं अकेला हूँ। कृपया सीट दीजिए।"),
                DialogueLine("Staff", false, "ご注文はお決まりですか？", "ごちゅうもんはおきまりですか", "Go-chuumon wa okimari desu ka?", "Have you decided on your order?", "क्या आपने ऑर्डर तय कर लिया है?"),
                DialogueLine("You", true, "みそラーメンと水をください。", "みそらーめんとみずをください", "Miso raamen to mizu o kudasai.", "Miso ramen and water, please.", "मिसो रामेन और पानी दीजिए।")
            )
        ),
        RoleplayScenario(
            id = "rp_n4_station",
            level = JlptLevel.N4,
            title = "Asking Directions at Kyoto Station",
            setting = "Kyoto JR Station Platform (रास्ता पूछना)",
            politenessLevel = "Polite Stranger (丁寧語)",
            lines = listOf(
                DialogueLine("You", true, "すみません、京都駅へ行きたいんですが。", "すみません きょうとえきへいきたいんですが", "Sumimasen, Kyouto-eki e ikitai n desu ga.", "Excuse me, I'd like to go to Kyoto Station...", "माफ़ कीजिए, मुझे क्योटो स्टेशन जाना है..."),
                DialogueLine("Local", false, "ああ、次の電車に乗れば、十分ぐらいで着きますよ。", "ああ つぎのでんしゃにのれば じゅっぷんぐらいでつきますよ", "Aa, tsugi no densha ni noreba, juppun gurai de tsukimasu yo.", "Ah, if you take the next train, you'll arrive in about 10 minutes.", "अरे, अगली ट्रेन पकड़ेंगे तो 10 मिनट में पहुँच जाएंगे।"),
                DialogueLine("You", true, "ありがとうございます。何番線ですか？", "ありがとうございます なんばんせんですか", "Arigatou gozaimasu. Nan-bansen desu ka?", "Thank you very much. Which platform number is it?", "बहुत धन्यवाद। यह किस प्लेटफ़ॉर्म नंबर पर है?"),
                DialogueLine("Local", false, "三番線ですよ。気をつけてね！", "さんばんせんですよ きをつけてね", "San-bansen desu yo. Ki o tsukete ne!", "It's Platform 3. Take care!", "प्लेटफ़ॉर्म 3 पर है। ध्यान से जाइएगा!")
            )
        ),
        RoleplayScenario(
            id = "rp_n3_standup",
            level = JlptLevel.N3,
            title = "Software Engineering Daily Standup",
            setting = "Tokyo Tech Office / Remote Call (प्रोजेक्ट अपडेट देना)",
            politenessLevel = "Workplace Polite (です・ます)",
            lines = listOf(
                DialogueLine("Tech Lead", false, "お疲れ様です。今日の進捗はどうですか？", "おつかれさまです きょうのしんちょくはどうですか", "Otsukaresama desu. Kyou no shinchoku wa dou desu ka?", "Good work today. How is today's progress?", "नमस्ते। आज काम की प्रगति कैसी है?"),
                DialogueLine("You", true, "原因を調べた結果、バグを解決できました。", "げんいんをしらべたけっか ばぐをかいけつできました", "Gen'in o shirabeta kekka, bagu o kaiketsu dekimashita.", "As a result of investigating the cause, I was able to solve the bug.", "कारण की जाँच के बाद मैं बग ठीक करने में सफल रहा।"),
                DialogueLine("Tech Lead", false, "素晴らしいですね！テストも終わりましたか？", "すばらしいですね てすともおわりましたか", "Subarashii desu ne! Tesuto mo owarimashita ka?", "That's wonderful! Has testing finished too?", "शानदार! क्या टेस्टिंग भी पूरी हो गई?"),
                DialogueLine("You", true, "はい、これからチームに説明します。", "はい これからちーむにせつめいします", "Hai, kore kara chiimu ni setsumei shimasu.", "Yes, I will explain it to the team from now.", "जी हाँ, अब मैं टीम को समझाने जा रहा हूँ।")
            )
        ),
        RoleplayScenario(
            id = "rp_n2_keigo",
            level = JlptLevel.N2,
            title = "Executive Client Proposal (Business Keigo)",
            setting = "Corporate Meeting Room (बिज़नेस मीटिंग और केइगो)",
            politenessLevel = "Formal Keigo (尊敬語・謙譲語)",
            lines = listOf(
                DialogueLine("Client", false, "今回の課題について、どうお考えですか？", "こんかいのかだいについて どうおかんがえですか", "Konkai no kadai ni tsuite, dou okangae desu ka?", "What are your thoughts regarding the current issue?", "इस बार की चुनौती के बारे में आपका क्या विचार है?"),
                DialogueLine("You", true, "状況を把握した上で、新しい案を検討しております。", "じょうきょうをはあくしたうえで あたらしいあんをけんとうしております", "Joukyou o haaku shita ue de, atarashii an o kentou shite orimasu.", "After grasping the situation, we are humbly considering a new proposal.", "हालात को पूरी तरह समझने के बाद हम एक नए प्रस्ताव पर विचार कर रहे हैं।"),
                DialogueLine("Client", false, "品質に関しては妥協できませんよ。", "ひんしつにかんしてはだきょうできませんよ", "Hinshitsu ni kanshite wa dakyou dekimasen yo.", "We cannot compromise when it comes to quality.", "गुणवत्ता के मामले में हम समझौता नहीं कर सकते।"),
                DialogueLine("You", true, "承知いたしました。責任を持って対応いたします。", "しょうちいたしました せきにんをもってたいおういたします", "Shouchi itashimashita. Sekinin o motte taiou itashimasu.", "Certainly, I understand. We will handle it with full responsibility.", "जी बिल्कुल समझ गया। हम पूरी ज़िम्मेदारी के साथ इसे संभालेंगे।")
            )
        ),
        RoleplayScenario(
            id = "rp_n1_research",
            level = JlptLevel.N1,
            title = "University Graduate Research Defense",
            setting = "Kyoto / Hokkaido University Lab Seminar (रिसर्च प्रेजेंटेशन)",
            politenessLevel = "Academic Formal (論文・学会調)",
            lines = listOf(
                DialogueLine("Professor", false, "このアルゴリズムの妥当性について説明してください。", "このあるごりずむのだとうせいについてせつめいしてください", "Kono arugorizumu no datousei ni tsuite setsumei shite kudasai.", "Please explain the validity of this algorithm.", "कृपया इस एल्गोरिदम की वैधता (validity) को स्पष्ट करें।"),
                DialogueLine("You", true, "試行錯誤を重ねた末、従来を凌駕する精度を達成しました。", "しこうさくごをかさねたすえ じゅうらいをりょうがするせいどをたっせいしました", "Shikou sakugo o kasaneta sue, juurai o ryouga suru seido o tassei shimashita.", "After repeated trial and error, we achieved accuracy surpassing conventional methods.", "बार-बार प्रयास के बाद हमने पारंपरिक तरीकों से कहीं बेहतर सटीकता हासिल की।"),
                DialogueLine("Professor", false, "計算コストの懸念はありませんか？", "けいさんこすとのけねんはありませんか", "Keisan kosuto no kenen wa arimasen ka?", "Are there no concerns regarding computational cost?", "क्या इसमें कंप्यूटेशनल लागत (CPU cost) की कोई चिंता नहीं है?"),
                DialogueLine("You", true, "事実に即して検証し、極めて軽量であることを確認しました。", "じじつにそくしてけんしょうし きわめてけいりょうであることをかくにんしました", "Jijitsu ni sokushite kenshou shi, kiwamete keiryou de aru koto o kakunin shimashita.", "We verified it strictly against empirical data and confirmed it is extremely lightweight.", "हमने डेटा के अनुरूप इसकी जाँच की और पुष्टि की कि यह अत्यंत हल्का (lightweight) है।")
            )
        )
    )

    // ---------------------------------------------------------------
    // PARTICLE & NUANCE DRILL BANK (For QuizMode.PARTICLE)
    // ---------------------------------------------------------------
    private data class ParticleDrill(
        val level: JlptLevel,
        val sentenceWithBlank: String,
        val options: List<String>,
        val correctParticle: String,
        val explanation: String,
        val fullAudio: String
    )

    private val particleDrills: List<ParticleDrill> = listOf(
        ParticleDrill(
            JlptLevel.N5,
            "私 ＿ 図書館で本を読みます。",
            listOf("は", "を", "に", "で"),
            "は",
            "は (wa) marks the main topic ('As for me...').",
            "私は図書館で本を読みます。"
        ),
        ParticleDrill(
            JlptLevel.N5,
            "毎朝、水 ＿ 飲みます。",
            listOf("を", "に", "で", "へ"),
            "を",
            "を (o) marks the direct object of a transitive verb (पानी को पीता हूँ).",
            "毎朝、水を飲みます。"
        ),
        ParticleDrill(
            JlptLevel.N5,
            "図書館 ＿ 勉強します。",
            listOf("で", "に", "を", "へ"),
            "で",
            "で (de) marks the location where an active action takes place (पुस्तकालय में पढ़ाई).",
            "図書館で勉強します。"
        ),
        ParticleDrill(
            JlptLevel.N5,
            "明日、六時 ＿ 起きます。",
            listOf("に", "で", "を", "から"),
            "に",
            "に (ni) marks a specific clock time (छह बजे पर).",
            "明日、六時に起きます。"
        ),
        ParticleDrill(
            JlptLevel.N5,
            "日本語 ＿ 分かります。",
            listOf("が", "を", "に", "で"),
            "が",
            "分かる (understand), 好き (like), and 上手 (skilled) always take が, never を!",
            "日本語が分かります。"
        ),
        ParticleDrill(
            JlptLevel.N4,
            "友達 ＿ 手伝ってもらいました。",
            listOf("に", "を", "で", "が"),
            "に",
            "With 〜てもらう, the person from whom you receive the favor is marked with に.",
            "友達に手伝ってもらいました。"
        ),
        ParticleDrill(
            JlptLevel.N4,
            "電車 ＿ 乗って会社へ行きます。",
            listOf("に", "を", "で", "と"),
            "に",
            "乗る (to board/ride) and 入る (to enter) always take target particle に (電車に乗る).",
            "電車に乗って会社へ行きます。"
        ),
        ParticleDrill(
            JlptLevel.N3,
            "説明を聞いた ＿ 、よく分かりませんでした。",
            listOf("のに", "から", "ので", "ため"),
            "のに",
            "のに expresses frustrated contrast ('Even though I heard the explanation, I didn't get it').",
            "説明を聞いたのに、よく分かりませんでした。"
        ),
        ParticleDrill(
            JlptLevel.N3,
            "先生 ＿ おかげで、試験に合格しました。",
            listOf("の", "に", "を", "が"),
            "の",
            "Nouns connect to おかげで / せいで / ために using the particle の.",
            "先生のおかげで、試験に合格しました。"
        ),
        ParticleDrill(
            JlptLevel.N2,
            "品質 ＿ 関しては、一切妥協できません。",
            listOf("に", "を", "で", "と"),
            "に",
            "〜に関して (regarding/concerning) is always formed with particle に.",
            "品質に関しては、一切妥協できません。"
        ),
        ParticleDrill(
            JlptLevel.N1,
            "事実 ＿ 即して判断すべきだ。",
            listOf("に", "を", "で", "が"),
            "に",
            "〜に即して means 'strictly in accordance with facts/rules'.",
            "事実に即して判断すべきだ。"
        ),
        ParticleDrill(
            JlptLevel.N1,
            "計画の縮小 ＿ 余儀なくされた。",
            listOf("を", "に", "が", "で"),
            "を",
            "〜を余儀なくされる always takes direct object particle を after a change/action noun.",
            "計画の縮小を余儀なくされた。"
        )
    )

    // ---------------------------------------------------------------
    // 6-MODE PRO QUIZ GENERATOR
    // ---------------------------------------------------------------
    fun makeQuiz(level: JlptLevel?, mode: QuizMode, count: Int = 10): List<QuizQuestion> {
        val pool = if (level == null) vocab else words(level).ifEmpty { vocab }
        val sentPool = if (level == null) allSentences else sentences(level).ifEmpty { allSentences }

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
                val wrong = pool.map { "${it.en} (${it.hi})" }.distinct().filter { it != "${w.en} (${w.hi})" }.shuffled().take(3)
                val target = "${w.en} (${w.hi})"
                val opts = (wrong + target).shuffled()
                QuizQuestion(
                    instruction = "What is the meaning of this ${w.level.label} word?",
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
                        instruction = "Select the correct Hiragana reading & pitch pattern",
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
                val drills = (if (level == null) particleDrills else particleDrills.filter { it.level == level })
                    .ifEmpty { particleDrills }
                drills.shuffled().take(count).map { d ->
                    val opts = d.options.shuffled()
                    QuizQuestion(
                        instruction = "Fill in the blank (＿) with the most natural Japanese particle",
                        big = d.sentenceWithBlank,
                        options = opts,
                        correctIndex = opts.indexOf(d.correctParticle),
                        explanation = "Correct: ${d.correctParticle}\n${d.explanation}\nFull Sentence: ${d.fullAudio}",
                        audio = d.fullAudio,
                        hint = "Level: ${d.level.label} Grammar Nuance"
                    )
                }
            }
        }
    }
}
