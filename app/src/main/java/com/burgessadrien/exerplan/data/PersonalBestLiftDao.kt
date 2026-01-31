package com.burgessadrien.exerplan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.burgessadrien.exerplan.model.PersonalBestLift
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBestLiftDao {
    @Query("SELECT * FROM personal_best_lifts ORDER BY exerciseName ASC")
    fun getAllPersonalBests(): Flow<List<PersonalBestLift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalBest(lift: PersonalBestLift)

    @Update
    suspend fun updatePersonalBest(lift: PersonalBestLift)

    @Delete
    suspend fun deletePersonalBest(lift: PersonalBestLift)

    @Query("SELECT * FROM personal_best_lifts WHERE exerciseName = :name AND repCount = :reps LIMIT 1")
    suspend fun getPersonalBest(name: String, reps: Int): PersonalBestLift?
}
