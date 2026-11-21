package com.bhora.app.data.repository

import com.bhora.app.data.cache.CacheEnvelope
import com.bhora.app.data.cache.CachedBroadcastEntry
import com.bhora.app.data.cache.FixtureDiskCache
import com.bhora.app.data.remote.EspnMatchStatus
import com.bhora.app.data.remote.EspnScoreboardClient
import com.bhora.app.data.remote.EspnSnapshot
import com.bhora.app.data.remote.LiveSoccerTvClient
import com.bhora.app.data.remote.LiveSoccerTvParser
import com.bhora.app.data.remote.MatchStub
import com.bhora.app.data.remote.SourceBlockedException
import com.bhora.app.domain.logic.TeamNameNormalizer
import com.bhora.app.domain.model.BroadcastRegion
import com.bhora.app.domain.model.BroadcastService
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

data class FeedResult(
    val fixtures: List<PrimeFixture>,
    val lastUpdatedMillis: Long?,
    val fromCache: Boolean,
    val noticeMessage: String? = null
)

private data class KnownChannelFeed(
    val slug: String,
    val regions: List<BroadcastRegion>
)

class PrimeFixturesRepository(
    private val liveSoccerTvClient: LiveSoccerTvClient,
    private val espnClient: EspnScoreboardClient,
    private val diskCache: FixtureDiskCache,
    private val clock: Clock = Clock.systemUTC(),
    private val localZoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val requestStaggerMillis: Long = DEFAULT_REQUEST_STAGGER_MS
) {
    fun loadCachedSnapshot(): FeedResult? {
        val cached = diskCache.read()
        if (cached.lastUpdatedMillis <= 0L) return null
        return FeedResult(
            fixtures = cached.fixtures,
            lastUpdatedMillis = cached.lastUpdatedMillis,
            fromCache = true,
            noticeMessage = null
        )
    }

    suspend fun refreshFixtures(forceBroadcastRescan: Boolean = false): FeedResult = coroutineScope {
        val nowInstant = clock.instant()
        val nowMillis = nowInstant.toEpochMilli()
        val localZone = localZoneProvider()
        val localToday = nowInstant.atZone(localZone).toLocalDate()
        val utcToday = nowInstant.atZone(ZoneOffset.UTC).toLocalDate()
        val existingCache = diskCache.read()

        val espnDeferred = async {
            runCatching { espnClient.fetchAllSupportedScoreboards() }.getOrNull()
        }

        // Fast path: if full cache was recently refreshed and not force-rescanned, just sync live ESPN statuses
        val cacheAgeMillis = nowMillis - existingCache.lastUpdatedMillis
        if (!forceBroadcastRescan &&
            existingCache.lastUpdatedMillis > 0L &&
            existingCache.fixtures.isNotEmpty() &&
            cacheAgeMillis in 0 until CHANNEL_SWEEP_TTL_MS
        ) {
            val espnSnapshot = espnDeferred.await()
            val statusSynced = applyEspnStatuses(existingCache.fixtures, espnSnapshot)
            if (statusSynced != existingCache.fixtures) {
                diskCache.write(existingCache.copy(fixtures = statusSynced))
            }
            return@coroutineScope FeedResult(
                fixtures = statusSynced,
                lastUpdatedMillis = existingCache.lastUpdatedMillis,
                fromCache = false,
                noticeMessage = null
            )
        }

        val stubsById = LinkedHashMap<String, MatchStub>()
        val regionsByMatchId = LinkedHashMap<String, MutableList<BroadcastRegion>>()
        var sourceBlocked = false
        var channelSuccessCount = 0

        // Stage 1: Fetch verified global channel schedules sequentially with polite pacing (9 requests total)
        // Covers Premier League (6), La Liga (8), and UCL (50) up to 2–3 weeks ahead without burst limits.
        for ((index, feed) in KNOWN_CHANNEL_FEEDS.withIndex()) {
            if (index > 0 && requestStaggerMillis > 0L) delay(requestStaggerMillis)
            try {
                val html = liveSoccerTvClient.fetchChannelSchedule(feed.slug)
                channelSuccessCount++
                for (stub in LiveSoccerTvParser.parseSupportedCompetitionRows(html)) {
                    if (!isWithinDisplayWindow(stub, localToday, localZone)) continue
                    stubsById[stub.id] = mergeStub(stubsById[stub.id], stub)
                    val list = regionsByMatchId.getOrPut(stub.id) { mutableListOf() }
                    list.addAll(feed.regions)
                }
            } catch (_: SourceBlockedException) {
                sourceBlocked = true
                break
            } catch (_: Exception) {
                // Continue with remaining channel pages on transient failure
            }
        }

        val espnSnapshot = espnDeferred.await()

        // Stage 2: Gentle global "any country" discovery for today's uncached matches
        // Catches any newly added or unexpected country worldwide without hammering LiveSoccerTV.
        val updatedChecks = existingCache.broadcastChecks.toMutableMap()
        val todayDates = linkedSetOf(localToday, utcToday)

        if (!sourceBlocked) {
            val todayStubs = LinkedHashMap<String, MatchStub>()
            for (date in todayDates) {
                if (requestStaggerMillis > 0L) delay(requestStaggerMillis)
                try {
                    val html = liveSoccerTvClient.fetchDailySchedule(date)
                    channelSuccessCount++
                    for (stub in LiveSoccerTvParser.parseSupportedCompetitionRows(html)) {
                        if (isWithinDisplayWindow(stub, localToday, localZone)) {
                            todayStubs[stub.id] = mergeStub(todayStubs[stub.id], stub)
                            // Also keep live status from daily schedule fresh for known channel matches
                            if (stubsById.containsKey(stub.id)) {
                                stubsById[stub.id] = mergeStub(stubsById[stub.id], stub)
                            }
                        }
                    }
                } catch (_: SourceBlockedException) {
                    sourceBlocked = true
                    break
                } catch (_: Exception) {
                    // Best-effort
                }
            }

            // Scan uncached today matches not already found in Stage 1 (capped and strictly sequential)
            if (!sourceBlocked) {
                var scannedCount = 0
                for (stub in todayStubs.values) {
                    if (scannedCount >= MAX_TODAY_GLOBAL_SCANS) break
                    val mergedStatus = resolveStatusWithEspn(stub, espnSnapshot)
                    val cachedEntry = existingCache.broadcastChecks[stub.id]
                    val cacheFresh = !forceBroadcastRescan &&
                        cachedEntry != null &&
                        FixtureDiskCache.isBroadcastCheckFresh(
                            entry = cachedEntry,
                            status = mergedStatus,
                            kickoffUtcMillis = stub.kickoffUtcMillis,
                            nowMillis = nowMillis
                        )

                    val discoveredRegions = if (cacheFresh) {
                        cachedEntry!!.primeRegions
                    } else if (stubsById.containsKey(stub.id)) {
                        // Already covered by Stage 1 channel pages; skip extra match-page hit
                        null
                    } else {
                        if (requestStaggerMillis > 0L) delay(requestStaggerMillis)
                        scannedCount++
                        try {
                            val matchHtml = liveSoccerTvClient.fetchMatchPage(stub.matchPath)
                            val parsed = LiveSoccerTvParser.parseInternationalPrimeRegions(matchHtml)
                            updatedChecks[stub.id] = CachedBroadcastEntry(
                                checkedAtMillis = nowMillis,
                                primeRegions = parsed
                            )
                            parsed
                        } catch (_: SourceBlockedException) {
                            sourceBlocked = true
                            break
                        } catch (_: Exception) {
                            cachedEntry?.primeRegions
                        }
                    }

                    if (!discoveredRegions.isNullOrEmpty()) {
                        stubsById[stub.id] = mergeStub(stubsById[stub.id], stub)
                        val list = regionsByMatchId.getOrPut(stub.id) { mutableListOf() }
                        list.addAll(discoveredRegions)
                    }
                }
            }
        }

        if (channelSuccessCount == 0) {
            return@coroutineScope fallbackToCache(
                existingCache = existingCache,
                espnSnapshot = espnSnapshot,
                notice = if (sourceBlocked) NOTICE_SOURCE_BLOCKED else NOTICE_NETWORK_UNAVAILABLE
            )
        }

        val resolvedFixtures = stubsById.values
            .mapNotNull { stub ->
                val dedupedRegions = dedupeAndSortRegions(regionsByMatchId[stub.id].orEmpty())
                if (dedupedRegions.isEmpty()) {
                    null
                } else {
                    val mergedStatus = resolveStatusWithEspn(stub, espnSnapshot)
                    PrimeFixture(
                        id = stub.id,
                        homeTeam = stub.homeTeam,
                        awayTeam = stub.awayTeam,
                        kickoffUtcMillis = stub.kickoffUtcMillis,
                        status = mergedStatus,
                        matchPath = stub.matchPath,
                        primeRegions = dedupedRegions,
                        competition = stub.competition
                    )
                }
            }
            .sortedWith(compareBy<PrimeFixture> { it.kickoffUtcMillis }.thenBy { it.homeTeam })

        val pruneBeforeMillis = nowMillis - CACHE_PRUNE_WINDOW_MS
        val prunedChecks = updatedChecks.filterValues { it.checkedAtMillis >= pruneBeforeMillis }

        val finalFixtures = if (sourceBlocked && resolvedFixtures.isEmpty() && existingCache.fixtures.isNotEmpty()) {
            applyEspnStatuses(existingCache.fixtures, espnSnapshot)
        } else {
            resolvedFixtures
        }

        val finalUpdatedMillis = if (sourceBlocked && resolvedFixtures.isEmpty() && existingCache.lastUpdatedMillis > 0L) {
            existingCache.lastUpdatedMillis
        } else {
            nowMillis
        }

        diskCache.write(
            CacheEnvelope(
                version = FixtureDiskCache.CURRENT_VERSION,
                lastUpdatedMillis = finalUpdatedMillis,
                fixtures = finalFixtures,
                broadcastChecks = prunedChecks
            )
        )

        FeedResult(
            fixtures = finalFixtures,
            lastUpdatedMillis = finalUpdatedMillis,
            fromCache = sourceBlocked && resolvedFixtures.isEmpty(),
            noticeMessage = if (sourceBlocked && resolvedFixtures.isEmpty()) NOTICE_SOURCE_BLOCKED else null
        )
    }

    suspend fun refreshLiveStatusesOnly(currentFixtures: List<PrimeFixture>): List<PrimeFixture> {
        if (currentFixtures.isEmpty()) return currentFixtures
        val snapshot = runCatching { espnClient.fetchAllSupportedScoreboards() }.getOrNull() ?: return currentFixtures
        val updated = applyEspnStatuses(currentFixtures, snapshot)
        if (updated != currentFixtures) {
            val existing = diskCache.read()
            diskCache.write(existing.copy(fixtures = updated))
        }
        return updated
    }

    private fun isWithinDisplayWindow(
        stub: MatchStub,
        localToday: java.time.LocalDate,
        localZone: ZoneId
    ): Boolean {
        val localDate = Instant.ofEpochMilli(stub.kickoffUtcMillis).atZone(localZone).toLocalDate()
        val horizonDate = localToday.plusDays(DISPLAY_HORIZON_DAYS)
        return !localDate.isBefore(localToday) && !localDate.isAfter(horizonDate)
    }

    private fun dedupeAndSortRegions(regions: List<BroadcastRegion>): List<BroadcastRegion> {
        val seen = LinkedHashSet<Pair<String, String>>()
        val result = mutableListOf<BroadcastRegion>()
        for (region in regions) {
            val key = region.country.lowercase() to region.channelName.lowercase()
            if (seen.add(key)) {
                result.add(region)
            }
        }
        return result.sortedWith(
            compareBy<BroadcastRegion> { it.country.lowercase() }.thenBy { it.channelName.lowercase() }
        )
    }

    private fun fallbackToCache(
        existingCache: CacheEnvelope,
        espnSnapshot: EspnSnapshot?,
        notice: String
    ): FeedResult {
        val fixtures = applyEspnStatuses(existingCache.fixtures, espnSnapshot)
        return FeedResult(
            fixtures = fixtures,
            lastUpdatedMillis = existingCache.lastUpdatedMillis.takeIf { it > 0L },
            fromCache = true,
            noticeMessage = notice
        )
    }

    private fun mergeStub(existing: MatchStub?, incoming: MatchStub): MatchStub {
        if (existing == null) return incoming
        val mergedStatus = when {
            existing.status == MatchStatus.LIVE || incoming.status == MatchStatus.LIVE -> MatchStatus.LIVE
            existing.status == MatchStatus.ENDED || incoming.status == MatchStatus.ENDED -> MatchStatus.ENDED
            else -> MatchStatus.UPCOMING
        }
        return existing.copy(status = mergedStatus)
    }

    private fun resolveStatusWithEspn(stub: MatchStub, snapshot: EspnSnapshot?): MatchStatus {
        val espnMatch = findMatchingEspnEvent(stub.homeTeam, stub.awayTeam, stub.kickoffUtcMillis, snapshot)
            ?: return stub.status
        return espnMatch.status
    }

    private fun applyEspnStatuses(
        fixtures: List<PrimeFixture>,
        snapshot: EspnSnapshot?
    ): List<PrimeFixture> {
        if (snapshot == null || snapshot.matches.isEmpty()) return fixtures
        return fixtures.map { fixture ->
            val espnMatch = findMatchingEspnEvent(
                fixture.homeTeam,
                fixture.awayTeam,
                fixture.kickoffUtcMillis,
                snapshot
            )
            if (espnMatch != null) fixture.copy(status = espnMatch.status) else fixture
        }
    }

    private fun findMatchingEspnEvent(
        homeTeam: String,
        awayTeam: String,
        kickoffUtcMillis: Long,
        snapshot: EspnSnapshot?
    ): EspnMatchStatus? {
        if (snapshot == null) return null
        return snapshot.matches.firstOrNull { candidate ->
            val timeClose = abs(candidate.kickoffUtcMillis - kickoffUtcMillis) <= KICKOFF_MATCH_TOLERANCE_MS
            timeClose && TeamNameNormalizer.matchPairsEqual(
                homeTeam,
                awayTeam,
                candidate.homeTeam,
                candidate.awayTeam
            )
        }
    }

    companion object {
        private const val DISPLAY_HORIZON_DAYS = 14L
        private const val MAX_TODAY_GLOBAL_SCANS = 8
        private const val CHANNEL_SWEEP_TTL_MS = 15L * 60L * 1000L
        private const val DEFAULT_REQUEST_STAGGER_MS = 250L
        private const val KICKOFF_MATCH_TOLERANCE_MS = 20L * 60L * 1000L
        private const val CACHE_PRUNE_WINDOW_MS = 14L * 24L * 60L * 60L * 1000L

        const val NOTICE_SOURCE_BLOCKED = "CACHED SCHEDULE · SOURCE TEMPORARILY UNAVAILABLE"
        const val NOTICE_NETWORK_UNAVAILABLE = "OFFLINE · SHOWING CACHED SCHEDULE"

        private val KNOWN_CHANNEL_FEEDS = listOf(
            KnownChannelFeed(
                slug = "amazon-prime-videodk",
                regions = listOf(
                    BroadcastRegion("Denmark", "Amazon Prime Video", "/channels/amazon-prime-videodk/", BroadcastService.PRIME_VIDEO),
                    BroadcastRegion("Sweden", "Amazon Prime Video", "/channels/amazon-prime-videodk/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "amazon-prime-videonl",
                regions = listOf(
                    BroadcastRegion("Netherlands", "Amazon Prime Video", "/channels/amazon-prime-videonl/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "amazon-prime-video",
                regions = listOf(
                    BroadcastRegion("Great Britain", "Amazon Prime Video", "/channels/amazon-prime-video/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "amazon-prime-video-germany",
                regions = listOf(
                    BroadcastRegion("Germany", "Amazon Prime Video", "/channels/amazon-prime-video-germany/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "amazon-prime-video-italy",
                regions = listOf(
                    BroadcastRegion("Italy", "Amazon Prime Video", "/channels/amazon-prime-video-italy/", BroadcastService.PRIME_VIDEO),
                    BroadcastRegion("San Marino", "Amazon Prime Video", "/channels/amazon-prime-video-italy/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "cazetv",
                regions = listOf(
                    BroadcastRegion("Brazil", "CazéTV (Prime Video)", "/channels/cazetv/", BroadcastService.PRIME_VIDEO)
                )
            ),
            KnownChannelFeed(
                slug = "hbo-max",
                regions = listOf(
                    BroadcastRegion("Mexico", "HBO Max", "/channels/hbo-max/", BroadcastService.HBO_MAX)
                )
            ),
            KnownChannelFeed(
                slug = "hbo-max-brazil",
                regions = listOf(
                    BroadcastRegion("Brazil", "HBO Max", "/channels/hbo-max-brazil/", BroadcastService.HBO_MAX)
                )
            ),
            KnownChannelFeed(
                slug = "max-norte",
                regions = listOf(
                    BroadcastRegion("Belize", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Costa Rica", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Dominican Republic", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("El Salvador", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Guatemala", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Honduras", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Nicaragua", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX),
                    BroadcastRegion("Panama", "HBO Max", "/channels/max-norte/", BroadcastService.HBO_MAX)
                )
            )
        )
    }
}
