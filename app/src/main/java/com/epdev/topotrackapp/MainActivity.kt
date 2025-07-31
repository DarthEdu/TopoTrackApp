package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.epdev.topotrackapp.Foregrounds.LocationForegroundService
import com.epdev.topotrackapp.databinding.ActivityMainBinding
import com.epdev.topotrackapp.utils.UserPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el usuario está logueado
        if (!UserPreferences.isLoggedIn(this)) {
            // Si no está logueado, ir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Configurar datos del usuario en el header de navegación
        setupUserHeader()
        
        // Configurar listener para el menú de navegación
        setupNavigationListener()
        // Iniciar servicio de ubicación en segundo plano
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val serviceIntent = Intent(this, LocationForegroundService::class.java)
            startForegroundService(serviceIntent)
        } else {
            val serviceIntent = Intent(this, LocationForegroundService::class.java)
            startService(serviceIntent)
        }
    }

    private fun setupNavigationListener() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> {
                    // Manejo estándar de navegación
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
            }
        }
    }

    private fun logout() {
        // Limpiar datos del usuario
        UserPreferences.clearUserData(this)
        
        // Ir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUserHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.nav_user_name)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.nav_user_email)
        
        // Obtener datos del usuario guardados
        val userName = UserPreferences.getUserName(this)
        val userEmail = UserPreferences.getUserEmail(this)
        
        // Mostrar los datos en el header
        userNameTextView.text = if (userName.isNotEmpty()) userName else "Usuario"
        userEmailTextView.text = if (userEmail.isNotEmpty()) userEmail else "email@ejemplo.com"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}