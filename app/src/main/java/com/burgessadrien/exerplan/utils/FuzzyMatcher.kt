package com.burgessadrien.exerplan.utils

import java.util.Locale

object FuzzyMatcher {

    /**
     * Calculates a similarity score between 0.0 and 1.0.
     * Combines Levenshtein distance with word-based overlap and keyword priority.
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        val str1 = s1.lowercase(Locale.getDefault()).trim()
        val str2 = s2.lowercase(Locale.getDefault()).trim()

        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0

        val lev = calculateLevenshteinSimilarity(str1, str2)

        val words1 = str1.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 }.toSet()
        val words2 = str2.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return lev

        // Handle synonyms like RDL -> Deadlift
        val expandedWords1 = expandSynonyms(words1)
        val expandedWords2 = expandSynonyms(words2)

        val common = expandedWords1.intersect(expandedWords2)
        val union = expandedWords1.union(expandedWords2)
        val jaccard = common.size.toDouble() / union.size
        
        // How much of the smaller word set is contained in the larger one
        val containment = common.size.toDouble() / Math.min(expandedWords1.size, expandedWords2.size)

        // Important exercise keywords
        val powerWords = setOf("squat", "bench", "deadlift", "press", "snatch", "clean", "row", "pullup", "dip", "rdl")
        val powerMatch = common.any { it in powerWords }

        var score = Math.max(lev, Math.max(jaccard, containment))
        
        if (powerMatch) {
            // Give a boost if it contains a primary power word and has decent overlap
            if (score > 0.3) {
                score *= 1.3 // Increased boost
            }
        }
        
        // Tie-breaker: small bonus for "Back Squat" or "Bench Press" being the match 
        // if it's a generic power-word match
        if (str2 == "back squat" || str2 == "bench press" || str2 == "deadlift") {
            score += 0.05
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun expandSynonyms(words: Set<String>): Set<String> {
        val result = words.toMutableSet()
        if (words.contains("rdl")) result.add("deadlift")
        if (words.contains("deadlift")) result.add("rdl")
        return result
    }

    private fun calculateLevenshteinSimilarity(str1: String, str2: String): Double {
        val costs = IntArray(str2.length + 1)
        for (j in 0..str2.length) {
            costs[j] = j
        }

        for (i in 1..str1.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..str2.length) {
                val cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), if (str1[i - 1] == str2[j - 1]) nw else nw + 1)
                nw = costs[j]
                costs[j] = cj
            }
        }

        val maxLength = Math.max(str1.length, str2.length)
        return (maxLength - costs[str2.length]).toDouble() / maxLength
    }

    /**
     * Finds the best match for [query] in [options] if similarity is above [threshold].
     */
    fun findBestMatch(query: String, options: List<String>, threshold: Double = 0.5): String? {
        if (options.isEmpty()) return null
        
        var bestMatch: String? = null
        var maxSimilarity = -1.0

        for (option in options) {
            val similarity = calculateSimilarity(query, option)
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
                bestMatch = option
            }
        }

        return if (maxSimilarity >= threshold) bestMatch else null
    }
}
