package com.stinkmtul.mytarget.ui.home

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.repository.TrainingRepository

class MainViewModel(application: Application) : ViewModel() {
    private val mTrainingRepository: TrainingRepository = TrainingRepository(application)

    fun getAllTraining(): LiveData<List<Training>> {
        return mTrainingRepository.getAllTraining()
    }

    fun deleteTraining(training: Training) {
        mTrainingRepository.deleteTrainingAndRelatedData(training)
    }

}
