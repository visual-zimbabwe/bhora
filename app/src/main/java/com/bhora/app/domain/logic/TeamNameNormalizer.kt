package com.bhora.app.domain.logic

object TeamNameNormalizer {
    private val canonicalAliases = mapOf(
        "man city" to "manchester city",
        "man utd" to "manchester united",
        "man united" to "manchester united",
        "spurs" to "tottenham hotspur",
        "tottenham" to "tottenham hotspur",
        "nottm forest" to "nottingham forest",
        "nott m forest" to "nottingham forest",
        "forest" to "nottingham forest",
        "wolves" to "wolverhampton wanderers",
        "wolverhampton" to "wolverhampton wanderers",
        "brighton" to "brighton and hove albion",
        "brighton hove albion" to "brighton and hove albion",
        "west ham" to "west ham united",
        "newcastle" to "newcastle united",
        "leeds" to "leeds united",
        "ipswich" to "ipswich town",
        "coventry" to "coventry city",
        "hull" to "hull city",
        "leicester" to "leicester city",
        "norwich" to "norwich city",
        "sheffield utd" to "sheffield united",
        "luton" to "luton town",
        "bournemouth" to "bournemouth",
        "afc bournemouth" to "bournemouth"
    )

    fun normalize(name: String): String {
        val cleaned = name
            .lowercase()
            .replace("&", " and ")
            .replace("'", " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\b(fc|afc|cf)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return canonicalAliases[cleaned] ?: cleaned
    }

    fun teamsMatch(teamA: String, teamB: String): Boolean {
        val a = normalize(teamA)
        val b = normalize(teamB)
        if (a.isEmpty() || b.isEmpty()) return false
        return a == b || a.contains(b) || b.contains(a)
    }

    fun matchPairsEqual(
        homeA: String,
        awayA: String,
        homeB: String,
        awayB: String
    ): Boolean {
        return teamsMatch(homeA, homeB) && teamsMatch(awayA, awayB)
    }
}
