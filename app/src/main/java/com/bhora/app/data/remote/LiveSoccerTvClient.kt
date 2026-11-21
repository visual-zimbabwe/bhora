package com.bhora.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class SourceBlockedException(message: String) : IOException(message)

class LiveSoccerTvClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun fetchPremierLeagueCompetition(): String =
        fetchHtml("$normalizedBaseUrl/competitions/england/premier-league/")

    suspend fun fetchDailySchedule(date: LocalDate): String {
        val formatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return fetchHtml("$normalizedBaseUrl/schedules/$formatted/")
    }

    suspend fun fetchChannelSchedule(channelSlug: String): String {
        val cleanSlug = channelSlug.trim().trim('/')
        return fetchHtml("$normalizedBaseUrl/channels/$cleanSlug/")
    }

    suspend fun fetchMatchPage(matchPath: String): String {
        val cleanPath = if (matchPath.startsWith("/")) matchPath else "/$matchPath"
        return fetchHtml("$normalizedBaseUrl$cleanPath")
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val headerMap = response.headers.names().associateWith { name ->
                response.headers[name].orEmpty()
            }

            if (LiveSoccerTvParser.isCloudflareChallenge(response.code, headerMap, body)) {
                throw SourceBlockedException("Source challenge (${response.code}) for $url")
            }

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching $url")
            }

            body
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://www.livesoccertv.com"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
