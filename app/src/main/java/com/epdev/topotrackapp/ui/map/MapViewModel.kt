package com.epdev.topotrackapp.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epdev.topotrackapp.utils.UserPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint



class MapViewModel : ViewModel() {

    private val _location = MutableLiveData<GeoPoint>()
    val location: LiveData<GeoPoint> = _location
    private var isUpdatingLocation = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val jsonParser = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }

    fun nombreUsuarioActual(context: Context) : String{
        val userName = UserPreferences.getUserName(context)
        return userName
    }
    fun requestLocationUpdates(context: Context) {
        if (isUpdatingLocation) return // Evita múltiples registros

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(10000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(10000L)
            .setMaxUpdateDelayMillis(12000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    if (_location.value != geoPoint) {
                        _location.postValue(geoPoint)
                        saveLocationToSupabase(context, location.latitude, location.longitude)
                    }
                    break
                }
            }
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isUpdatingLocation = true
        }
    }

    fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isUpdatingLocation = false
        }
    }

    @Serializable
    data class LocationSupabaseData(val latitud : Double, val longitud : Double, val usuario : String)

    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM" // tu clave aquí

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }

    //funcion para actualizacion de ubicaciones en tiempo real
    fun saveLocationToSupabase(context: Context, lat: Double, lon: Double) {
        val userEmail = UserPreferences.getUserEmail(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = LocationSupabaseData(latitud = lat, longitud = lon, usuario = userEmail)
                val response: HttpResponse = httpClient.post(
                    "$supabaseUrl/rest/v1/Ubicaciones?on_conflict=usuario"
                ) {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(data)
                }
                if (!response.status.isSuccess()) {
                    android.util.Log.e("Supabase", "Error: ${response.status}")
                } else {
                    android.util.Log.i("Supabase", "Ubicación actualizada: ${response.status}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Excepción: ${e.message}")
            }
        }
    }
    fun fetchOtherUsersLocations(context: Context, onResult: (List<Pair<String, GeoPoint>>) -> Unit) {
        val currentUser = UserPreferences.getUserEmail(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = httpClient.get("$supabaseUrl/rest/v1/Ubicaciones") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    accept(ContentType.Application.Json)
                }

                if (response.status.isSuccess()) {
                    val json = response.bodyAsText()
                    val locations = jsonParser.decodeFromString<List<LocationSupabaseData>>(json)
                    val otherUsers = locations.filter { it.usuario != currentUser }
                        .map {
                            it.usuario to GeoPoint(it.latitud, it.longitud)
                        }

                    // Enviar al hilo principal
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(otherUsers)
                    }
                } else {
                    android.util.Log.e("Supabase", "Error al obtener ubicaciones: ${response.status}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Excepción al obtener ubicaciones: ${e.message}")
            }
        }
    }

}
