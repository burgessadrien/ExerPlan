package com.burgessadrien.exerplan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrengthMathTest {

    private val TOLERANCE = 0.6 // Allowing for rounding to nearest 0.5

    @Test
    fun `calculateOneRepMax - 1 rep returns the same weight`() {
        val result = StrengthMath.calculateOneRepMax(335.0, 1)
        assertEquals(335.0, result, TOLERANCE)
    }

    @Test
    fun `calculateOneRepMax - 305 for 3 reps uses 30 constant`() {
        // 305 * (1 + 3/30.0) = 305 * 1.1 = 335.5
        val result = StrengthMath.calculateOneRepMax(305.0, 3)
        assertEquals(335.5, result, TOLERANCE)
    }

    @Test
    fun `estimateLoad - 1 rep at RPE 10 returns 1RM`() {
        val result = StrengthMath.estimateLoad(335.0, 1, 10.0)
        assertEquals(335.0, result, TOLERANCE)
    }

    @Test
    fun `estimateLoad - 3 reps at RPE 10 with 335 1RM`() {
        // 335 / (1 + 3/30.0) = 335 / 1.1 = 304.545...
        // Rounded to nearest 0.5 = 304.5
        val result = StrengthMath.estimateLoad(335.0, 3, 10.0)
        assertEquals(304.5, result, TOLERANCE)
    }

    @Test
    fun `estimateLoad - submaximal RPE 8 uses effective reps`() {
        val oneRepMax = 100.0
        // 5 reps @ RPE 8 = 7 effective reps (5 actual + 2 RIR)
        // 100 / (1 + 7/30.0) = 100 / 1.2333 = 81.08
        // Rounded to nearest 0.5 = 81.0
        val result = StrengthMath.estimateLoad(oneRepMax, 5, 8.0)
        assertEquals(81.0, result, TOLERANCE)
    }

    @Test
    fun `parseRpe - handles decimals and ranges`() {
        assertEquals(8.0, StrengthMath.parseRpe("8")!!, 0.01)
        assertEquals(8.5, StrengthMath.parseRpe("8.5")!!, 0.01)
        assertEquals(8.0, StrengthMath.parseRpe("7-9")!!, 0.01)
        assertEquals(8.25, StrengthMath.parseRpe("8-8.5")!!, 0.01)
        assertNull(StrengthMath.parseRpe("N/A"))
        assertNull(StrengthMath.parseRpe(""))
    }
}
