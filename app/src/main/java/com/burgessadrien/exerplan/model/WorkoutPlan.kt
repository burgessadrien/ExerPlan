package com.burgessadrien.exerplan.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_primary", defaultValue = "0")
    val isPrimary: Boolean = false,
    val notes: List<String> = emptyList()
)
