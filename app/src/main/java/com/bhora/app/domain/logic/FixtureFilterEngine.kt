package com.bhora.app.domain.logic

import com.bhora.app.domain.model.BroadcastService
import com.bhora.app.domain.model.FixtureDayGroup
import com.bhora.app.domain.model.MatchFilter
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import com.bhora.app.domain.model.ServiceFilter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object FixtureFilterEngine {
    private const val STALE_UPCOMING_GRACE_MILLIS = 4L * 60L * 60L * 1000L

    fun filterAndGroup(
        fixtures: List<PrimeFixture>,
        filter: MatchFilter,
        searchQuery: String,
        serviceFilter: ServiceFilter = ServiceFilter.ALL,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
        locale: Locale = Locale.getDefault()
    ): List<FixtureDayGroup> {
        val localToday = now.atZone(zoneId).toLocalDate()
        val nowMillis = now.toEpochMilli()
        val normalizedQuery = searchQuery.trim().lowercase(locale)

        val visible = fixtures
            .asSequence()
            .mapNotNull { fixture ->
                val matchingRegions = when (serviceFilter) {
                    ServiceFilter.ALL -> fixture.primeRegions
                    ServiceFilter.PRIME -> fixture.primeRegions.filter { it.service == BroadcastService.PRIME_VIDEO }
                    ServiceFilter.MAX -> fixture.primeRegions.filter { it.service == BroadcastService.HBO_MAX }
                }
                if (matchingRegions.isEmpty()) null else fixture.copy(primeRegions = matchingRegions)
            }
            .filter { fixture ->
                val localDate = Instant.ofEpochMilli(fixture.kickoffUtcMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                when (fixture.status) {
                    MatchStatus.LIVE -> true
                    MatchStatus.ENDED -> !localDate.isBefore(localToday)
                    MatchStatus.UPCOMING ->
                        !localDate.isBefore(localToday) ||
                            fixture.kickoffUtcMillis >= nowMillis - STALE_UPCOMING_GRACE_MILLIS
                }
            }
            .filter { fixture ->
                when (filter) {
                    MatchFilter.ALL -> true
                    MatchFilter.LIVE_ONLY -> fixture.status == MatchStatus.LIVE
                }
            }
            .filter { fixture ->
                if (normalizedQuery.isEmpty()) {
                    true
                } else {
                    fixture.homeTeam.lowercase(locale).contains(normalizedQuery) ||
                        fixture.awayTeam.lowercase(locale).contains(normalizedQuery) ||
                        fixture.competition.displayLabel.lowercase(locale).contains(normalizedQuery) ||
                        (normalizedQuery == "ucl" && fixture.competition.name == "CHAMPIONS_LEAGUE") ||
                        (normalizedQuery == "epl" && fixture.competition.name == "PREMIER_LEAGUE")
                }
            }
            .sortedWith(compareBy<PrimeFixture> { it.kickoffUtcMillis }.thenBy { it.homeTeam })
            .toList()

        return visible
            .groupBy { fixture ->
                Instant.ofEpochMilli(fixture.kickoffUtcMillis)
                    .atZone(zoneId)
                    .toLocalDate()
            }
            .entries
            .sortedBy { it.key }
            .map { (date, dayFixtures) ->
                FixtureDayGroup(
                    date = date,
                    dateLabel = FixtureTimeFormatter.formatDateHeader(date, locale),
                    fixtures = dayFixtures
                )
            }
    }
}
