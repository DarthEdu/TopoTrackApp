package com.epdev.topotrackapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Complete todos los campos"
            return
        }

        _isLoading.value = true
        
        // Simulación simple - reemplazar con tu lógica
        if (email == "admin" && password == "123456") {
            _loginSuccess.value = true
        } else {
            _errorMessage.value = "Credenciales incorrectas"
        }
        
        _isLoading.value = false
    }
}
