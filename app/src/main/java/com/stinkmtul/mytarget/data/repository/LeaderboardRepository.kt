package com.stinkmtul.mytarget.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.MyArcheryRoomDatabase
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.LeaderboardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LeaderboardRepository(application: Application) {
    private val mLeaderboardDao: LeaderboardDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        val db = MyArcheryRoomDatabase.getDatabase(application)
        mLeaderboardDao = db.leaderboardDao()
    }

    fun insert(leaderboard: Leaderboard) {
        executorService.execute { mLeaderboardDao.insert(leaderboard) }
    }

    fun getAllLeaderboard(training_id: Int): LiveData<List<Leaderboard>> = mLeaderboardDao.getAllLeaderboard(training_id)

    suspend fun getAllLeaderboardSync(trainingId: Int): List<Leaderboard> {
        return withContext(Dispatchers.IO) {
            mLeaderboardDao.getAllLeaderboardSync(trainingId)
        }
    }
}