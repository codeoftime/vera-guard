package com.vera.guard

import com.vera.guard.escalation.EventType
import com.vera.guard.escalation.Facility
import com.vera.guard.escalation.SmsBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsBuilderTest {
    private val hosp = listOf(Facility("City Hospital", 800), Facility("Lotus Clinic", 1500), Facility("Care Centre", 2300), Facility("Fourth", 4000))
    private fun fall(lang: String, demo: Boolean = false) = SmsBuilder.alert(
        lang, "Asha", "04:30 am", EventType.FALL, "HIGH", 82, "https://v.test/track/abc", "https://v.test/brief/abc", 55, hosp, demo,
    )

    @Test fun english() {
        val s = fall("en")
        for (x in listOf("Asha", "FALL", "04:30 am", "High (82/100)", "https://v.test/track/abc", "https://v.test/brief/abc", "55%", "City Hospital (800 m)", "Care Centre (2.3 km)", "112/108")) assertTrue("missing $x in $s", s.contains(x))
        assertFalse("only 3 hospitals", s.contains("Fourth")); assertFalse(s.contains("[DEMO]"))
    }
    @Test fun hindi() {
        val s = fall("hi")
        assertTrue(s.contains("गिरना")); assertTrue(s.contains("उच्च (82/100)")); assertTrue(s.contains("https://v.test/track/abc")); assertTrue(s.contains("City Hospital (800 m)"))
    }
    @Test fun kannada() {
        val s = fall("kn")
        assertTrue(s.contains("ಬೀಳುವಿಕೆ")); assertTrue(s.contains("ಹೆಚ್ಚು (82/100)")); assertTrue(s.contains("https://v.test/brief/abc")); assertTrue(s.contains("112/108"))
    }
    @Test fun unknownLanguageFallsBackToEnglishAndDemoIsLabelled() {
        val s = fall("fr", demo = true)
        assertTrue(s.startsWith("[DEMO] ")); assertTrue(s.contains("FALL"))
    }
    @Test fun sosHasNoSeverityLine() {
        val s = SmsBuilder.alert("en", "Asha", "now", EventType.SOS, null, null, "https://t", null, null, emptyList(), false)
        assertTrue(s.contains("SOS")); assertFalse(s.contains("Severity")); assertFalse(s.contains("Battery"))
    }
    @Test fun shortSmsHasCoordinatesBloodGroupAndHospital() {
        val s = SmsBuilder.shortAlert("en", EventType.FALL, "HIGH", 82, 12.9716, 77.5946, "B+", "City Hospital")
        assertTrue(s.contains("12.97160,77.59460")); assertTrue(s.contains("Blood group: B+")); assertTrue(s.contains("City Hospital")); assertTrue(s.length < 160)
        assertFalse(SmsBuilder.shortAlert("en", EventType.SOS, null, null, 1.0, 2.0, null, null).contains("Blood"))
    }
    @Test fun journeyMessages() {
        for (l in listOf("en", "hi", "kn")) {
            val s = SmsBuilder.journeyStart(l, "Asha", "Office", "06:10 pm", "https://v.test/track/j")
            assertTrue(s.contains("Asha") && s.contains("Office") && s.contains("06:10 pm") && s.contains("https://v.test/track/j"))
            assertTrue(SmsBuilder.arrived(l, "Asha").contains("Asha"))
        }
    }
}
