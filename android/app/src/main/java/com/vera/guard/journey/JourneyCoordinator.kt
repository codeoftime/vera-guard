package com.vera.guard.journey

import com.vera.guard.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class JourneyRequest(
    val destName: String, val dest: LatLng, val mode: String, val route: List<LatLng>,
    val etaMs: Long, val bufferMs: Long, val followers: List<Contact>,
    val cabVehicle: String = "", val cabDriver: String = "",
)

data class JourneyUiState(
    val active: Boolean = false,
    val destName: String = "",
    val mode: String = "",
    val status: String = "on_route",     // on_route | deviated | stopped | delayed | arrived | alert
    val etaMs: Long = 0,
    val trackUrl: String? = null,
    val simulatingDead: Boolean = false,
    val message: String? = null,
    val current: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val error: String? = null,
)

/** Hand-off between the Journey screen and GuardService, and the shared UI state of the running journey. */
@Singleton
class JourneyCoordinator @Inject constructor() {
    @Volatile var pending: JourneyRequest? = null
    private val _state = MutableStateFlow(JourneyUiState())
    val state: StateFlow<JourneyUiState> = _state.asStateFlow()
    fun update(f: (JourneyUiState) -> JourneyUiState) { _state.value = f(_state.value) }
    fun reset() { _state.value = JourneyUiState() }
}
