package com.burgessadrien.exerplan.utils

import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import kotlin.math.roundToInt

/**
 * Utility for strength-related calculations.
 * Uses a modified Epley-style formula optimized for RPE-based training.
 * Formula: 1RM = Weight * (1 + (reps - 1) / 24.0)
 */
object StrengthMath {

    private const val EPLEY_CONSTANT = 24.0

    /**
     * Calculates estimated 1RM.
     * 1 rep always returns the weight itself (100%).
     */
    fun calculateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1.0 + (reps - 1) / EPLEY_CONSTANT)
    }

    /**
     * Estimates the load for a given number of reps and target RPE based on a 1RM.
     * Uses the reverse of the 1RM formula.
     * Total effective reps = targetReps + (10 - targetRpe)
     */
    fun estimateLoad(oneRepMax: Double, targetReps: Int, targetRpe: Double): Double {
        val repsInReserve = 10.0 - targetRpe
        val effectiveReps = targetReps + repsInReserve
        
        if (effectiveReps <= 1.0) return oneRepMax
        
        val estimated = oneRepMax / (1.0 + (effectiveReps - 1) / EPLEY_CONSTANT)
        
        // Round to nearest 0.5
        return (estimated * 2).roundToInt() / 2.0
    }

    /**
     * Helper to parse RPE strings like "8", "8.5", "7-9" (takes average)
     */
    fun parseRpe(rpeString: String): Double? {
        if (rpeString.isBlank() || rpeString.contains("N/A", ignoreCase = true)) return null
        
        val rangeRegex = Regex("(\\d+\\.?\\d*)\\s*-\\s*(\\d+\\.?\\d*)")
        val rangeMatch = rangeRegex.find(rpeString)
        if (rangeMatch != null) {
            val start = rangeMatch.groupValues[1].toDoubleOrNull() ?: return null
            val end = rangeMatch.groupValues[2].toDoubleOrNull() ?: return null
            return (start + end) / 2.0
        }

        val singleRegex = Regex("(\\d+\\.?\\d*)")
        val singleMatch = singleRegex.find(rpeString)
        return singleMatch?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun getEstimatedLoadForWorkout(
        workout: LiftingWorkout,
        personalBests: List<PersonalBestLift>,
        defaultPrTypes: List<String>
    ): Double? {
        val targetRpe = parseRpe(workout.rpe) ?: return null
        val targetReps = workout.reps ?: return null
        
        val pbNames = (personalBests.map { it.exerciseName } + defaultPrTypes).distinct()
        val bestMatchName = FuzzyMatcher.findBestMatch(workout.exerciseName, pbNames) ?: return null
        
        val matchingPbs = personalBests.filter { it.exerciseName == bestMatchName }
        if (matchingPbs.isEmpty()) return null
        
        val maxOneRepMax = matchingPbs.maxOf { calculateOneRepMax(it.load, it.repCount) }
        return estimateLoad(maxOneRepMax, targetReps, targetRpe)
    }
}
