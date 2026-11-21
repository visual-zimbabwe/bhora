package com.bhora.app.data.remote

import com.bhora.app.domain.logic.TeamNameNormalizer
import com.bhora.app.domain.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EspnScoreboardParserTest {

    @Test
    fun `parses calendar dates and live, finished, and upcoming match statuses from ESPN JSON`() {
        val sampleJson = """
            {
              "leagues": [
                {
                  "calendar": [
                    "2026-11-21T07:00Z",
                    "2026-11-22T07:00Z",
                    "2026-11-28T07:00Z"
                  ]
                }
              ],
              "events": [
                {
                  "date": "2026-11-21T12:30Z",
                  "competitions": [
                    {
                      "competitors": [
                        {"homeAway": "home", "team": {"displayName": "Manchester City"}},
                        {"homeAway": "away", "team": {"displayName": "Fulham"}}
                      ]
                    }
                  ],
                  "status": {
                    "type": {"state": "post", "name": "STATUS_FULL_TIME"}
                  }
                },
                {
                  "date": "2026-11-21T15:00Z",
                  "competitions": [
                    {
                      "competitors": [
                        {"homeAway": "home", "team": {"displayName": "AFC Bournemouth"}},
                        {"homeAway": "away", "team": {"displayName": "Nottingham Forest"}}
                      ]
                    }
                  ],
                  "status": {
                    "type": {"state": "in", "name": "STATUS_FIRST_HALF"}
                  }
                },
                {
                  "date": "2026-11-21T17:30Z",
                  "competitions": [
                    {
                      "competitors": [
                        {"homeAway": "home", "team": {"displayName": "Newcastle United"}},
                        {"homeAway": "away", "team": {"displayName": "Arsenal"}}
                      ]
                    }
                  ],
                  "status": {
                    "type": {"state": "pre", "name": "STATUS_SCHEDULED"}
                  }
                }
              ]
            }
        """.trimIndent()

        val snapshot = EspnScoreboardClient.parseScoreboardJson(sampleJson)
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 21),
                LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 11, 28)
            ),
            snapshot.calendarDates
        )

        assertEquals(3, snapshot.matches.size)
        assertEquals("Manchester City", snapshot.matches[0].homeTeam)
        assertEquals("Fulham", snapshot.matches[0].awayTeam)
        assertEquals(MatchStatus.ENDED, snapshot.matches[0].status)
        assertEquals(MatchStatus.LIVE, snapshot.matches[1].status)
        assertEquals(MatchStatus.UPCOMING, snapshot.matches[2].status)
    }

    @Test
    fun `normalizes club name variants across LiveSoccerTV and ESPN`() {
        assertTrue(TeamNameNormalizer.matchPairsEqual("AFC Bournemouth", "Nott'm Forest", "Bournemouth", "Nottingham Forest"))
        assertTrue(TeamNameNormalizer.matchPairsEqual("Man City", "Brighton & Hove Albion", "Manchester City", "Brighton"))
        assertTrue(TeamNameNormalizer.matchPairsEqual("Spurs", "Wolves", "Tottenham Hotspur", "Wolverhampton Wanderers"))
    }
}
