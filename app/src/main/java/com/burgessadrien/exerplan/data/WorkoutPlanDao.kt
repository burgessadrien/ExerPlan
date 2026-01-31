package com.burgessadrien.exerplan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Transaction
    suspend fun insertFullWorkoutPlan(plan: WorkoutPlan, daysWithWorkouts: Map<WorkoutDay, List<LiftingWorkout>>): Long {
        val planId = insertPlan(plan)
        
        daysWithWorkouts.forEach { (day, workouts) ->
            day.planId = planId
            val dayId = insertDay(day)
            workouts.forEach { it.dayId = dayId }
            insertWorkouts(workouts)
        }
        return planId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    // Block CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: WorkoutBlock): Long

    @Update
    suspend fun updateBlock(block: WorkoutBlock)

    @Delete
    suspend fun deleteBlock(block: WorkoutBlock)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: WorkoutDay): Long

    @Update
    suspend fun updateDay(day: WorkoutDay)

    @Delete
    suspend fun deleteDay(day: WorkoutDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<LiftingWorkout>)

    @Update
    suspend fun updateWorkout(workout: LiftingWorkout)

    @Delete
    suspend fun deleteWorkout(workout: LiftingWorkout)

    @Transaction
    @Query("SELECT * FROM workout_plans")
    fun getAllWorkoutPlansWithDays(): Flow<List<WorkoutPlanWithDays>>

    @Transaction
    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    suspend fun getWorkoutDayWithWorkoutsSync(dayId: Long): WorkoutDayWithWorkouts

    @Query("UPDATE workout_plans SET is_primary = 0")
    suspend fun resetPrimaryPlans()

    @Query("UPDATE workout_plans SET is_primary = 1 WHERE id = :planId")
    suspend fun setPrimaryPlan(planId: Long)

    @Transaction
    suspend fun setAsPrimary(planId: Long) {
        resetPrimaryPlans()
        setPrimaryPlan(planId)
    }
}
