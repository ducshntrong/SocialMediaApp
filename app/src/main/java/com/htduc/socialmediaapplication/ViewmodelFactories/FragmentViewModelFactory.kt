package com.htduc.socialmediaapplication.ViewmodelFactories

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel

class FragmentViewModelFactory(private val application: Application, private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FragmentViewModel(application, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}