package com.stinkmtul.mytarget.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.MyArcheryRoomDatabase
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.person.PersonDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PersonRepository(application: Application) {
    private val mPersonDao: PersonDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        val db = MyArcheryRoomDatabase.getDatabase(application)
        mPersonDao = db.personDao()
    }

    fun getAllPerson(): LiveData<List<Person>> = mPersonDao.getAllPerson()

    fun insert(person: Person) {
        executorService.execute { mPersonDao.insert(person) }
    }

    fun delete(person: Person) {
        executorService.execute { mPersonDao.delete(person) }
    }

    fun getPersonId(name: String): LiveData<Int?> {
        return mPersonDao.getPersonId(name)
    }

    fun getNamePerson(person_id: Int): LiveData<String?> {
        return mPersonDao.getNamePerson(person_id)
    }

    suspend fun getNamePersonSync(personId: Int): String? {
        return withContext(Dispatchers.IO) {
            mPersonDao.getNamePersonSync(personId)
        }
    }
}
