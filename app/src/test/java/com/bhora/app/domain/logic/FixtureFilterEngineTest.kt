package com.bhora.app.domain.logic

import com.bhora.app.domain.model.BroadcastRegion
import com.bhora.app.domain.model.BroadcastService
import com.bhora.app.domain.model.Competition
import com.bhora.app.domain.model.MatchFilter
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import com.bhora.app.domain.model.ServiceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class FixtureFilterEngineTest {

    private val denmarkPrime = listOf(BroadcastRegion("Denmark", "Amazon Prime Video"))

    private fun fixture(
        id: String,
        home: String,
        away: String,
        isoUtc: String,
        status: MatchStatus,
        regions: List<BroadcastRegion> = denmarkPrime,
        competition: Competition = Competition.PREMIER_LEAGUE
    ) = PrimeFixture(
        id = id,
        homeTeam = home,
        awayTeam = away,
        kickoffUtcMillis = Instant.parse(isoUtc).toEpochMilli(),
        status = status,
        matchPath = "/match/$id",
        primeRegions = regions,
        competition = competition
    )

    @Test
    fun `keeps today finished, live, and upcoming games while pruning yesterday finished games`() {
        val zone = ZoneId.of("America/Vancouver") // UTC-8
        // Local time: Saturday 2026-11-21 18:00 PST (which is Sunday 2026-11-22 02:00 UTC!)
        val now = Instant.parse("2026-11-22T02:00:00Z")

        val fixtures = listOf(
            fixture("old", "Chelsea", "Man Utd", "2026-11-20T12:30:00Z", MatchStatus.ENDED),
            fixture("today_ft", "Manchester City", "Fulham", "2026-11-21T12:30:00Z", MatchStatus.ENDED),
            fixture("live", "Arsenal", "Chelsea", "2026-11-22T01:30:00Z", MatchStatus.LIVE),
            fixture("next_week", "Chelsea", "Liverpool", "2026-11-28T12:30:00Z", MatchStatus.UPCOMING),
            fixture("no_prime", "Everton", "Brentford", "2026-11-22T15:00:00Z", MatchStatus.UPCOMING, emptyList())
        )

        val groups = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = MatchFilter.ALL,
            searchQuery = "",
            zoneId = zone,
            now = now,
            locale = Locale.US
        )

        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 11, 21), groups[0].date)
        assertEquals("SATURDAY 21 NOVEMBER", groups[0].dateLabel)
        assertEquals(listOf("today_ft", "live"), groups[0].fixtures.map { it.id })

        assertEquals(LocalDate.of(2026, 11, 28), groups[1].date)
        assertEquals(listOf("next_week"), groups[1].fixtures.map { it.id })
    }

    @Test
    fun `filters by LIVE_ONLY and case-insensitive club search, omitting empty date groups`() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-11-21T15:00:00Z")

        val fixtures = listOf(
            fixture("f1", "Manchester City", "Fulham", "2026-11-21T12:30:00Z", MatchStatus.ENDED),
            fixture("f2", "Arsenal", "Chelsea", "2026-11-21T15:00:00Z", MatchStatus.LIVE),
            fixture("f3", "Liverpool", "Manchester United", "2026-11-22T16:30:00Z", MatchStatus.UPCOMING)
        )

        val liveOnly = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = MatchFilter.LIVE_ONLY,
            searchQuery = "",
            zoneId = zone,
            now = now,
            locale = Locale.UK
        )
        assertEquals(1, liveOnly.size)
        assertEquals(listOf("f2"), liveOnly[0].fixtures.map { it.id })

        val clubSearch = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = MatchFilter.ALL,
            searchQuery = "  manchester ",
            zoneId = zone,
            now = now,
            locale = Locale.UK
        )
        assertEquals(2, clubSearch.size)
        assertEquals(listOf("f1"), clubSearch[0].fixtures.map { it.id })
        assertEquals(listOf("f3"), clubSearch[1].fixtures.map { it.id })

        val noMatch = FixtureFilterEngine.filterAndGroup(
            fixtures = fixtures,
            filter = MatchFilter.ALL,
            searchQuery = "Barcelona",
            zoneId = zone,
            now = now,
            locale = Locale.UK
        )
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `filters by ServiceFilter ALL, PRIME, and MAX and scopes displayed regions to active service`() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-11-21T15:00:00Z")
        val maxMexico = listOf(BroadcastRegion("Mexico", "HBO Max", "/channels/hbo-max/", BroadcastService.HBO_MAX))
        val both = denmarkPrime + maxMexico

        val fixtures = listOf(
            fixture("prime_only", "Manchester City", "Fulham", "2026-11-21T12:30:00Z", MatchStatus.ENDED, denmarkPrime),
            fixture("max_only", "Chelsea", "Leeds United", "2026-11-21T15:00:00Z", MatchStatus.LIVE, maxMexico),
            fixture("both_services", "Newcastle United", "Arsenal", "2026-11-21T17:30:00Z", MatchStatus.UPCOMING, both)
        )

        val all = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "", ServiceFilter.ALL, zone, now, Locale.UK)
        assertEquals(listOf("prime_only", "max_only", "both_services"), all[0].fixtures.map { it.id })
        assertEquals(2, all[0].fixtures[2].primeRegions.size)

        val primeOnly = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "", ServiceFilter.PRIME, zone, now, Locale.UK)
        assertEquals(listOf("prime_only", "both_services"), primeOnly[0].fixtures.map { it.id })
        assertEquals(listOf("Denmark"), primeOnly[0].fixtures[1].primeRegions.map { it.country })

        val maxOnly = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "", ServiceFilter.MAX, zone, now, Locale.UK)
        assertEquals(listOf("max_only", "both_services"), maxOnly[0].fixtures.map { it.id })
        assertEquals(listOf("Mexico"), maxOnly[0].fixtures[1].primeRegions.map { it.country })

        val liveMax = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.LIVE_ONLY, "", ServiceFilter.MAX, zone, now, Locale.UK)
        assertEquals(listOf("max_only"), liveMax[0].fixtures.map { it.id })
    }

    @Test
    fun `formats kickoff time in local timezone`() {
        val millis = Instant.parse("2026-11-21T12:30:00Z").toEpochMilli()
        val formattedCopenhagen = FixtureTimeFormatter.formatKickoffTime(
            kickoffUtcMillis = millis,
            zoneId = ZoneId.of("Europe/Copenhagen"),
            locale = Locale.UK
        )
        assertEquals("13:30 LOCAL", formattedCopenhagen)

        val formattedUtcMinus7 = FixtureTimeFormatter.formatKickoffTime(
            kickoffUtcMillis = millis,
            zoneId = ZoneId.of("UTC-7"),
            locale = Locale.CANADA
        )
        assertEquals("05:30 LOCAL", formattedUtcMinus7)
    }

    @Test
    fun `matches Bundesliga, Serie A, and Ligue 1 competition queries`() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-11-21T12:00:00Z")
        val cazeBrazil = listOf(BroadcastRegion("Brazil", "CazéTV (Prime Video)", "/channels/cazetv/", BroadcastService.PRIME_VIDEO))

        val fixtures = listOf(
            fixture("de1", "Bayern München", "Borussia Dortmund", "2026-11-21T17:30:00Z", MatchStatus.UPCOMING, cazeBrazil, Competition.BUNDESLIGA),
            fixture("it1", "Internazionale", "Juventus", "2026-11-21T19:45:00Z", MatchStatus.UPCOMING, cazeBrazil, Competition.SERIE_A),
            fixture("fr1", "Nice", "PSG", "2026-11-21T20:00:00Z", MatchStatus.UPCOMING, cazeBrazil, Competition.LIGUE_1)
        )

        val bundesliga = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "bundesliga", ServiceFilter.ALL, zone, now, Locale.UK)
        assertEquals(listOf("de1"), bundesliga.single().fixtures.map { it.id })

        val serieA = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "serie a", ServiceFilter.ALL, zone, now, Locale.UK)
        assertEquals(listOf("it1"), serieA.single().fixtures.map { it.id })

        val ligue1 = FixtureFilterEngine.filterAndGroup(fixtures, MatchFilter.ALL, "ligue 1", ServiceFilter.ALL, zone, now, Locale.UK)
        assertEquals(listOf("fr1"), ligue1.single().fixtures.map { it.id })
    }
}
