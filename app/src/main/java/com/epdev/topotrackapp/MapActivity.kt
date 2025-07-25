package com.epdev.topotrackapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.epdev.topotrackapp.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest // Especificando para evitar ambigüedad
    private lateinit var locationCallback: LocationCallback
    private val supabaseUrl = "https://mqpsbzrziuppiigkbiva.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1xcHNienJ6aXVwcGlpZ2tiaXZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEzMjIzNzIsImV4cCI6MjA2Njg5ODM3Mn0.yiCxB62ygVCmULMttRlrnC3HXmmh-vmCj4CAQYbD5zo"
    private val httpClient = HttpClient(Android) { // OJO: io.ktor.client.HttpClient
        install(ContentNegotiation) {
            json()
        }
    }

    private var contador : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización del mapa
        val prefs = applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)

        val map = binding.map

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(15.0)

        val startPoint = GeoPoint(-0.1807, -78.4678) // Quito, Ecuador por ejemplo
        mapController.setCenter(startPoint)

        // Marcador
        val marker = Marker(map)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Estoy aquí"
        map.overlays.add(marker)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar la solicitud de ubicación (Usando el builder correcto)
        locationRequest = com.google.android.gms.location.LocationRequest.Builder(25000L) // 30 segundos
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(15000L)
            .setMaxUpdateDelayMillis(30000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val newLocation = GeoPoint(location.latitude, location.longitude)
                    marker.position = newLocation
                    contador--
                    if (contador==0){
                        binding.map.controller.animateTo(newLocation)
                    }
                    binding.map.invalidate()
                    android.util.Log.i("Moviendo...", "Objetivo moviendose")
                    saveLocationToSupabase(location.latitude, location.longitude) // Guardar en Supabase
                }
            }
        }
        checkLocationPermissionAndStartUpdates()
    }

    @Serializable
    data class LocationSupabaseData(
        val latitud: Double,
        val longitud: Double
    )
    private fun saveLocationToSupabase(latitude: Double, longitude: Double) {
        // Usar CoroutineScope para operaciones de red en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {

            try {
                val locationData = LocationSupabaseData(latitud = latitude, longitud = longitude)

                val response: HttpResponse = httpClient.post("$supabaseUrl/rest/v1/Ubicaciones") { // Nombre de tu tabla
                    header("apikey", supabaseKey) // Clave de API anónima de Supabase
                    header("Authorization", "Bearer $supabaseKey") // Para RLS que usan la anon key como Bearer
                    contentType(ContentType.Application.Json) // Indicamos que enviamos JSON
                    accept(ContentType.Application.Json) // Indicamos que aceptamos JSON como respuesta
                    setBody(locationData) // El objeto a enviar, Ktor lo serializará a JSON
                }

                withContext(Dispatchers.Main) { // Volver al hilo principal para actualizar UI
                    if (response.status.isSuccess()) {
                        android.util.Log.i("Supabase", "Ubicación guardada con éxito: ${response.status}")
                    } else {
                        val errorBody = response.bodyAsText() // Intenta obtener más detalles del error
                        Toast.makeText(this@MapActivity, "Error al guardar ubicación: ${response.status} - $errorBody", Toast.LENGTH_LONG).show()
                        android.util.Log.e("Supabase", "Error al guardar: ${response.status}, Body: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapActivity, "Excepción al guardar ubicación: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("Supabase", "Excepción al guardar: ${e.message}", e)
                }
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLocationPermissionAndStartUpdates() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Se necesita permiso de ubicación para mostrar latitud y longitud.",
                    Toast.LENGTH_LONG
                ).show()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Error de seguridad al iniciar actualizaciones: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        // Reanudar las actualizaciones si el permiso está concedido
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {     startLocationUpdates()
            android.util.Log.d("MainActivityLifecycle", "onResume - Location updates started")
        } else {
            android.util.Log.d("MainActivityLifecycle", "onResume - Location permission not granted")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de detener las actualizaciones si la actividad se destruye completamente
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // Opcional: Cerrar el httpClient si no se va a usar más en toda la app
        // httpClient.close() // Considera el ciclo de vida de tu cliente si es compartido
        android.util.Log.d("MainActivityLifecycle", "onDestroy - Location updates removed and activity destroyed")
    }
}