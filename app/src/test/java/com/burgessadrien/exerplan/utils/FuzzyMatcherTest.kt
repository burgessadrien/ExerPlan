package com.burgessadrien.exerplan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class FuzzyMatcherTest {

    private val CORE_LIFTS = listOf(
        "Back Squat",
        "Bench Press",
        "Deadlift",
        "Overhead Press",
        "Front Squat",
        "Clean",
        "Snatch"
    )

    @Test
    fun `exact match returns 1_0 similarity`() {
        assertEquals(1.0, FuzzyMatcher.calculateSimilarity("Back Squat", "Back Squat"), 0.001)
    }

    @Test
    fun `case insensitive matching works`() {
        assertEquals(1.0, FuzzyMatcher.calculateSimilarity("back squat", "Back Squat"), 0.001)
    }

    @Test
    fun `squat variations match Back Squat`() {
        // From Powerbuilding 3.0.csv: "SSB squat to low box"
        val variation = "SSB squat to low box"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.4)
        assertEquals("Back Squat", match)
    }

    @Test
    fun `bench variations match Bench Press`() {
        // From Powerbuilding 3.0.csv: "Incline barbell bench press"
        val variation = "Incline barbell bench press"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.4)
        assertEquals("Bench Press", match)
    }

    @Test
    fun `deadlift variations match Deadlift`() {
        // From Powerbuilding 3.0.csv: "Deficit deadlift"
        val variation = "Deficit deadlift"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.4)
        assertEquals("Deadlift", match)
    }

    @Test
    fun `RDL variations match Deadlift`() {
        // From Powerbuilding 3.0.csv: "1\" deficit RDL"
        val variation = "1\" deficit RDL"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.4)
        assertEquals("Deadlift", match)
    }

    @Test
    fun `overhead press variations match Overhead Press`() {
        // From Powerbuilding 3.0.csv: "Strict standing overhead press"
        val variation = "Strict standing overhead press"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.4)
        assertEquals("Overhead Press", match)
    }

    @Test
    fun `unrelated exercises do not match core lifts`() {
        // From Powerbuilding 3.0.csv: "Hanging leg raise"
        val variation = "Hanging leg raise"
        val match = FuzzyMatcher.findBestMatch(variation, CORE_LIFTS, threshold = 0.5)
        assertNull(match)
    }

    @Test
    fun `keyword priority works`() {
        // "Bench" should favor "Bench Press" over something like "Back Squat" despite sharing starting letter
        val benchScore = FuzzyMatcher.calculateSimilarity("Bench", "Bench Press")
        val squatScore = FuzzyMatcher.calculateSimilarity("Bench", "Back Squat")
        assertTrue("Bench score ($benchScore) should be higher than Squat score ($squatScore)", benchScore > squatScore)
    }
}
