package com.burgessadrien.exerplan

import android.app.Application
import com.burgessadrien.exerplan.data.AppDatabase
import com.burgessadrien.exerplan.data.WorkoutRepository

class ExerPlanApplication : Application() {
    // Using by lazy so the database and repository are only created when they're needed
    // rather than when the application starts.
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        WorkoutRepository(
            database.workoutPlanDao(), 
            database.personalBestLiftDao(),
            database.settingsDao()
        ) 
    }
}
