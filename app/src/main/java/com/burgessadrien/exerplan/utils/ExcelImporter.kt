package com.burgessadrien.exerplan.utils

import android.content.Context
import android.net.Uri
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelImporter(private val context: Context) {

    fun importWorkoutPlan(uri: Uri): Pair<WorkoutPlan, Map<WorkoutDay, List<LiftingWorkout>>>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            
            val planName = uri.lastPathSegment?.substringBeforeLast(".") ?: "Imported Plan"
            val workoutPlan = WorkoutPlan(name = planName)
            val planData = mutableMapOf<WorkoutDay, List<LiftingWorkout>>()

            // This is a basic implementation assuming a specific structure
            // In a real app, you'd want a more robust mapping system
            var currentDay: WorkoutDay? = null
            val currentWorkouts = mutableListOf<LiftingWorkout>()

            for (row in sheet) {
                val firstCell = row.getCell(0)?.toString() ?: ""
                
                if (firstCell.startsWith("DAY", ignoreCase = true) || firstCell.startsWith("WEEK", ignoreCase = true)) {
                    // Save previous day if exists
                    currentDay?.let { planData[it] = currentWorkouts.toList() }
                    
                    // Start new day
                    currentDay = WorkoutDay(name = firstCell, dayType = DayType.WORKING)
                    currentWorkouts.clear()
                } else if (firstCell.isNotBlank() && currentDay != null) {
                    // Assume it's a workout row: Exercise, Sets, Reps, Load, RPE, Rest, Notes
                    try {
                        val rawLoad = row.getCell(3)?.numericCellValue
                        val load: Double? = if (rawLoad != null && rawLoad == 0.0) {
                            null // Set to null if the numeric value is 0.0
                        } else {
                            rawLoad // Otherwise, use the raw numeric value
                        }

                        val workout = LiftingWorkout(
                            exerciseName = firstCell,
                            sets = row.getCell(1)?.numericCellValue?.toInt() ?: 0,
                            reps = row.getCell(2)?.numericCellValue?.toInt() ?: 0,
                            warmUpSets = 0,
                            workingSets = row.getCell(1)?.numericCellValue?.toInt() ?: 0,
                            load = load,
                            rpe = row.getCell(4)?.toString() ?: "",
                            rest = row.getCell(5)?.toString() ?: "",
                            notes = row.getCell(6)?.toString() ?: ""
                        )
                        currentWorkouts.add(workout)
                    } catch (e: Exception) {
                        // Skip header rows or malformed rows
                    }
                }
            }
            
            // Add last day
            currentDay?.let { planData[it] = currentWorkouts.toList() }

            workbook.close()
            Pair(workoutPlan, planData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
