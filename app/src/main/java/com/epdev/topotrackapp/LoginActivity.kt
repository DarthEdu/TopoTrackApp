package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.epdev.topotrackapp.databinding.ActivityLoginBinding
import com.epdev.topotrackapp.utils.SupabaseManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }

    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            login(email, password)
        }

        // Link para ir al registro
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        // Validaciones básicas
        if (email.isBlank() || password.isBlank()) {
            showError("Complete todos los campos")
            return
        }

        // Mostrar loading
        showLoading(true)

        // Login con Supabase Auth
        lifecycleScope.launch {
            val result = SupabaseManager.signIn(email, password)
            
            showLoading(false)
            
            if (result.isSuccess) {
                Toast.makeText(this@LoginActivity, "¡Login exitoso!", Toast.LENGTH_SHORT).show()
                
                // Ir a MainActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                showError("Error al iniciar sesión: $error")
            }
        }
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        
        if (!show) {
            binding.errorMessage.visibility = View.GONE
        }
    }
}
