package com.burgessadrien.exerplan.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
    val warmUpSets: Int,
    val workingSets: Int,
    val reps: Int,
    val setTime: String = "",
    val load: Double? = null,
    val rpe: String,
    val rest: String,
    override val notes: String,
    override val isCompleted: Boolean = false
) : Workout(id, dayId, exerciseName, notes, isCompleted)
