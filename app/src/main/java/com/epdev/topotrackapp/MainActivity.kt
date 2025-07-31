package com.epdev.topotrackapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.epdev.topotrackapp.Foregrounds.LocationForegroundService
import com.epdev.topotrackapp.databinding.ActivityMainBinding
import com.epdev.topotrackapp.utils.UserPreferences
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // Lanzador para permisos de ubicación en primer plano
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                Toast.makeText(this, "Permisos de ubicación en primer plano concedidos", Toast.LENGTH_SHORT).show()
                checkAndRequestBackgroundLocationPermission() // Continuar con el permiso de fondo si es necesario
            } else {
                Toast.makeText(this, "Permisos de ubicación en primer plano denegados", Toast.LENGTH_SHORT).show()
                showPermissionDeniedDialog(isBackground = false)
            }
        }

    // Lanzador para permiso de ubicación en segundo plano
    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de ubicación en segundo plano concedido", Toast.LENGTH_SHORT).show()
                startLocationServiceIfPermitted() // Iniciar servicio después de que todos los permisos necesarios estén OK
            } else {
                Toast.makeText(this, "Permiso de ubicación en segundo plano denegado", Toast.LENGTH_SHORT).show()
                showPermissionDeniedDialog(isBackground = true)
                // Aunque se deniegue el de fondo, podríamos iniciar el servicio si solo necesitamos primer plano.
                // Depende de la lógica de tu app. Por ahora, asumimos que si se pide fondo, se espera.
                // Si la app puede funcionar solo con ubicación en primer plano, puedes llamar a startLocationServiceIfPermitted() aquí también.
            }
        }

    // Lanzador para permiso de notificaciones (necesario para servicios en primer plano en Android 13+)
    private lateinit var requestNotificationPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el usuario está logueado ANTES de inflar la UI y configurar todo
        if (!UserPreferences.isLoggedIn(this)) {
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

        setupUserHeader()
        setupNavigationListener()

        // Inicializar el lanzador de permisos de notificación aquí,
        // ya que registerForActivityResult debe llamarse durante la inicialización del Fragment/Activity.
        requestNotificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de notificaciones denegado. Las notificaciones del servicio podrían no mostrarse.", Toast.LENGTH_LONG).show()
                }
                // Después de la solicitud de notificación (concedida o no), proceder con los permisos de ubicación.
                checkAndRequestLocationPermissions()
            }

        // Iniciar el flujo de solicitud de permisos
        requestNecessaryPermissions()
    }

    private fun requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso de notificación ya concedido, proceder con ubicación
                    checkAndRequestLocationPermissions()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showRationaleDialog(
                        "Permiso de Notificaciones",
                        "Para mostrar el estado del servicio de ubicación, necesitamos permiso para enviar notificaciones.",
                        Manifest.permission.POST_NOTIFICATIONS,
                        requestNotificationPermissionLauncher
                    )
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Versiones anteriores a Android 13 no necesitan permiso de notificación explícito para foreground services
            checkAndRequestLocationPermissions()
        }
    }


    private fun checkAndRequestLocationPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            // Permisos de primer plano ya concedidos
            Toast.makeText(this, "Permisos de ubicación en primer plano ya estaban concedidos.", Toast.LENGTH_SHORT).show()
            checkAndRequestBackgroundLocationPermission()
        } else {
            // Solicitar permisos de primer plano
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showRationaleDialog(
                    "Permisos de Ubicación",
                    "Necesitamos acceso a tu ubicación para la funcionalidad de seguimiento.",
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    requestLocationPermissionLauncher
                )
            } else {
                requestLocationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    private fun checkAndRequestBackgroundLocationPermission() {
        // Solo solicitar si se necesita ubicación en segundo plano y los permisos de primer plano están concedidos.
        // Y solo en Android 10 (API 29) o superior.
        // **IMPORTANTE**: Decide si REALMENTE necesitas ACCESS_BACKGROUND_LOCATION.
        // Si tu servicio solo rastrea mientras la app está visible o en primer plano (con notificación),
        // podrías no necesitarlo, simplificando los permisos.
        // Por ahora, asumiremos que lo necesitas según tu manifiesto.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de ubicación en segundo plano ya concedido.", Toast.LENGTH_SHORT).show()
                startLocationServiceIfPermitted()
            } else {
                // Es buena práctica siempre mostrar una justificación clara para la ubicación en segundo plano.
                showRationaleDialog(
                    "Ubicación en Segundo Plano",
                    "Para continuar rastreando tu ubicación incluso cuando la app está en segundo plano (ej. durante una actividad larga), necesitamos permiso para acceder a la ubicación en segundo plano. Serás llevado a la configuración de la app para habilitarlo.",
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    requestBackgroundLocationPermissionLauncher,
                    isBackgroundPermission = true
                )
            }
        } else {
            // En versiones anteriores a Android Q, el permiso de ubicación en primer plano
            // era suficiente si ACCESS_BACKGROUND_LOCATION estaba en el manifiesto.
            startLocationServiceIfPermitted()
        }
    }


    private fun startLocationServiceIfPermitted() {
        // Verificar todos los permisos necesarios ANTES de iniciar el servicio
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        // El permiso FOREGROUND_SERVICE_LOCATION se concede implícitamente si se concede FINE o COARSE
        // y el servicio está correctamente declarado con foregroundServiceType="location".

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No se requiere permiso explícito antes de Android 13
        }

        if ((fineLocationGranted || coarseLocationGranted) && notificationsGranted) {
            Log.d("MainActivity", "Todos los permisos necesarios concedidos. Iniciando LocationForegroundService.")
            val serviceIntent = Intent(this, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Log.w("MainActivity", "No se iniciará el servicio. Faltan permisos. Fine/Coarse: ${fineLocationGranted || coarseLocationGranted}, Notificaciones: $notificationsGranted")
            var missingPermissions = "Faltan permisos:"
            if (!(fineLocationGranted || coarseLocationGranted)) missingPermissions += " Ubicación."
            if (!notificationsGranted) missingPermissions += " Notificaciones."
            Toast.makeText(this, "No se puede iniciar el servicio de ubicación. $missingPermissions", Toast.LENGTH_LONG).show()

            // Podrías mostrar un diálogo aquí para guiar al usuario a la configuración si aún faltan
            // showPermissionDeniedDialog(isBackground = !(fineLocationGranted || coarseLocationGranted))
        }
    }


    private fun showRationaleDialog(
        title: String,
        message: String,
        permissionOrPermissions: Any, // String o Array<String>
        launcher: ActivityResultLauncher<*>,
        isBackgroundPermission: Boolean = false // Para manejar la lógica específica de background si es necesario
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continuar") { _, _ ->
                when (permissionOrPermissions) {
                    is String -> (launcher as ActivityResultLauncher<String>).launch(permissionOrPermissions)
                    is Array<*> -> (launcher as ActivityResultLauncher<Array<String>>).launch(permissionOrPermissions as Array<String>)
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Funcionalidad limitada debido a permisos denegados.", Toast.LENGTH_LONG).show()
                // Si se cancela la solicitud de permisos críticos, considera qué hacer.
                // ¿La app puede funcionar sin ellos? Si no, podrías cerrar la actividad o deshabilitar funciones.
                // Por ahora, solo mostramos un Toast.
                if (isBackgroundPermission || permissionOrPermissions == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                    // Si se canceló la solicitud de background, aún podríamos tener los de primer plano.
                    // Intentar iniciar el servicio solo con permisos de primer plano si la app lo soporta.
                    startLocationServiceIfPermitted() // Intentará iniciar si los de primer plano están OK
                }
            }
            .setCancelable(false) // El usuario debe tomar una decisión
            .show()
    }

    private fun showPermissionDeniedDialog(isBackground: Boolean) {
        val message = if (isBackground) {
            "El permiso de ubicación en segundo plano es necesario para el rastreo continuo. Por favor, habilítalo en la configuración de la app."
        } else {
            "Los permisos de ubicación (y notificaciones en Android 13+) son necesarios para la funcionalidad principal. Por favor, habilítalos en la configuración de la app."
        }
        AlertDialog.Builder(this)
            .setTitle("Permiso Denegado")
            .setMessage(message)
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "La funcionalidad de ubicación no estará disponible.", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }


    // --- Métodos existentes (sin cambios) ---
    private fun setupNavigationListener() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    logout()
                    true // Devuelve true para indicar que el evento ha sido manejado
                }
                else -> {
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    // Dejar que NavigationUI maneje el item si no es nuestro caso especial
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    // Si NavigationUI no lo manejó (por ejemplo, es un item que no navega),
                    // y tú tampoco lo hiciste, devuelve false o lo que corresponda.
                    // En este caso, si llega aquí, es porque onNavDestinationSelected se encargó o no.
                    handled // o super.onOptionsItemSelected si esto fuera un menú de opciones
                }
            }
        }
    }


    private fun logout() {
        UserPreferences.clearUserData(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUserHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.nav_user_name)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.nav_user_email)
        val userName = UserPreferences.getUserName(this)
        val userEmail = UserPreferences.getUserEmail(this)
        userNameTextView.text = if (userName.isNotEmpty()) userName else "Usuario"
        userEmailTextView.text = if (userEmail.isNotEmpty()) userEmail else "email@ejemplo.com"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
