package com.burgessadrien.exerplan.model

abstract class Workout(
    open val id: Long = 0,
    open var dayId: Long = 0,
    open val exerciseName: String,
    open val notes: String,
    open val isCompleted: Boolean = false
)
