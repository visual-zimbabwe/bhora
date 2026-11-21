package com.bhora.app.data.cache

import com.bhora.app.domain.model.BroadcastRegion
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FixtureDiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `round trips cache envelope atomically and recovers from corrupt files`() {
        val file = File(tempFolder.root, "bhora_cache.json")
        val cache = FixtureDiskCache(file)

        val envelope = CacheEnvelope(
            version = FixtureDiskCache.CURRENT_VERSION,
            lastUpdatedMillis = 1795273200000L,
            fixtures = listOf(
                PrimeFixture(
                    id = "5661137",
                    homeTeam = "Manchester City",
                    awayTeam = "Fulham",
                    kickoffUtcMillis = 1795264200000L,
                    status = MatchStatus.ENDED,
                    matchPath = "/match/fulham-vs-manchester-city/17tcj",
                    primeRegions = listOf(
                        BroadcastRegion("Denmark", "Amazon Prime Video", "/channels/amazon-prime-videodk/"),
                        BroadcastRegion("Netherlands", "Amazon Prime Video", "/channels/amazon-prime-videonl/"),
                        BroadcastRegion("Sweden", "Amazon Prime Video", "/channels/amazon-prime-videodk/")
                    )
                )
            ),
            broadcastChecks = mapOf(
                "5661137" to CachedBroadcastEntry(
                    checkedAtMillis = 1795273200000L,
                    primeRegions = listOf(BroadcastRegion("Denmark", "Amazon Prime Video"))
                )
            )
        )

        cache.write(envelope)
        assertEquals(envelope, cache.read())

        // Older schema version clears stale broadcastChecks while preserving fixtures
        cache.write(envelope.copy(version = 1))
        val migrated = cache.read()
        assertEquals(FixtureDiskCache.CURRENT_VERSION, migrated.version)
        assertTrue(migrated.broadcastChecks.isEmpty())
        assertEquals(1, migrated.fixtures.size)

        // Corrupt JSON gracefully resets to empty envelope
        file.writeText("{ broken json")
        val recovered = cache.read()
        assertTrue(recovered.fixtures.isEmpty())
        assertEquals(0L, recovered.lastUpdatedMillis)
    }

    @Test
    fun `applies distinct TTLs for live, near-upcoming, far-upcoming, and finished matches`() {
        val now = 1_000_000_000L
        val fiveMinAgo = CachedBroadcastEntry(now - 5 * 60_000L, emptyList())
        val fifteenMinAgo = CachedBroadcastEntry(now - 15 * 60_000L, emptyList())
        val twoHoursAgo = CachedBroadcastEntry(now - 2 * 3_600_000L, emptyList())
        val sixHoursAgo = CachedBroadcastEntry(now - 6 * 3_600_000L, emptyList())

        // LIVE: 10m TTL
        assertTrue(FixtureDiskCache.isBroadcastCheckFresh(fiveMinAgo, MatchStatus.LIVE, now, now))
        assertFalse(FixtureDiskCache.isBroadcastCheckFresh(fifteenMinAgo, MatchStatus.LIVE, now, now))

        // Near upcoming (<24h): 1h TTL
        val inThreeHours = now + 3 * 3_600_000L
        assertTrue(FixtureDiskCache.isBroadcastCheckFresh(fifteenMinAgo, MatchStatus.UPCOMING, inThreeHours, now))
        assertFalse(FixtureDiskCache.isBroadcastCheckFresh(twoHoursAgo, MatchStatus.UPCOMING, inThreeHours, now))

        // Far upcoming (>24h): 4h TTL
        val inThreeDays = now + 72 * 3_600_000L
        assertTrue(FixtureDiskCache.isBroadcastCheckFresh(twoHoursAgo, MatchStatus.UPCOMING, inThreeDays, now))
        assertFalse(FixtureDiskCache.isBroadcastCheckFresh(sixHoursAgo, MatchStatus.UPCOMING, inThreeDays, now))

        // Finished: 24h TTL
        assertTrue(FixtureDiskCache.isBroadcastCheckFresh(sixHoursAgo, MatchStatus.ENDED, now - 3_600_000L, now))
    }
}
