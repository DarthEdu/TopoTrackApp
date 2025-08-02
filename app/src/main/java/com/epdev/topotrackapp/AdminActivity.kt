package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epdev.topotrackapp.databinding.ActivityAdminBinding
import com.epdev.topotrackapp.utils.UserPreferences

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Panel de Administrador"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupUI() {
        // Configurar las tarjetas de estadísticas
        setupStatisticsCards()
        
        // Configurar botones de acción
        setupActionButtons()
    }

    private fun setupStatisticsCards() {
        // Aquí puedes agregar lógica para mostrar estadísticas reales
        binding.cardTotalUsers.setOnClickListener {
            Toast.makeText(this, "Ver usuarios registrados", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir una nueva actividad o fragmento para mostrar usuarios
        }
        
        binding.cardActiveSessions.setOnClickListener {
            Toast.makeText(this, "Ver sesiones activas", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir una nueva actividad o fragmento para mostrar sesiones
        }
        
        binding.cardSystemStatus.setOnClickListener {
            Toast.makeText(this, "Estado del sistema", Toast.LENGTH_SHORT).show()
            // Aquí puedes mostrar el estado del sistema
        }
    }

    private fun setupActionButtons() {
        binding.btnManageUsers.setOnClickListener {
            Toast.makeText(this, "Gestionar usuarios", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir la gestión de usuarios
        }
        
        binding.btnSystemSettings.setOnClickListener {
            Toast.makeText(this, "Configuración del sistema", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir la configuración del sistema
        }
        
        binding.btnViewLogs.setOnClickListener {
            Toast.makeText(this, "Ver logs del sistema", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir los logs del sistema
        }
        
        binding.btnBackupData.setOnClickListener {
            Toast.makeText(this, "Respaldar datos", Toast.LENGTH_SHORT).show()
            // Aquí puedes implementar la funcionalidad de respaldo
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        UserPreferences.clearUserData(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 