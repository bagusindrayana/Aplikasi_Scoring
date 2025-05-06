package com.stinkmtul.mytarget.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.MyArcheryRoomDatabase
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.shot.ShotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShotRepository(application: Application) {
    private val mShotDao: ShotDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        val db = MyArcheryRoomDatabase.getDatabase(application)
        mShotDao = db.shotDao()
    }

    fun insert(shot: Shot) {
        executorService.execute { mShotDao.insert(shot) }
    }

    fun update(shot: Shot) {
        executorService.execute {
            mShotDao.update(shot)
        }
    }

    fun getShotsForPerson(personId: Int, trainingId: Int): LiveData<List<Shot>> {
        return mShotDao.getShotsForPerson(personId, trainingId)
    }

    fun getDistinctPersonByTraining(trainingId: Int): LiveData<List<Int>> {
        return mShotDao.getDistinctPersonByTraining(trainingId)
    }

    suspend fun getShotsForPersonSync(personId: Int, trainingId: Int): List<Shot> {
        return withContext(Dispatchers.IO) {
            mShotDao.getShotsForPersonSync(personId, trainingId)
        }
    }
}