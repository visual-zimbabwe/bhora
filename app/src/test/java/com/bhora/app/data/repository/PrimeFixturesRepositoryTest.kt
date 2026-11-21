package com.bhora.app.data.repository

import com.bhora.app.data.cache.FixtureDiskCache
import com.bhora.app.data.remote.EspnScoreboardClient
import com.bhora.app.data.remote.LiveSoccerTvClient
import com.bhora.app.domain.model.Competition
import com.bhora.app.domain.model.MatchStatus
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PrimeFixturesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `discovers PL, La Liga, and UCL across channel feeds and unexpected global countries, caches results, and falls back gracefully on 403`() = runTest {
        val blockLiveSoccerTv = AtomicBoolean(false)
        val channelFetches = AtomicInteger(0)
        val matchPageFetches = AtomicInteger(0)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (path.startsWith("/apis/site/v2/sports/soccer/eng.1/scoreboard")) {
                    return MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "leagues": [{"calendar": ["2026-11-21T07:00Z"]}],
                          "events": [
                            {
                              "date": "2026-11-21T12:30Z",
                              "competitions": [{
                                "competitors": [
                                  {"homeAway": "home", "team": {"displayName": "Manchester City"}},
                                  {"homeAway": "away", "team": {"displayName": "Fulham"}}
                                ]
                              }],
                              "status": {"type": {"state": "in", "name": "STATUS_SECOND_HALF"}}
                            }
                          ]
                        }
                        """.trimIndent()
                    )
                }

                if (path.startsWith("/apis/site/v2/sports/soccer/")) {
                    return MockResponse().setResponseCode(200).setBody("""{"leagues":[],"events":[]}""")
                }

                if (blockLiveSoccerTv.get()) {
                    return MockResponse()
                        .setResponseCode(403)
                        .addHeader("cf-mitigated", "challenge")
                        .setBody("<html><title>Just a moment...</title></html>")
                }

                return when {
                    path == "/channels/amazon-prime-videodk/" -> {
                        channelFetches.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <table class="schedules">
                              <tr id="5661137" class="matchrow" data-timer="" data-cid="6">
                                <td><span class="ts" dv="1795264200000">12:30</span></td>
                                <td class="matchcol"><a href="/match/fulham-vs-manchester-city/17tcj#5661137" title="Manchester City vs Fulham">Manchester City vs Fulham</a></td>
                              </tr>
                            </table>
                            """.trimIndent()
                        )
                    }

                    path == "/channels/amazon-prime-videonl/" -> {
                        channelFetches.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <table class="schedules">
                              <tr id="5661137" class="matchrow" data-timer="" data-cid="6">
                                <td><span class="ts" dv="1795264200000">12:30</span></td>
                                <td class="matchcol"><a href="/match/fulham-vs-manchester-city/17tcj#5661137" title="Manchester City vs Fulham">Manchester City vs Fulham</a></td>
                              </tr>
                            </table>
                            """.trimIndent()
                        )
                    }

                    path == "/channels/cazetv/" -> {
                        channelFetches.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <table class="schedules">
                              <tr id="5667404" class="matchrow" data-timer="" data-cid="8">
                                <td><span class="ts" dv="1795266000000">13:00</span></td>
                                <td class="matchcol"><a href="/match/deportivo-la-coruna-vs-deportivo-alaves/59il2#5667404" title="Deportivo Alavés vs Deportivo La Coruña">Deportivo Alavés vs Deportivo La Coruña</a></td>
                              </tr>
                            </table>
                            """.trimIndent()
                        )
                    }

                    path == "/channels/amazon-prime-video/" ||
                        path == "/channels/amazon-prime-video-germany/" ||
                        path == "/channels/hbo-max-brazil/" -> {
                        channelFetches.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <table class="schedules">
                              <tr id="5774627" class="matchrow" data-timer="" data-cid="50">
                                <td><span class="ts" dv="1795550400000">20:00</span></td>
                                <td class="matchcol"><a href="/match/arsenal-vs-borussia-dortmund/182qc#5774627" title="Arsenal vs Borussia Dortmund">Arsenal vs Borussia Dortmund</a></td>
                              </tr>
                            </table>
                            """.trimIndent()
                        )
                    }

                    path.startsWith("/channels/") -> {
                        channelFetches.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody("""<table class="schedules"></table>""")
                    }

                    path.startsWith("/schedules/2026-11-21/") ->
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <table class="schedules">
                              <tr id="5661137" class="matchrow" data-timer="" data-cid="6">
                                <td><span class="ts" dv="1795264200000">12:30</span></td>
                                <td class="matchcol"><a href="/match/fulham-vs-manchester-city/17tcj#5661137" title="Manchester City vs Fulham">Manchester City vs Fulham</a></td>
                              </tr>
                              <tr id="5661138" class="matchrow" data-timer="" data-cid="6">
                                <td><span class="ts" dv="1795282200000">17:30</span></td>
                                <td class="matchcol"><a href="/match/arsenal-vs-newcastle-united/1dq2l#5661138" title="Newcastle United vs Arsenal">Newcastle United vs Arsenal</a></td>
                              </tr>
                            </table>
                            """.trimIndent()
                        )

                    path == "/match/arsenal-vs-newcastle-united/1dq2l" -> {
                        matchPageFetches.incrementAndGet()
                        // Simulates discovering a brand-new country (Japan) not in known channel feeds!
                        MockResponse().setResponseCode(200).setBody(
                            """
                            <div id="dynamic-international-tv">
                              <table class="data aleft ichannels">
                                <tr><td>Japan</td><td><a href="/channels/prime-video-jp/">Prime Video Japan</a></td></tr>
                              </table>
                            </div>
                            """.trimIndent()
                        )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val baseUrl = server.url("/").toString().trimEnd('/')
        val http = OkHttpClient()
        val fixedClock = Clock.fixed(Instant.parse("2026-11-21T13:30:00Z"), ZoneOffset.UTC)
        val cache = FixtureDiskCache(File(tempFolder.root, "repo_cache.json"))

        val repo = PrimeFixturesRepository(
            liveSoccerTvClient = LiveSoccerTvClient(http, baseUrl = baseUrl),
            espnClient = EspnScoreboardClient(http, baseUrl = baseUrl),
            diskCache = cache,
            clock = fixedClock,
            localZoneProvider = { ZoneOffset.UTC },
            requestStaggerMillis = 0L
        )

        // 1. Cold refresh: discovers PL (DK/NL/SE), La Liga (Brazil CazéTV), today's global surprise (Japan), and UCL (Brazil/Germany/GB)
        val first = repo.refreshFixtures(forceBroadcastRescan = false)
        assertFalse(first.fromCache)
        assertNull(first.noticeMessage)
        assertEquals(4, first.fixtures.size)

        assertEquals("Manchester City", first.fixtures[0].homeTeam)
        assertEquals(Competition.PREMIER_LEAGUE, first.fixtures[0].competition)
        assertEquals(MatchStatus.LIVE, first.fixtures[0].status)
        assertEquals(listOf("Denmark", "Netherlands", "Sweden"), first.fixtures[0].primeRegions.map { it.country })

        assertEquals(Competition.LA_LIGA, first.fixtures[1].competition)
        assertEquals(listOf("Brazil"), first.fixtures[1].primeRegions.map { it.country })

        assertEquals("Newcastle United", first.fixtures[2].homeTeam)
        assertEquals(listOf("Japan"), first.fixtures[2].primeRegions.map { it.country })

        assertEquals(Competition.CHAMPIONS_LEAGUE, first.fixtures[3].competition)
        assertEquals(listOf("Brazil", "Germany", "Great Britain"), first.fixtures[3].primeRegions.map { it.country })

        assertEquals(9, channelFetches.get())
        assertEquals(1, matchPageFetches.get())

        // 2. Warm refresh immediately after: 15-min sweep cache avoids redundant LiveSoccerTV requests
        val second = repo.refreshFixtures(forceBroadcastRescan = false)
        assertEquals(4, second.fixtures.size)
        assertEquals(9, channelFetches.get())
        assertEquals(1, matchPageFetches.get())

        // 3. Forced refresh during Cloudflare 403 block: gracefully falls back to cached fixtures
        blockLiveSoccerTv.set(true)
        val blocked = repo.refreshFixtures(forceBroadcastRescan = true)
        assertTrue(blocked.fromCache)
        assertEquals(PrimeFixturesRepository.NOTICE_SOURCE_BLOCKED, blocked.noticeMessage)
        assertEquals(4, blocked.fixtures.size)
    }
}
