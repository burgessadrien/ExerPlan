package com.burgessadrien.exerplan.utils

import android.content.Context
import android.net.Uri
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Importer for "Moose Programming" style CSV files.
 * Each CSV is imported as a single Block. Weeks are flattened into a sequence of days.
 */
class MooseProgrammingImporter(private val context: Context) {

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
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

    /**
     * Imports a Moose CSV as a single Block sequence.
     * Returns the Block Name, Block Notes (Goals), and the ordered map of Days to Workouts.
     */
    fun importAsBlock(uri: Uri): Triple<String, List<String>, Map<WorkoutDay, List<LiftingWorkout>>> {
        val planData = LinkedHashMap<WorkoutDay, List<LiftingWorkout>>()
        val blockNotes = mutableListOf<String>()
        val blockName = uri.lastPathSegment?.substringBeforeLast(".") ?: "Moose Block"

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Triple(blockName, emptyList(), emptyMap())
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            // 1. Extract Goals/Notes from global header area
            lines.forEach { line ->
                val cells = splitCsvLine(line)
                val text = cells.getOrNull(1) ?: ""
                if (text.startsWith("1.") || text.startsWith("2.") || text.startsWith("3.")) {
                    blockNotes.add(text)
                }
            }

            // 2. Discover week offsets dynamically.
            val weekOffsets = mutableListOf<Int>()
            for (col in 1..50 step 7) {
                var foundWeek = false
                for (rowIndex in 0..10) {
                    val rowCells = lines.getOrNull(rowIndex)?.let { splitCsvLine(it) } ?: emptyList()
                    val cellText = rowCells.getOrNull(col) ?: ""
                    if (cellText.contains("WEEK", ignoreCase = true) || cellText.contains("Day 1", ignoreCase = true)) {
                        foundWeek = true
                        break
                    }
                }
                if (foundWeek) {
                    weekOffsets.add(col)
                }
            }

            // 3. Iterate through discovered weeks and flatten them into the block
            for (weekIndex in weekOffsets.indices) {
                val offset = weekOffsets[weekIndex]
                val weekNum = weekIndex + 1
                
                var currentDayWorkouts = mutableListOf<Pair<String, List<LiftingWorkout>>>()
                var activeDayName = ""
                var currentWorkouts = mutableListOf<LiftingWorkout>()

                for (line in lines) {
                    val cells = splitCsvLine(line)
                    if (cells.size <= offset) continue

                    val firstCell = cells[offset]
                    
                    if (firstCell.startsWith("Day", ignoreCase = true)) {
                        if (activeDayName.isNotBlank()) {
                            currentDayWorkouts.add(activeDayName to currentWorkouts.toList())
                        }
                        activeDayName = "W$weekNum $firstCell"
                        currentWorkouts = mutableListOf()
                    } 
                    else if (firstCell.isNotBlank() && 
                             activeDayName.isNotBlank() && 
                             firstCell != "Sets" && 
                             !firstCell.contains("focused", ignoreCase = true) &&
                             !firstCell.startsWith("WEEK", ignoreCase = true) &&
                             !firstCell.startsWith("GOALS", ignoreCase = true) &&
                             !firstCell.startsWith("NOTES", ignoreCase = true)) {
                        
                        val exerciseName = firstCell
                        val sets = cells.getOrNull(offset + 1)?.toIntOrNull() ?: 0
                        val repsRaw = cells.getOrNull(offset + 2) ?: ""
                        
                        var reps: Int? = null
                        var time: String? = null
                        if (repsRaw.contains(":")) time = repsRaw else reps = repsRaw.filter { it.isDigit() }.toIntOrNull()

                        val loadRaw = cells.getOrNull(offset + 3) ?: ""
                        val load: Double? = when {
                            loadRaw.contains("BW", ignoreCase = true) -> null
                            loadRaw.toDoubleOrNull() == 0.0 -> null
                            else -> loadRaw.toDoubleOrNull()
                        }
                        
                        val col4 = cells.getOrNull(offset + 4) ?: ""
                        val col5 = cells.getOrNull(offset + 5) ?: ""
                        
                        var rpeValue = ""
                        var notes = ""
                        
                        if (col4.contains("@") || col4.toDoubleOrNull() != null) {
                            rpeValue = col4
                            notes = col5
                        } else {
                            notes = col4
                        }

                        currentWorkouts.add(
                            LiftingWorkout(
                                exerciseName = exerciseName,
                                sets = sets,
                                reps = reps,
                                time = time,
                                load = load,
                                rpe = rpeValue,
                                rest = "3-5 min",
                                notes = if (repsRaw.contains("side")) "$notes (Per side)".trim() else notes
                            )
                        )
                    }
                }
                if (activeDayName.isNotBlank()) {
                    currentDayWorkouts.add(activeDayName to currentWorkouts.toList())
                }

                // Add working days and insert rest days after every 2 working days
                currentDayWorkouts.forEachIndexed { index, (name, workouts) ->
                    val day = WorkoutDay(name = name, dayType = DayType.WORKING)
                    planData[day] = workouts
                    
                    // After every 2 working days, add a rest day
                    if ((index + 1) % 2 == 0) {
                        val restDay = WorkoutDay(name = "W$weekNum Rest Day", dayType = DayType.REST)
                        planData[restDay] = emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return Triple(blockName, blockNotes, planData)
    }
}
