package com.epdev.topotrackapp.ui.polygon

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.osmdroid.util.GeoPoint

class PolygonViewModel : ViewModel() {

    private val _points = MutableLiveData<List<GeoPoint>>()
    val points: LiveData<List<GeoPoint>> = _points

    private val _area = MutableLiveData<Double>()
    val area: LiveData<Double> = _area

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    @Serializable
    data class Ubicacion(val latitud: Double, val longitud: Double, val usuario: String)

    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun fetchUbicaciones() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: HttpResponse = httpClient.get("$supabaseUrl/rest/v1/Ubicaciones") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    accept(ContentType.Application.Json)
                }

                val rawJson = response.bodyAsText()
                val ubicaciones: List<Ubicacion> = Json.decodeFromString(rawJson)

                if (ubicaciones.isNotEmpty()) {
                    val puntos = ubicaciones.map { GeoPoint(it.latitud, it.longitud) }

                    _points.postValue(puntos)
                    _area.postValue(if (puntos.size >= 3) calcularAreaMetrosCuadrados(puntos) else 0.0)
                } else {
                    _errorMessage.postValue("⚠️ No se encontraron ubicaciones.")
                }

            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al obtener puntos: ${e.message}")
                _errorMessage.postValue("🚫 Error de red o Supabase: ${e.message}")
            }
        }
    }

    // Hacemos esta función pública para que se pueda usar desde el Fragment
    fun calcularAreaMetrosCuadrados(points: List<GeoPoint>): Double {
        var area = 0.0
        val radius = 6371000.0

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            area += Math.toRadians(p2.longitude - p1.longitude) *
                    (2 + Math.sin(Math.toRadians(p1.latitude)) + Math.sin(Math.toRadians(p2.latitude)))
        }

        area = area * radius * radius / 2.0
        return kotlin.math.abs(area)
    }
}
