package com.epdev.topotrackapp.ui.polygon

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.osmdroid.util.GeoPoint

class PolygonViewModel : ViewModel() {

    private val _points = MutableLiveData<List<GeoPoint>>()
    val points: LiveData<List<GeoPoint>> = _points

    private val _area = MutableLiveData<Double>()
    val area: LiveData<Double> = _area

    @Serializable
    data class Ubicacion(val latitud: Double, val longitud: Double, val usuario: String)

    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "TU_SUPABASE_KEY" // remplaza por seguridad

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }

    fun fetchUbicaciones() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ubicaciones: List<Ubicacion> = httpClient.get("$supabaseUrl/rest/v1/Ubicaciones") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    accept(ContentType.Application.Json)
                }.body()

                val puntos = ubicaciones.map { GeoPoint(it.latitud, it.longitud) }

                if (puntos.size >= 3) {
                    _points.postValue(puntos)
                    _area.postValue(calcularAreaMetrosCuadrados(puntos))
                } else {
                    _points.postValue(puntos)
                    _area.postValue(0.0)
                }

            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al obtener puntos: ${e.message}")
            }
        }
    }

    private fun calcularAreaMetrosCuadrados(points: List<GeoPoint>): Double {
        var area = 0.0
        val radius = 6371000.0 // radio terrestre

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
