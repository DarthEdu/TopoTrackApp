package com.epdev.topotrackapp.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegisterViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun register(name: String, email: String, phone: String, password: String) {
        
        // Validaciones básicas
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Complete los campos obligatorios"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (!isValidEmail(email)) {
            _errorMessage.value = "Formato de email inválido"
            return
        }

        _isLoading.value = true
        
        // Simulación simple - reemplazar con tu lógica
        // Aquí puedes agregar validación de email duplicado, etc.
        _registerSuccess.value = true
        _isLoading.value = false
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
