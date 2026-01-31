package com.burgessadrien.exerplan.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class DayType {
    WORKING, REST
}

@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutBlock::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var planId: Long = 0,
    var blockId: Long? = null,
    val name: String,
    val dayType: DayType = DayType.WORKING,
    val isCompleted: Boolean = false
)
