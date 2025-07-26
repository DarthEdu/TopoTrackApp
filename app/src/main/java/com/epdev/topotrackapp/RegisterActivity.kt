package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.epdev.topotrackapp.databinding.ActivityRegisterBinding
import com.epdev.topotrackapp.utils.SupabaseManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }

    private fun setupUI() {
        // Botón crear cuenta
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            register(name, email, phone, password)
        }

        // Link para ir al login
        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun register(name: String, email: String, phone: String, password: String) {
        
        // Validaciones básicas
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            showError("Complete los campos obligatorios")
            return
        }

        if (password.length < 6) {
            showError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        // Mostrar loading
        showLoading(true)

        // Registro con Supabase Auth
        lifecycleScope.launch {
            val result = SupabaseManager.signUp(email, password)
            
            showLoading(false)
            
            if (result.isSuccess) {
                Toast.makeText(this@RegisterActivity, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
                
                // Ir al login después de registro exitoso
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                showError("Error al crear cuenta: $error")
            }
        }
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !show
        
        if (!show) {
            binding.errorMessage.visibility = View.GONE
        }
    }
}
