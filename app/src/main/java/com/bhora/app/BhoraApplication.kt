package com.bhora.app

import android.app.Application
import com.bhora.app.data.cache.FixtureDiskCache
import com.bhora.app.data.remote.EspnScoreboardClient
import com.bhora.app.data.remote.LiveSoccerTvClient
import com.bhora.app.data.repository.PrimeFixturesRepository
import java.io.File

class BhoraApplication : Application() {
    val repository: PrimeFixturesRepository by lazy {
        val httpClient = LiveSoccerTvClient.defaultHttpClient()
        PrimeFixturesRepository(
            liveSoccerTvClient = LiveSoccerTvClient(httpClient = httpClient),
            espnClient = EspnScoreboardClient(httpClient = httpClient),
            diskCache = FixtureDiskCache(File(filesDir, "bhora_fixture_cache.json"))
        )
    }
}
