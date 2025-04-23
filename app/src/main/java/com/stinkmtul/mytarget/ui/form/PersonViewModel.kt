package com.stinkmtul.mytarget.ui.form

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.repository.PersonRepository
import kotlinx.coroutines.launch

class PersonViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PersonRepository = PersonRepository(application)
    val allPersons: LiveData<List<Person>> = repository.getAllPerson()

    fun insert(person: Person) {
        viewModelScope.launch {
            repository.insert(person)
        }
    }
}