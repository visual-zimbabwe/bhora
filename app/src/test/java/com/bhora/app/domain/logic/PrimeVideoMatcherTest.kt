package com.bhora.app.domain.logic

import com.bhora.app.domain.model.BroadcastService
import com.bhora.app.domain.model.Competition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimeVideoMatcherTest {

    @Test
    fun `matches regional Amazon Prime Video channels across any country`() {
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Amazon Prime Video", "/channels/amazon-prime-videodk/"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Amazon Prime Video", "/channels/amazon-prime-videonl/"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Amazon Prime Video", "/channels/amazon-prime-video/"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Prime Video UK", "/channels/prime-video-uk/"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Prime Video Japan", "/channels/jp-streaming/"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Amazon Prime", ""))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Stream", "/channels/amazon-prime-video-br/"))
    }

    @Test
    fun `rejects non-Prime broadcasters`() {
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("Viaplay Denmark", "/channels/viaplay-denmark/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("DAZN Canada", "/channels/dazn-canada/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("fuboTV Canada", "/channels/fubo-tv-canada/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("Peacock", "/channels/peacock/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("Sky Sport Premier League", "/channels/sky-sport-premier-league-de/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("SuperSport Premier League", "/channels/supersport-premier-league-roa/"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("USA Network", "/channels/usa-network/"))
    }

    @Test
    fun `matches CazeTV in Brazil as Prime Video partner channel only`() {
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("CazéTV", "/channels/cazetv/", "Brazil"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("CazeTV", "/channels/caze-tv/", "Brazil"))
        assertTrue(PrimeVideoMatcher.isPrimeVideoChannel("Cazé TV", "", "Brasil"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("CazéTV", "/channels/cazetv/", "Portugal"))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("CazéTV", "/channels/cazetv/", ""))
        assertFalse(PrimeVideoMatcher.isPrimeVideoChannel("ESPN Brazil", "/channels/espn-brasil/", "Brazil"))
        assertEquals(
            "CazéTV (Prime Video)",
            PrimeVideoMatcher.normalizeChannelDisplayName("CazéTV", "/channels/cazetv/", "Brazil")
        )
    }

    @Test
    fun `matches no-addon HBO Max in Mexico and Central America while excluding UK add-on and unrelated Max networks`() {
        assertEquals(
            BroadcastService.HBO_MAX,
            PrimeVideoMatcher.matchBroadcastService("Max Mexico", "/channels/hbo-max/", "Mexico")
        )
        assertEquals(
            BroadcastService.HBO_MAX,
            PrimeVideoMatcher.matchBroadcastService("Max", "/channels/max-norte/", "Costa Rica")
        )
        assertEquals(
            BroadcastService.HBO_MAX,
            PrimeVideoMatcher.matchBroadcastService("Max", "/channels/max-norte/", "Dominican Republic")
        )
        assertEquals(
            "HBO Max",
            PrimeVideoMatcher.normalizeChannelDisplayName("Max Mexico", "/channels/hbo-max/", "Mexico")
        )

        assertEquals(
            BroadcastService.HBO_MAX,
            PrimeVideoMatcher.matchBroadcastService("Max Brazil", "/channels/hbo-max-brazil/", "Brazil")
        )
        assertEquals(
            "HBO Max",
            PrimeVideoMatcher.normalizeChannelDisplayName("Max Brazil", "/channels/hbo-max-brazil/", "Brazil")
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService("Amazon Prime Video", "/channels/amazon-prime-video-germany/", "Germany")
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService("Amazon Prime Video", "/channels/amazon-prime-video-italy/", "Italy")
        )

        // Exclude UK & Ireland paid TNT Sports add-on on HBO Max
        assertNull(PrimeVideoMatcher.matchBroadcastService("HBO Max", "/channels/hbo-maxgb/", "Great Britain"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("HBO Max", "/channels/hbo-maxgb/", "Ireland Republic"))

        // Exclude unrelated broadcasters containing "Max" or "Prime"
        assertNull(PrimeVideoMatcher.matchBroadcastService("MAXtv To Go", "/channels/max-tv-croatia/", "Croatia"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("MaxTV Go", "/channels/max-tv-live-macedonia/", "Macedonia"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("MAX Sport 4", "/channels/max-sport-4-bulgaria/", "Bulgaria"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("TV3 MAX", "/channels/tv3-max-denmark/", "Denmark"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("beIN SPORTS MAX 1", "/channels/bein-sports-max1-turkey/", "Turkey"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("beIN SPORTS MAX Thailand", "/channels/bein-sports-max-1-thailand/", "Thailand"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("Kantipur Max", "/channels/kantipur-max/", "Nepal"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("SuperSport MaXimo 1", "/channels/supersport-maximo/", "Kenya"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("SPOTV Prime+", "/channels/spotv-primeplus/", "Korea Republic"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("WOWOW Prime", "/channels/wowow-prime/", "Japan"))
        assertNull(PrimeVideoMatcher.matchBroadcastService("Nova Sports Prime", "/channels/nova-sports-stories/", "Greece"))
    }

    @Test
    fun `normalizes channel display name cleanly`() {
        assertEquals("Amazon Prime Video", PrimeVideoMatcher.normalizeChannelDisplayName("  Amazon   Prime Video "))
        assertEquals("Amazon Prime Video", PrimeVideoMatcher.normalizeChannelDisplayName("   "))
    }

    @Test
    fun `excludes UK and Ireland Prime Video PPV for Bundesliga and Ligue 1 while keeping base-plan broadcasts`() {
        assertNull(
            PrimeVideoMatcher.matchBroadcastService(
                "Amazon Prime Video",
                "/channels/amazon-prime-video/",
                "Great Britain",
                Competition.BUNDESLIGA
            )
        )
        assertNull(
            PrimeVideoMatcher.matchBroadcastService(
                "Amazon Prime Video",
                "/channels/amazon-prime-video/",
                "Ireland Republic",
                Competition.LIGUE_1
            )
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService(
                "Amazon Prime Video",
                "/channels/amazon-prime-video/",
                "Great Britain",
                Competition.CHAMPIONS_LEAGUE
            )
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService(
                "Amazon Prime Video",
                "/channels/amazon-prime-video-germany/",
                "Germany",
                Competition.BUNDESLIGA
            )
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService(
                "CazéTV",
                "/channels/cazetv/",
                "Brazil",
                Competition.BUNDESLIGA
            )
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService(
                "CazéTV",
                "/channels/cazetv/",
                "Brazil",
                Competition.SERIE_A
            )
        )
        assertEquals(
            BroadcastService.PRIME_VIDEO,
            PrimeVideoMatcher.matchBroadcastService(
                "CazéTV",
                "/channels/cazetv/",
                "Brazil",
                Competition.LIGUE_1
            )
        )
        assertNull(
            PrimeVideoMatcher.matchBroadcastService(
                "HBO Max",
                "/channels/hbo-maxgb/",
                "Great Britain",
                Competition.SERIE_A
            )
        )
    }
}
