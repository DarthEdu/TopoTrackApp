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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PolygonViewModel : ViewModel() {

    @Serializable
    data class Poligono(
        val id: String,
        val coordenadas: List<Coordenada>,
        val fecha_creacion: String,
        val area: Double,
        val autor: String
    )

    @Serializable
    data class Coordenada(val lat: Double, val lon: Double)

    private val _poligonos = MutableLiveData<List<Poligono>>()
    val poligonos: LiveData<List<Poligono>> = _poligonos

    private val _selectedPoligono = MutableLiveData<Poligono?>()
    val selectedPoligono: LiveData<Poligono?> = _selectedPoligono

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun fetchPoligonos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: HttpResponse = httpClient.get("$supabaseUrl/rest/v1/poligonos") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    accept(ContentType.Application.Json)
                }

                val rawJson = response.bodyAsText()
                val poligonosFromApi: List<PoligonoSupabase> = Json.decodeFromString(rawJson)

                val poligonosFormateados = poligonosFromApi.map { poligono ->
                    val coordenadas = poligono.coordenadas.map { Coordenada(it.lat, it.lon) }
                    val fechaFormateada = formatFecha(poligono.fecha_creacion)

                    Poligono(
                        id = poligono.id,
                        coordenadas = coordenadas,
                        fecha_creacion = fechaFormateada,
                        area = poligono.area ?: 0.0,
                        autor = poligono.autor ?: "Anónimo"
                    )
                }

                _poligonos.postValue(poligonosFormateados)

            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al obtener polígonos: ${e.message}")
                _errorMessage.postValue("Error al cargar polígonos: ${e.message}")
            }
        }
    }

    fun selectPoligono(poligono: Poligono) {
        _selectedPoligono.value = poligono
    }

    private fun formatFecha(fechaOriginal: String): String {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val fecha = LocalDateTime.parse(fechaOriginal, formatter)
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(fecha)
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    // Modelo temporal para mapear la respuesta de Supabase
    @Serializable
    private data class PoligonoSupabase(
        val id: String,
        val coordenadas: List<CoordenadaSupabase>,
        val fecha_creacion: String,
        val area: Double?,
        val autor: String?
    )

    @Serializable
    private data class CoordenadaSupabase(
        val lat: Double,
        val lon: Double
    )
}