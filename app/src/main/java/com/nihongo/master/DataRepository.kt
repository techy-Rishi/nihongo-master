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
    // WORDS  (type|kanji|kana|romaji|english|hindi|example|example romaji|example english)
    // ---------------------------------------------------------------
    private val wordTypeByCode: Map<String, WordType> by lazy { WordType.entries.associateBy { it.code } }

    private fun parseWords(level: JlptLevel, raw: String): List<VocabItem> {
        return raw.trim().lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
            val p = line.split("|").map { it.trim() }
            fun f(i: Int) = p.getOrElse(i) { "" }
            VocabItem(
                id = "${level.label}_${idx + 1}",
                level = level,
                type = wordTypeByCode[f(0)] ?: WordType.OTHER,
                kanji = f(1), kana = f(2), romaji = f(3), en = f(4), hi = f(5),
                exJp = f(6), exRomaji = f(7), exEn = f(8)
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
n|雨|あめ|ame|rain|बारिश|雨が降っています。|Ame ga futte imasu.|It is raining.
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
    // SENTENCES  (level|japanese|romaji|english|hindi)
    // ---------------------------------------------------------------
    private val sentenceData = """
N5|はじめまして。|Hajimemashite.|Nice to meet you.|पहली बार मिल रहे हैं, नमस्ते।
N5|私の名前はリシです。|Watashi no namae wa Rishi desu.|My name is Rishi.|मेरा नाम ऋषि है।
N5|インドから来ました。|Indo kara kimashita.|I came from India.|मैं भारत से आया हूँ।
N5|これはいくらですか。|Kore wa ikura desu ka.|How much is this?|यह कितने का है?
N5|トイレはどこですか。|Toire wa doko desu ka.|Where is the toilet?|टॉयलेट कहाँ है?
N5|水をください。|Mizu o kudasai.|Please give me water.|मुझे पानी दीजिए।
N5|もう一度言ってください。|Mou ichido itte kudasai.|Please say it once more.|कृपया एक बार और बोलिए।
N5|ゆっくり話してください。|Yukkuri hanashite kudasai.|Please speak slowly.|कृपया धीरे बोलिए।
N5|日本語が少し分かります。|Nihongo ga sukoshi wakarimasu.|I understand a little Japanese.|मैं थोड़ी जापानी समझता हूँ।
N5|駅まで歩いて十分です。|Eki made aruite juppun desu.|It's ten minutes on foot to the station.|स्टेशन तक पैदल दस मिनट लगते हैं।
N5|毎朝六時に起きます。|Maiasa rokuji ni okimasu.|I wake up at six every morning.|मैं हर सुबह छह बजे उठता हूँ।
N5|明日、友達と映画を見ます。|Ashita, tomodachi to eiga o mimasu.|I will watch a movie with a friend tomorrow.|कल दोस्त के साथ फ़िल्म देखूँगा।
N4|日本に行ったことがあります。|Nihon ni itta koto ga arimasu.|I have been to Japan.|मैं जापान जा चुका हूँ।
N4|毎日勉強しなければなりません。|Mainichi benkyou shinakereba narimasen.|I have to study every day.|रोज़ पढ़ना ही पड़ेगा।
N4|この漢字の読み方を教えてください。|Kono kanji no yomikata o oshiete kudasai.|Please tell me how to read this kanji.|इस कांजी को कैसे पढ़ें, बताइए।
N4|雨が降りそうです。|Ame ga furisou desu.|It looks like it's going to rain.|बारिश होने वाली लगती है।
N4|友達に手伝ってもらいました。|Tomodachi ni tetsudatte moraimashita.|A friend helped me.|दोस्त से मदद मिली।
N4|窓を開けてもいいですか。|Mado o akete mo ii desu ka.|May I open the window?|क्या मैं खिड़की खोल सकता हूँ?
N4|ここで写真を撮ってはいけません。|Koko de shashin o totte wa ikemasen.|You must not take photos here.|यहाँ फोटो खींचना मना है।
N4|将来、日本で働きたいです。|Shourai, Nihon de hatarakitai desu.|I want to work in Japan in the future.|भविष्य में जापान में काम करना चाहता हूँ।
N4|時間があれば、手伝います。|Jikan ga areba, tetsudaimasu.|If I have time, I will help.|समय हुआ तो मदद करूँगा।
N4|彼は日本語を話すのが上手です。|Kare wa Nihongo o hanasu no ga jouzu desu.|He is good at speaking Japanese.|वह जापानी बोलने में अच्छा है।
N3|努力すれば、必ず上手になります。|Doryoku sureba, kanarazu jouzu ni narimasu.|If you try, you will surely improve.|मेहनत करो तो ज़रूर बेहतर हो जाओगे।
N3|彼が来るかどうか分かりません。|Kare ga kuru ka dou ka wakarimasen.|I don't know whether he will come.|पता नहीं वह आएगा या नहीं।
N3|説明を聞いたのに、よく分かりませんでした。|Setsumei o kiita noni, yoku wakarimasen deshita.|Even though I heard the explanation, I didn't understand.|समझाने के बावजूद ठीक से समझ नहीं आया।
N3|先生のおかげで、試験に合格しました。|Sensei no okage de, shiken ni goukaku shimashita.|Thanks to my teacher, I passed the exam.|शिक्षक की वजह से परीक्षा पास हुआ।
N3|忙しいにもかかわらず、来てくれました。|Isogashii ni mo kakawarazu, kite kuremashita.|Despite being busy, he came.|व्यस्त होने के बावजूद वह आया।
N3|情報を集めてから、決めます。|Jouhou o atsumete kara, kimemasu.|I will decide after collecting information.|जानकारी जुटाने के बाद तय करूँगा।
N3|この問題は簡単というわけではありません。|Kono mondai wa kantan to iu wake dewa arimasen.|This problem isn't necessarily easy.|ऐसा नहीं कि यह समस्या आसान है।
N3|約束した以上、守らなければなりません。|Yakusoku shita ijou, mamoranakereba narimasen.|Since I promised, I must keep it.|वादा किया है तो निभाना ही होगा।
N3|天気予報によると、明日は雨だそうです。|Tenki yohou ni yoru to, ashita wa ame da sou desu.|According to the forecast, it will rain tomorrow.|मौसम पूर्वानुमान के अनुसार कल बारिश होगी।
N2|状況を把握した上で、判断します。|Joukyou o haaku shita ue de, handan shimasu.|I will judge after grasping the situation.|हालात समझने के बाद फ़ैसला करूँगा।
N2|品質に関しては、一切妥協できません。|Hinshitsu ni kanshite wa, issai dakyou dekimasen.|We can't compromise at all on quality.|गुणवत्ता पर कोई समझौता नहीं।
N2|努力に応じて、成果が出ます。|Doryoku ni oujite, seika ga demasu.|Results come in proportion to effort.|मेहनत के अनुसार नतीजे मिलते हैं।
N2|彼の説明は矛盾だらけです。|Kare no setsumei wa mujun darake desu.|His explanation is full of contradictions.|उसकी बातों में विरोधाभास ही विरोधाभास हैं।
N2|忙しくても、運動を続けるべきです。|Isogashikute mo, undou o tsuzukeru beki desu.|Even if busy, you should keep exercising.|व्यस्त हों तो भी कसरत जारी रखनी चाहिए।
N2|この計画は実現するとは限りません。|Kono keikaku wa jitsugen suru to wa kagirimasen.|This plan won't necessarily come true.|ज़रूरी नहीं कि यह योजना सफल हो।
N2|調査の結果、問題点が明らかになった。|Chousa no kekka, mondaiten ga akiraka ni natta.|As a result of the survey, the problems became clear.|जाँच के नतीजे में समस्याएँ साफ़ हो गईं।
N2|彼女は笑顔を見せつつ、話を続けた。|Kanojo wa egao o misetsutsu, hanashi o tsuzuketa.|She continued talking while smiling.|वह मुस्कुराते हुए बात करती रही।
N1|予算不足のため、計画の縮小を余儀なくされた。|Yosan busoku no tame, keikaku no shukushou o yoginaku sareta.|Due to a budget shortage, we were forced to scale down the plan.|बजट की कमी से योजना छोटी करनी पड़ी।
N1|継続的な学習は成長に不可欠である。|Keizokuteki na gakushuu wa seichou ni fukaketsu de aru.|Continuous learning is essential for growth.|लगातार सीखना विकास के लिए अनिवार्य है।
N1|事実に即して判断すべきだ。|Jijitsu ni sokushite handan subeki da.|We should judge based on facts.|तथ्यों के अनुरूप फ़ैसला करना चाहिए।
N1|彼の功績は称賛に値する。|Kare no kouseki wa shousan ni atai suru.|His achievements deserve praise.|उसकी उपलब्धियाँ प्रशंसा के योग्य हैं।
N1|忙しさのあまり、食事を忘れてしまった。|Isogashisa no amari, shokuji o wasurete shimatta.|I was so busy that I forgot to eat.|इतना व्यस्त था कि खाना भूल गया।
N1|試行錯誤を重ねた末、ようやく完成した。|Shikou sakugo o kasaneta sue, youyaku kansei shita.|After repeated trial and error, it was finally completed.|बार-बार प्रयास के बाद आख़िरकार पूरा हुआ।
N1|彼は謝るどころか、逆に怒り出した。|Kare wa ayamaru dokoro ka, gyaku ni okoridashita.|Far from apologizing, he got angry instead.|माफ़ी माँगना तो दूर, उल्टा गुस्सा हो गया।
N1|この問題は一概には言えない。|Kono mondai wa ichigai ni wa ienai.|You can't generalize about this issue.|इस बारे में एकदम से कुछ नहीं कह सकते।
"""

    private val levelByLabel: Map<String, JlptLevel> by lazy { JlptLevel.entries.associateBy { it.label } }

    private val allSentences: List<SentenceItem> by lazy {
        sentenceData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            SentenceItem(levelByLabel[p[0]] ?: JlptLevel.N5, p[1], p[2], p[3], p[4])
        }
    }

    fun sentences(level: JlptLevel): List<SentenceItem> = allSentences.filter { it.level == level }

    // ---------------------------------------------------------------
    // GRAMMAR  (level|title|structure|explanation|hindi tip|example|example romaji|example english)
    // ---------------------------------------------------------------
    private val grammarData = """
N5|Particles: は を に で の|Topic は Place で Object を Verb|Particles come AFTER the noun, just like postpositions in Hindi.|は = तो / के बारे में, を = को, に = में / पर (target), で = में / से (place or tool), の = का / की / के|私は図書館で本を読みます。|Watashi wa toshokan de hon o yomimasu.|I read a book at the library.
N5|Word order (SOV)|Subject – Object – Verb|The verb always goes at the END of the sentence, exactly like Hindi.|मैं सेब खाता हूँ = 私は りんごを 食べます|私はりんごを食べます。|Watashi wa ringo o tabemasu.|I eat an apple.
N5|です and ます (polite style)|Noun + です / Verb stem + ます|です means is / am / are. ます makes a verb polite.|です = है / हूँ, ます = polite verb ending|私は学生です。|Watashi wa gakusei desu.|I am a student.
N5|〜ています (doing now)|Verb て-form + います|Shows an action happening now or a continuing state.|रहा हूँ / रहा है (खा रहा हूँ)|今、勉強しています。|Ima, benkyou shite imasu.|I am studying now.
N5|〜たいです (want to)|Verb stem + たいです|Say what you want to do.|करना चाहता हूँ|日本へ行きたいです。|Nihon e ikitai desu.|I want to go to Japan.
N4|〜てください / 〜てもいいですか|Verb て-form + ください|Ask for a favor or ask permission.|कृपया ... कीजिए / क्या मैं ... सकता हूँ|ここに名前を書いてください。|Koko ni namae o kaite kudasai.|Please write your name here.
N4|〜なければならない (must)|Verb ない-form (without い) + ければならない|Express an obligation.|करना ही पड़ेगा|毎日勉強しなければなりません。|Mainichi benkyou shinakereba narimasen.|I have to study every day.
N4|〜たことがある (have done)|Verb た-form + ことがある|Talk about past experience.|... चुका हूँ|日本に行ったことがあります。|Nihon ni itta koto ga arimasu.|I have been to Japan.
N4|〜てあげる / くれる / もらう|Verb て-form + あげる / くれる / もらう|Giving and receiving favors. くれる always points toward ME.|くれる = मेरे लिए किया, もらう = मुझे मिला|友達が教えてくれました。|Tomodachi ga oshiete kuremashita.|My friend taught me (kindly).
N4|〜と思います (I think)|Plain form + と思います|Soft way to give an opinion.|मुझे लगता है कि ...|明日は雨だと思います。|Ashita wa ame da to omoimasu.|I think it will rain tomorrow.
N3|〜のに (even though)|Plain form + のに|Shows contrast or surprise.|... के बावजूद|勉強したのに、忘れました。|Benkyou shita noni, wasuremashita.|Even though I studied, I forgot.
N3|〜おかげで / 〜せいで|Noun の + おかげで / せいで|おかげで = thanks to (good). せいで = because of (bad).|की वजह से (अच्छा / बुरा)|先生のおかげで、合格しました。|Sensei no okage de, goukaku shimashita.|Thanks to my teacher, I passed.
N3|〜わけではない (not necessarily)|Plain form + わけではない|Partly denies something. Not the same as わけがない (impossible).|ऐसा नहीं कि ...|高いものが全部いいわけではない。|Takai mono ga zenbu ii wake dewa nai.|Not everything expensive is good.
N3|〜そうだ (I heard that)|Plain form + そうだ|Passing on information you heard.|सुना है कि ...|明日は雨だそうです。|Ashita wa ame da sou desu.|I heard it will rain tomorrow.
N3|〜ために (purpose)|Verb dictionary form + ために|Shows purpose: in order to.|... के लिए / ... करने के लिए|合格するために、勉強します。|Goukaku suru tame ni, benkyou shimasu.|I study in order to pass.
N2|〜ざるを得ない (no choice)|Verb ない-stem + ざるを得ない|You have no choice but to do it.|करना ही पड़ेगा (मजबूरी)|行かざるを得ません。|Ikazaru o emasen.|I have no choice but to go.
N2|〜に違いない (must be)|Plain form + に違いない|Strong guess based on evidence.|ज़रूर होगा / होगी|彼は疲れているに違いない。|Kare wa tsukarete iru ni chigainai.|He must be tired.
N2|〜に関して (regarding)|Noun + に関して|Formal way to say about / regarding.|... के बारे में / के संबंध में|品質に関しては妥協できない。|Hinshitsu ni kanshite wa dakyou dekinai.|We can't compromise regarding quality.
N2|〜つつ (while / although)|Verb stem + つつ|Two actions at once, or contrast.|करते हुए / करते हुए भी|悩みつつ、決めました。|Nayami tsutsu, kimemashita.|I decided while worrying.
N2|Keigo: お〜になる / お〜する|お + stem + になる (respect) / する (humble)|Respect raises the other person. Humble lowers yourself.|आप पधारिए (आदर) / मैं करता हूँ (विनम्र)|先生はもうお帰りになりました。|Sensei wa mou okaeri ni narimashita.|The teacher has already gone home.
N1|〜を余儀なくされる (forced to)|Noun + を余儀なくされる|Forced by circumstances. Used in formal writing.|... के लिए बाध्य होना पड़ा|計画の変更を余儀なくされた。|Keikaku no henkou o yoginaku sareta.|We were forced to change the plan.
N1|〜に即して (in line with)|Noun + に即して|Based strictly on facts or rules.|तथ्यों / नियमों के अनुरूप|事実に即して判断する。|Jijitsu ni sokushite handan suru.|Judge based on the facts.
N1|〜ずにはいられない (can't help)|Verb ない-stem + ずにはいられない|You cannot stop yourself from doing it.|किए बिना रह नहीं सकता|笑わずにはいられない。|Warawazu ni wa irarenai.|I can't help laughing.
N1|〜ものの (although)|Plain form + ものの|Formal 'although', result is different from expectation.|हालाँकि ...|買ったものの、使っていない。|Katta mono no, tsukatte inai.|Although I bought it, I don't use it.
N1|〜に値する (worthy of)|Noun + に値する|Deserves or is worth something.|... के योग्य|称賛に値する成果です。|Shousan ni atai suru seika desu.|It's a result worthy of praise.
"""

    private val allGrammar: List<GrammarPoint> by lazy {
        grammarData.trim().lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split("|").map { it.trim() }
            fun f(i: Int) = p.getOrElse(i) { "" }
            GrammarPoint(levelByLabel[f(0)] ?: JlptLevel.N5, f(1), f(2), f(3), f(4), f(5), f(6), f(7))
        }
    }

    fun grammar(level: JlptLevel): List<GrammarPoint> = allGrammar.filter { it.level == level }

    // ---------------------------------------------------------------
    // QUIZ
    // ---------------------------------------------------------------
    fun makeQuiz(level: JlptLevel?, mode: QuizMode, count: Int = 10): List<QuizQuestion> {
        val pool = if (level == null) vocab else words(level)
        return when (mode) {
            QuizMode.KANA -> {
                val kanaPool = allKana.filter { it.group != KanaGroup.COMBO }
                kanaPool.shuffled().take(count).map { k ->
                    val wrong = kanaPool.map { it.romaji }.distinct().filter { it != k.romaji }.shuffled().take(3)
                    val opts = (wrong + k.romaji).shuffled()
                    QuizQuestion(
                        instruction = "What is the sound of this ${if (k.type == KanaType.HIRAGANA) "hiragana" else "katakana"}?",
                        big = k.char,
                        options = opts,
                        correctIndex = opts.indexOf(k.romaji),
                        explanation = "${k.char} is read \"${k.romaji}\".",
                        audio = k.char
                    )
                }
            }
            QuizMode.MEANING -> pool.shuffled().take(count).map { w ->
                val wrong = pool.map { it.en }.distinct().filter { it != w.en }.shuffled().take(3)
                val opts = (wrong + w.en).shuffled()
                QuizQuestion(
                    instruction = "What does this word mean?",
                    big = w.kanji,
                    options = opts,
                    correctIndex = opts.indexOf(w.en),
                    explanation = "${w.kanji} (${w.kana}) = ${w.en} · ${w.hi}",
                    audio = w.kana
                )
            }
            QuizMode.READING -> {
                val kanjiWords = pool.filter { it.kanji != it.kana }
                kanjiWords.shuffled().take(count).map { w ->
                    val wrong = kanjiWords.map { it.kana }.distinct().filter { it != w.kana }.shuffled().take(3)
                    val opts = (wrong + w.kana).shuffled()
                    QuizQuestion(
                        instruction = "How do you read this word?",
                        big = w.kanji,
                        options = opts,
                        correctIndex = opts.indexOf(w.kana),
                        explanation = "${w.kanji} is read ${w.kana} (${w.romaji}) = ${w.en}",
                        audio = w.kana
                    )
                }
            }
            QuizMode.LISTEN -> pool.shuffled().take(count).map { w ->
                val wrong = pool.map { it.en }.distinct().filter { it != w.en }.shuffled().take(3)
                val opts = (wrong + w.en).shuffled()
                QuizQuestion(
                    instruction = "Listen and choose the meaning",
                    big = "🔊",
                    options = opts,
                    correctIndex = opts.indexOf(w.en),
                    explanation = "You heard ${w.kanji} (${w.kana}) = ${w.en} · ${w.hi}",
                    audio = w.kana,
                    autoPlay = true
                )
            }
        }
    }
}
