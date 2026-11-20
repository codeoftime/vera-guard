package com.vera.guard.data

/** Bundled offline first-aid cards (spec 7): CPR, bleeding, burns, choking, fracture, seizure in en / hi / kn. Informational only. */
data class AidCard(val id: String, val keywords: List<String>, val title: Map<String, String>, val steps: Map<String, List<String>>)

object FirstAid {
    val emergencyLine = mapOf("en" to "Call 112 or 108 first.", "hi" to "सबसे पहले 112 या 108 पर कॉल करें।", "kn" to "ಮೊದಲು 112 ಅಥವಾ 108 ಗೆ ಕರೆ ಮಾಡಿ.")
    val disclaimer = mapOf(
        "en" to "Informational only, not a medical diagnosis.", "hi" to "केवल जानकारी के लिए, चिकित्सकीय निदान नहीं।", "kn" to "ಮಾಹಿತಿಗಾಗಿ ಮಾತ್ರ, ವೈದ್ಯಕೀಯ ರೋಗನಿರ್ಣಯವಲ್ಲ.",
    )

    val cards = listOf(
        AidCard("cpr", listOf("cpr", "heart", "unconscious", "not breathing", "सीपीआर", "बेहोश", "साँस", "ಸಿಪಿಆರ್", "ಪ್ರಜ್ಞೆ", "ಉಸಿರು"),
            mapOf("en" to "CPR (adult)", "hi" to "CPR (वयस्क)", "kn" to "CPR (ವಯಸ್ಕ)"),
            mapOf(
                "en" to listOf("Call 112 / 108 and ask someone to bring an AED.", "Check if the person responds and breathes normally.", "Push hard and fast in the centre of the chest: about 5 cm deep, 100–120 times a minute.", "Do not stop until help arrives or the person recovers.", "Use an AED as soon as it arrives and follow its voice prompts."),
                "hi" to listOf("112 / 108 पर कॉल करें और किसी को AED लाने को कहें।", "देखें कि व्यक्ति जवाब दे रहा है और सामान्य साँस ले रहा है या नहीं।", "छाती के बीचोंबीच तेज़ और ज़ोर से दबाएँ: लगभग 5 सेमी गहरा, प्रति मिनट 100–120 बार।", "मदद आने या व्यक्ति के ठीक होने तक न रुकें।", "AED आते ही उसका इस्तेमाल करें और उसके निर्देश मानें।"),
                "kn" to listOf("112 / 108 ಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು AED ತರಲು ಯಾರಿಗಾದರೂ ಹೇಳಿ.", "ವ್ಯಕ್ತಿ ಪ್ರತಿಕ್ರಿಯಿಸುತ್ತಿದ್ದಾರೆಯೇ, ಸಾಮಾನ್ಯವಾಗಿ ಉಸಿರಾಡುತ್ತಿದ್ದಾರೆಯೇ ಎಂದು ನೋಡಿ.", "ಎದೆಯ ಮಧ್ಯದಲ್ಲಿ ಗಟ್ಟಿಯಾಗಿ, ವೇಗವಾಗಿ ಒತ್ತಿ: ಸುಮಾರು 5 ಸೆಂ.ಮೀ ಆಳ, ನಿಮಿಷಕ್ಕೆ 100–120 ಬಾರಿ.", "ಸಹಾಯ ಬರುವವರೆಗೆ ಅಥವಾ ವ್ಯಕ್ತಿ ಚೇತರಿಸಿಕೊಳ್ಳುವವರೆಗೆ ನಿಲ್ಲಿಸಬೇಡಿ.", "AED ಬಂದ ತಕ್ಷಣ ಬಳಸಿ ಮತ್ತು ಅದರ ಸೂಚನೆ ಪಾಲಿಸಿ."),
            )),
        AidCard("bleeding", listOf("bleed", "blood", "cut", "wound", "खून", "रक्त", "चोट", "ರಕ್ತ", "ಗಾಯ"),
            mapOf("en" to "Heavy bleeding", "hi" to "ज़्यादा खून बहना", "kn" to "ಹೆಚ್ಚು ರಕ್ತಸ್ರಾವ"),
            mapOf(
                "en" to listOf("Call 112 / 108 for heavy bleeding.", "Press firmly on the wound with a clean cloth. Keep pressing.", "If the cloth soaks through, add more on top; do not remove it.", "Raise the injured limb if there is no broken bone.", "Keep the person lying down and warm."),
                "hi" to listOf("ज़्यादा खून बहे तो 112 / 108 पर कॉल करें।", "साफ कपड़े से घाव को कसकर दबाएँ और दबाए रखें।", "कपड़ा भीग जाए तो उसके ऊपर और कपड़ा रखें; हटाएँ नहीं।", "हड्डी न टूटी हो तो घायल अंग को ऊपर उठाएँ।", "व्यक्ति को लिटाए रखें और गर्म रखें।"),
                "kn" to listOf("ಹೆಚ್ಚು ರಕ್ತಸ್ರಾವವಿದ್ದರೆ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ.", "ಸ್ವಚ್ಛ ಬಟ್ಟೆಯಿಂದ ಗಾಯವನ್ನು ಗಟ್ಟಿಯಾಗಿ ಒತ್ತಿ ಹಿಡಿಯಿರಿ.", "ಬಟ್ಟೆ ಒದ್ದೆಯಾದರೆ ಅದರ ಮೇಲೆ ಇನ್ನಷ್ಟು ಬಟ್ಟೆ ಇಡಿ; ತೆಗೆಯಬೇಡಿ.", "ಮೂಳೆ ಮುರಿದಿಲ್ಲದಿದ್ದರೆ ಗಾಯಗೊಂಡ ಅಂಗವನ್ನು ಮೇಲೆತ್ತಿ.", "ವ್ಯಕ್ತಿಯನ್ನು ಮಲಗಿಸಿ, ಬೆಚ್ಚಗಿಡಿ."),
            )),
        AidCard("burns", listOf("burn", "scald", "fire", "जल", "आग", "ಸುಟ್ಟ", "ಬೆಂಕಿ"),
            mapOf("en" to "Burns", "hi" to "जलना", "kn" to "ಸುಟ್ಟ ಗಾಯ"),
            mapOf(
                "en" to listOf("Cool the burn under cool running water for 20 minutes.", "Remove rings and tight items before swelling starts.", "Cover loosely with clean cling film or a clean non-fluffy cloth.", "Do not use ice, butter, toothpaste or burst blisters.", "Call 112 / 108 for large, deep, face or breathing-related burns."),
                "hi" to listOf("जले हुए हिस्से को 20 मिनट ठंडे बहते पानी में रखें।", "सूजन से पहले अंगूठी और तंग चीज़ें हटा दें।", "साफ क्लिंग फिल्म या साफ कपड़े से हल्का ढकें।", "बर्फ, मक्खन, टूथपेस्ट न लगाएँ और छाले न फोड़ें।", "बड़ी, गहरी, चेहरे की या साँस से जुड़ी जलन में 112 / 108 पर कॉल करें।"),
                "kn" to listOf("ಸುಟ್ಟ ಜಾಗವನ್ನು 20 ನಿಮಿಷ ತಣ್ಣನೆಯ ಹರಿಯುವ ನೀರಿನಲ್ಲಿ ಇಡಿ.", "ಊತ ಬರುವ ಮುನ್ನ ಉಂಗುರ, ಬಿಗಿ ವಸ್ತುಗಳನ್ನು ತೆಗೆಯಿರಿ.", "ಸ್ವಚ್ಛ ಕ್ಲಿಂಗ್ ಫಿಲ್ಮ್ ಅಥವಾ ಸ್ವಚ್ಛ ಬಟ್ಟೆಯಿಂದ ಸಡಿಲವಾಗಿ ಮುಚ್ಚಿ.", "ಐಸ್, ಬೆಣ್ಣೆ, ಟೂತ್‌ಪೇಸ್ಟ್ ಹಚ್ಚಬೇಡಿ; ಗುಳ್ಳೆ ಒಡೆಯಬೇಡಿ.", "ದೊಡ್ಡ, ಆಳವಾದ, ಮುಖ ಅಥವಾ ಉಸಿರಾಟದ ಸುಟ್ಟಗಾಯಗಳಿಗೆ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ."),
            )),
        AidCard("choking", listOf("chok", "swallow", "stuck", "दम घुट", "गले", "निगल", "ಉಸಿರುಗಟ್ಟ", "ಗಂಟಲು"),
            mapOf("en" to "Choking", "hi" to "दम घुटना", "kn" to "ಉಸಿರುಗಟ್ಟುವಿಕೆ"),
            mapOf(
                "en" to listOf("If they can cough or speak, encourage coughing.", "If they cannot breathe: give 5 firm back blows between the shoulder blades.", "Then give 5 abdominal thrusts (Heimlich) just above the navel.", "Alternate 5 and 5 until the object comes out.", "If they become unconscious, call 112 / 108 and start CPR."),
                "hi" to listOf("खांस या बोल सकते हों तो खांसने को कहें।", "साँस न आ रही हो तो कंधों के बीच 5 ज़ोरदार थपकी मारें।", "फिर नाभि के ठीक ऊपर 5 बार पेट पर दबाव (हाइमलिक) दें।", "वस्तु निकलने तक 5 और 5 बारी-बारी करें।", "बेहोश हो जाएँ तो 112 / 108 पर कॉल करें और CPR शुरू करें।"),
                "kn" to listOf("ಕೆಮ್ಮಲು ಅಥವಾ ಮಾತನಾಡಲು ಸಾಧ್ಯವಾದರೆ ಕೆಮ್ಮಲು ಪ್ರೋತ್ಸಾಹಿಸಿ.", "ಉಸಿರಾಡಲಾಗದಿದ್ದರೆ ಭುಜಗಳ ನಡುವೆ 5 ಬಲವಾದ ಹೊಡೆತ ನೀಡಿ.", "ನಂತರ ಹೊಕ್ಕಳ ಮೇಲ್ಭಾಗದಲ್ಲಿ 5 ಬಾರಿ ಹೊಟ್ಟೆಯ ಒತ್ತಡ (ಹೈಮ್ಲಿಕ್) ನೀಡಿ.", "ವಸ್ತು ಹೊರ ಬರುವವರೆಗೆ 5 ಮತ್ತು 5 ಪರ್ಯಾಯವಾಗಿ ಮಾಡಿ.", "ಪ್ರಜ್ಞೆ ತಪ್ಪಿದರೆ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು CPR ಆರಂಭಿಸಿ."),
            )),
        AidCard("fracture", listOf("fractur", "broken", "bone", "sprain", "फ्रैक्चर", "हड्डी", "टूट", "ಮೂಳೆ", "ಮುರಿ"),
            mapOf("en" to "Suspected fracture", "hi" to "हड्डी टूटने का शक", "kn" to "ಮೂಳೆ ಮುರಿತದ ಸಂಶಯ"),
            mapOf(
                "en" to listOf("Keep the person still; do not try to straighten the limb.", "Support the injured part in the position found, with padding on both sides.", "Apply a cold pack wrapped in cloth to reduce swelling.", "Suspected head, neck or back injury: do not move them; call 112 / 108.", "Get medical help; do not give food or drink if surgery may be needed."),
                "hi" to listOf("व्यक्ति को स्थिर रखें; अंग को सीधा करने की कोशिश न करें।", "घायल हिस्से को जैसा है वैसा ही दोनों ओर गद्दी रखकर सहारा दें।", "सूजन घटाने के लिए कपड़े में लपेटा ठंडा पैक लगाएँ।", "सिर, गर्दन या पीठ की चोट का शक हो तो न हिलाएँ; 112 / 108 पर कॉल करें।", "चिकित्सा मदद लें; ऑपरेशन की आशंका हो तो खाना-पानी न दें।"),
                "kn" to listOf("ವ್ಯಕ್ತಿಯನ್ನು ಅಲುಗಾಡದಂತೆ ಇಡಿ; ಅಂಗವನ್ನು ನೇರಗೊಳಿಸಲು ಪ್ರಯತ್ನಿಸಬೇಡಿ.", "ಗಾಯಗೊಂಡ ಭಾಗವನ್ನು ಇದ್ದ ಸ್ಥಿತಿಯಲ್ಲೇ ಎರಡೂ ಬದಿ ಮೆತ್ತೆ ಇಟ್ಟು ಆಧರಿಸಿ.", "ಊತ ಕಡಿಮೆ ಮಾಡಲು ಬಟ್ಟೆಯಲ್ಲಿ ಸುತ್ತಿದ ತಣ್ಣನೆಯ ಪ್ಯಾಕ್ ಇಡಿ.", "ತಲೆ, ಕುತ್ತಿಗೆ ಅಥವಾ ಬೆನ್ನಿಗೆ ಗಾಯದ ಸಂಶಯವಿದ್ದರೆ ಸರಿಸಬೇಡಿ; 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ.", "ವೈದ್ಯಕೀಯ ಸಹಾಯ ಪಡೆಯಿರಿ; ಶಸ್ತ್ರಚಿಕಿತ್ಸೆ ಬೇಕಾಗಬಹುದಾದರೆ ಆಹಾರ, ನೀರು ನೀಡಬೇಡಿ."),
            )),
        AidCard("seizure", listOf("seizure", "fit", "convuls", "epilep", "दौरा", "मिर्गी", "ಸೆಳೆತ", "ಅಪಸ್ಮಾರ"),
            mapOf("en" to "Seizure", "hi" to "दौरा / मिर्गी", "kn" to "ಸೆಳೆತ / ಅಪಸ್ಮಾರ"),
            mapOf(
                "en" to listOf("Stay with the person and note the time it started.", "Clear hard objects away and cushion the head.", "Do not hold them down and never put anything in their mouth.", "When it stops, gently turn them onto their side.", "Call 112 / 108 if it lasts over 5 minutes, repeats, or they are injured or not waking."),
                "hi" to listOf("व्यक्ति के साथ रहें और शुरू होने का समय नोट करें।", "कठोर चीज़ें हटाएँ और सिर के नीचे गद्दी रखें।", "उन्हें पकड़कर न रखें और मुँह में कुछ न डालें।", "रुकने पर धीरे से करवट दिला दें।", "5 मिनट से ज़्यादा चले, दोबारा आए, चोट लगे या होश न आए तो 112 / 108 पर कॉल करें।"),
                "kn" to listOf("ವ್ಯಕ್ತಿಯ ಜೊತೆಯಲ್ಲೇ ಇದ್ದು ಆರಂಭವಾದ ಸಮಯ ಗಮನಿಸಿ.", "ಗಟ್ಟಿ ವಸ್ತುಗಳನ್ನು ದೂರ ಸರಿಸಿ, ತಲೆಯ ಕೆಳಗೆ ಮೆತ್ತೆ ಇಡಿ.", "ಅವರನ್ನು ಹಿಡಿದಿಡಬೇಡಿ; ಬಾಯಿಯಲ್ಲಿ ಏನನ್ನೂ ಹಾಕಬೇಡಿ.", "ನಿಂತ ನಂತರ ನಿಧಾನವಾಗಿ ಒಂದು ಬದಿಗೆ ತಿರುಗಿಸಿ.", "5 ನಿಮಿಷಕ್ಕಿಂತ ಹೆಚ್ಚು ಇದ್ದರೆ, ಪುನಃ ಬಂದರೆ, ಗಾಯವಾದರೆ ಅಥವಾ ಎಚ್ಚರವಾಗದಿದ್ದರೆ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ."),
            )),
    )

    fun match(question: String): AidCard? {
        val q = question.lowercase()
        return cards.firstOrNull { c -> c.keywords.any { q.contains(it.lowercase()) } }
    }

    /** Offline reply used when the assistant cannot be reached. Always starts with the emergency line. */
    fun offlineReply(question: String, lang: String): String {
        val l = if (lang in listOf("en", "hi", "kn")) lang else "en"
        val c = match(question) ?: return emergencyLine.getValue(l) + "\n" + cards.joinToString(", ") { it.title.getValue(l) }
        return emergencyLine.getValue(l) + "\n" + c.title.getValue(l) + ":\n" + c.steps.getValue(l).mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
    }
}
