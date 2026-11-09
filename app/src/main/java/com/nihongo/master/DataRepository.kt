package com.nihongo.master

object DataRepository {

    // Complete 104 Hiragana + 104 Katakana + Confusion Traps = 216 Kana Items
    val allKana: List<KanaChar> by lazy {
        val list = mutableListOf<KanaChar>()

        val gojuonRows = listOf(
            "A-Row" to listOf(Triple("あ", "ア", "a") to "Apple / Axe", Triple("い", "イ", "i") to "Two eels / Easel", Triple("う", "ウ", "u") to "U-curve / Roof", Triple("え", "エ", "e") to "Exotic bird / Elevator", Triple("お", "オ", "o") to "Golf ball / Opera singer"),
            "KA-Row" to listOf(Triple("か", "カ", "ka") to "Cutting blade", Triple("き", "キ", "ki") to "Skeleton Key", Triple("く", "ク", "ku") to "Cuckoo beak / Cook hat", Triple("け", "ケ", "ke") to "Wooden Keg", Triple("こ", "コ", "ko") to "Two Koi fish / Two Corners"),
            "SA-Row" to listOf(Triple("さ", "サ", "sa") to "Sake pourer / Sardines", Triple("し", "シ", "shi") to "Fishhook / Sideways smile", Triple("す", "ス", "su") to "Swing loop / Ski jumper", Triple("せ", "セ", "se") to "Seat bench", Triple("そ", "ソ", "so") to "Zigzag sewing / Needle drop"),
            "TA-Row" to listOf(Triple("た", "タ", "ta") to "Ta cross / Taco", Triple("ち", "チ", "chi") to "Cheerleader 5", Triple("つ", "ツ", "tsu") to "Tsunami wave / Three drops", Triple("て", "テ", "te") to "Telescope / Telephone pole", Triple("と", "ト", "to") to "Toe splinter / Totem pole"),
            "NA-Row" to listOf(Triple("な", "ナ", "na") to "Nun praying / Nutcracker", Triple("に", "ニ", "ni") to "Needle & thread / Two lines", Triple("ぬ", "ヌ", "nu") to "Noodles loop / Chopsticks", Triple("ね", "ネ", "ne") to "Neko cat tail / Necktie", Triple("の", "ノ", "no") to "No-smoking sign / Slash"),
            "HA-Row" to listOf(Triple("は", "ハ", "ha") to "H+a / House roof", Triple("ひ", "ヒ", "hi") to "Smiling 'hee' / Boot heel", Triple("ふ", "フ", "fu") to "Mt. Fuji / Cliff edge", Triple("へ", "ヘ", "he") to "Pointing to Heaven", Triple("ほ", "ホ", "ho") to "House chimney / Holy cross"),
            "MA-Row" to listOf(Triple("ま", "マ", "ma") to "Twin sail mast / Math angle", Triple("み", "ミ", "mi") to "Musical note 21 / 3 Missiles", Triple("む", "ム", "mu") to "Moo cow horn / Muscle triangle", Triple("め", "メ", "me") to "Eye (Me) / Sword scar", Triple("も", "モ", "mo") to "More worms on hook"),
            "YA-Row" to listOf(Triple("や", "ヤ", "ya") to "Yak horns", Triple("ゆ", "ユ", "yu") to "U-turn fish / U-boat periscope", Triple("よ", "ヨ", "yo") to "Yo-yo dangling / Toy blocks"),
            "RA-Row" to listOf(Triple("ら", "ラ", "ra") to "Rabbit / Ramen lid", Triple("り", "リ", "ri") to "River reeds", Triple("る", "ル", "ru") to "Route loop / Tree roots", Triple("れ", "レ", "re") to "Runner kneeling / Red check", Triple("ろ", "ロ", "ro") to "Plain road / Robot square"),
            "WA-Row" to listOf(Triple("わ", "ワ", "wa") to "Wasp belly / Wine glass", Triple("を", "ヲ", "wo") to "Object particle O", Triple("ん", "ン", "n") to "Cursive n / One-eyed wink")
        )

        gojuonRows.forEach { (rowName, chars) ->
            chars.forEach { (triple, hint) ->
                list.add(KanaChar(triple.first, triple.third, KanaType.HIRAGANA, KanaGroup.GOJUON, rowName, hint))
                list.add(KanaChar(triple.second, triple.third, KanaType.KATAKANA, KanaGroup.GOJUON, rowName, hint))
            }
        }

        val dakuonPairs = listOf(
            Triple("が", "ガ", "ga"), Triple("ぎ", "ギ", "gi"), Triple("ぐ", "グ", "gu"), Triple("げ", "ゲ", "ge"), Triple("ご", "ゴ", "go"),
            Triple("ざ", "ザ", "za"), Triple("じ", "ジ", "ji"), Triple("ず", "ズ", "zu"), Triple("ぜ", "ゼ", "ze"), Triple("ぞ", "ゾ", "zo"),
            Triple("だ", "ダ", "da"), Triple("ぢ", "ヂ", "ji (di)"), Triple("づ", "ヅ", "zu (du)"), Triple("で", "デ", "de"), Triple("ど", "ド", "do"),
            Triple("ば", "バ", "ba"), Triple("び", "ビ", "bi"), Triple("ぶ", "ブ", "bu"), Triple("べ", "ベ", "be"), Triple("ぼ", "ボ", "bo"),
            Triple("ぱ", "パ", "pa"), Triple("ぴ", "ピ", "pi"), Triple("ぷ", "プ", "pu"), Triple("ぺ", "ペ", "pe"), Triple("ぽ", "ポ", "po")
        )
        dakuonPairs.forEach { (h, k, rom) ->
            list.add(KanaChar(h, rom, KanaType.HIRAGANA, KanaGroup.DAKUON, "Voiced", "Dakuten / Maru voiced sound"))
            list.add(KanaChar(k, rom, KanaType.KATAKANA, KanaGroup.DAKUON, "Voiced", "Dakuten / Maru voiced sound"))
        }

        val yoonPairs = listOf(
            Triple("きゃ", "キャ", "kya"), Triple("きゅ", "キュ", "kyu"), Triple("きょ", "キョ", "kyo"),
            Triple("しゃ", "シャ", "sha"), Triple("しゅ", "シュ", "shu"), Triple("しょ", "ショ", "sho"),
            Triple("ちゃ", "チャ", "cha"), Triple("ちゅ", "チュ", "chu"), Triple("ちょ", "チョ", "cho"),
            Triple("にゃ", "ニャ", "nya"), Triple("にゅ", "ニュ", "nyu"), Triple("にょ", "ニョ", "nyo"),
            Triple("ひゃ", "ヒャ", "hya"), Triple("ひゅ", "ヒュ", "hyu"), Triple("ひょ", "ヒョ", "hyo"),
            Triple("みゃ", "ミャ", "mya"), Triple("みゅ", "ミュ", "myu"), Triple("みょ", "ミョ", "myo"),
            Triple("りゃ", "リャ", "rya"), Triple("りゅ", "リュ", "ryu"), Triple("りょ", "リョ", "ryo"),
            Triple("ぎゃ", "ギャ", "gya"), Triple("ぎゅ", "ギュ", "gyu"), Triple("ぎょ", "ギョ", "gyo"),
            Triple("じゃ", "ジャ", "ja"), Triple("じゅ", "ジュ", "ju"), Triple("じょ", "ジョ", "jo"),
            Triple("びゃ", "ビャ", "bya"), Triple("びゅ", "ビュ", "byu"), Triple("びょ", "ビョ", "byo"),
            Triple("ぴゃ", "ピャ", "pya"), Triple("ぴゅ", "ピュ", "pyu"), Triple("ぴょ", "ピョ", "pyo")
        )
        yoonPairs.forEach { (h, k, rom) ->
            list.add(KanaChar(h, rom, KanaType.HIRAGANA, KanaGroup.YOON, "Yōon Combo", "Glide palatalized sound"))
            list.add(KanaChar(k, rom, KanaType.KATAKANA, KanaGroup.YOON, "Yōon Combo", "Glide palatalized sound"))
        }

        val traps = listOf(
            KanaChar("シ", "shi", KanaType.KATAKANA, KanaGroup.LOOKALIKES, "Shi vs Tsu", "Horizontal strokes moving UP-RIGHT (like a sideways smiley)"),
            KanaChar("ツ", "tsu", KanaType.KATAKANA, KanaGroup.LOOKALIKES, "Shi vs Tsu", "Vertical ticks + stroke moving DOWN-LEFT (Tsunami drops)"),
            KanaChar("ソ", "so", KanaType.KATAKANA, KanaGroup.LOOKALIKES, "So vs N", "Top tick + downward stroke (Sewing needle)"),
            KanaChar("ン", "n", KanaType.KATAKANA, KanaGroup.LOOKALIKES, "So vs N", "Side tick + upward stroke (One-eyed cyclops)"),
            KanaChar("る", "ru", KanaType.HIRAGANA, KanaGroup.LOOKALIKES, "Ru vs Ro", "Has a loop at the bottom (RU-by loop)"),
            KanaChar("ろ", "ro", KanaType.HIRAGANA, KanaGroup.LOOKALIKES, "Ru vs Ro", "No loop at the bottom (Plain RO-ad)"),
            KanaChar("わ", "wa", KanaType.HIRAGANA, KanaGroup.LOOKALIKES, "Wa vs Re vs Ne", "Round smooth belly on the right"),
            KanaChar("れ", "re", KanaType.HIRAGANA, KanaGroup.LOOKALIKES, "Wa vs Re vs Ne", "Kicks outward to the right"),
            KanaChar("ね", "ne", KanaType.HIRAGANA, KanaGroup.LOOKALIKES, "Wa vs Re vs Ne", "Curls into a loop like a Neko (cat) tail")
        )
        list.addAll(traps)
        list
    }

    val vocabList: List<VocabItem> = listOf(
        // ==================== JLPT N5 ====================
        VocabItem("n5_1", "食べる", "たべる", "taberu", "To eat", "खाना (Khaana)", JlptLevel.N5, "Verb", VerbGroup.ICHIDAN, listOf(0, 1, 0), PitchType.NAKADAKA, "食 (Food/Eat)", "ショク (shoku)", "た-べる (ta-beru)", "A person under a roof with good rice.", "毎朝パンを食べます。", "Maiasa pan o tabemasu.", "I eat bread every morning.", "मैं हर सुबह ब्रेड खाता हूँ।"),
        VocabItem("n5_2", "飲む", "のむ", "nomu", "To drink", "पीना (Peena)", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(1, 0), PitchType.ATAMADAKA, "飠(Food) + 欠 (Yawn/Open mouth)", "イン (in)", "の-む (no-mu)", "Opening your mouth wide beside food/liquid.", "冷たい水を飲む。", "Tsumetai mizu o nomu.", "I drink cold water.", "मैं ठंडा पानी पीता हूँ।"),
        VocabItem("n5_3", "行く", "いく", "iku", "To go", "जाना (Jaana)", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(0, 1), PitchType.HEIBAN, "彳 (Step) + 亍 (Step)", "コウ / ギョウ", "い-く / ゆ-く", "Footsteps at a crossroads.", "明日東京へ行きます。", "Ashita Toukyou e ikimasu.", "I will go to Tokyo tomorrow.", "मैं कल टोक्यो जाऊँगा।"),
        VocabItem("n5_4", "見る", "みる", "miru", "To see / watch", "देखना (Dekhna)", JlptLevel.N5, "Verb", VerbGroup.ICHIDAN, listOf(1, 0), PitchType.ATAMADAKA, "目 (Eye) + 儿 (Legs)", "ケン (ken)", "み-る (mi-ru)", "An eye walking on two legs looking around.", "日本のアニメを見る。", "Nihon no anime o miru.", "I watch Japanese anime.", "मैं जापानी एनीमे देखता हूँ।"),
        VocabItem("n5_5", "話す", "はなす", "hanasu", "To speak / talk", "बात करना / बोलना", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(0, 1, 0), PitchType.NAKADAKA, "言 (Words) + 舌 (Tongue)", "ワ (wa)", "はな-す (hana-su)", "Using your tongue to produce words.", "日本語を話すことができます。", "Nihongo o hanasu koto ga dekimasu.", "I can speak Japanese.", "मैं जापानी बोल सकता हूँ।"),
        VocabItem("n5_6", "聞く", "きく", "kiku", "To listen / ask", "सुनना / पूछना", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(0, 1), PitchType.HEIBAN, "門 (Gate) + 耳 (Ear)", "ブン / モン", "き-く (ki-ku)", "An ear pressed against a gate listening closely.", "音楽を聞いています。", "Ongaku o kiite imasu.", "I am listening to music.", "मैं संगीत सुन रहा हूँ।"),
        VocabItem("n5_7", "読む", "よむ", "yomu", "To read", "पढ़ना (Padhna)", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(1, 0), PitchType.ATAMADAKA, "言 (Words) + 売 (Sell)", "ドク (doku)", "よ-む (yo-mu)", "Reading words aloud like a street vendor.", "毎晩本を読みます。", "Maiban hon o yomimasu.", "I read a book every night.", "मैं हर रात किताब पढ़ता हूँ।"),
        VocabItem("n5_8", "書く", "かく", "kaku", "To write", "लिखना (Likhna)", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(1, 0), PitchType.ATAMADAKA, "聿 (Brush) + 日 (Sun/Day)", "ショ (sho)", "か-く (ka-ku)", "Holding a calligraphy brush to write daily.", "漢字をきれいに書く。", "Kanji o kirei ni kaku.", "Write Kanji neatly.", "कांजी को साफ-सुथरा लिखें।"),
        VocabItem("n5_9", "買う", "かう", "kau", "To buy", "खरीदना (Khareedna)", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(0, 1), PitchType.HEIBAN, "罒 (Net) + 貝 (Shell/Money)", "バイ (bai)", "か-う (ka-u)", "Using shell money to net items at a market.", "新しいパソコンを買った。", "Atarashii pasokon o katta.", "I bought a new laptop.", "मैंने नया लैपटॉप खरीदा।"),
        VocabItem("n5_10", "待つ", "まつ", "matsu", "To wait", "इंतज़ार करना", JlptLevel.N5, "Verb", VerbGroup.GODAN, listOf(1, 0), PitchType.ATAMADAKA, "彳 (Step) + 寺 (Temple)", "タイ (tai)", "ま-つ (ma-tsu)", "Stopping your steps outside a temple to wait.", "ちょっと待ってください。", "Chotto matte kudasai.", "Please wait a moment.", "कृपया थोड़ा इंतज़ार करें।"),
        VocabItem("n5_11", "勉強する", "べんきょうする", "benkyou suru", "To study", "पढ़ाई करना", JlptLevel.N5, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1, 1), PitchType.HEIBAN, "勉 (Exertion) + 強 (Strong)", "ベンキョウ", "-", "Forcing yourself to become strong through study.", "毎日二時間勉強します。", "Mainichi nijikan benkyou shimasu.", "I study two hours every day.", "मैं रोज़ दो घंटे पढ़ाई करता हूँ।"),
        VocabItem("n5_12", "来る", "くる", "kuru", "To come", "आना (Aana)", JlptLevel.N5, "Verb", VerbGroup.IRREGULAR, listOf(1, 0), PitchType.ATAMADAKA, "木 (Tree) + wheat ears", "ライ (rai)", "く-る / き / こ", "Grain ripening as the harvest season comes.", "友達が家に来ました。", "Tomodachi ga ie ni kimashita.", "My friend came to my house.", "मेरा दोस्त मेरे घर आया।"),
        VocabItem("n5_13", "先生", "せんせい", "sensei", "Teacher / Doctor", "शिक्षक / गुरु", JlptLevel.N5, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1, 0), PitchType.NAKADAKA, "先 (Before/Ahead) + 生 (Life)", "センセイ", "-", "One who was born before and guides life.", "先生に質問しました。", "Sensei ni shitsumon shimashita.", "I asked the teacher a question.", "मैंने शिक्षक से एक प्रश्न पूछा।"),
        VocabItem("n5_14", "友達", "ともだち", "tomodachi", "Friend", "दोस्त / मित्र", JlptLevel.N5, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1), PitchType.HEIBAN, "友 (Two hands clasped) + 達 (Plural/Reach)", "ユウ / タツ", "とも", "Two hands working together side by side.", "友達と映画を見に行く。", "Tomodachi to eiga o mi ni iku.", "I go to watch a movie with my friend.", "मैं दोस्त के साथ फिल्म देखने जाता हूँ।"),
        VocabItem("n5_15", "電車", "でんしゃ", "densha", "Electric Train", "ट्रेन / रेलगाड़ी", JlptLevel.N5, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1), PitchType.HEIBAN, "電 (Rain + Lightning) + 車 (Wheel/Car)", "デンシャ", "-", "A wheeled carriage powered by lightning.", "電車で大学に通っています。", "Densha de daigaku ni kayotte imasu.", "I commute to university by train.", "मैं ट्रेन से यूनिवर्सिटी जाता हूँ।"),

        // ==================== JLPT N4 ====================
        VocabItem("n4_1", "続ける", "つづける", "tsuzukeru", "To continue", "जारी रखना (Jaari rakhna)", JlptLevel.N4, "Verb", VerbGroup.ICHIDAN, listOf(0, 1, 1, 1), PitchType.HEIBAN, "糸 (Thread) + 売 (Sell/Connect)", "ゾク (zoku)", "つづ-ける", "Spinning thread continuously without breaking.", "諦めずに努力を続ける。", "Akiramezu ni doryoku o tsuzukeru.", "Continue making efforts without giving up.", "बिना हार माने प्रयास जारी रखें।"),
        VocabItem("n4_2", "決める", "きめる", "kimeru", "To decide", "तय करना / निर्णय लेना", JlptLevel.N4, "Verb", VerbGroup.ICHIDAN, listOf(0, 1, 1), PitchType.HEIBAN, "氵(Water) + 夬 (Break open)", "ケツ (ketsu)", "き-める", "Water breaking through a dam decisively.", "将来の目標を決めた。", "Shourai no mokuhyou o kimeta.", "I decided my future goal.", "मैंने अपने भविष्य का लक्ष्य तय कर लिया।"),
        VocabItem("n4_3", "調べる", "しらべる", "shiraberu", "To investigate / look up", "जाँच करना / खोजना", JlptLevel.N4, "Verb", VerbGroup.ICHIDAN, listOf(0, 1, 1, 0), PitchType.NAKADAKA, "言 (Words) + 周 (Circumference/All around)", "チョウ (chou)", "しら-べる", "Inquiring words all around to check every detail.", "辞書で新しい単語を調べる。", "Jisho de atarashii tango o shiraberu.", "Look up new words in a dictionary.", "शब्दकोश में नए शब्द खोजना।"),
        VocabItem("n4_4", "選ぶ", "えらぶ", "erabu", "To choose / select", "चुनना (Chunna)", JlptLevel.N4, "Verb", VerbGroup.GODAN, listOf(0, 1, 0), PitchType.NAKADAKA, "⻌ (Road) + 巽 (Two people on pedestal)", "セン (sen)", "えら-ぶ", "Selecting candidates to place on a pedestal.", "好きな色を選んでください。", "Suki na iro o erande kudasai.", "Please choose your favorite color.", "कृपया अपना पसंदीदा रंग चुनें।"),
        VocabItem("n4_5", "直す", "なおす", "naosu", "To repair / correct", "ठीक करना / सुधारना", JlptLevel.N4, "Verb", VerbGroup.GODAN, listOf(0, 1, 0), PitchType.NAKADAKA, "十 (Ten) + 目 (Eye) + L (Corner)", "チョク / ジキ", "なお-す", "Looking with ten eyes to straighten a crooked line.", "間違いを赤ペンで直す。", "Machigai o akapen de naosu.", "Correct mistakes with a red pen.", "लाल पेन से गलतियाँ सुधारें।"),
        VocabItem("n4_6", "約束する", "やくそくする", "yakusoku suru", "To promise", "वादा करना (Vaada karna)", JlptLevel.N4, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1, 1), PitchType.HEIBAN, "糸 (Thread) + 束 (Bundle)", "ヤクソク", "-", "Binding threads into a bundle as a firm pledge.", "時間を守ると約束した。", "Jikan o mamoru to yakusoku shita.", "I promised to be punctual.", "मैंने समय का पालन करने का वादा किया।"),
        VocabItem("n4_7", "準備する", "じゅんびする", "junbi suru", "To prepare", "तैयारी करना", JlptLevel.N4, "Verb", VerbGroup.IRREGULAR, listOf(1, 0, 0, 0, 0), PitchType.ATAMADAKA, "準 (Level/Standard) + 備 (Provide/Equip)", "ジュンビ", "-", "Equipping everything up to standard beforehand.", "試験の準備はバッチリだ。", "Shiken no junbi wa batchiri da.", "Preparation for the exam is complete.", "परीक्षा की तैयारी एकदम पक्की है।"),
        VocabItem("n4_8", "経験", "けいけん", "keiken", "Experience", "अनुभव (Anubhav)", JlptLevel.N4, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1), PitchType.HEIBAN, "経 (Pass through warp threads) + 験 (Test/Verify)", "ケイケン", "-", "What you verify by passing through life's tests.", "留学は貴重な経験になった。", "Ryuugaku wa kichou na keiken ni natta.", "Studying abroad became a valuable experience.", "विदेश में पढ़ाई एक बहुमूल्य अनुभव बन गया।"),

        // ==================== JLPT N3 ====================
        VocabItem("n3_1", "解決する", "かいけつする", "kaiketsu suru", "To solve / resolve", "समाधान करना", JlptLevel.N3, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1, 1), PitchType.HEIBAN, "解 (Untie horn with knife) + 決 (Decide)", "カイケツ", "-", "Untying a complex knot decisively.", "技術の力で社会問題を解決する。", "Gijutsu no chikara de shakai mondai o kaiketsu suru.", "Solve social problems with the power of technology.", "तकनीक की शक्ति से सामाजिक समस्याओं का समाधान करना।"),
        VocabItem("n3_2", "認める", "みとめる", "mitomeru", "To recognize / admit / approve", "स्वीकार करना / मान्यता देना", JlptLevel.N3, "Verb", VerbGroup.ICHIDAN, listOf(0, 1, 1, 1), PitchType.HEIBAN, "言 (Words) + 忍 (Endure/Blade over heart)", "ニン (nin)", "みと-める", "Speaking words of acknowledgment from the heart.", "自分のミスを素直に認める。", "Jibun no misu o sunao ni mitomeru.", "Honestly admit one's own mistake.", "अपनी गलती को ईमानदारी से स्वीकार करना।"),
        VocabItem("n3_3", "守る", "まもる", "mamoru", "To protect / obey (rules)", "रक्षा करना / पालन करना", JlptLevel.N3, "Verb", VerbGroup.GODAN, listOf(0, 1, 0), PitchType.NAKADAKA, "宀 (Roof) + 寸 (Inch/Law)", "シュ / ス", "まも-る", "Enforcing the law under a roof to guard the house.", "交通ルールを必ず守る。", "Koutsuu ruuru o kanarazu mamoru.", "Always obey traffic rules.", "यातायात नियमों का हमेशा पालन करें।"),
        VocabItem("n3_4", "影響", "えいきょう", "eikyou", "Influence / Impact", "प्रभाव / असर", JlptLevel.N3, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1), PitchType.HEIBAN, "影 (Shadow) + 響 (Echo/Sound)", "エイキョウ", "-", "Like a shadow and an echo following an action.", "AIは世界経済に大きな影響を与えている。", "AI wa sekai keizai ni ookina eikyou o ataete iru.", "AI is having a huge impact on the global economy.", "AI विश्व अर्थव्यवस्था पर बड़ा प्रभाव डाल रहा है।"),
        VocabItem("n3_5", "努力", "どりょく", "doryoku", "Effort / Hard work", "प्रयास / कड़ी मेहनत", JlptLevel.N3, "Noun", VerbGroup.NOT_VERB, listOf(1, 0, 0), PitchType.ATAMADAKA, "努 (Toil) + 力 (Power/Muscle)", "ドリョク", "-", "Applying all your muscle and toil toward a goal.", "毎日の努力が合格につながる。", "Mainichi no doryoku ga goukaku ni tsunagaru.", "Daily effort leads to passing the exam.", "रोज़ की मेहनत सफलता दिलाती है।"),
        VocabItem("n3_6", "比較する", "ひかくする", "hikaku suru", "To compare", "तुलना करना (Tulna karna)", JlptLevel.N3, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1), PitchType.HEIBAN, "比 (Two people side by side) + 較 (Carriage check)", "ヒカク", "-", "Lining two things up side by side to measure differences.", "二つのアルゴリズムの速度を比較する。", "Futatsu no arugorizumu no sokudo o hikaku suru.", "Compare the speed of two algorithms.", "दो एल्गोरिदम की गति की तुलना करना।"),

        // ==================== JLPT N2 ====================
        VocabItem("n2_1", "把握する", "はあくする", "haaku suru", "To grasp / comprehend fully", "पूरी तरह समझना / पकड़ बनाना", JlptLevel.N2, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1), PitchType.HEIBAN, "扌(Hand) + 巴 (Grip) + 屋 (Room)", "ハアク", "-", "Closing your hand firmly around the core of an issue.", "ユーザーのニーズを正確に把握する。", "Yuuzaa no niizu o seikaku ni haaku suru.", "Accurately grasp the needs of the users.", "यूज़र्स की ज़रूरतों को सटीक रूप से समझना।"),
        VocabItem("n2_2", "妥協する", "だきょうする", "dakyou suru", "To compromise", "समझौता करना", JlptLevel.N2, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1), PitchType.HEIBAN, "妥 (Gentle/Settle) + 協 (Cooperate with 3 strengths)", "ダキョウ", "-", "Settling a dispute by meeting halfway.", "安全性に関しては一切妥協できない。", "Anzensei ni kanshite wa issai dakyou dekinai.", "We cannot compromise at all regarding safety.", "सुरक्षा के मामले में कोई समझौता नहीं किया जा सकता।"),
        VocabItem("n2_3", "矛盾", "むじゅん", "mujun", "Contradiction / Paradox", "विरोधाभास (Virodhaabhaas)", JlptLevel.N2, "Noun", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1), PitchType.HEIBAN, "矛 (Unstoppable Spear) + 盾 (Impenetrable Shield)", "ムジュン", "-", "From the classic Chinese tale of an unstoppable spear vs an unbreakable shield.", "彼の理論には明らかな矛盾がある。", "Kare no riron ni wa akiraka na mujun ga aru.", "There is an obvious contradiction in his theory.", "उनके सिद्धांत में स्पष्ट विरोधाभास है।"),
        VocabItem("n2_4", "貢献する", "こうけんする", "kouken suru", "To contribute", "योगदान देना (Yogdaan dena)", JlptLevel.N2, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1, 1), PitchType.HEIBAN, "貢 (Tribute/Work) + 献 (Offer/Dedicate)", "コウケン", "-", "Offering valuable work to advance society.", "オープンソースにコードを貢献する。", "Oopun soosu ni koudo o kouken suru.", "Contribute code to open source.", "ओपन सोर्स में कोड का योगदान देना।"),
        VocabItem("n2_5", "克服する", "こくふくする", "kokufuku suru", "To overcome / conquer", "विजय पाना / काबू पाना", JlptLevel.N2, "Verb", VerbGroup.IRREGULAR, listOf(0, 1, 1, 1, 1, 1), PitchType.HEIBAN, "克 (Overcome hardship) + 服 (Submit/Subdue)", "コクフク", "-", "Subduing a weakness through perseverance.", "弱点を克服して試験に挑む。", "Jakuten o kokufuku shite shiken ni idomu.", "Overcome weaknesses and challenge the exam.", "कमज़ोरियों पर काबू पाकर परीक्षा का सामना करना।"),

        // ==================== JLPT N1 ====================
        VocabItem("n1_1", "不可欠", "ふかけつ", "fukaketsu", "Indispensable / Essential", "अनिवार्य / अत्यंत आवश्यक", JlptLevel.N1, "Na-Adjective", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1), PitchType.HEIBAN, "不 (Not) + 可 (Possible) + 欠 (Lack)", "フカケツ", "-", "Something that is impossible to lack.", "継続的な学習は成長に不可欠である。", "Keizokuteki na gakushuu wa seichou ni fukaketsu de aru.", "Continuous learning is indispensable for growth.", "विकास के लिए निरंतर सीखना अनिवार्य है।"),
        VocabItem("n1_2", "画期的", "かっきてき", "kakkiteki", "Groundbreaking / Epoch-making", "क्रांतिकारी / अभूतपूर्व", JlptLevel.N1, "Na-Adjective", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1, 1), PitchType.HEIBAN, "画 (Divide/Draw line) + 期 (Era) + 的 (Target)", "カッキテキ", "-", "Drawing a line that marks a brand new era.", "画期的なアルゴリズムを発表した。", "Kakkiteki na arugorizumu o happyou shita.", "Published a groundbreaking algorithm.", "एक क्रांतिकारी एल्गोरिदम प्रकाशित किया।"),
        VocabItem("n1_3", "網羅する", "もうらする", "moura suru", "To encompass exhaustively / cover all", "संपूर्ण रूप से शामिल करना", JlptLevel.N1, "Verb", VerbGroup.IRREGULAR, listOf(1, 0, 0, 0, 0), PitchType.ATAMADAKA, "網 (Fishing net) + 羅 (Bird net)", "モウラ", "-", "Casting both fish nets and bird nets to leave nothing out.", "このアプリはN5からN1まで網羅している。", "Kono apuri wa N5 kara N1 made moura shite iru.", "This app exhaustively covers from N5 to N1.", "यह ऐप N5 से N1 तक सब कुछ संपूर्ण रूप से कवर करता है।"),
        VocabItem("n1_4", "凌駕する", "りょうがする", "ryouga suru", "To surpass / outstrip / eclipse", "पीछे छोड़ देना / श्रेष्ठ साबित होना", JlptLevel.N1, "Verb", VerbGroup.IRREGULAR, listOf(1, 0, 0, 0, 0), PitchType.ATAMADAKA, "凌 (Rise above ice) + 駕 (Ride/Mount horse)", "リョウガ", "-", "Rising far above the competition.", "既存の学習アプリを凌駕する性能。", "Kizon no gakushuu apuri o ryouga suru seinou.", "Performance that surpasses existing learning apps.", "मौजूदा लर्निंग ऐप्स को पीछे छोड़ देने वाली क्षमता।"),
        VocabItem("n1_5", "試行錯誤", "しこうさくご", "shikou sakugo", "Trial and error", "प्रयास और भूल (Yojipukago 4-Kanji Idiom)", JlptLevel.N1, "Yojijukugo", VerbGroup.NOT_VERB, listOf(0, 1, 1, 1, 0, 0), PitchType.NAKADAKA, "試 (Try) + 行 (Act) + 錯 (Mix) + 誤 (Mistake)", "シコウサクゴ", "-", "Trying, acting, making mistakes, and refining until perfection.", "試行錯誤を重ねてシステムを完成させた。", "Shikou sakugo o kasanete shisutemu o kansei saseta.", "Through repeated trial and error, we completed the system.", "बार-बार प्रयास और सुधार करके सिस्टम को पूरा किया।")
    )

    val grammarList: List<GrammarPoint> = listOf(
        GrammarPoint(
            id = "g_n5_1",
            title = "Core Particles Matrix (は・が・を・に・で・の)",
            level = JlptLevel.N5,
            structure = "[Topic] は [Place] で [Object] を [Verb]",
            explanation = "Unlike English prepositions (in, at, to) which come BEFORE nouns, Japanese particles are postpositions that come AFTER nouns—100% identical to Hindi!",
            hindiParallel = "は (wa) = तो/के बारे में | が (ga) = ने | を (o) = को | に (ni) = में/पर/को (लक्ष्य) | で (de) = से/में (साधन या क्रिया का स्थान) | の (no) = का/के/की",
            commonMistake = "Don't use に for active actions at a place! Use で for where an action happens (図書館で勉強する = लाइब्रेरी में पढ़ाई करना).",
            examples = listOf(
                ExampleSentence("私は図書館で日本語の本を読みます。", "Watashi wa toshokan de Nihongo no hon o yomimasu.", "I read a Japanese book at the library.", "मैं (私は) लाइब्रेरी में (図書館で) जापानी की (日本語の) किताब को (本を) पढ़ता हूँ (読みます)।")
            )
        ),
        GrammarPoint(
            id = "g_n5_2",
            title = "〜ている (Ongoing Action vs Resulting State)",
            level = JlptLevel.N5,
            structure = "Verb (Te-form) + いる / います",
            explanation = "For action verbs (eat, read), 〜ている means 'is doing right now'. For instant change-of-state verbs (marry, know, open, die), it means a resulting state ('is married', 'knows').",
            hindiParallel = "रहा है / रही है (खा रहा है) या अवस्था (शादीशुदा है / जानता है)।",
            commonMistake = "Negative of 知っている (I know) is ALWAYS 知っています → 知りません (never 知らないでいる).",
            examples = listOf(
                ExampleSentence("今、コードをテストしています。", "Ima, koudo o tesuto shite imasu.", "I am testing the code right now.", "अभी कोड टेस्ट कर रहा हूँ।"),
                ExampleSentence("田中さんは結婚しています。", "Tanaka-san wa kekkon shite imasu.", "Mr. Tanaka is married.", "तानाका जी शादीशुदा हैं।")
            )
        ),
        GrammarPoint(
            id = "g_n4_1",
            title = "Giving & Receiving Matrix (あげる・くれる・もらう)",
            level = JlptLevel.N4,
            structure = "〜てあげる (Do for others) | 〜てくれる (Others do for me) | 〜てもらう (I receive favor)",
            explanation = "Japanese encodes gratitude and direction into favors: くれる always points INWARD toward you (the speaker), while あげる points OUTWARD.",
            hindiParallel = "〜てくれる = मेरे लिए कर दिया (कृपा) | 〜てもらう = मैंने उनसे करवाया/मदद पाई।",
            commonMistake = "When a friend helps YOU, say 友達が教えてくれた (never あげた)!",
            examples = listOf(
                ExampleSentence("先輩がバグを直してくれました。", "Senpai ga bagu o naoshite kuremashita.", "My senior kindly fixed the bug for me.", "सीनियर ने मेरे लिए बग ठीक कर दिया।")
            )
        ),
        GrammarPoint(
            id = "g_n3_1",
            title = "〜わけだ vs 〜わけではない (Logical Conclusion vs Partial Denial)",
            level = JlptLevel.N3,
            structure = "Plain Form + わけだ / わけではない",
            explanation = "〜わけだ means 'No wonder / It makes logical sense that...'. Adding ではない means 'It doesn't necessarily mean that all X is Y'.",
            hindiParallel = "〜わけだ = तभी तो / इसका मतलब यह हुआ | 〜わけではない = ऐसा भी नहीं है कि...",
            commonMistake = "Don't confuse 〜わけがない (Impossible!) with 〜わけではない (Not necessarily).",
            examples = listOf(
                ExampleSentence("日本に10年住んでいたから、日本語が上手なわけだ。", "Nihon ni juunen sunde ita kara, Nihongo ga jouzu na wake da.", "He lived in Japan for 10 years, so no wonder his Japanese is great!", "वह 10 साल जापान में रहा है, तभी तो उसकी जापानी इतनी अच्छी है।"),
                ExampleSentence("高いものがすべて良いというわけではない。", "Takai mono ga subete yoi to iu wake dewa nai.", "It's not necessarily true that all expensive things are good.", "ऐसा ज़रूरी नहीं कि हर महँगी चीज़ अच्छी ही हो।")
            )
        ),
        GrammarPoint(
            id = "g_n2_1",
            title = "Keigo Master Matrix: Sonkeigo (尊敬語) vs Kenjougo (謙譲語)",
            level = JlptLevel.N2,
            structure = "お + V-stem + になる (Respectful) vs お + V-stem + する (Humble)",
            explanation = "In Japanese business/interviews: Sonkeigo elevates the listener/professor/client. Kenjougo lowers your own actions to show humility.",
            hindiParallel = "Sonkeigo = आप पधारिए / ग्रहण कीजिए (आदरसूचक) | Kenjougo = मैं प्रस्तुत करता हूँ (विनम्रता)।",
            commonMistake = "Special verbs: 行く/来る → いらっしゃる (Respectful) vs 参る/伺う (Humble). 言う → おっしゃる (Respectful) vs 申し上げる (Humble).",
            examples = listOf(
                ExampleSentence("先生はもうお帰りになりました。", "Sensei wa mou okaeri ni narimashita.", "The professor has already returned home (Respectful).", "प्रोफेसर साहब घर प्रस्थान कर चुके हैं।"),
                ExampleSentence("明日、研究室に伺います。", "Ashita, kenkyuushitsu ni ukagaimasu.", "I will humbly visit your laboratory tomorrow (Humble).", "मैं कल आपकी प्रयोगशाला में उपस्थित होऊँगा।")
            )
        ),
        GrammarPoint(
            id = "g_n1_1",
            title = "〜を余儀なくされる & 〜に即して (Formal N1 Expressions)",
            level = JlptLevel.N1,
            structure = "Noun + を余儀なくされる (Compelled to) | Noun + に即して (In accordance with reality)",
            explanation = "High-level academic and news expressions. 〜を余儀なくされる indicates being forced by circumstances. 〜に即して means adhering strictly to facts/rules.",
            hindiParallel = "〜を余儀なくされる = के लिए बाध्य होना पड़ा | 〜に即して = तथ्यों/नियमों के अनुरूप।",
            commonMistake = "〜を余儀なくされる is always used with a negative/reluctant situation (cancellation, retreat, modification).",
            examples = listOf(
                ExampleSentence("予算不足のため、計画の縮小を余儀なくされた。", "Yosan busoku no tame, keikaku no shukushou o yoginaku sareta.", "Due to budget shortages, we were forced to scale down the plan.", "बजट की कमी के कारण योजना को छोटा करने के लिए बाध्य होना पड़ा।")
            )
        )
    )

    val stories: List<ImmersionStory> = listOf(
        ImmersionStory(
            id = "s1",
            titleJp = "コンビニでの買い物",
            titleEn = "Shopping at a Tokyo Convenience Store",
            level = JlptLevel.N5,
            lines = listOf(
                StoryLine("店員 (Clerk)", "いらっしゃいませ！お弁当温めますか？", "Irasshaimase! Obentou atatamemasu ka?", "Welcome! Would you like your bento heated up?", "स्वागत है! क्या आपका बेंतो गर्म कर दूँ?", listOf("いらっしゃいませ" to "Welcome (to a shop)", "お弁当" to "Bento lunch box", "温めます" to "To heat up")),
                StoryLine("あなた (You)", "はい、お願いします。あと、袋はいりません。", "Hai, onegaishimasu. Ato, fukuro wa irimasen.", "Yes, please. Also, I don't need a plastic bag.", "जी हाँ, कृपया। और मुझे बैग (पॉलिथीन) नहीं चाहिए।", listOf("お願いします" to "Please (request)", "袋 (ふくろ)" to "Bag", "いりません" to "Do not need")),
                StoryLine("店員 (Clerk)", "かしこまりました。お支払いはどうなさいますか？", "Kashikomarimashita. Oshiharai wa dou nasaimasu ka?", "Certainly. How would you like to pay?", "जी बिल्कुल। आप भुगतान कैसे करना चाहेंगे?", listOf("かしこまりました" to "Certainly / Understood (Humble)", "お支払い" to "Payment")),
                StoryLine("あなた (You)", "Suica（スイカ）で払います。", "Suica de haraimasu.", "I will pay with Suica (IC Card).", "मैं Suica कार्ड से पेमेंट करूँगा।", listOf("で" to "By means of (Particle)", "払います (はらいます)" to "To pay"))
            ),
            comprehensionQuestion = "Why did the customer say 「袋はいりません」?",
            options = listOf("They wanted the bento cold", "They did not need a shopping bag", "They wanted to pay by cash", "They asked for chopsticks"),
            correctOption = 1
        ),
        ImmersionStory(
            id = "s2",
            titleJp = "大学院の研究室訪問（敬語）",
            titleEn = "Visiting a University Research Lab in Japan",
            level = JlptLevel.N2,
            lines = listOf(
                StoryLine("学生 (Student)", "失礼いたします。本日お時間をいただき、ありがとうございます。", "Shitsurei itashimasu. Honjitsu ojikan o itadaki, arigatou gozaimasu.", "Excuse me for entering. Thank you very much for granting me your time today.", "क्षमा करें (प्रवेश करते हुए)। आज अपना कीमती समय देने के लिए बहुत-बहुत धन्यवाद।", listOf("失礼いたします" to "Excuse me (Humble Keigo)", "いただき" to "Humbly receiving")),
                StoryLine("教授 (Professor)", "どうぞ座ってください。君の論文を読みましたよ。画期的な手法ですね。", "Douzo suwatte kudasai. Kimi no ronbun o yomimashita yo. Kakkiteki na shuhou desu ne.", "Please have a seat. I read your research paper. It's a groundbreaking approach!", "कृपया बैठिए। मैंने आपका रिसर्च पेपर पढ़ा। यह एक क्रांतिकारी तरीका है!", listOf("論文 (ろんぶん)" to "Research paper / Thesis", "画期的" to "Groundbreaking", "手法" to "Methodology")),
                StoryLine("学生 (Student)", "光栄です。CPUのみで高速に動作するよう試行錯誤を重ねました。", "Kouei desu. CPU nomi de kousoku ni dousa suru you shikou sakugo o kasanemashita.", "I am honored. I went through repeated trial and error so it runs fast on CPU only.", "यह मेरा सौभाग्य है। केवल CPU पर तेज़ चलने के लिए मैंने कई बार प्रयास और सुधार किए।", listOf("光栄 (こうえい)" to "Honor / Privilege", "試行錯誤" to "Trial and error", "高速" to "High speed"))
            ),
            comprehensionQuestion = "What did the professor praise about the student's work?",
            options = listOf("Their Japanese handwriting", "Their groundbreaking research methodology", "Their punctuality", "Their university grades"),
            correctOption = 1
        )
    )

    val sentencePuzzles: List<SentencePuzzle> = listOf(
        SentencePuzzle(
            id = "p1",
            level = JlptLevel.N5,
            englishPrompt = "I study Japanese every day.",
            hindiPrompt = "मैं (私は) हर दिन (毎日) जापानी को (日本語を) पढ़ता हूँ (勉強します)।",
            correctTiles = listOf("私は", "毎日", "日本語を", "勉強します"),
            distractorTiles = listOf("日本語に", "食べます", "が"),
            grammarNote = "SOV Order: Topic (私は) + Time (毎日) + Object (日本語を) + Verb (勉強します)."
        ),
        SentencePuzzle(
            id = "p2",
            level = JlptLevel.N5,
            englishPrompt = "You must not take photos here.",
            hindiPrompt = "यहाँ पर (ここで) फोटो को (写真を) खींचना (撮っては) मना है (いけません)।",
            correctTiles = listOf("ここで", "写真を", "撮っては", "いけません"),
            distractorTiles = listOf("ここに", "撮る", "ください"),
            grammarNote = "Prohibition uses Te-form + はいけません (撮ってはいけません)."
        ),
        SentencePuzzle(
            id = "p3",
            level = JlptLevel.N4,
            englishPrompt = "I have been to Japan before.",
            hindiPrompt = "मैं जापान (日本へ) जा (行った) चुका हूँ (ことがあります)।",
            correctTiles = listOf("日本へ", "行った", "ことが", "あります"),
            distractorTiles = listOf("行く", "ことを", "います"),
            grammarNote = "Past experience uses Past Plain (Ta-form: 行った) + ことがあります."
        ),
        SentencePuzzle(
            id = "p4",
            level = JlptLevel.N3,
            englishPrompt = "Thanks to my teacher, I passed the exam.",
            hindiPrompt = "शिक्षक की कृपा से (先生のおかげで) परीक्षा में (試験に) पास हो गया (合格しました)।",
            correctTiles = listOf("先生の", "おかげで", "試験に", "合格しました"),
            distractorTiles = listOf("せいで", "試験を", "合格する"),
            grammarNote = "Positive gratitude uses Noun + のおかげで, and 合格する takes particle に!"
        ),
        SentencePuzzle(
            id = "p5",
            level = JlptLevel.N2,
            englishPrompt = "We cannot compromise at all regarding quality.",
            hindiPrompt = "गुणवत्ता के संबंध में (品質に関しては) बिल्कुल भी (一切) समझौता नहीं कर सकते (妥協できない)।",
            correctTiles = listOf("品質に", "関しては", "一切", "妥協できない"),
            distractorTiles = listOf("品質を", "おそらく", "妥協する"),
            grammarNote = "〜に関しては means 'regarding/concerning', and 一切 pairs with a negative verb ('not at all')."
        )
    )

    fun generateQuizQuestions(selectedLevel: JlptLevel?, mode: QuizMode): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val pool = if (selectedLevel == null) vocabList else vocabList.filter { it.level == selectedLevel }
        val verbs = pool.filter { it.verbGroup != VerbGroup.NOT_VERB }.ifEmpty { vocabList.filter { it.verbGroup != VerbGroup.NOT_VERB } }

        // 1. Conjugation Drills
        if (mode == QuizMode.MIXED || mode == QuizMode.CONJUGATION) {
            verbs.forEach { verb ->
                val forms = VerbConjugator.conjugate(verb)
                forms.drop(1).shuffled().take(3).forEach { targetForm ->
                    val wrongOptions = forms.filter { it.japanese != targetForm.japanese }.shuffled().take(3).map { it.japanese }
                    val allOpts = (wrongOptions + targetForm.japanese).shuffled()
                    questions.add(
                        QuizQuestion(
                            prompt = "Conjugate 「${verb.kanji}」 (${verb.kana} - ${verb.meaning}) into ${targetForm.formName}:",
                            subPrompt = "${verb.verbGroup.label} • ${verb.level.label}",
                            options = allOpts,
                            correctIndex = allOpts.indexOf(targetForm.japanese),
                            explanation = "${targetForm.formName} of ${verb.kanji} is 「${targetForm.japanese}」 (${targetForm.reading}). Hint: ${targetForm.usageHint}",
                            level = verb.level,
                            audioText = targetForm.japanese
                        )
                    )
                }
            }
        }

        // 2. Listening Only Mode
        if (mode == QuizMode.MIXED || mode == QuizMode.LISTENING) {
            pool.forEach { item ->
                val distractors = vocabList.filter { it.id != item.id }.shuffled().take(3).map { "${it.meaning} (${it.hindiHint})" }
                val correctAns = "${item.meaning} (${item.hindiHint})"
                val opts = (distractors + correctAns).shuffled()
                questions.add(
                    QuizQuestion(
                        prompt = "🎧 Tap the Speaker icon, listen carefully, and select the meaning:",
                        subPrompt = "Audio Listening Comprehension • ${item.level.label}",
                        options = opts,
                        correctIndex = opts.indexOf(correctAns),
                        explanation = "You heard 「${item.kanji}」 (${item.kana} / ${item.romaji}) which means '${item.meaning}' (${item.hindiHint}).",
                        level = item.level,
                        audioText = item.kanji,
                        isListeningOnly = true
                    )
                )
            }
        }

        // 3. Kanji Reading & Radicals
        if (mode == QuizMode.MIXED || mode == QuizMode.KANJI_READING) {
            pool.forEach { item ->
                val wrongKana = vocabList.filter { it.id != item.id }.shuffled().take(3).map { it.kana }
                val opts = (wrongKana + item.kana).shuffled()
                questions.add(
                    QuizQuestion(
                        prompt = "What is the correct reading (Furigana) for 「${item.kanji}」?",
                        subPrompt = "Kanji Reading • Radicals: ${item.radicals}",
                        options = opts,
                        correctIndex = opts.indexOf(item.kana),
                        explanation = "「${item.kanji}」 is read as 「${item.kana}」 (${item.romaji}) meaning '${item.meaning}'. Mnemonic: ${item.mnemonic}",
                        level = item.level,
                        audioText = item.kanji
                    )
                )
            }
        }

        // 4. Grammar & Particle Questions
        if (mode == QuizMode.MIXED || mode == QuizMode.GRAMMAR) {
            val gPool = if (selectedLevel == null) grammarList else grammarList.filter { it.level == selectedLevel }
            gPool.forEach { g ->
                val ex = g.examples.first()
                val allTitles = grammarList.map { it.title }.distinct()
                val wrong = allTitles.filter { it != g.title }.shuffled().take(3)
                val opts = (wrong + g.title).shuffled()
                questions.add(
                    QuizQuestion(
                        prompt = "Which grammar concept applies to:\n「${ex.jp}」\n(${ex.en})",
                        subPrompt = "Grammar Dojo • ${g.level.label}",
                        options = opts,
                        correctIndex = opts.indexOf(g.title),
                        explanation = "${g.explanation}\n🇮🇳 Hindi Parallel: ${g.hindiParallel}",
                        level = g.level,
                        audioText = ex.jp
                    )
                )
            }
        }

        return questions.shuffled()
    }
}
