package com.bhora.app.ui.matchday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhora.app.data.repository.PrimeFixturesRepository
import com.bhora.app.domain.logic.FixtureFilterEngine
import com.bhora.app.domain.model.MatchFilter
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import com.bhora.app.domain.model.ServiceFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MatchdayViewModel(
    private val repository: PrimeFixturesRepository,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
) : ViewModel() {

    private val rawFixtures = MutableStateFlow<List<PrimeFixture>>(emptyList())
    private val _uiState = MutableStateFlow(MatchdayUiState())
    val uiState: StateFlow<MatchdayUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var livePollJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val cached = repository.loadCachedSnapshot()
        if (cached != null) {
            rawFixtures.value = cached.fixtures
            _uiState.update { state ->
                recomputeDisplay(
                    state = state.copy(
                        isInitialLoading = false,
                        lastUpdatedMillis = cached.lastUpdatedMillis
                    ),
                    fixtures = cached.fixtures
                )
            }
        }
        refresh(forceBroadcastRescan = false, userInitiated = false)
    }

    fun onSearchQueryChange(query: String) {
        val clamped = query.take(24)
        _uiState.update { state ->
            recomputeDisplay(state.copy(searchQuery = clamped), rawFixtures.value)
        }
    }

    fun onFilterSelected(filter: MatchFilter) {
        _uiState.update { state ->
            recomputeDisplay(state.copy(activeFilter = filter), rawFixtures.value)
        }
    }

    fun onServiceFilterSelected(serviceFilter: ServiceFilter) {
        _uiState.update { state ->
            recomputeDisplay(state.copy(serviceFilter = serviceFilter), rawFixtures.value)
        }
    }

    fun onTimeZoneChanged(newZoneId: ZoneId = zoneIdProvider()) {
        _uiState.update { state ->
            if (state.zoneId == newZoneId) {
                state
            } else {
                recomputeDisplay(state.copy(zoneId = newZoneId), rawFixtures.value)
            }
        }
    }

    fun onToggleFixtureExpanded(fixtureId: String) {
        _uiState.update { state ->
            val updated = if (fixtureId in state.expandedFixtureIds) {
                state.expandedFixtureIds - fixtureId
            } else {
                state.expandedFixtureIds + fixtureId
            }
            state.copy(expandedFixtureIds = updated)
        }
    }

    fun onPullToRefresh() {
        refresh(forceBroadcastRescan = true, userInitiated = true)
    }

    fun onRetry() {
        refresh(forceBroadcastRescan = true, userInitiated = true)
    }

    fun startForegroundLivePolling() {
        if (livePollJob?.isActive == true) return
        livePollJob = viewModelScope.launch {
            while (isActive) {
                delay(LIVE_POLL_INTERVAL_MS)
                val current = rawFixtures.value
                val nowMillis = System.currentTimeMillis()
                val shouldPoll = current.any { fixture ->
                    fixture.status == MatchStatus.LIVE ||
                        (fixture.status == MatchStatus.UPCOMING &&
                            fixture.kickoffUtcMillis - nowMillis <= PRE_KICKOFF_POLL_WINDOW_MS)
                }
                if (shouldPoll) {
                    val updated = repository.refreshLiveStatusesOnly(current)
                    if (updated != current) {
                        rawFixtures.value = updated
                        _uiState.update { state ->
                            recomputeDisplay(state, updated)
                        }
                    }
                }
            }
        }
    }

    fun stopForegroundLivePolling() {
        livePollJob?.cancel()
        livePollJob = null
    }

    private fun refresh(forceBroadcastRescan: Boolean, userInitiated: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isInitialLoading = state.lastUpdatedMillis == null && state.dayGroups.isEmpty(),
                    isRefreshing = userInitiated || state.lastUpdatedMillis != null
                )
            }

            val result = repository.refreshFixtures(forceBroadcastRescan = forceBroadcastRescan)
            rawFixtures.value = result.fixtures
            _uiState.update { state ->
                recomputeDisplay(
                    state = state.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        noticeMessage = result.noticeMessage,
                        lastUpdatedMillis = result.lastUpdatedMillis
                    ),
                    fixtures = result.fixtures
                )
            }
        }
    }

    private fun recomputeDisplay(
        state: MatchdayUiState,
        fixtures: List<PrimeFixture>
    ): MatchdayUiState {
        val zoneId = zoneIdProvider()
        val now = Instant.now()
        val allRelevant = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = MatchFilter.ALL,
            searchQuery = "",
            serviceFilter = state.serviceFilter,
            zoneId = zoneId,
            now = now
        )
        val filtered = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = state.activeFilter,
            searchQuery = state.searchQuery,
            serviceFilter = state.serviceFilter,
            zoneId = zoneId,
            now = now
        )
        return state.copy(
            dayGroups = filtered,
            totalPrimeFixtureCount = allRelevant.sumOf { it.fixtures.size },
            zoneId = zoneId
        )
    }

    companion object {
        private const val LIVE_POLL_INTERVAL_MS = 60_000L
        private const val PRE_KICKOFF_POLL_WINDOW_MS = 15L * 60L * 1000L

        fun factory(repository: PrimeFixturesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MatchdayViewModel(repository) as T
                }
            }
    }
}
