package com.aipoweredgita.app.ui.components

// ── Translation Helper ──────────────────────────────────────────────────────
fun translateConfigText(text: String, language: String): String {
    if (language != "tel") return text
    return when (text) {
        "Questions per battle" -> "సమరానికి ప్రశ్నలు"
        "Language" -> "భాష"
        "WHAT TO EXPECT" -> "ఏమి ఆశించాలి"
        "Context-aware Gita questions" -> "సందర్భోచిత గీతా ప్రశ్నలు"
        "Intelligent difficulty scaling" -> "కఠినత్వ స్థాయిల క్రమబద్ధీకరణ"
        "Telugu language support" -> "తెలుగు భాషా మద్దతు"
        "100% offline & private" -> "100% ఆఫ్‌లైన్ & వ్యక్తిగతం"
        "Gita Quiz" -> "గీతా క్విజ్"
        "Test your knowledge of the sacred scripture" -> "పవిత్ర గ్రంథంపై మీ జ్ఞానాన్ని పరీక్షించుకోండి"
        "Sprint" -> "లఘు ప్రశ్నలు"
        "Deep Dive" -> "లోతైన విశ్లేషణ"
        "LANGUAGE" -> "భాష"
        "Download AI Engine" -> "AI ఇంజిన్‌ను డౌన్‌లోడ్ చేయండి"
        "Select a model to download:" -> "డౌన్‌లోడ్ చేయడానికి ఒక నమూనాను ఎంచుకోండి:"
        "UNLOCKS" -> "అన్‌లాక్ అవుతాయి"
        "Download once, quiz anytime — fully offline." -> "ఒక్కసారి డౌన్‌లోడ్ చేయండి, ఎప్పుడైనా క్విజ్ ఆడండి — పూర్తిగా ఆఫ్‌లైన్."
        "Not now" -> "ఇప్పుడు వద్దు"
        "Download  →" -> "డౌన్‌లోడ్  →"
        "All models ready" -> "అన్ని మోడల్స్ సిద్ధంగా ఉన్నాయి"
        "The Arena Prepares" -> "యుద్ధరంగం సిద్ధమవుతోంది"
        "AI models are downloading to power your quiz experience. This happens only once." -> "మీ క్విజ్ అనుభవాన్ని మెరుగుపరచడానికి AI మోడల్స్ డౌన్‌లోడ్ అవుతున్నాయి. ఇది ఒక్కసారి మాత్రమే జరుగుతుంది."
        "AWAITING YOU" -> "మీ కొరకు సిద్ధంగా ఉన్నవి"
        "Intelligent Questions" -> "మేధోపరమైన ప్రశ్నలు"
        "Theme-Based Learning" -> "విషయ-ఆధారిత అభ్యాసం"
        "Fully Offline" -> "పూర్తిగా ఆఫ్‌లైన్"
        "Context Aware" -> "సందర్భోచితం"
        "← Return to Home" -> "← హోమ్‌కి తిరిగి వెళ్ళండి"
        "Please wait until models finish downloading." -> "దయచేసి మోడల్స్ డౌన్‌లోడ్ పూర్తయ్యే వరకు వేచి ఉండండి."
        "DOWNLOAD PROGRESS" -> "డౌన్‌లోడ్ పురోగతి"
        "Preparing download…" -> "డౌన్‌లోడ్ సిద్ధమవుతోంది…"
        "file(s) remaining" -> "ఫైల్(లు) మిగిలి ఉన్నాయి"
        "MB left" -> "MB మిగిలి ఉంది"
        "Smart context-aware questions" -> "సందర్భోచిత మేధో ప్రశ్నలు"
        "Offline & private" -> "ఆఫ్‌లైన్ & వ్యక్తిగతం"
        else -> text
    }
}

