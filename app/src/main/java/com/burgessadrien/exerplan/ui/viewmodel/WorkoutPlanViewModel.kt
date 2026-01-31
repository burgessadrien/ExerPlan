package com.burgessadrien.exerplan.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burgessadrien.exerplan.data.WorkoutPlanWithDays
import com.burgessadrien.exerplan.data.WorkoutRepository
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.UserSettings
import com.burgessadrien.exerplan.model.WeightUnit
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import com.burgessadrien.exerplan.utils.CsvImporter
import com.burgessadrien.exerplan.utils.ExcelImporter
import com.burgessadrien.exerplan.utils.MooseProgrammingImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutPlanViewModel(private val workoutRepository: WorkoutRepository) : ViewModel() {

    val userSettings: StateFlow<UserSettings> =
        workoutRepository.getUserSettings()
            .map { it ?: UserSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = UserSettings()
            )

    val workoutPlanUiState: StateFlow<WorkoutPlanUiState> = 
        combine(
            workoutRepository.getAllWorkoutPlansStream(),
            userSettings
        ) { plans, settings ->
            WorkoutPlanUiState(plans, settings.weightUnit)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = WorkoutPlanUiState()
        )

    val personalBests: StateFlow<List<PersonalBestLift>> =
        workoutRepository.getAllPersonalBests()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            if (workoutRepository.getAllWorkoutPlansStream().first().isEmpty()) {
                seedDatabase()
            }
        }
    }

    // Plan CRUD
    fun createWorkoutPlan(name: String, notes: List<String> = emptyList(), isPrimary: Boolean = false) {
        viewModelScope.launch {
            val planId = workoutRepository.insertWorkoutPlan(WorkoutPlan(name = name, notes = notes, isPrimary = isPrimary), emptyMap())
            if (isPrimary) {
                workoutRepository.setPrimaryPlan(planId)
            }
        }
    }

    fun updateWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutPlan(plan)
            if (plan.isPrimary) {
                workoutRepository.setPrimaryPlan(plan.id)
            }
        }
    }

    fun deleteWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutPlan(plan)
        }
    }

    // Block CRUD
    fun createWorkoutBlock(planId: Long, name: String) {
        viewModelScope.launch {
            workoutRepository.insertWorkoutBlock(WorkoutBlock(planId = planId, name = name))
        }
    }

    fun updateWorkoutBlock(block: WorkoutBlock) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutBlock(block)
        }
    }

    fun deleteWorkoutBlock(block: WorkoutBlock) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutBlock(block)
        }
    }

    fun toggleBlockCompletion(block: WorkoutBlock) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutBlock(block.copy(isCompleted = !block.isCompleted))
        }
    }

    // Day CRUD
    fun createWorkoutDay(planId: Long, name: String, type: DayType, blockId: Long? = null) {
        viewModelScope.launch {
            workoutRepository.insertWorkoutDay(WorkoutDay(planId = planId, name = name, dayType = type, blockId = blockId))
        }
    }

    fun updateWorkoutDay(day: WorkoutDay) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutDay(day)
        }
    }

    fun deleteWorkoutDay(day: WorkoutDay) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutDay(day)
        }
    }

    // Workout CRUD
    fun createWorkout(dayId: Long, workout: LiftingWorkout) {
        viewModelScope.launch {
            workoutRepository.insertWorkout(workout.copy(dayId = dayId))
        }
    }

    fun updateWorkout(workout: LiftingWorkout) {
        viewModelScope.launch {
            workoutRepository.updateWorkout(workout)
        }
    }

    fun deleteWorkout(workout: LiftingWorkout) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workout)
        }
    }

    // PB CRUD
    fun savePersonalBest(pb: PersonalBestLift) {
        viewModelScope.launch {
            workoutRepository.insertPersonalBest(pb)
        }
    }

    fun updatePersonalBest(pb: PersonalBestLift) {
        viewModelScope.launch {
            workoutRepository.updatePersonalBest(pb)
        }
    }

    fun deletePersonalBest(pb: PersonalBestLift) {
        viewModelScope.launch {
            workoutRepository.deletePersonalBest(pb)
        }
    }

    fun setPrimaryPlan(planId: Long) {
        viewModelScope.launch {
            workoutRepository.setPrimaryPlan(planId)
        }
    }

    fun toggleWorkoutCompletion(workout: LiftingWorkout) {
        viewModelScope.launch {
            workoutRepository.updateWorkout(workout.copy(isCompleted = !workout.isCompleted))
        }
    }

    fun updateWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            workoutRepository.updateUserSettings(userSettings.value.copy(weightUnit = unit))
        }
    }

    fun toggleWeightUnit() {
        val currentUnit = userSettings.value.weightUnit
        val newUnit = if (currentUnit == WeightUnit.KG) WeightUnit.LBS else WeightUnit.KG
        updateWeightUnit(newUnit)
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            workoutRepository.updateUserSettings(settings)
        }
    }

    fun importExcelPlan(context: Context, uri: Uri, targetPlanId: Long? = null, customName: String? = null, makePrimary: Boolean = false) {
        viewModelScope.launch {
            val importer = ExcelImporter(context)
            val result = importer.importWorkoutPlan(uri) ?: return@launch
            val (importedPlan, days) = result
            
            val finalPlanId: Long
            if (targetPlanId != null) {
                val blockName = customName ?: importedPlan.name
                workoutRepository.insertWorkoutsToBlock(targetPlanId, blockName, importedPlan.notes, days)
                finalPlanId = targetPlanId
            } else {
                val planName = customName ?: importedPlan.name
                finalPlanId = workoutRepository.insertWorkoutPlan(importedPlan.copy(name = planName, isPrimary = makePrimary), days)
            }

            if (makePrimary) {
                workoutRepository.setPrimaryPlan(finalPlanId)
            }
        }
    }

    fun importMoosePlan(context: Context, uri: Uri, targetPlanId: Long? = null, customName: String? = null, makePrimary: Boolean = false) {
        viewModelScope.launch {
            val importer = MooseProgrammingImporter(context)
            val (importedBlockName, blockNotes, days) = importer.importAsBlock(uri)
            if (days.isEmpty()) return@launch

            val finalPlanId: Long
            if (targetPlanId == null) {
                val planName = customName ?: (uri.lastPathSegment?.substringBeforeLast(".") ?: "Moose Plan")
                finalPlanId = workoutRepository.insertWorkoutPlan(WorkoutPlan(name = planName, isPrimary = makePrimary), emptyMap())
            } else {
                finalPlanId = targetPlanId
            }

            val blockName = customName ?: importedBlockName
            workoutRepository.insertWorkoutsToBlock(finalPlanId, blockName, blockNotes, days)

            if (makePrimary) {
                workoutRepository.setPrimaryPlan(finalPlanId)
            }
        }
    }

    fun importNippardPlan(context: Context, uri: Uri, targetPlanId: Long? = null, customName: String? = null, makePrimary: Boolean = false) {
        viewModelScope.launch {
            val importer = CsvImporter(context)
            val weeks = importer.importNippardPlan(uri)
            if (weeks.isEmpty()) return@launch

            val finalPlanId: Long
            if (targetPlanId == null) {
                val planName = customName ?: (uri.lastPathSegment?.substringBeforeLast(".") ?: "Nippard Plan")
                finalPlanId = workoutRepository.insertWorkoutPlan(WorkoutPlan(name = planName, isPrimary = makePrimary), emptyMap())
            } else {
                finalPlanId = targetPlanId
            }

            weeks.forEachIndexed { index, (plan, days) ->
                val blockName = if (weeks.size > 1) {
                    if (customName != null && targetPlanId != null) "$customName - Week ${index + 1}"
                    else "Week ${index + 1}"
                } else {
                    customName ?: "Imported Block"
                }
                workoutRepository.insertWorkoutsToBlock(finalPlanId, blockName, plan.notes, days)
            }

            if (makePrimary) {
                workoutRepository.setPrimaryPlan(finalPlanId)
            }
        }
    }

    private suspend fun seedDatabase() {
        val plan = WorkoutPlan(name = "PowerBuilding 3.0 - 4x", isPrimary = true)
        val fullBody1 = WorkoutDay(name = "FULL BODY 1", dayType = DayType.WORKING)
        val restDay = WorkoutDay(name = "Rest Day", dayType = DayType.REST)
        
        val workouts1 = listOf(
            LiftingWorkout(exerciseName = "Back Squat (Top Single)", sets = 5, warmUpSets = 4, workingSets = 1, reps = 1, setTime = "", load = 265.0, rpe = "6-8", rest = "3-5min", notes = "Top set. Focus on technique and explosive power."),
            LiftingWorkout(exerciseName = "Back Squat", sets = 8, warmUpSets = 3, workingSets = 5, reps = 5, setTime = "", load = 235.0, rpe = "7-9", rest = "3-5min", notes = "Be strict with form, keep your upper back tight to the bar."),
            LiftingWorkout(exerciseName = "Close Grip Bench Press", sets = 5, warmUpSets = 2, workingSets = 3, reps = 8, setTime = "", load = 175.0, rpe = "9", rest = "2-4min", notes = "Shoulder width grip, tuck your elbows in close to your sides."),
            LiftingWorkout(exerciseName = "Barbell RDL", sets = 3, warmUpSets = 1, workingSets = 2, reps = 8, setTime = "", load = 200.0, rpe = "9", rest = "2-3min", notes = "Back from rounding."),
            LiftingWorkout(exerciseName = "Chest-supported row", sets = 5, warmUpSets = 1, workingSets = 4, reps = 8, setTime = "", load = 135.0, rpe = "9", rest = "1-2min", notes = "Can use machine or brace against bench. Minimize cheating."),
            LiftingWorkout(exerciseName = "Dumbbell Lateral Raise", sets = 4, warmUpSets = 0, workingSets = 4, reps = 10, setTime = "", load = 25.0, rpe = "9", rest = "1-2min", notes = "get sloppy toward the end of the set."),
            LiftingWorkout(exerciseName = "Hanging Leg Raise", sets = 3, warmUpSets = 0, workingSets = 3, reps = 8, setTime = "", load = 0.0, rpe = "9", rest = "1-2min", notes = "difficulty.")
        )
        workoutRepository.insertWorkoutPlan(plan, mapOf(fullBody1 to workouts1, restDay to emptyList()))

        // Seed default PRs
        workoutRepository.insertPersonalBest(PersonalBestLift(exerciseName = "Back Squat", load = 0.0, repCount = 1))
        workoutRepository.insertPersonalBest(PersonalBestLift(exerciseName = "Deadlift", load = 0.0, repCount = 1))
        workoutRepository.insertPersonalBest(PersonalBestLift(exerciseName = "Bench Press", load = 0.0, repCount = 1))
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class WorkoutPlanUiState(
    val workoutPlans: List<WorkoutPlanWithDays> = listOf(),
    val weightUnit: WeightUnit = WeightUnit.LBS
)
