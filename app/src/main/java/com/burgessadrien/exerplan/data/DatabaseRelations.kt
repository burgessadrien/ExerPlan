package com.burgessadrien.exerplan.data

import androidx.room.Embedded
import androidx.room.Relation
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan

/**
 * Represents a [WorkoutDay] and its list of associated [LiftingWorkout]s.
 */
data class WorkoutDayWithWorkouts(
    @Embedded val workoutDay: WorkoutDay,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val workouts: List<LiftingWorkout>
)

/**
 * Represents a [WorkoutBlock] and its list of [WorkoutDayWithWorkouts].
 */
data class WorkoutBlockWithDays(
    @Embedded val block: WorkoutBlock,
    @Relation(
        entity = WorkoutDay::class,
        parentColumn = "id",
        entityColumn = "blockId"
    )
    val workoutDays: List<WorkoutDayWithWorkouts>
)

/**
 * Represents a [WorkoutPlan] and its list of [WorkoutDayWithWorkouts] and [WorkoutBlock].
 */
data class WorkoutPlanWithDays(
    @Embedded val plan: WorkoutPlan,
    @Relation(
        entity = WorkoutDay::class,
        parentColumn = "id",
        entityColumn = "planId"
    )
    val workoutDays: List<WorkoutDayWithWorkouts>,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val blocks: List<WorkoutBlock>
)
