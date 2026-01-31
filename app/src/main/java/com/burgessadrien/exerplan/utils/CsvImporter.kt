package com.burgessadrien.exerplan.utils

import android.content.Context
import android.net.Uri
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvImporter(private val context: Context) {

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString().trim())
        return result
    }

    fun importNippardPlan(uri: Uri): List<Pair<WorkoutPlan, Map<WorkoutDay, List<LiftingWorkout>>>> {
        val result = mutableListOf<Pair<WorkoutPlan, Map<WorkoutDay, List<LiftingWorkout>>>>()
        
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            
            var currentWeekName = ""
            var currentBlockNotes = mutableListOf<String>()
            var currentPlanData = mutableMapOf<WorkoutDay, List<LiftingWorkout>>()
            var currentDay: WorkoutDay? = null
            var currentWorkouts = mutableListOf<LiftingWorkout>()

            for (line in lines) {
                val cells = splitCsvLine(line)
                if (cells.isEmpty()) continue
                
                // Detection logic: usually Jeff Nippard's CSV has columns starting at index 1
                val labelCell = cells.getOrNull(1) ?: ""
                
                // 1. Detect New Week
                if (labelCell.startsWith("Week", ignoreCase = true)) {
                    // Save existing week data before moving to the next
                    if (currentWeekName.isNotBlank()) {
                        currentDay?.let { currentPlanData[it] = currentWorkouts.toList() }
                        result.add(Pair(WorkoutPlan(name = currentWeekName, notes = currentBlockNotes.toList()), currentPlanData.toMap()))
                    }
                    
                    // Reset for new week
                    currentWeekName = labelCell
                    currentBlockNotes = mutableListOf()
                    currentPlanData = mutableMapOf()
                    currentDay = null
                    currentWorkouts = mutableListOf()
                    continue
                }

                // 2. Detect Day Start
                if (labelCell.contains("FULL BODY", ignoreCase = true) || 
                    labelCell.contains("REST DAY", ignoreCase = true) || 
                    labelCell.contains("TEST", ignoreCase = true)) {
                    
                    // Save the workouts from the previous day into the current week's map
                    currentDay?.let { currentPlanData[it] = currentWorkouts.toList() }
                    
                    val dayType = if (labelCell.contains("REST DAY", ignoreCase = true)) DayType.REST else DayType.WORKING
                    currentDay = WorkoutDay(name = labelCell, dayType = dayType)
                    currentWorkouts = mutableListOf()
                    continue
                }

                // 3. Detect Exercise within a Day
                val exerciseName = cells.getOrNull(2) ?: ""
                if (currentDay != null && exerciseName.isNotBlank() && 
                    exerciseName != "Exercise" && 
                    !exerciseName.startsWith("Jeff Nippard", ignoreCase = true)) {
                    
                    val warmUpSets = cells.getOrNull(3)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    val workingSets = cells.getOrNull(4)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    val repsRaw = cells.getOrNull(5) ?: ""
                    
                    var reps: Int? = null
                    if (!repsRaw.contains("AMRAP", ignoreCase = true)) {
                        reps = repsRaw.split("-").firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()
                    }

                    val rpe = cells.getOrNull(8) ?: ""
                    val rest = cells.getOrNull(9) ?: ""
                    val notes = cells.getOrNull(10) ?: ""

                    currentWorkouts.add(
                        LiftingWorkout(
                            exerciseName = exerciseName,
                            sets = warmUpSets + workingSets,
                            warmUpSets = warmUpSets,
                            workingSets = workingSets,
                            reps = reps,
                            rpe = rpe,
                            rest = rest,
                            notes = if (repsRaw.contains("AMRAP", ignoreCase = true)) "AMRAP. $notes".trim() else notes
                        )
                    )
                } 
                // 4. Capture notes/goals if we're inside a week but haven't hit a day yet
                else if (currentWeekName.isNotBlank() && currentDay == null && labelCell.isNotBlank()) {
                    currentBlockNotes.add(labelCell)
                }
            }
            
            // Final Week Save
            if (currentWeekName.isNotBlank()) {
                currentDay?.let { currentPlanData[it] = currentWorkouts.toList() }
                result.add(Pair(WorkoutPlan(name = currentWeekName, notes = currentBlockNotes.toList()), currentPlanData.toMap()))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return result
    }
}
