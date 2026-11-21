package com.bhora.app.data.cache

import com.bhora.app.domain.model.BroadcastRegion
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedBroadcastEntry(
    val checkedAtMillis: Long,
    val primeRegions: List<BroadcastRegion>
)

@Serializable
data class CacheEnvelope(
    val version: Int = FixtureDiskCache.CURRENT_VERSION,
    val lastUpdatedMillis: Long = 0L,
    val fixtures: List<PrimeFixture> = emptyList(),
    val broadcastChecks: Map<String, CachedBroadcastEntry> = emptyMap()
)

class FixtureDiskCache(
    private val cacheFile: File
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Synchronized
    fun read(): CacheEnvelope {
        if (!cacheFile.exists()) return CacheEnvelope()
        return runCatching {
            val decoded = json.decodeFromString<CacheEnvelope>(cacheFile.readText())
            if (decoded.version != CURRENT_VERSION) {
                decoded.copy(version = CURRENT_VERSION, broadcastChecks = emptyMap())
            } else {
                decoded
            }
        }.getOrElse {
            CacheEnvelope()
        }
    }

    @Synchronized
    fun write(envelope: CacheEnvelope) {
        val parent = cacheFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${cacheFile.name}.tmp")
        tempFile.writeText(json.encodeToString(envelope))
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.copyTo(cacheFile, overwrite = true)
            tempFile.delete()
        }
    }

    companion object {
        const val CURRENT_VERSION = 6
        private const val MINUTE_MS = 60_000L
        private const val HOUR_MS = 60L * MINUTE_MS
        private const val LIVE_TTL_MS = 10L * MINUTE_MS
        private const val NEAR_UPCOMING_TTL_MS = 1L * HOUR_MS
        private const val FAR_UPCOMING_TTL_MS = 4L * HOUR_MS
        private const val ENDED_TTL_MS = 24L * HOUR_MS
        private const val NEAR_WINDOW_MS = 24L * HOUR_MS

        fun isBroadcastCheckFresh(
            entry: CachedBroadcastEntry,
            status: MatchStatus,
            kickoffUtcMillis: Long,
            nowMillis: Long
        ): Boolean {
            val age = nowMillis - entry.checkedAtMillis
            if (age < 0L) return false

            val ttl = when (status) {
                MatchStatus.LIVE -> LIVE_TTL_MS
                MatchStatus.ENDED -> ENDED_TTL_MS
                MatchStatus.UPCOMING -> {
                    val timeUntilKickoff = kickoffUtcMillis - nowMillis
                    if (timeUntilKickoff <= NEAR_WINDOW_MS) NEAR_UPCOMING_TTL_MS else FAR_UPCOMING_TTL_MS
                }
            }
            return age < ttl
        }
    }
}
