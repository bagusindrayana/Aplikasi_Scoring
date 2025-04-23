package com.stinkmtul.mytarget.ui.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stinkmtul.mytarget.ui.form.FormViewModel

class DetailViewModelFactory (private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FormViewModel::class.java)) {
            return FormViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}