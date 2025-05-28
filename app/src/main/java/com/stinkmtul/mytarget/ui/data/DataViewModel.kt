package com.stinkmtul.mytarget.ui.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.repository.LeaderboardRepository
import com.stinkmtul.mytarget.data.repository.PersonRepository
import com.stinkmtul.mytarget.data.repository.ShotRepository
import com.stinkmtul.mytarget.data.repository.TrainingRepository

class DataViewModel(application: Application) : ViewModel() {
    private val mShotRepository: ShotRepository = ShotRepository(application)
    private val mLeaderboardRepository: LeaderboardRepository = LeaderboardRepository(application)
    private val mTrainingRepository: TrainingRepository = TrainingRepository(application)
    private val mPersonRepository: PersonRepository = PersonRepository(application)

    fun insert(shot: Shot) {
        mShotRepository.insert(shot)
    }

    fun update(shot: Shot) {
        mShotRepository.update(shot)
    }

    fun updateLeaderboard(leaderboard: Leaderboard) {
        mLeaderboardRepository.update(leaderboard)
    }

    fun insertLeaderboard(leaderboard: Leaderboard) {
        mLeaderboardRepository.insert(leaderboard)
    }

    fun deleteAllShot(trainingId: Int) {
        mTrainingRepository.deleteShotData(trainingId)
    }

    fun saveData(trainingId: Int, shots : List<Shot>){
        mTrainingRepository.deleteShotData(trainingId)
        mShotRepository.insertAll(shots)
    }

    fun deleteTrainingById(trainingId: Int) {
        mTrainingRepository.deleteTrainingById(trainingId)
    }

    fun getShotsForPerson(personId: Int, trainingId: Int): LiveData<List<Shot>> {
        return mShotRepository.getShotsForPerson(personId, trainingId)
    }

    fun getTrainingById(trainingId: Int): LiveData<Training> {
        return mTrainingRepository.getTrainingById(trainingId)
    }

    suspend fun getAllLeaderboardSync(trainingId: Int): List<Leaderboard> {
        return mLeaderboardRepository.getAllLeaderboardSync(trainingId)
    }

    fun getDistinctPersonByTraining(trainingId: Int): LiveData<List<Int>> {
        return mShotRepository.getDistinctPersonByTraining(trainingId)
    }

    fun getNamePerson(person_id: Int): LiveData<String?> {
        return mPersonRepository.getNamePerson(person_id)
    }

    fun hubla() {
        Log.i("HUBLA","HUBLA")
    }
}