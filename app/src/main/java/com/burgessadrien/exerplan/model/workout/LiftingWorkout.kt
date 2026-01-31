package com.burgessadrien.exerplan.model.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.burgessadrien.exerplan.model.WorkoutDay

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(
        entity = WorkoutDay::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LiftingWorkout(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override var dayId: Long = 0,
    override val exerciseName: String,
    val sets: Int,
    val warmUpSets: Int = 0,
    val workingSets: Int = 0,
    val reps: Int? = null,
    val time: String? = null,
    val setTime: String = "",
    val load: Double? = null,
    val rpe: String,
    val rest: String,
    override val notes: String,
    override val isCompleted: Boolean = false,
    override val type: WorkoutType = WorkoutType.LIFTING
) : Workout(id, dayId, exerciseName, notes, isCompleted, type)
