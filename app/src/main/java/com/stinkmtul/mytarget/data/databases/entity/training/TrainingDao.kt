package com.stinkmtul.mytarget.data.databases.entity.training

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stinkmtul.mytarget.ui.detail.TrainingCounts

@Dao
interface TrainingDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(training: Training)

    @Query("DELETE FROM Shot WHERE training_id = :trainingId")
    fun deleteShotByTrainingId(trainingId: Int)

    @Query("DELETE FROM Leaderboard WHERE training_id = :trainingId")
    fun deleteLeaderboardByTrainingId(trainingId: Int)

    @Delete
    fun delete(training: Training)

    @Transaction
    fun deleteTrainingAndRelatedData(training: Training) {
        deleteShotByTrainingId(training.training_id)
        deleteLeaderboardByTrainingId(training.training_id)
        delete(training)
    }

    @Query("SELECT session_count, shot_count FROM Training WHERE training_id = :trainingId")
    fun getTrainingCountsSync(trainingId: Int): TrainingCounts

    @Query("DELETE FROM Training WHERE training_id = :trainingId")
    fun deleteTrainingById(trainingId: Int)

    @Query("SELECT training_id FROM Training WHERE date = :date AND token = :token LIMIT 1")
    fun getTrainingId(date: String, token: String): LiveData<Int?>

    @Query("SELECT * FROM training ORDER BY training_id DESC")
    fun getAllTraining(): LiveData<List<Training>>

    @Query("SELECT session_count, shot_count FROM Training WHERE training_id = :trainingId")
    fun getTrainingCounts(trainingId: Int): LiveData<TrainingCounts>

<<<<<<< HEAD
    @Query("SELECT * FROM training WHERE training_id = :trainingId")
    fun getTrainingById(trainingId: Int): LiveData<Training>
=======
    @Query("SELECT session_count, shot_count FROM Training WHERE training_id = :trainingId")
    fun getTrainingCountsSync(trainingId: Int): TrainingCounts
>>>>>>> 5738cb5dc0488deb19cb65e29d286d9f20c5f787
}
