package com.bhora.app.data.remote

import com.bhora.app.domain.model.BroadcastService
import com.bhora.app.domain.model.Competition
import com.bhora.app.domain.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSoccerTvParserTest {

    @Test
    fun `parses only Premier League rows and distinguishes finished, live, and upcoming statuses`() {
        val html = """
            <html><body>
            <table class="schedules blueborder">
              <tr id="5661137" class="matchrow" data-timer="FT" data-t1="1012" data-t2="1009" data-cid="6" data-has-tv="1">
                <td class="timecol">
                  <span class="livecell ft">Live</span>
                  <span class="timecell"><span id="ko5661137" class="ts started" dv="1795264200000">7:30am</span></span>
                  <span id="t5661137" class="inprogress">FT</span>
                </td>
                <td class="matchcol">
                  <a href="/match/fulham-vs-manchester-city/17tcj#5661137" title="Manchester City vs Fulham">Manchester City <score>1 - 1</score> Fulham</a>
                </td>
              </tr>
              <tr id="5661130" class="matchrow livematch" data-timer="34" data-t1="1002" data-t2="1016" data-cid="6" data-has-tv="1">
                <td class="timecol">
                  <span class="livecell live">Live</span>
                  <span class="timecell"><span id="ko5661130" class="ts started" dv="1795273200000">10:00am</span></span>
                  <span id="t5661130" class="inprogress">34<blink>'</blink></span>
                </td>
                <td class="matchcol">
                  <a href="/match/aston-villa-vs-sunderland/17oob#5661130" title="Aston Villa vs Sunderland">Aston Villa <score>1 - 0</score> Sunderland</a>
                </td>
              </tr>
              <tr id="5661138" class="matchrow" data-timer="" data-t1="1152" data-t2="1001" data-cid="6" data-has-tv="1">
                <td class="timecol">
                  <span class="livecell live"></span>
                  <span class="timecell"><span id="ko5661138" class="ts" dv="1795282200000">12:30pm</span></span>
                  <span id="t5661138" class="inprogress"></span>
                </td>
                <td class="matchcol">
                  <a href="/match/arsenal-vs-newcastle-united/1dq2l#5661138" title="Newcastle United vs Arsenal">Newcastle United vs Arsenal</a>
                </td>
              </tr>
              <tr id="5667412" class="matchrow" data-timer="" data-t1="2001" data-t2="2002" data-cid="8" data-has-tv="1">
                <td class="timecol">
                  <span class="timecell"><span class="ts" dv="1795290000000">3:00pm</span></span>
                </td>
                <td class="matchcol">
                  <a href="/match/real-madrid-vs-celta/2cvac#5667412" title="Real Madrid vs Celta de Vigo">Real Madrid vs Celta de Vigo</a>
                </td>
              </tr>
              <tr id="5774627" class="matchrow" data-timer="" data-t1="1001" data-t2="3001" data-cid="50" data-has-tv="1">
                <td class="timecol">
                  <span class="timecell"><span class="ts" dv="1795550400000">8:00pm</span></span>
                </td>
                <td class="matchcol">
                  <a href="/match/arsenal-vs-borussia-dortmund/182qc#5774627" title="Arsenal vs Borussia Dortmund">Arsenal vs Borussia Dortmund</a>
                </td>
              </tr>
              <tr id="9999999" class="matchrow" data-timer="" data-t1="4001" data-t2="4002" data-cid="999" data-has-tv="1">
                <td class="timecol">
                  <span class="timecell"><span class="ts" dv="1795550400000">8:00pm</span></span>
                </td>
                <td class="matchcol">
                  <a href="/match/other-a-vs-other-b/xyz#9999999" title="Other A vs Other B">Other A vs Other B</a>
                </td>
              </tr>
            </table>
            </body></html>
        """.trimIndent()

        val plOnly = LiveSoccerTvParser.parsePremierLeagueRows(html)
        assertEquals(3, plOnly.size)

        val allSupported = LiveSoccerTvParser.parseSupportedCompetitionRows(html)
        assertEquals(5, allSupported.size)
        assertEquals(Competition.PREMIER_LEAGUE, allSupported[0].competition)
        assertEquals(Competition.LA_LIGA, allSupported[3].competition)
        assertEquals("Real Madrid", allSupported[3].homeTeam)
        assertEquals(Competition.CHAMPIONS_LEAGUE, allSupported[4].competition)
        assertEquals("Arsenal", allSupported[4].homeTeam)

        val parsed = plOnly

        assertEquals("5661137", parsed[0].id)
        assertEquals("Manchester City", parsed[0].homeTeam)
        assertEquals("Fulham", parsed[0].awayTeam)
        assertEquals(1795264200000L, parsed[0].kickoffUtcMillis)
        assertEquals(MatchStatus.ENDED, parsed[0].status)
        assertEquals("/match/fulham-vs-manchester-city/17tcj", parsed[0].matchPath)

        assertEquals("5661130", parsed[1].id)
        assertEquals(MatchStatus.LIVE, parsed[1].status)

        assertEquals("5661138", parsed[2].id)
        assertEquals("Newcastle United", parsed[2].homeTeam)
        assertEquals("Arsenal", parsed[2].awayTeam)
        assertEquals(MatchStatus.UPCOMING, parsed[2].status)
    }

    @Test
    fun `extracts Prime Video across all countries from international coverage table`() {
        val html = """
            <html><body>
            <div id="dynamic-international-tv">
              <div class="r-section">
                <h2>International Coverage</h2>
                <table class="data aleft ichannels">
                  <tr>
                    <td><span class="flag brazil">Brazil</span></td>
                    <td><a href="/channels/espn-brasil/">ESPN Brazil</a><a href="/channels/cazetv/">CazéTV</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag canada">Canada</span></td>
                    <td><a href="/channels/dazn-canada/">DAZN Canada</a><a href="/channels/fubo-tv-canada/">fuboTV Canada</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag costa-rica">Costa Rica</span></td>
                    <td><a href="/channels/max-norte/">Max</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag croatia">Croatia</span></td>
                    <td><a href="/channels/max-tv-croatia/">MAXtv To Go</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag great-britain">Great Britain</span></td>
                    <td><a href="/channels/hbo-maxgb/">HBO Max</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag mexico">Mexico</span></td>
                    <td><a href="/channels/hbo-max/">Max Mexico</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag sweden">Sweden</span></td>
                    <td><a href="/channels/amazon-prime-videodk/">Amazon Prime Video</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag denmark">Denmark</span></td>
                    <td><a href="/channels/amazon-prime-videodk/">Amazon Prime Video</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag japan">Japan</span></td>
                    <td><a href="/channels/u-next/">U-NEXT</a><a href="/channels/prime-video-jp/">Prime Video Japan</a></td>
                  </tr>
                  <tr>
                    <td><span class="flag netherlands">Netherlands</span></td>
                    <td><a href="/channels/amazon-prime-videonl/">Amazon Prime Video</a></td>
                  </tr>
                </table>
              </div>
            </div>
            </body></html>
        """.trimIndent()

        val regions = LiveSoccerTvParser.parseInternationalPrimeRegions(html)
        assertEquals(7, regions.size)
        assertEquals(
            listOf("Brazil", "Costa Rica", "Denmark", "Japan", "Mexico", "Netherlands", "Sweden"),
            regions.map { it.country }
        )
        assertEquals("CazéTV (Prime Video)", regions[0].channelName)
        assertEquals(BroadcastService.PRIME_VIDEO, regions[0].service)
        assertEquals("HBO Max", regions[1].channelName)
        assertEquals(BroadcastService.HBO_MAX, regions[1].service)
        assertEquals("Amazon Prime Video", regions[2].channelName)
        assertEquals("Prime Video Japan", regions[3].channelName)
        assertEquals("HBO Max", regions[4].channelName)
        assertEquals(BroadcastService.HBO_MAX, regions[4].service)
    }

    @Test
    fun `detects Cloudflare challenge responses accurately`() {
        assertTrue(
            LiveSoccerTvParser.isCloudflareChallenge(
                statusCode = 403,
                headers = mapOf("cf-mitigated" to "challenge"),
                body = "<html><title>Just a moment...</title></html>"
            )
        )
        assertTrue(
            LiveSoccerTvParser.isCloudflareChallenge(
                statusCode = 503,
                headers = emptyMap(),
                body = "<html><script src='https://challenges.cloudflare.com/turnstile'></script></html>"
            )
        )
        assertFalse(
            LiveSoccerTvParser.isCloudflareChallenge(
                statusCode = 200,
                headers = mapOf("server" to "cloudflare"),
                body = "<html><body>Normal match page</body></html>"
            )
        )
    }

    @Test
    fun `parses Bundesliga, Serie A, and Ligue 1 rows alongside existing competitions`() {
        val html = """
            <html><body>
            <table class="schedules blueborder">
              <tr id="5672922" class="matchrow" data-timer="" data-cid="7" data-has-tv="1">
                <td class="timecol"><span class="timecell"><span class="ts" dv="1795275000000">15:30</span></span></td>
                <td class="matchcol"><a href="/match/bayern-munchen-vs-hamburger-sv/19996#5672922" title="Hamburger SV vs Bayern München">Hamburger SV vs Bayern München</a></td>
              </tr>
              <tr id="5680001" class="matchrow" data-timer="" data-cid="39" data-has-tv="1">
                <td class="timecol"><span class="timecell"><span class="ts" dv="1795280400000">17:00</span></span></td>
                <td class="matchcol"><a href="/match/internazionale-vs-como/6ifzs#5680001" title="Internazionale vs Como">Internazionale vs Como</a></td>
              </tr>
              <tr id="5653094" class="matchrow" data-timer="" data-cid="10" data-has-tv="1">
                <td class="timecol"><span class="timecell"><span class="ts" dv="1795291200000">20:00</span></span></td>
                <td class="matchcol"><a href="/match/nice-vs-psg/abc12#5653094" title="Nice vs PSG">Nice vs PSG</a></td>
              </tr>
              <tr id="9999999" class="matchrow" data-timer="" data-cid="999" data-has-tv="1">
                <td class="timecol"><span class="timecell"><span class="ts" dv="1795291200000">20:00</span></span></td>
                <td class="matchcol"><a href="/match/other-a-vs-other-b/xyz#9999999" title="Other A vs Other B">Other A vs Other B</a></td>
              </tr>
            </table>
            </body></html>
        """.trimIndent()

        val parsed = LiveSoccerTvParser.parseSupportedCompetitionRows(html)
        assertEquals(3, parsed.size)
        assertEquals(Competition.BUNDESLIGA, parsed[0].competition)
        assertEquals("Hamburger SV", parsed[0].homeTeam)
        assertEquals("Bayern München", parsed[0].awayTeam)
        assertEquals(Competition.SERIE_A, parsed[1].competition)
        assertEquals("Internazionale", parsed[1].homeTeam)
        assertEquals(Competition.LIGUE_1, parsed[2].competition)
        assertEquals("Nice", parsed[2].homeTeam)
        assertEquals("PSG", parsed[2].awayTeam)
        assertTrue(LiveSoccerTvParser.parsePremierLeagueRows(html).isEmpty())
    }

    @Test
    fun `excludes UK and Ireland Prime Video PPV for Bundesliga and Ligue 1 while keeping base-plan regions`() {
        val html = """
            <html><body>
            <div id="dynamic-international-tv">
              <table class="data aleft ichannels">
                <tr>
                  <td><span class="flag brazil">Brazil</span></td>
                  <td><a href="/channels/cazetv/">CazéTV</a></td>
                </tr>
                <tr>
                  <td><span class="flag great-britain">Great Britain</span></td>
                  <td><a href="/channels/amazon-prime-video/">Amazon Prime Video</a></td>
                </tr>
                <tr>
                  <td><span class="flag ireland">Ireland Republic</span></td>
                  <td><a href="/channels/amazon-prime-video/">Amazon Prime Video</a></td>
                </tr>
              </table>
            </div>
            </body></html>
        """.trimIndent()

        val bundesligaRegions = LiveSoccerTvParser.parseInternationalPrimeRegions(html, Competition.BUNDESLIGA)
        assertEquals(listOf("Brazil"), bundesligaRegions.map { it.country })

        val ligue1Regions = LiveSoccerTvParser.parseInternationalPrimeRegions(html, Competition.LIGUE_1)
        assertEquals(listOf("Brazil"), ligue1Regions.map { it.country })

        val uclRegions = LiveSoccerTvParser.parseInternationalPrimeRegions(html, Competition.CHAMPIONS_LEAGUE)
        assertEquals(listOf("Brazil", "Great Britain", "Ireland Republic"), uclRegions.map { it.country })
    }
}
