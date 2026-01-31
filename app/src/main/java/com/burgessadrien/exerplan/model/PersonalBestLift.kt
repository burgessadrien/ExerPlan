package com.burgessadrien.exerplan.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_best_lifts")
data class PersonalBestLift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseName: String,
    val load: Double,
    val repCount: Int
)
