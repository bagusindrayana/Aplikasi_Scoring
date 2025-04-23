package com.stinkmtul.mytarget.data.databases.entity.shot

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShotDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(shot: Shot)

    @Query("SELECT * FROM shot WHERE person_id = :personId AND training_id = :trainingId")
    fun getShotsForPerson(personId: Int, trainingId: Int): LiveData<List<Shot>>

    @Query("SELECT DISTINCT person_id FROM shot WHERE training_id = :trainingId")
    fun getDistinctPersonByTraining(trainingId: Int): LiveData<List<Int>>

    @Query("SELECT * FROM shot WHERE person_id = :personId AND training_id = :trainingId ORDER BY session, shot_number")
    suspend fun getShotsForPersonSync(personId: Int, trainingId: Int): List<Shot>

    @Query("SELECT DISTINCT person_id FROM shot WHERE training_id = :trainingId")
    suspend fun getDistinctPersonByTrainingSync(trainingId: Int): List<Int>
}