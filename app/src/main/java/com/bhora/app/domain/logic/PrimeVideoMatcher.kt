package com.bhora.app.domain.logic

import com.bhora.app.domain.model.BroadcastService

object PrimeVideoMatcher {
    private val namePatterns = listOf(
        "amazon prime",
        "prime video"
    )

    private val hrefPatterns = listOf(
        "amazon-prime",
        "prime-video",
        "amazon_prime",
        "prime_video"
    )

    private val cazeTvNamePatterns = listOf(
        "cazetv",
        "cazétv",
        "caze tv",
        "cazé tv"
    )

    private val cazeTvHrefPatterns = listOf(
        "cazetv",
        "caze-tv",
        "caze_tv"
    )

    private val hboMaxNoAddonCountries = setOf(
        "brazil",
        "brasil",
        "mexico",
        "belize",
        "costa rica",
        "dominican republic",
        "el salvador",
        "guatemala",
        "honduras",
        "nicaragua",
        "panama"
    )

    private val hboMaxExcludedAddonCountries = setOf(
        "great britain",
        "united kingdom",
        "uk",
        "ireland",
        "ireland republic",
        "republic of ireland"
    )

    private val hboMaxNoAddonHrefSlugs = setOf(
        "hbo-max",
        "hbo-max-brazil",
        "max-brazil",
        "hbo-max-brasil",
        "max-brasil",
        "max-norte",
        "max-mexico",
        "hbo-max-mexico",
        "hbo-max-latam"
    )

    private val hboMaxExactNames = setOf(
        "max",
        "max mexico",
        "max brazil",
        "max brasil",
        "hbo max",
        "hbo max mexico",
        "hbo max brazil",
        "hbo max brasil"
    )

    fun matchBroadcastService(
        channelName: String,
        channelHref: String = "",
        country: String = ""
    ): BroadcastService? {
        if (isPrimeVideoChannel(channelName, channelHref, country)) {
            return BroadcastService.PRIME_VIDEO
        }
        if (isNoAddonHboMaxChannel(channelName, channelHref, country)) {
            return BroadcastService.HBO_MAX
        }
        return null
    }

    fun isPrimeVideoChannel(
        channelName: String,
        channelHref: String = "",
        country: String = ""
    ): Boolean {
        val normalizedName = channelName.trim().lowercase()
        val normalizedHref = channelHref.trim().lowercase()

        if (namePatterns.any { normalizedName.contains(it) } ||
            hrefPatterns.any { normalizedHref.contains(it) }
        ) {
            return true
        }

        return isBrazilCazeTv(normalizedName, normalizedHref, country)
    }

    fun isNoAddonHboMaxChannel(
        channelName: String,
        channelHref: String = "",
        country: String = ""
    ): Boolean {
        val normalizedCountry = country.trim().lowercase()
        val normalizedName = channelName.trim().replace(Regex("\\s+"), " ").lowercase()
        val slug = extractChannelSlug(channelHref)

        if (normalizedCountry in hboMaxExcludedAddonCountries) {
            return false
        }
        if (slug.contains("maxgb") || slug.contains("max-gb") || slug.contains("max-uk")) {
            return false
        }

        if (slug in hboMaxNoAddonHrefSlugs) {
            return true
        }

        if (normalizedCountry in hboMaxNoAddonCountries && normalizedName in hboMaxExactNames) {
            return true
        }

        return false
    }

    fun normalizeChannelDisplayName(
        channelName: String,
        channelHref: String = "",
        country: String = ""
    ): String {
        val trimmed = channelName.trim().replace(Regex("\\s+"), " ")
        val lowerName = trimmed.lowercase()
        val lowerHref = channelHref.trim().lowercase()
        if (isBrazilCazeTv(lowerName, lowerHref, country)) {
            return "CazéTV (Prime Video)"
        }
        if (isNoAddonHboMaxChannel(trimmed, channelHref, country)) {
            return "HBO Max"
        }
        return trimmed.ifBlank { "Amazon Prime Video" }
    }

    private fun isBrazilCazeTv(
        normalizedName: String,
        normalizedHref: String,
        country: String
    ): Boolean {
        val normalizedCountry = country.trim().lowercase()
        if (normalizedCountry != "brazil" && normalizedCountry != "brasil") {
            return false
        }
        return cazeTvNamePatterns.any { normalizedName.contains(it) } ||
            cazeTvHrefPatterns.any { normalizedHref.contains(it) }
    }

    private fun extractChannelSlug(channelHref: String): String {
        return channelHref
            .trim()
            .lowercase()
            .substringBefore("?")
            .substringBefore("#")
            .trimEnd('/')
            .substringAfterLast('/')
    }
}
