package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epdev.topotrackapp.databinding.ActivityRegisterBinding

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
            val lastName = binding.lastNameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val address = binding.addressEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            register(name, lastName, email, address, phone, password)
        }

        // Link para ir al login
        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun register(name: String, lastName: String, email: String, 
                        address: String, phone: String, password: String) {
        
        // Validaciones básicas
        if (name.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            showError("Complete los campos obligatorios")
            return
        }

        if (password.length < 6) {
            showError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        // Mostrar loading
        showLoading(true)

        // Simulación de registro (reemplazar con tu lógica)
        binding.registerButton.postDelayed({
            showLoading(false)
            Toast.makeText(this, "Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
            
            // Ir al login después de registro exitoso
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2000)
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
