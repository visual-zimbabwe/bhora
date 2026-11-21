package com.bhora.app.domain.logic

import java.text.Normalizer

object TeamNameNormalizer {
    private val diacriticsRegex = Regex("\\p{M}+")

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
        "afc bournemouth" to "bournemouth",
        "bayern munchen" to "bayern munich",
        "bayern muenchen" to "bayern munich",
        "borussia m gladbach" to "borussia monchengladbach",
        "borussia mgladbach" to "borussia monchengladbach",
        "m gladbach" to "borussia monchengladbach",
        "monchengladbach" to "borussia monchengladbach",
        "hamburger sv" to "hamburg sv",
        "hsv" to "hamburg sv",
        "koln" to "cologne",
        "1 koln" to "cologne",
        "koeln" to "cologne",
        "1 koeln" to "cologne",
        "1 cologne" to "cologne",
        "1 fsv mainz 05" to "mainz",
        "mainz 05" to "mainz",
        "fsv mainz 05" to "mainz",
        "bayer 04 leverkusen" to "bayer leverkusen",
        "tsg 1899 hoffenheim" to "tsg hoffenheim",
        "inter" to "internazionale",
        "inter milan" to "internazionale",
        "milan" to "ac milan",
        "psg" to "paris saint germain",
        "paris sg" to "paris saint germain",
        "paris" to "paris fc",
        "rennes" to "stade rennais",
        "olympique lyonnais" to "lyon",
        "olympique de marseille" to "marseille",
        "olympique marseille" to "marseille",
        "stade brestois" to "brest",
        "stade brestois 29" to "brest"
    )

    fun normalize(name: String): String {
        val ascii = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
        val cleaned = ascii
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
