package com.bhora.app.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class MatchStatus {
    LIVE,
    UPCOMING,
    ENDED
}

enum class MatchFilter {
    ALL,
    LIVE_ONLY
}

@Serializable
enum class Competition(
    val liveSoccerTvCid: String,
    val displayLabel: String
) {
    PREMIER_LEAGUE("6", "PREMIER LEAGUE"),
    LA_LIGA("8", "LA LIGA"),
    CHAMPIONS_LEAGUE("50", "CHAMPIONS LEAGUE");

    companion object {
        fun fromLiveSoccerTvCid(cid: String): Competition? =
            entries.firstOrNull { it.liveSoccerTvCid == cid.trim() }
    }
}

@Serializable
enum class BroadcastService {
    PRIME_VIDEO,
    HBO_MAX
}

enum class ServiceFilter {
    ALL,
    PRIME,
    MAX
}

@Serializable
data class BroadcastRegion(
    val country: String,
    val channelName: String,
    val channelPath: String = "",
    val service: BroadcastService = BroadcastService.PRIME_VIDEO
)

@Serializable
data class PrimeFixture(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffUtcMillis: Long,
    val status: MatchStatus,
    val matchPath: String,
    val primeRegions: List<BroadcastRegion>,
    val competition: Competition = Competition.PREMIER_LEAGUE
)

data class FixtureDayGroup(
    val date: LocalDate,
    val dateLabel: String,
    val fixtures: List<PrimeFixture>
)
