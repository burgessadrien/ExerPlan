package com.burgessadrien.exerplan.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.burgessadrien.exerplan.ExerPlanApplication

/**
 * Provides Factory to create instance of ViewModel for the entire app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for WorkoutPlanViewModel
        initializer {
            WorkoutPlanViewModel(
                exerplanApplication().repository
            )
        }
        // Initializer for TimerViewModel
        initializer {
            TimerViewModel(
                exerplanApplication()
            )
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [ExerPlanApplication].
 */
fun CreationExtras.exerplanApplication(): ExerPlanApplication = 
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExerPlanApplication)
