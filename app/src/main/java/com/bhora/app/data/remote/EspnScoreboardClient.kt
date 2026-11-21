package com.bhora.app.data.remote

import com.bhora.app.domain.model.MatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class EspnMatchStatus(
    val homeTeam: String,
    val awayTeam: String,
    val kickoffUtcMillis: Long,
    val status: MatchStatus
)

data class EspnSnapshot(
    val calendarDates: List<LocalDate>,
    val matches: List<EspnMatchStatus>
)

class EspnScoreboardClient(
    private val httpClient: OkHttpClient = LiveSoccerTvClient.defaultHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun fetchScoreboard(leagueSlug: String = DEFAULT_LEAGUE_SLUG): EspnSnapshot = withContext(Dispatchers.IO) {
        val url = "$normalizedBaseUrl/apis/site/v2/sports/soccer/$leagueSlug/scoreboard"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Bhora/1.0 (Android)")
            .header("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("ESPN HTTP ${response.code} for $leagueSlug")
            }
            val body = response.body?.string().orEmpty()
            parseScoreboardJson(body)
        }
    }

    suspend fun fetchAllSupportedScoreboards(): EspnSnapshot = withContext(Dispatchers.IO) {
        val allDates = LinkedHashSet<LocalDate>()
        val allMatches = mutableListOf<EspnMatchStatus>()

        for (slug in SUPPORTED_LEAGUE_SLUGS) {
            val snap = runCatching { fetchScoreboard(slug) }.getOrNull() ?: continue
            allDates.addAll(snap.calendarDates)
            allMatches.addAll(snap.matches)
        }

        EspnSnapshot(
            calendarDates = allDates.sorted(),
            matches = allMatches
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://site.api.espn.com"
        const val DEFAULT_LEAGUE_SLUG = "eng.1"
        val SUPPORTED_LEAGUE_SLUGS = listOf("eng.1", "esp.1", "uefa.champions", "ger.1", "ita.1", "fra.1")
        private val json = Json { ignoreUnknownKeys = true }

        fun parseScoreboardJson(jsonText: String): EspnSnapshot {
            val root = json.parseToJsonElement(jsonText).jsonObject

            val calendarDates = root["leagues"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("calendar")?.jsonArray
                ?.mapNotNull { elem ->
                    val raw = elem.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                    parseIsoDate(raw)
                }
                ?.distinct()
                ?.sorted()
                .orEmpty()

            val matches = root["events"]?.jsonArray
                ?.mapNotNull { elem -> parseEvent(elem.jsonObject) }
                .orEmpty()

            return EspnSnapshot(
                calendarDates = calendarDates,
                matches = matches
            )
        }

        private fun parseEvent(event: JsonObject): EspnMatchStatus? {
            val dateStr = event["date"]?.jsonPrimitive?.contentOrNull ?: return null
            val kickoffUtcMillis = parseIsoInstantMillis(dateStr) ?: return null

            val competition = event["competitions"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            val competitors = competition["competitors"]?.jsonArray ?: return null

            var homeTeam: String? = null
            var awayTeam: String? = null
            for (compElem in competitors) {
                val compObj = compElem.jsonObject
                val side = compObj["homeAway"]?.jsonPrimitive?.contentOrNull
                val teamName = compObj["team"]?.jsonObject?.get("displayName")?.jsonPrimitive?.contentOrNull
                    ?: compObj["team"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                when (side) {
                    "home" -> homeTeam = teamName
                    "away" -> awayTeam = teamName
                }
            }

            if (homeTeam.isNullOrBlank() || awayTeam.isNullOrBlank()) return null

            val statusType = event["status"]?.jsonObject?.get("type")?.jsonObject
            val statusState = statusType?.get("state")?.jsonPrimitive?.contentOrNull?.lowercase()
            val statusName = statusType?.get("name")?.jsonPrimitive?.contentOrNull?.uppercase()

            val status = mapEspnStatus(statusState, statusName)
            return EspnMatchStatus(
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                kickoffUtcMillis = kickoffUtcMillis,
                status = status
            )
        }

        private fun mapEspnStatus(state: String?, name: String?): MatchStatus {
            if (state == "in") return MatchStatus.LIVE
            if (state == "post") return MatchStatus.ENDED
            if (state == "pre") return MatchStatus.UPCOMING

            return when {
                name == null -> MatchStatus.UPCOMING
                name.contains("FULL_TIME") || name.contains("FINAL") || name.contains("POSTPONED") ->
                    MatchStatus.ENDED
                name.contains("HALF") || name.contains("IN_PROGRESS") || name.contains("EXTRA_TIME") ||
                    name.contains("SHOOTOUT") ->
                    MatchStatus.LIVE
                else -> MatchStatus.UPCOMING
            }
        }

        private fun parseIsoInstantMillis(raw: String): Long? {
            return runCatching { Instant.parse(raw).toEpochMilli() }
                .recoverCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
                .getOrNull()
        }

        private fun parseIsoDate(raw: String): LocalDate? {
            return parseIsoInstantMillis(raw)?.let { millis ->
                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            } ?: runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
        }
    }
}
