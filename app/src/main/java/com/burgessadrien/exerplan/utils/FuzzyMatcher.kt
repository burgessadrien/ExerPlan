package com.burgessadrien.exerplan.utils

import java.util.Locale

object FuzzyMatcher {

    /**
     * Calculates the normalized Levenshtein distance between two strings.
     * Result is between 0.0 (completely different) and 1.0 (identical).
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        val str1 = s1.lowercase(Locale.getDefault()).trim()
        val str2 = s2.lowercase(Locale.getDefault()).trim()

        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0

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
