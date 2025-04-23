package com.stinkmtul.mytarget.ui.form

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.repository.PersonRepository
import com.stinkmtul.mytarget.data.repository.TrainingRepository

class FormViewModel(application: Application) : AndroidViewModel(application) {

    private val trainingRepository: TrainingRepository = TrainingRepository(application)
    private val personRepository: PersonRepository = PersonRepository(application)

    fun insert(training: Training) {
        trainingRepository.insert(training)
    }

    fun getTrainingId(date: String, token: String): LiveData<Int?> {
        return trainingRepository.getTrainingId(date, token)
    }

    fun getPersonId(name: String): LiveData<Int?> {
        return personRepository.getPersonId(name)
    }
}
