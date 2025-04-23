package com.stinkmtul.mytarget.ui.data

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.repository.LeaderboardRepository
import com.stinkmtul.mytarget.data.repository.ShotRepository
import com.stinkmtul.mytarget.data.repository.TrainingRepository

class DataViewModel(application: Application) : ViewModel() {
    private val mShotRepository: ShotRepository = ShotRepository(application)
    private val mLeaderboardRepository: LeaderboardRepository = LeaderboardRepository(application)
    private val mTrainingRepository: TrainingRepository = TrainingRepository(application)

    fun insert(shot: Shot) {
        mShotRepository.insert(shot)
    }

    fun insertLeaderboard(leaderboard: Leaderboard) {
        mLeaderboardRepository.insert(leaderboard)
    }

    fun deleteTrainingById(trainingId: Int) {
        mTrainingRepository.deleteTrainingById(trainingId)
    }

    fun getShotsForPerson(personId: Int, trainingId: Int): LiveData<List<Shot>> {
        return mShotRepository.getShotsForPerson(personId, trainingId)
    }
}