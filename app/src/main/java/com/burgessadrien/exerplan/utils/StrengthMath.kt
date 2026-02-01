package com.burgessadrien.exerplan.utils

import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import kotlin.math.roundToInt

/**
 * Utility for strength-related calculations.
 * Uses the industry-standard Epley formula for 1RM estimation and load prediction.
 * Formula: 1RM = Weight * (1 + reps / 30.0)
 */
object StrengthMath {

    private const val EPLEY_CONSTANT = 30.0

    private val CORE_LIFTS = mapOf(
        "squat" to "Back Squat",
        "bench" to "Bench Press",
        "deadlift" to "Deadlift",
        "press" to "Overhead Press",
        "snatch" to "Snatch",
        "clean" to "Clean"
    )

    /**
     * Calculates estimated 1RM based on a given weight and reps.
     * Formula: 1RM = Weight * (1 + reps / 30.0)
     */
    fun calculateOneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weight
        // Standard Epley formula
        return weight * (1.0 + reps.toDouble() / EPLEY_CONSTANT)
    }

    /**
     * Estimates the load for a given number of reps and target RPE based on a known 1RM.
     * This is the "opposite direction" of 1RM calculation.
     * Total effective reps = targetReps + (10 - targetRpe)
     * Load = 1RM / (1 + effectiveReps / 30.0)
     */
    fun estimateLoad(oneRepMax: Double, targetReps: Int, targetRpe: Double): Double {
        val repsInReserve = 10.0 - targetRpe
        val effectiveReps = targetReps + repsInReserve
        
        if (effectiveReps <= 1.0) return oneRepMax
        
        // Inverse Epley formula to find weight from 1RM and reps
        val estimated = oneRepMax / (1.0 + effectiveReps / EPLEY_CONSTANT)
        
        // Round to nearest 0.5 (standard for KG increments)
        return (estimated * 2).roundToInt() / 2.0
    }

    /**
     * Helper to parse RPE strings like "8", "8.5", "7-9" (takes average)
     */
    fun parseRpe(rpeString: String?): Double? {
        if (rpeString == null || rpeString.isBlank() || rpeString.contains("N/A", ignoreCase = true)) return null
        
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

    /**
     * Centralized estimation logic used by the UI to show suggested weights.
     */
    fun getEstimatedLoadForWorkout(
        workout: LiftingWorkout,
        personalBests: List<PersonalBestLift>,
        defaultPrTypes: List<String>
    ): Double? {
        val targetRpe = parseRpe(workout.rpe) ?: return null
        val targetReps = workout.reps ?: return null
        
        // 1. Try exact or fuzzy match
        val pbNames = (personalBests.map { it.exerciseName } + defaultPrTypes).distinct()
        var bestMatchName = FuzzyMatcher.findBestMatch(workout.exerciseName, pbNames)
        
        // 2. Fallback: if no good fuzzy match, look for core lift keywords
        if (bestMatchName == null) {
            val lowerName = workout.exerciseName.lowercase()
            for ((keyword, coreName) in CORE_LIFTS) {
                if (lowerName.contains(keyword)) {
                    // See if we have this core lift in our PBs
                    if (pbNames.any { it.equals(coreName, ignoreCase = true) }) {
                        bestMatchName = coreName
                        break
                    }
                }
            }
        }

        if (bestMatchName == null) return null
        
        val matchingPbs = personalBests.filter { it.exerciseName.equals(bestMatchName, ignoreCase = true) }
        if (matchingPbs.isEmpty()) return null
        
        // Find the best 1RM from all matching PR entries
        val maxOneRepMax = matchingPbs.maxOf { calculateOneRepMax(it.load, it.repCount) }
        
        // Use the inverse formula to calculate what load matches that 1RM for the target volume/intensity
        return estimateLoad(maxOneRepMax, targetReps, targetRpe)
    }
}
