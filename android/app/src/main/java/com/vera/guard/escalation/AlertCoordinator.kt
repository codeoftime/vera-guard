package com.vera.guard.escalation

import com.vera.guard.data.SecureStore
import com.vera.guard.detect.FallMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AlertPhase { NONE, COUNTDOWN, CHECKIN, ESCALATING, ESCALATED, CANCELLED }

data class AlertUiState(
    val phase: AlertPhase = AlertPhase.NONE,
    val type: EventType? = null,
    val severity: String? = null, val score: Int? = null, val metrics: FallMetrics? = null,
    val remaining: Int = 0, val total: Int = 0,
    val silent: Boolean = false,
    val fakeCancelled: Boolean = false,         // duress PIN: UI shows "cancelled" while the alert continues
    val trackUrl: String? = null, val briefUrl: String? = null,
    val ambulanceNumber: String? = null, val ambulanceNeedsManualCall: Boolean = false,
    val steps: List<String> = emptyList(),
    val acked: Boolean = false,
)

enum class PinResult { WRONG, CANCELLED, DURESS }

/** Shared state between GuardService (owner) and AlertActivity (view). */
@Singleton
class AlertCoordinator @Inject constructor(private val store: SecureStore) {
    private val _state = MutableStateFlow(AlertUiState())
    val state: StateFlow<AlertUiState> = _state.asStateFlow()

    @Volatile var onCancel: ((String) -> Unit)? = null     // source: tap | voice | pin | walking
    @Volatile var onNeedHelp: (() -> Unit)? = null          // check-in: "I need help" / escalate now
    @Volatile var onDuress: (() -> Unit)? = null

    fun update(f: (AlertUiState) -> AlertUiState) { _state.value = f(_state.value) }
    fun set(s: AlertUiState) { _state.value = s }
    fun clear() { _state.value = AlertUiState() }

    fun cancelByUser(source: String) { onCancel?.invoke(source) }
    fun needHelp() { onNeedHelp?.invoke() }

    fun submitPin(pin: String): PinResult {
        if (store.checkPin("duress", pin)) { onDuress?.invoke(); return PinResult.DURESS }
        if (store.checkPin("cancel", pin)) { onCancel?.invoke("pin"); return PinResult.CANCELLED }
        return PinResult.WRONG
    }
    fun hasAnyPin() = store.hasPin("cancel") || store.hasPin("duress")
}
