package com.vera.guard.escalation

data class Facility(val name: String, val distanceM: Int, val phone: String? = null, val lat: Double = 0.0, val lng: Double = 0.0)

/** Builds SMS text in the contact's preferred language (en / hi / kn). Pure, unit-tested. */
object SmsBuilder {
    private class Pack(
        val types: Map<EventType, String>, val sev: Map<String, String>,
        val head: (String, String, String) -> String, val severityLine: (String, String, Int) -> String,
        val track: String, val brief: String, val battery: String, val hospitals: String, val demo: String,
        val call: String, val journey: (String, String, String, String) -> String, val arrived: (String) -> String, val blood: String,
    )

    private val packs = mapOf(
        "en" to Pack(
            mapOf(EventType.FALL to "FALL", EventType.CRASH to "CRASH", EventType.SOS to "SOS", EventType.JOURNEY_ALERT to "JOURNEY ALERT"),
            mapOf("LOW" to "Low", "MEDIUM" to "Medium", "HIGH" to "High"),
            { n, time, type -> "VERA ALERT: $n - $type at $time." }, { type, sev, score -> "Severity $sev ($score/100)." },
            "Live location", "Responder Brief", "Battery", "Nearest hospitals", "[DEMO] ", "Call 112/108 if unreachable.",
            { n, place, eta, link -> "$n started a journey to $place. ETA $eta. Track: $link" }, { n -> "$n arrived safely." }, "Blood group",
        ),
        "hi" to Pack(
            mapOf(EventType.FALL to "गिरना", EventType.CRASH to "दुर्घटना", EventType.SOS to "SOS", EventType.JOURNEY_ALERT to "यात्रा अलर्ट"),
            mapOf("LOW" to "कम", "MEDIUM" to "मध्यम", "HIGH" to "उच्च"),
            { n, time, type -> "VERA अलर्ट: $n - $type, समय $time।" }, { _, sev, score -> "गंभीरता $sev ($score/100)।" },
            "लाइव लोकेशन", "रेस्पॉन्डर ब्रीफ़", "बैटरी", "नज़दीकी अस्पताल", "[डेमो] ", "संपर्क न हो तो 112/108 पर कॉल करें।",
            { n, place, eta, link -> "$n ने $place की यात्रा शुरू की। ETA $eta। ट्रैक करें: $link" }, { n -> "$n सुरक्षित पहुँच गए।" }, "रक्त समूह",
        ),
        "kn" to Pack(
            mapOf(EventType.FALL to "ಬೀಳುವಿಕೆ", EventType.CRASH to "ಅಪಘಾತ", EventType.SOS to "SOS", EventType.JOURNEY_ALERT to "ಪ್ರಯಾಣ ಎಚ್ಚರಿಕೆ"),
            mapOf("LOW" to "ಕಡಿಮೆ", "MEDIUM" to "ಮಧ್ಯಮ", "HIGH" to "ಹೆಚ್ಚು"),
            { n, time, type -> "VERA ಎಚ್ಚರಿಕೆ: $n - $type, ಸಮಯ $time." }, { _, sev, score -> "ತೀವ್ರತೆ $sev ($score/100)." },
            "ಲೈವ್ ಸ್ಥಳ", "ರೆಸ್ಪಾಂಡರ್ ಬ್ರೀಫ್", "ಬ್ಯಾಟರಿ", "ಹತ್ತಿರದ ಆಸ್ಪತ್ರೆಗಳು", "[ಡೆಮೊ] ", "ಸಂಪರ್ಕ ಸಾಧ್ಯವಾಗದಿದ್ದರೆ 112/108 ಗೆ ಕರೆ ಮಾಡಿ.",
            { n, place, eta, link -> "$n $place ಗೆ ಪ್ರಯಾಣ ಆರಂಭಿಸಿದ್ದಾರೆ. ETA $eta. ಟ್ರ್ಯಾಕ್: $link" }, { n -> "$n ಸುರಕ್ಷಿತವಾಗಿ ತಲುಪಿದ್ದಾರೆ." }, "ರಕ್ತದ ಗುಂಪು",
        ),
    )

    private fun pack(lang: String) = packs[lang] ?: packs.getValue("en")

    fun alert(
        lang: String, userName: String, timeText: String, type: EventType, severity: String?, score: Int?,
        trackUrl: String?, briefUrl: String?, battery: Int?, hospitals: List<Facility>, demo: Boolean,
    ): String {
        val p = pack(lang)
        val sb = StringBuilder()
        if (demo) sb.append(p.demo)
        sb.append(p.head(userName, timeText, p.types.getValue(type)))
        if (severity != null && score != null && (type == EventType.FALL || type == EventType.CRASH)) sb.append(' ').append(p.severityLine(type.api, p.sev[severity] ?: severity, score))
        if (trackUrl != null) sb.append(' ').append(p.track).append(": ").append(trackUrl)
        if (briefUrl != null) sb.append(' ').append(p.brief).append(": ").append(briefUrl)
        if (battery != null) sb.append(' ').append(p.battery).append(' ').append(battery).append('%')
        if (hospitals.isNotEmpty()) {
            sb.append(' ').append(p.hospitals).append(": ")
            sb.append(hospitals.take(3).joinToString("; ") { "${it.name} (${formatDistance(it.distanceM)})" })
        }
        sb.append(' ').append(p.call)
        return sb.toString()
    }

    /** Short SMS for no-internet situations: type, severity, coordinates, blood group (if consented), nearest hospital. */
    fun shortAlert(lang: String, type: EventType, severity: String?, score: Int?, lat: Double?, lng: Double?, bloodGroup: String?, hospital: String?): String {
        val p = pack(lang)
        val parts = ArrayList<String>()
        parts += "VERA ${p.types.getValue(type)}"
        if (severity != null && score != null) parts += "$severity $score"
        if (lat != null && lng != null) parts += String.format(java.util.Locale.US, "%.5f,%.5f", lat, lng)
        if (!bloodGroup.isNullOrBlank()) parts += "${p.blood}: $bloodGroup"
        if (!hospital.isNullOrBlank()) parts += hospital
        return parts.joinToString(" | ")
    }

    fun journeyStart(lang: String, name: String, place: String, eta: String, link: String) = pack(lang).journey(name, place, eta, link)
    fun arrived(lang: String, name: String) = pack(lang).arrived(name)

    fun formatDistance(m: Int) = if (m >= 1000) String.format(java.util.Locale.US, "%.1f km", m / 1000.0) else "$m m"
}
