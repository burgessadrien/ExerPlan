package com.burgessadrien.exerplan.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_blocks",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlan::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WorkoutBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val name: String,
    val isCompleted: Boolean = false,
    val notes: List<String> = emptyList()
)
