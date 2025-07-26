package com.epdev.topotrackapp.utils

import android.util.Patterns

object ValidationUtils {
    
    /**
     * Valida que un email tenga un formato válido
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Valida que la contraseña tenga al menos 6 caracteres
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Valida datos de registro - SIMPLIFICADO
     */
    fun validateRegistration(name: String, email: String, phone: String, password: String): ValidationResult {
        when {
            name.isBlank() -> return ValidationResult.Error("El nombre es obligatorio")
            email.isBlank() -> return ValidationResult.Error("El email es obligatorio")
            !isValidEmail(email) -> return ValidationResult.Error("El formato del email no es válido")
            password.isBlank() -> return ValidationResult.Error("La contraseña es obligatoria")
            !isValidPassword(password) -> return ValidationResult.Error("La contraseña debe tener al menos 6 caracteres")
            else -> return ValidationResult.Success
        }
    }
    
    /**
     * Valida datos de login - SIMPLIFICADO
     */
    fun validateLogin(email: String, password: String): ValidationResult {
        when {
            email.isBlank() -> return ValidationResult.Error("El email es obligatorio")
            !isValidEmail(email) -> return ValidationResult.Error("El formato del email no es válido")
            password.isBlank() -> return ValidationResult.Error("La contraseña es obligatoria")
            else -> return ValidationResult.Success
        }
    }
    
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
