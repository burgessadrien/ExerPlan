package com.burgessadrien.exerplan.data

import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.UserSettings
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [WorkoutPlan] from a given
 * data source.
 */
class WorkoutRepository(
    private val workoutPlanDao: WorkoutPlanDao,
    private val personalBestLiftDao: PersonalBestLiftDao,
    private val settingsDao: SettingsDao
) {

    fun getAllWorkoutPlansStream(): Flow<List<WorkoutPlanWithDays>> = workoutPlanDao.getAllWorkoutPlansWithDays()

    suspend fun insertWorkoutPlan(plan: WorkoutPlan, daysWithWorkouts: Map<WorkoutDay, List<LiftingWorkout>>): Long {
        return workoutPlanDao.insertFullWorkoutPlan(plan, daysWithWorkouts)
    }

    suspend fun updateWorkoutPlan(plan: WorkoutPlan) = workoutPlanDao.updatePlan(plan)

    suspend fun setPrimaryPlan(planId: Long) {
        workoutPlanDao.setAsPrimary(planId)
    }

    suspend fun updateWorkout(workout: LiftingWorkout) {
        workoutPlanDao.updateWorkout(workout)
    }

    suspend fun deleteWorkoutPlan(plan: WorkoutPlan) {
        workoutPlanDao.deletePlan(plan)
    }

    // Block operations
    suspend fun insertWorkoutBlock(block: WorkoutBlock) = workoutPlanDao.insertBlock(block)
    suspend fun updateWorkoutBlock(block: WorkoutBlock) = workoutPlanDao.updateBlock(block)
    suspend fun deleteWorkoutBlock(block: WorkoutBlock) = workoutPlanDao.deleteBlock(block)

    suspend fun insertWorkoutDay(day: WorkoutDay) = workoutPlanDao.insertDay(day)

    suspend fun updateWorkoutDay(day: WorkoutDay) = workoutPlanDao.updateDay(day)

    suspend fun deleteWorkoutDay(day: WorkoutDay) = workoutPlanDao.deleteDay(day)

    suspend fun insertWorkout(workout: LiftingWorkout) = workoutPlanDao.insertWorkouts(listOf(workout))

    suspend fun deleteWorkout(workout: LiftingWorkout) = workoutPlanDao.deleteWorkout(workout)

    fun getAllPersonalBests(): Flow<List<PersonalBestLift>> = personalBestLiftDao.getAllPersonalBests()

    suspend fun insertPersonalBest(lift: PersonalBestLift) {
        personalBestLiftDao.insertPersonalBest(lift)
    }

    suspend fun updatePersonalBest(lift: PersonalBestLift) = personalBestLiftDao.updatePersonalBest(lift)

    suspend fun deletePersonalBest(lift: PersonalBestLift) = personalBestLiftDao.deletePersonalBest(lift)

    fun getUserSettings(): Flow<UserSettings?> = settingsDao.getUserSettings()

    suspend fun updateUserSettings(settings: UserSettings) {
        settingsDao.updateSettings(settings)
    }

    suspend fun insertWorkoutsToBlock(planId: Long, blockName: String, blockNotes: List<String> = emptyList(), daysWithWorkouts: Map<WorkoutDay, List<LiftingWorkout>>) {
        val blockId = workoutPlanDao.insertBlock(WorkoutBlock(planId = planId, name = blockName, notes = blockNotes))
        daysWithWorkouts.forEach { (day, workouts) ->
            day.planId = planId
            day.blockId = blockId
            val dayId = workoutPlanDao.insertDay(day)
            workouts.forEach { it.dayId = dayId }
            workoutPlanDao.insertWorkouts(workouts)
        }
    }
}
