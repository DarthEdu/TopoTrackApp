package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.epdev.topotrackapp.databinding.ActivityLoginBinding
import com.epdev.topotrackapp.ui.login.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            viewModel.login(email, password)
        }

        // Link para ir al registro
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = !isLoading
        }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login exitoso!", Toast.LENGTH_SHORT).show()
                // Ir a MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Cerrar LoginActivity
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                binding.errorMessage.text = it
                binding.errorMessage.visibility = View.VISIBLE
            } ?: run {
                binding.errorMessage.visibility = View.GONE
            }
        }
    }
}
