package com.burgessadrien.exerplan.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WeightUnit {
    KG, LBS
}

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.LBS,
    val setupTimerSeconds: Int = 5
)
