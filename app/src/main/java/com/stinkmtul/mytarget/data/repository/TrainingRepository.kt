package com.stinkmtul.mytarget.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.MyArcheryRoomDatabase
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.databases.entity.training.TrainingDao
import com.stinkmtul.mytarget.ui.detail.TrainingCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TrainingRepository(application: Application) {
    private val mTrainingDao: TrainingDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        val db = MyArcheryRoomDatabase.getDatabase(application)
        mTrainingDao = db.trainingDao()
    }

    fun insert(training: Training) {
        executorService.execute {
            mTrainingDao.insert(training)
        }
    }

    fun getAllTraining(): LiveData<List<Training>> = mTrainingDao.getAllTraining()

    fun getTrainingId(date: String, token: String): LiveData<Int?> {
        return mTrainingDao.getTrainingId(date, token)
    }

    fun deleteTrainingAndRelatedData(training: Training) {
        executorService.execute {
            mTrainingDao.deleteTrainingAndRelatedData(training)
        }
    }

    fun getTrainingCounts(trainingId: Int): LiveData<TrainingCounts> {
        return mTrainingDao.getTrainingCounts(trainingId)
    }

    fun deleteTrainingById(trainingId: Int) {
        executorService.execute {
            mTrainingDao.deleteTrainingById(trainingId)
        }
    }

<<<<<<< HEAD
    fun getTrainingById(trainingId: Int): LiveData<Training> {
        return mTrainingDao.getTrainingById(trainingId)
    }

=======
>>>>>>> 5738cb5dc0488deb19cb65e29d286d9f20c5f787
    suspend fun getTrainingCountsSync(trainingId: Int): TrainingCounts {
        return withContext(Dispatchers.IO) {
            mTrainingDao.getTrainingCountsSync(trainingId)
        }
    }
<<<<<<< HEAD
=======

>>>>>>> 5738cb5dc0488deb19cb65e29d286d9f20c5f787
}