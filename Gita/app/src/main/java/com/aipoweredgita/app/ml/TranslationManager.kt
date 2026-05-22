package com.aipoweredgita.app.ml

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class TranslationManager {
    private val optionsTeToEn = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.TELUGU)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()

    private val optionsEnToTe = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.TELUGU)
        .build()

    private val teluguToEnglishTranslator = Translation.getClient(optionsTeToEn)
    private val englishToTeluguTranslator = Translation.getClient(optionsEnToTe)

    private val directTranslations = mapOf(
        // General Quiz & Meaning-to-Verse translations
        "which chapter is this verse from?" to "ఈ శ్లోకం ఏ అధ్యాయానికి చెందినది?",
        "chapter" to "అధ్యాయం",
        "complete this verse:" to "ఈ శ్లోకాన్ని పూర్తి చేయండి:",
        "which verse expresses this meaning?" to "ఈ భావాన్ని ఏ శ్లోకం వ్యక్తపరుస్తుంది?",
        "bhagavad gita" to "భగవద్గీత",
        "this verse teaches about performing one's duty with dedication and detachment" to "ఈ శ్లోకం సమర్పణ భావం మరియు అనాసక్తతతో కర్తవ్యాన్ని నిర్వహించడం గురించి బోధిస్తుంది",
        "this verse emphasizes the importance of spiritual knowledge and wisdom" to "ఈ శ్లోకం ఆధ్యాత్మిక జ్ఞానం మరియు వివేకం యొక్క ప్రాముఖ్యతను నొక్కి చెబుతుంది",
        "this verse speaks about the path of devotion and surrender to the divine" to "ఈ శ్లోకం భక్తి మార్గం మరియు భగవంతునికి శరణాగతి గురించి తెలియజేస్తుంది",
        "this verse discusses the practice of yoga and mental discipline" to "ఈ శ్లోకం యోగ సాధన మరియు మానసిక నిగ్రహం గురించి వివరిస్తుంది",
        "this verse teaches about spiritual enlightenment and inner peace" to "ఈ శ్లోకం ఆధ్యాత్మిక జ్ఞానోదయం మరియు అంతఃశాంతి గురించి బోధిస్తుంది",
        "how should one approach action according to this verse?" to "ఈ శ్లోకం ప్రకారం కర్మను ఎలా ఆచరించాలి?",
        "with detachment and dedication" to "అనాసక్తత మరియు సమర్పణ భావంతో",
        "by avoiding all responsibilities" to "అన్ని బాధ్యతల నుండి తప్పించుకోవడం ద్వారా",
        "through force and compulsion" to "బలవంతం మరియు నిర్బంధం ద్వారా",
        "without any spiritual purpose" to "ఆధ్యాత్మిక ఉద్దేశ్యం లేకుండా",
        "what does this verse teach about one's duty?" to "ఈ శ్లోకం కర్తవ్యం గురించి ఏమి బోధిస్తుంది?",
        "that duty is unimportant" to "కర్తవ్యం ముఖ్యం కాదు అని",
        "to escape from all duties" to "బాధ్యతల నుండి తప్పించుకోవడం అని",
        "that duties cause suffering" to "కర్తవ్యాలు దుఃఖాన్ని కలిగిస్తాయి అని",
        "what kind of knowledge does this verse emphasize?" to "ఈ శ్లోకం ఏ విధమైన జ్ఞానాన్ని నొక్కి చెబుతుంది?",
        "meditation" to "ధ్యానం",
        "wisdom" to "వివేకం",
        "what is the core spiritual message of this verse?" to "ఈ శ్లోకం యొక్క ముఖ్య ఆధ్యాత్మిక సందేశం ఏమిటి?",
        "duty and dharma" to "కర్తవ్యం మరియు ధర్మం",
        "knowledge and wisdom" to "జ్ఞానం మరియు వివేకం",
        "devotion and love" to "భక్తి మరియు ప్రేమ",
        "path to liberation" to "మోక్ష మార్గం",

        // Standard Options
        "selfless action without attachment to results" to "ఫలాపేక్ష లేని నిష్కామ కర్మ",
        "pursuing personal gain" to "వ్యక్తిగత ప్రయోజనాల కోసం శ్రమించడం",
        "avoiding all action" to "కర్మలను పూర్తిగా విస్మరించడం",
        "seeking others' approval" to "ఇతరుల ప్రశంసలను ఆశించడం",
        "detachment from the fruits of action" to "కర్మ ఫలాల పట్ల అనాసక్తత",
        "working only for rewards" to "ఫలితం కోసమే పని చేయడం",
        "refusing to act" to "పని చేయడానికి నిరాకరించడం",
        "competing with others" to "ఇతరులతో పోటీపడటం",
        "equanimity in pleasure and pain" to "సుఖదుఃఖాల పట్ల సమభావం",
        "seeking only pleasure" to "సుఖాన్ని మాత్రమే వెతకడం",
        "avoiding all pain" to "దుఃఖాన్ని మాత్రమే నివారించడం",
        "ignoring others' suffering" to "ఇతరుల బాధలను నిర్లక్ష్యం చేయడం",
        "devotion and complete surrender to god" to "భగవంతుని పట్ల అనన్య భక్తి మరియు శరణాగతి",
        "intellectual pride" to "జ్ఞాన గర్వం",
        "material accumulation" to "భౌతిక వస్తువుల సేకరణ",
        "social status" to "సామాజిక హోదా",
        "knowledge that liberates the soul" to "ఆత్మకు ముక్తిని ప్రసాదించే జ్ఞానం",
        "blind following of rituals" to "ఆచారాలను గ్రుడ్డిగా అనుసరించడం",
        "accumulating wealth" to "సంపదను కూడబెట్టడం",
        "political power" to "రాజకీయ అధికారం",
        "meditation and control of the mind" to "ధ్యాన సాధన మరియు మనో నిగ్రహం",
        "constant activity" to "నిరంతరం ఏదో ఒక పనిలో నిమగ్నమవడం",
        "endless sleep" to "అధిక నిద్ర",
        "idle gossip" to "వృథా కాలక్షేపం",
        "righteous conduct in all circumstances" to "అన్ని పరిస్థితులలోనూ ధర్మబద్ధమైన ప్రవర్తన",
        "compromising ethics for success" to "విజయం కోసం నైతికతను వదులుకోవడం",
        "ignoring moral rules" to "నైతిక నియమాలను ఉల్లంఘించడం",
        "judging others" to "ఇతరులను తప్పుపట్టడం",
        "faith that sustains through difficulty" to "కష్టాలలో కూడా అండగా నిలిచే నిజమైన విశ్వాసం (శ్రద్ధ)",
        "cynicism about spiritual paths" to "ఆధ్యాత్మిక మార్గాలపై అపనమ్మకం",
        "despair at obstacles" to "అంతరాయాలకు నిరాశపడటం",
        "apathy toward growth" to "ఆధ్యాత్మిక ఎదుగుదల పట్ల ఉదాసీనత",
        "treating friend and enemy alike" to "శత్రు మిత్రులను సమానంగా చూడటం",
        "showing favoritism" to "పక్షపాతం చూపించడం",
        "avoiding all relationships" to "అన్ని సంబంధాలను తెంచుకోవడం",
        "seeking revenge" to "ప్రతీకారం తీర్చుకోవడం",
        "performing one's own duty imperfectly" to "తన సొంత కర్తవ్యాన్ని అసంపూర్ణంగానైనా ఆచరించడం",
        "perfectly copying another's path" to "ఇతరుల కర్తవ్యాన్ని పరిపూర్ణంగా అనుకరించడం",
        "avoiding all responsibilities" to "బాధ్యతలన్నింటినీ విస్మరించడం",
        "blaming others" to "ఇతరులపై నిందలు వేయడం",
        "rising above the three modes of nature" to "త్రిగుణాలకు (సత్వ, రజ, తమస్సులకు) అతీతంగా ఎదగడం",
        "indulging in sensory pleasures" to "ఇంద్రియ సుఖాలలో మునిగిపోవడం",
        "suppressing all emotions" to "భావోద్వేగాలను బలవంతంగా అణచివేయడం",
        "living in isolation" to "ఒంటరిగా జీవించడం",
        "offering all actions to the divine" to "సమస్త కర్మలను భగవదర్పణం చేయడం",
        "claiming credit for everything" to "ప్రతిదానికీ తానే కర్తనని చెప్పుకోవడం",
        "ignoring spiritual practice" to "ఆధ్యాత్మిక సాధనను నిర్లక్ష్యం చేయడం",
        "competing in piety" to "భక్తిలో పోటీపడటం",
        "steady wisdom unaffected by circumstances" to "పరిస్థితులకు ప్రభావితం కాని స్థిర బుద్ధి (స్థితప్రజ్ఞత)",
        "temporary enthusiasm" to "తాత్కాలిక ఉత్సాహం",
        "intellectual debate" to "మేధోపరమైన వాదోపవాదాలు",
        "social conformity" to "సమాజానికి అనుగుణంగా నడచుకోవడం",
        "controlling the senses through practice" to "నిరంతర అభ్యాసం ద్వారా ఇంద్రియాలను నిగ్రహించడం",
        "indulging every desire" to "ప్రతి కోరికనూ తీర్చుకోవడానికి ప్రయత్నించడం",
        "punishing the body" to "శరీరాన్ని కష్టపెట్టడం",
        "ignoring physical needs" to "శారీరక అవసరాలను పట్టించుకోకపోవడం",
        "seeing the self in all beings" to "సర్వభూతాలలోనూ పరమాత్మను దర్శించడం",
        "judging by outward appearance" to "బాహ్య రూపాలను బట్టి అంచనా వేయడం",
        "separating oneself from others" to "తన్ను తాను ఇతరుల నుండి వేరుగా చూసుకోవడం",
        "fearing the unknown" to "తెలియని విషయాలకు భయపడటం",
        "acting according to one's own nature" to "తన స్వభావానికి అనుగుణంగా ప్రవర్తించడం",
        "imitating great personalities" to "గొప్ప వ్యక్తులను గ్రుడ్డిగా అనుకరించడం",
        "following trends blindly" to "మారుతున్న ధోరణులను గుడ్డిగా అనుసరించడం",
        "rebelling without cause" to "కారణం లేకుండా తిరుగుబాటు చేయడం",
        "finding joy within through meditation" to "ధ్యానం ద్వారా తనలోనే నిజమైన ఆనందాన్ని కనుగొనడం",
        "seeking happiness externally" to "బాహ్య విషయాలలో సంతోషాన్ని వెతకడం",
        "depending on others' praise" to "ఇతరుల ప్రశంసలపై ఆధారపడటం",
        "avoiding all effort" to "ప్రయత్నాలన్నింటినీ విరమించుకోవడం",
        "accepting both joy and sorrow with balance" to "సుఖదుఃఖాలను సమతుల్యతతో స్వీకరించడం",
        "chasing only happiness" to "కేవలం సంతోషాన్ని మాత్రమే వెంటాడటం",
        "running from all pain" to "ప్రతి కష్టాన్నీ చూసి పారిపోవడం",
        "numbing the emotions" to "భావాలను మొద్దుబార్చుకోవడం",
        "serving others without expectation" to "ఎటువంటి ప్రతిఫలం ఆశించకుండా ఇతరులకు సేవ చేయడం",
        "demanding recognition" to "గుర్తింపును డిమాండ్ చేయడం",
        "withholding help from strangers" to "అపరిచితులకు సహాయం చేయడానికి నిరాకరించడం",
        "competing in charity" to "దానధర్మాలలో పోటీపడటం",
        "understanding the eternal nature of the soul" to "ఆత్మ యొక్క నిత్యత్వాన్ని (శాశ్వతత్వాన్ని) గ్రహించడం",
        "identifying only with the body" to "శరీరమే సర్వస్వమని భావించడం",
        "fearing death excessively" to "మరణాన్ని చూసి అమితంగా భయపడటం",
        "ignoring spiritual teachings" to "ఆధ్యాత్మిక బోధనలను పెడచెవిన పెట్టడం",

        // Core spiritual key terms for themes
        "duty" to "కర్తవ్యం",
        "dharma" to "ధర్మం",
        "yoga" to "యోగం",
        "unity" to "ఐక్యత",
        "devotion" to "భక్తి",
        "love" to "ప్రేమ",
        "knowledge" to "జ్ఞానం",
        "wisdom" to "వివేకం",
        "liberation" to "మోక్షం",
        "freedom" to "స్వేచ్ఛ",
        "karma" to "కర్మ",
        "action" to "కార్యాచరణ",
        "self" to "ఆత్మ",
        "soul" to "అంతరాత్మ",
        "renunciation" to "సన్యాసం",
        "detachment" to "అనాసక్తత",
        "spiritual wisdom" to "ఆధ్యాత్మిక జ్ఞానం",
        "inner growth" to "ఆంతరిక ఎదుగుదల",
        "path to truth" to "సత్య మార్గం",
        "reflection" to "ఆత్మపరిశీలన",
        "application" to "ఆచరణ",
        "comparison" to "పోలిక",
        "chapter identification" to "అధ్యాయం గుర్తింపు",
        "fill in the blank" to "ఖాళీలను పూరించడం",
        "meaning to verse" to "భావం నుండి శ్లోకం",

        // Explanations & prompts
        "identify the chapter context from the verse." to "శ్లోకం ఆధారంగా అధ్యాయం యొక్క సందర్భాన్ని గుర్తించండి.",
        "choose the word that preserves the verse’s meaning." to "శ్లోకం యొక్క భావాన్ని కాపాడే పదాన్ని ఎంచుకోండి.",
        "match the described meaning to the correct verse reference." to "వివరించిన భావానికి సరిపోయే శ్లోక సూచికను జతపరచండి.",
        "true detachment = engaged action without clinging, not apathy." to "నిజమైన అనాసక్తత అంటే కర్మల పట్ల మమకారం లేకపోవడం, బద్ధకం కాదు.",
        "connect teachings to practical steps (e.g., duty without attachment, compassion)." to "బోధనలను ఆచరణాత్మక దశలతో అనుసంధానించండి (ఉదాహరణకు, ఫలాపేక్ష లేని కర్తవ్య నిర్వహణ, కరుణ).",
        "choose the theme most directly taught by the verse." to "శ్లోకం ద్వారా నేరుగా బోధించబడే అంశాన్ని ఎంచుకోండి.",
        "in 3-5 sentences, explain how this verse guides conduct and mindset." to "ఈ శ్లోకం ప్రవర్తనను మరియు మనస్తత్వాన్ని ఎలా నిర్దేశిస్తుందో 3-5 వాక్యాలలో వివరించండి.",
        "which best contrasts this verse’s teaching with a common misconception?" to "ఈ శ్లోక బోధనను ఒక సాధారణ అపోహతో ఏది చక్కగా పోల్చి చూపుతుంది?",
        "describe how you would apply this verse in a workplace challenge." to "కార్యాలయంలో ఎదురయ్యే సవాలుకు ఈ శ్లోకాన్ని ఎలా అన్వయిస్తారో వివరించండి."
    )

    private val templateRegexes = listOf(
        Regex("(?i)What is the primary teaching of Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 యొక్క ముఖ్యమైన బోధన ఏమిటి?",
        Regex("(?i)According to Chapter (\\d+), Verse (\\d+), what should one focus on\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం, ఒకరు దేనిపై దృష్టి పెట్టాలి?",
        Regex("(?i)How does Chapter (\\d+), Verse (\\d+) describe the path to wisdom\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 జ్ఞాన మార్గాన్ని ఎలా వివరిస్తుంది?",
        Regex("(?i)What quality does Chapter (\\d+), Verse (\\d+) emphasize\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ఏ గుణాన్ని నొక్కి చెబుతుంది?",
        Regex("(?i)What does Krishna teach in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో శ్రీకృష్ణుడు ఏమి బోధిస్తున్నాడు?",
        Regex("(?i)How should one act according to Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం ఒకరు ఎలా ప్రవర్తించాలి?",
        Regex("(?i)What virtue is highlighted in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో ఏ సద్గుణం ప్రస్తావించబడింది?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) say about duty\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 కర్తవ్యం గురించి ఏమి చెబుతుంది?",
        Regex("(?i)How does one achieve peace according to Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం ప్రశాంతతను ఎలా పొందవచ్చు?",
        Regex("(?i)What lesson does Chapter (\\d+), Verse (\\d+) impart\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 మనకు ఏ పాఠాన్ని అందిస్తుంది?",
        Regex("(?i)In Chapter (\\d+), Verse (\\d+), what does Krishna say about the self\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో ఆత్మ గురించి శ్రీకృష్ణుడు ఏమి చెబుతున్నాడు?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) reveal about the nature of action\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 కర్మ యొక్క స్వభావం గురించి ఏమి వెల్లడిస్తుంది?",
        Regex("(?i)How should a wise person approach their duties, as taught in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో బోధించినట్లుగా, జ్ఞాని తన కర్తవ్యాలను ఎలా నిర్వహించాలి?",
        Regex("(?i)What attitude towards results is recommended in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో కర్మ ఫలాల పట్ల ఎటువంటి దృక్పథం సిఫార్సు చేయబడింది?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) say about devotion\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 భక్తి గురించి ఏమి చెబుతుంది?",
        Regex("(?i)How does one overcome desire and anger, according to Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం కామ క్రోధాలను ఎలా జయించవచ్చు?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) teach about the eternal soul\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 నిత్యమైన ఆత్మ గురించి ఏమి బోధిస్తుంది?",
        Regex("(?i)What is the fate of one who neglects their duty, per Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం తన కర్తవ్యాన్ని నిర్లక్ష్యం చేసేవారి గతి ఏమిటి?",
        Regex("(?i)How does Chapter (\\d+), Verse (\\d+) describe a person of steady wisdom\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 స్థితప్రజ్ఞుడిని (స్థిరమైన బుద్ధి కలవాడిని) ఎలా వివరిస్తుంది?",
        Regex("(?i)What does Krishna say about the results of one's actions in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో కర్మ ఫలాల గురించి శ్రీకృష్ణుడు ఏమి చెబుతున్నాడు?",
        Regex("(?i)What practice does Chapter (\\d+), Verse (\\d+) recommend for controlling the mind\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 మనస్సును అదుపులో ఉంచుకోవడానికి ఏ సాధనను సిఫార్సు చేస్తుంది?",
        Regex("(?i)How does one attain liberation according to Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం మోక్షాన్ని ఎలా పొందవచ్చు?",
        Regex("(?i)What is the greatest gift one can give, as taught in Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 లో బోధించినట్లుగా, ఒకరు ఇవ్వగలిగే అత్యున్నతమైన దానం ఏది?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) say about the power of faith\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 శ్రద్ధ (విశ్వాసం) యొక్క శక్తి గురించి ఏమి చెబుతుంది?",
        Regex("(?i)How should one view success and failure, per Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం జయాపజయాలను ఎలా స్వీకరించాలి?",
        Regex("(?i)What quality leads to spiritual growth, according to Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం ఆధ్యాత్మిక ఎదుగుదలకు తోడ్పడే గుణం ఏది?",
        Regex("(?i)What does Chapter (\\d+), Verse (\\d+) teach about selfless service\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 నిష్కామ సేవ గురించి ఏమి బోధిస్తుంది?",
        Regex("(?i)How does one remain steady in both joy and sorrow, per Chapter (\\d+), Verse (\\d+)\\?") to 
            "అధ్యాయం $1, శ్లోకం $2 ప్రకారం సుఖదుఃఖాలలో ఎలా స్థిరంగా ఉండాలి?",
        Regex("(?i)Bhagavad Gita (\\d+)\\.(\\d+)") to "భగవద్గీత $1.$2",
        Regex("(?i)Chapter (\\d+)\\.(\\d+)") to "అధ్యాయం $1.$2",
        Regex("(?i)This is based on the teachings in Chapter (\\d+), Verse (\\d+) of the Bhagavad Gita\\.") to 
            "ఇది భగవద్గీతలోని అధ్యాయం $1, శ్లోకం $2 యొక్క బోధనలపై ఆధారపడి ఉంది."
    )

    suspend fun downloadModelsIfNeeded(): Boolean {
        val conditions = DownloadConditions.Builder().build()
        return try {
            teluguToEnglishTranslator.downloadModelIfNeeded(conditions).await()
            englishToTeluguTranslator.downloadModelIfNeeded(conditions).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun translateTeluguToEnglish(text: String): String {
        return try {
            teluguToEnglishTranslator.translate(text).await()
        } catch (e: Exception) {
            e.printStackTrace()
            text // fallback
        }
    }

    private fun translateThemesList(themesListStr: String): String {
        return themesListStr.split(",")
            .map { it.trim() }
            .map { theme ->
                val lowerTheme = theme.lowercase()
                directTranslations[lowerTheme] ?: when {
                    lowerTheme.contains(" and ") -> {
                        val parts = theme.split(" and ").map { it.trim() }
                        val t1 = directTranslations[parts[0].lowercase()] ?: parts[0]
                        val t2 = directTranslations[parts[1].lowercase()] ?: parts[1]
                        "$t1 మరియు $t2"
                    }
                    else -> theme
                }
            }
            .joinToString(", ")
    }

    suspend fun translateEnglishToTelugu(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        
        // 1. Check direct map
        val directMatch = directTranslations[lower]
        if (directMatch != null) {
            return directMatch
        }

        // 2. Check regex/dynamic template matches
        for ((regex, replacement) in templateRegexes) {
            val match = regex.matchEntire(trimmed)
            if (match != null) {
                return regex.replace(trimmed, replacement)
            }
        }

        // 3. Specific pattern handling for Reflection essay prompt
        if (lower.startsWith("reference key themes (e.g.,") && lower.endsWith(") and connect to actions.")) {
            val themesPart = trimmed.substringAfter("e.g., ").substringBefore(")")
            val translatedThemes = translateThemesList(themesPart)
            return "ముఖ్యమైన అంశాలను (ఉదాహరణకు, $translatedThemes) ప్రస్తావిస్తూ, వాటిని మీ పనులతో అనుసంధానించండి."
        }

        // 4. Default: fall back to ML Kit translator
        return try {
            englishToTeluguTranslator.translate(text).await()
        } catch (e: Exception) {
            e.printStackTrace()
            text // fallback
        }
    }
}
