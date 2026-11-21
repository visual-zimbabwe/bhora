package com.bhora.app.ui.matchday

import com.bhora.app.domain.model.FixtureDayGroup
import com.bhora.app.domain.model.MatchFilter
import com.bhora.app.domain.model.ServiceFilter
import java.time.ZoneId

data class MatchdayUiState(
    val dayGroups: List<FixtureDayGroup> = emptyList(),
    val totalPrimeFixtureCount: Int = 0,
    val activeFilter: MatchFilter = MatchFilter.ALL,
    val serviceFilter: ServiceFilter = ServiceFilter.ALL,
    val searchQuery: String = "",
    val expandedFixtureIds: Set<String> = emptySet(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val noticeMessage: String? = null,
    val lastUpdatedMillis: Long? = null
)
