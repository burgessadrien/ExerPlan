# ExerPlan

ExerPlan is a modern Android application designed for gym-goers and powerlifters to manage their workout plans, track their progress, and import structured training programs.

## Features

- **Workout Plan Management**: Create and organize multiple workout plans with support for blocks and individual days.
- **Progress Tracking**: Log your personal bests (PBs) for various exercises and see them automatically integrated into your workouts.
- **Smart Importers**: Import training programs from structured formats including:
    - Standard Excel spreadsheets.
    - Moose Coaching CSV files (Power Building 3.0 style).
    - Jeff Nippard's CSV programming.
- **In-App Rest Timer**: Integrated rest timer with customizable setup and extension durations.
- **Dynamic Load Estimation**: Automatically estimates suggested loads based on your recorded personal bests and RPE/reps targets.
- **Modern UI**: Built entirely with Jetpack Compose following Material 3 design principles.

## Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- Android SDK 34+
- A compatible Android device or emulator running Android 8.0 (API 26) or higher.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/ExerPlan.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on your device or emulator.

## Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern declarative UI toolkit.
- **Architecture**: MVVM (Model-View-ViewModel) with a Repository pattern for data management.
- **Database**: [Room](https://developer.android.com/training/data-storage/room) - SQLite abstraction for local persistence.
- **Asynchronous Work**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html).
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation).
- **Excel/CSV Parsing**: Apache POI for Excel and custom robust CSV parsing for program imports.

## Development Rules

This project follows strict development guidelines documented in [AGENTS.md](app/AGENTS.md). Contributors should prioritize:
- **KISS**: Simplicity over complexity.
- **Functional Programming**: Immutability and pure functions.
- **SOLID Principles**: Maintainable and extensible architecture.
- **Clean Code**: Meaningful naming and small, focused functions.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
