
# ExerPlan App Development Summary

This document summarizes the development progress of the ExerPlan Android application.

## Data Models

We have defined the core data structures for the application in the `com.burgessadrien.exerplan.model` package:

- `Workout`: Represents a single exercise with properties like name, sets, reps, load, etc.
- `WorkoutDay`: Represents a collection of `Workout` objects for a specific day.
- `PersonalBestLift`: Represents a personal record for a specific exercise, including the weight and rep count.
- `PersonalBests`: A collection of `PersonalBestLift` records.

## User Interface

The UI is built using Jetpack Compose and Material 3, located in the `com.burgessadrien.exerplan.view` package.

- `WorkoutPlanScreen`: The main screen of the app, which includes:
    - A `NavHost` to manage navigation between different screens.
    - A bottom navigation bar with two tabs: "Workout Plan" and "Workout".
    - A list of workout days.
- **Workout Detail Screen**: A screen that displays the details of a selected workout day, with each exercise shown in a `Card`.
- **Navigation**: The app uses `NavHost` to navigate between the main workout list and the detail screen. The state of the last-viewed workout is saved.

## Project Setup

- **Dependencies**: We have added the necessary dependencies for Jetpack Compose, Material 3, and Navigation to the `build.gradle.kts` and `libs.versions.toml` files.
- **AMOLED Theme**: The UI is designed to be AMOLED-friendly, using dark colors and system colors.
