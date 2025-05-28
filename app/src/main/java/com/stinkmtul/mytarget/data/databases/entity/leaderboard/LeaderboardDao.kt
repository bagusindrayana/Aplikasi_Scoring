package com.stinkmtul.mytarget.data.databases.entity.leaderboard

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LeaderboardDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(leaderboard: Leaderboard)


    @Update
    fun update(leaderboard: Leaderboard)

    @Insert
    fun insertAll(leaderboards: List<Leaderboard>)


    @Update
    fun updateAll(leaderboards: List<Leaderboard>)

    @Query("SELECT * FROM Leaderboard WHERE training_id = :training_id")
    fun getAllLeaderboard(training_id : Int): LiveData<List<Leaderboard>>

    @Query("SELECT * FROM leaderboard WHERE training_id = :trainingId ORDER BY the_champion ASC")
    suspend fun getAllLeaderboardSync(trainingId: Int): List<Leaderboard>
}