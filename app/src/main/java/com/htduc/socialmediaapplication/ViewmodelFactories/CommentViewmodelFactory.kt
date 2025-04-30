package com.htduc.socialmediaapplication.ViewmodelFactories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.htduc.socialmediaapplication.ViewModel.CommentViewModel

class CommentViewmodelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommentViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}