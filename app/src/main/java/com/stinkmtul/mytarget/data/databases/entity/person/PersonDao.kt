package com.stinkmtul.mytarget.data.databases.entity.person

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(person: Person)

    @Delete
    fun delete(person: Person)

    @Query("SELECT * FROM person")
    fun getAllPerson(): LiveData<List<Person>>

    @Query("SELECT person_id FROM person WHERE name = :name")
    fun getPersonId(name: String): LiveData<Int?>

    @Query("SELECT name FROM person WHERE person_id = :person_id")
    fun getNamePerson(person_id: Int): LiveData<String?>

    @Query("SELECT name FROM person WHERE person_id = :person_id LIMIT 1")
    suspend fun getNamePersonSync(person_id: Int): String?
}

