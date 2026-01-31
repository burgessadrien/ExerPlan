package com.burgessadrien.exerplan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.UserSettings
import com.burgessadrien.exerplan.model.WeightUnit
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.model.workout.WorkoutType
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan

class Converters {
    @TypeConverter
    fun fromDayType(value: DayType): String {
        return value.name
    }

    @TypeConverter
    fun toDayType(value: String): DayType {
        return DayType.valueOf(value)
    }

    @TypeConverter
    fun fromWeightUnit(value: WeightUnit): String {
        return value.name
    }

    @TypeConverter
    fun toWeightUnit(value: String): WeightUnit {
        return WeightUnit.valueOf(value)
    }

    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType {
        return WorkoutType.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split("|||")
    }
}

@Database(
    entities = [WorkoutPlan::class, WorkoutDay::class, LiftingWorkout::class, PersonalBestLift::class, UserSettings::class, WorkoutBlock::class],
    version = 16,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun personalBestLiftDao(): PersonalBestLiftDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exerplan_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
