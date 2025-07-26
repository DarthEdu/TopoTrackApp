package com.epdev.topotrackapp.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epdev.topotrackapp.utils.SupabaseManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint



class MapViewModel : ViewModel() {

    private val _location = MutableLiveData<GeoPoint>()
    val location: LiveData<GeoPoint> = _location

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    fun requestLocationUpdates(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(25000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(15000L)
            .setMaxUpdateDelayMillis(30000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    if (_location.value != geoPoint) { // evita duplicados iguales
                        _location.postValue(geoPoint)
                        viewModelScope.launch {
                            saveLocationToSupabase(location.latitude, location.longitude)
                        }
                    }
                    break // Salte del loop si solo quieres guardar una por evento
                }
            }
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @Serializable
    data class LocationSupabaseData(val latitud: Double, val longitud: Double)

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }
    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"
    suspend fun saveLocationToSupabase(lat: Double, lon: Double) {
        try {
            val data = LocationSupabaseData(latitud = lat, longitud = lon)
            SupabaseManager.supabase.postgrest["Ubicaciones"].insert(data, upsert = true)
            android.util.Log.d("Supabase", "Ubicación guardada en Supabase")
        } catch (e: Exception) {
            android.util.Log.e("Supabase", "Excepción: ${e.message}")
        }
    }

    fun enviarUbicacion(lat: Double, lon: Double) {
        viewModelScope.launch {
            saveLocationToSupabase(lat, lon)
        }
    }

}
