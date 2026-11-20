package com.vera.guard.escalation

enum class EventType(val api: String) {
    FALL("fall"), CRASH("crash"), SOS("sos"), JOURNEY_ALERT("journey_alert");
}
