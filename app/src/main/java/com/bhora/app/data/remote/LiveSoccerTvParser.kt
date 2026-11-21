package com.bhora.app.data.remote

import com.bhora.app.domain.logic.PrimeVideoMatcher
import com.bhora.app.domain.model.BroadcastRegion
import com.bhora.app.domain.model.Competition
import com.bhora.app.domain.model.MatchStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class MatchStub(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffUtcMillis: Long,
    val status: MatchStatus,
    val matchPath: String,
    val competition: Competition = Competition.PREMIER_LEAGUE
)

object LiveSoccerTvParser {
    private val supportedCids: Set<String> = Competition.entries.map { it.liveSoccerTvCid }.toSet()

    fun isCloudflareChallenge(
        statusCode: Int,
        headers: Map<String, String>,
        body: String
    ): Boolean {
        val mitigated = headers.entries.firstOrNull {
            it.key.equals("cf-mitigated", ignoreCase = true)
        }?.value
        if (mitigated.equals("challenge", ignoreCase = true)) return true
        if (statusCode == 403 || statusCode == 503) {
            val lower = body.lowercase()
            if (lower.contains("challenges.cloudflare.com") ||
                lower.contains("<title>just a moment...</title>") ||
                lower.contains("_cf_chl_opt")
            ) {
                return true
            }
        }
        return statusCode == 403
    }

    fun parseSupportedCompetitionRows(html: String): List<MatchStub> {
        val doc = Jsoup.parse(html)
        val selector = supportedCids.joinToString(",") { cid -> "tr.matchrow[data-cid=$cid]" }
        val rows = doc.select(selector)
        val results = LinkedHashMap<String, MatchStub>()

        for (row in rows) {
            val stub = parseRow(row) ?: continue
            results[stub.id] = stub
        }

        return results.values.toList()
    }

    fun parsePremierLeagueRows(html: String): List<MatchStub> =
        parseSupportedCompetitionRows(html).filter { it.competition == Competition.PREMIER_LEAGUE }

    private fun parseRow(row: Element): MatchStub? {
        val competition = Competition.fromLiveSoccerTvCid(row.attr("data-cid")) ?: return null
        val link = row.selectFirst("td.matchcol a[href^=/match/]")
            ?: row.selectFirst("a[href^=/match/]")
            ?: return null

        val rawHref = link.attr("href").trim()
        if (rawHref.isBlank()) return null
        val matchPath = rawHref.substringBefore("#")

        val rowId = row.attr("id").trim().ifBlank {
            rawHref.substringAfter("#", missingDelimiterValue = matchPath)
        }

        val dvAttr = row.selectFirst("span.ts[dv]")?.attr("dv")?.trim()
            ?: row.selectFirst("[dv]")?.attr("dv")?.trim()
        val kickoffUtcMillis = dvAttr?.toLongOrNull() ?: return null

        val (homeTeam, awayTeam) = extractTeamNames(link) ?: return null
        val status = parseStatus(row)

        return MatchStub(
            id = rowId,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            kickoffUtcMillis = kickoffUtcMillis,
            status = status,
            matchPath = matchPath,
            competition = competition
        )
    }

    private fun extractTeamNames(link: Element): Pair<String, String>? {
        val title = link.attr("title").trim()
        splitTeams(title)?.let { return it }

        val clone = link.clone()
        clone.select("score").forEach { it.replaceWith(org.jsoup.nodes.TextNode(" vs ")) }
        return splitTeams(clone.text())
    }

    private fun splitTeams(raw: String): Pair<String, String>? {
        val cleaned = raw.replace(Regex("\\s+"), " ").trim()
        val parts = cleaned.split(Regex("\\s+vs\\s+", RegexOption.IGNORE_CASE), limit = 2)
        if (parts.size != 2) return null
        val home = parts[0].trim()
        val away = parts[1].trim()
        if (home.isEmpty() || away.isEmpty()) return null
        return home to away
    }

    private fun parseStatus(row: Element): MatchStatus {
        val timer = row.attr("data-timer").trim().uppercase()
        val inProgressText = row.selectFirst("span.inprogress")?.text()?.trim()?.uppercase().orEmpty()
        val hasFtCell = row.selectFirst("span.livecell.ft") != null
        val isLiveRow = row.hasClass("livematch")

        val finishedTokens = setOf("FT", "AET", "AP", "PEN.", "POSTP.", "CANC.", "ABD.")
        if (hasFtCell || timer in finishedTokens || inProgressText in finishedTokens) {
            return MatchStatus.ENDED
        }

        if (isLiveRow) {
            return MatchStatus.LIVE
        }

        val liveTokens = setOf("HT", "ET", "PEN", "LIVE")
        if (timer in liveTokens || inProgressText in liveTokens) {
            return MatchStatus.LIVE
        }

        if (timer.isNotEmpty() && timer.any { it.isDigit() }) {
            return MatchStatus.LIVE
        }

        if (inProgressText.contains("'") && inProgressText.any { it.isDigit() }) {
            return MatchStatus.LIVE
        }

        return MatchStatus.UPCOMING
    }

    fun parseInternationalPrimeRegions(
        html: String,
        competition: Competition? = null
    ): List<BroadcastRegion> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("#dynamic-international-tv table.ichannels tr").ifEmpty {
            doc.select("table.ichannels tr")
        }

        val seen = LinkedHashSet<Pair<String, String>>()
        val regions = mutableListOf<BroadcastRegion>()

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 2) continue

            val country = cells[0].text().replace(Regex("\\s+"), " ").trim()
            if (country.isEmpty()) continue

            val channelLinks = cells[1].select("a[href]")
            for (anchor in channelLinks) {
                val rawName = anchor.text().trim()
                val href = anchor.attr("href").trim()
                val matchedService = PrimeVideoMatcher.matchBroadcastService(rawName, href, country, competition)
                if (matchedService != null) {
                    val normalizedChannel = PrimeVideoMatcher.normalizeChannelDisplayName(rawName, href, country)
                    val dedupeKey = country.lowercase() to normalizedChannel.lowercase()
                    if (seen.add(dedupeKey)) {
                        regions.add(
                            BroadcastRegion(
                                country = country,
                                channelName = normalizedChannel,
                                channelPath = href,
                                service = matchedService
                            )
                        )
                    }
                }
            }
        }

        return regions.sortedWith(compareBy<BroadcastRegion> { it.country.lowercase() }.thenBy { it.channelName.lowercase() })
    }
}
