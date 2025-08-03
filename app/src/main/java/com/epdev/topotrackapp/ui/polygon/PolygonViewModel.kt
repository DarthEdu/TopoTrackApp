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
                val response: HttpResponse = httpClient.get("$supabaseUrl/rest/v1/poligonos?order=fecha_creacion.desc") {
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
                        terreno = poligono.terreno ?: "Sin nombre",
                        coordenadas = coordenadas,
                        fecha_creacion = fechaFormateada,
                        area = poligono.area ?: 0.0
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

    private val _poligonoEliminado = MutableLiveData<Boolean>()
    val poligonoEliminado: LiveData<Boolean> get() = _poligonoEliminado

    fun deletePoligono(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = httpClient.delete("$supabaseUrl/rest/v1/poligonos") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    contentType(ContentType.Application.Json)
                    parameter("id", "eq.$id")
                }

                if (response.status.isSuccess()) {
                    _poligonoEliminado.postValue(true)
                } else {
                    _errorMessage.postValue("Error: ${response.status}")
                    _poligonoEliminado.postValue(false)
                }

            } catch (e: Exception) {
                Log.e("PolygonViewModel", "Error al eliminar: ${e.message}")
                _errorMessage.postValue("Error al eliminar polígono: ${e.message}")
                _poligonoEliminado.postValue(false)
            }
        }
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
