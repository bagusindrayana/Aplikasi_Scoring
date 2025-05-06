package com.stinkmtul.mytarget.ui.home

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.repository.PersonRepository
import com.stinkmtul.mytarget.data.repository.ShotRepository
import com.stinkmtul.mytarget.data.repository.TrainingRepository

class MainViewModel(application: Application) : ViewModel() {
    private val mTrainingRepository: TrainingRepository = TrainingRepository(application)
    private val mShotRepository: ShotRepository = ShotRepository(application)
    private val mPersonRepository: PersonRepository = PersonRepository(application)

    fun getAllTraining(): LiveData<List<Training>> {
        return mTrainingRepository.getAllTraining()
    }

    fun deleteTraining(training: Training) {
        mTrainingRepository.deleteTrainingAndRelatedData(training)
    }

    fun getDistinctPersonByTraining(trainingId: Int): LiveData<List<Int>> {
        return mShotRepository.getDistinctPersonByTraining(trainingId)
    }

    fun getNamePerson(person_id: Int): LiveData<String?> {
        return mPersonRepository.getNamePerson(person_id)
    }
}
