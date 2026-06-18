package com.siffmember.info.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth


class ViewModelFactory(private val auth: FirebaseAuth) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
       if (modelClass.isAssignableFrom(AuthPhoneNumberViewModel::class.java)) {
            return AuthPhoneNumberViewModel(auth) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}