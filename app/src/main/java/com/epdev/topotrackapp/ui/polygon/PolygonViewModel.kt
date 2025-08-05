package com.epdev.topotrackapp.ui.polygon

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PolygonViewModel : ViewModel() {

    @Serializable
    data class Poligono(
        val id: String,
        val terreno: String,
        val coordenadas: List<Coordenada>,
        val fecha_creacion: String,
        val area: Double
    )

    @Serializable
    data class Coordenada(val lat: Double, val lon: Double)

    private val _poligonos = MutableLiveData<List<Poligono>>()
    val poligonos: LiveData<List<Poligono>> = _poligonos

    private val _selectedPoligono = MutableLiveData<Poligono?>()
    val selectedPoligono: LiveData<Poligono?> = _selectedPoligono

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _poligonoEliminado = MutableLiveData<Boolean>()
    val poligonoEliminado: LiveData<Boolean> = _poligonoEliminado

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val polygonCache = mutableMapOf<String, Poligono>()

    // Configuración de Supabase
    private val supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    fun fetchPoligonos() {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = httpClient.get("$supabaseUrl/rest/v1/poligonos") {
                    header("apikey", supabaseKey)
                    header(HttpHeaders.Authorization, "Bearer $supabaseKey")
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    parameter("select", "id,terreno,coordenadas,fecha_creacion,area")
                    parameter("order", "fecha_creacion.desc")
                }

                val poligonosFromApi: List<PoligonoSupabase> = response.body()
                val poligonosFormateados = poligonosFromApi.map { convertToPoligono(it) }

                _poligonos.postValue(poligonosFormateados)
                poligonosFormateados.firstOrNull()?.let { selectPoligono(it) }

            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al obtener polígonos", e)
                _errorMessage.postValue("Error al cargar polígonos: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun selectPoligono(poligono: Poligono) {
        polygonCache[poligono.id] = poligono
        _selectedPoligono.postValue(poligono)
    }

    fun getCachedPolygon(id: String): Poligono? = polygonCache[id]

    fun deletePoligono(id: String) {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = httpClient.delete("$supabaseUrl/rest/v1/poligonos") {
                    header("apikey", supabaseKey)
                    header(HttpHeaders.Authorization, "Bearer $supabaseKey")
                    contentType(ContentType.Application.Json)
                    parameter("id", "eq.$id")
                }

                if (response.status.isSuccess()) {
                    polygonCache.remove(id)
                    _poligonoEliminado.postValue(true)
                    fetchPoligonos() // Actualizar la lista después de eliminar
                } else {
                    _errorMessage.postValue("Error al eliminar: ${response.status}")
                    _poligonoEliminado.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al eliminar polígono", e)
                _errorMessage.postValue("Error al eliminar: ${e.localizedMessage}")
                _poligonoEliminado.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun convertToPoligono(poligono: PoligonoSupabase): Poligono {
        val coordenadas = poligono.coordenadas.map { Coordenada(it.lat, it.lon) }
        val fechaFormateada = formatFecha(poligono.fecha_creacion)

        return Poligono(
            id = poligono.id,
            terreno = poligono.terreno ?: "Sin nombre",
            coordenadas = coordenadas,
            fecha_creacion = fechaFormateada,
            area = poligono.area ?: 0.0
        )
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

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    @Serializable
    private data class PoligonoSupabase(
        val id: String,
        val terreno: String? = null,
        val coordenadas: List<CoordenadaSupabase>,
        val fecha_creacion: String,
        val area: Double?
    )

    @Serializable
    private data class CoordenadaSupabase(
        val lat: Double,
        val lon: Double
    )
}
