package com.epdev.topotrackapp.utils

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SupabaseManager {
    private const val SUPABASE_URL = "https://fhqgsnjqdbyqgcoynxhr.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"
    
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    @Serializable
    data class AuthRequest(
        val email: String,
        val password: String,
        val data: UserMetadata? = null
    )

    @Serializable
    data class UserMetadata(
        val name: String,
        val phone: String
    )
    
    @Serializable
    data class AuthResponse(
        val access_token: String? = null,
        val user: User? = null,
        val error: AuthError? = null
    )
    
    @Serializable
    data class User(
        val id: String,
        val email: String
    )
    
    @Serializable
    data class AuthError(
        val message: String
    )
    
    suspend fun signUp(email: String, password: String, name: String, phone: String): Result<String> {
        return try {
            Log.d("SUPABASE", "Iniciando registro con metadata: name=$name, phone=$phone")
            
            val requestBody = AuthRequest(
                email = email,
                password = password,
                data = UserMetadata(name = name, phone = phone)
            )
            
            val response: HttpResponse = httpClient.post("$SUPABASE_URL/auth/v1/signup") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val responseBody = response.bodyAsText()
            Log.d("SUPABASE", "Respuesta del servidor: $responseBody")
            
            if (response.status.isSuccess()) {
                Log.d("SUPABASE", "Usuario registrado exitosamente con metadata")
                Result.success("Usuario registrado exitosamente")
            } else {
                Log.e("SUPABASE", "Error al registrar: ${response.status} - $responseBody")
                Result.failure(Exception("Error al registrar: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SUPABASE", "Excepción al registrar: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val response: HttpResponse = httpClient.post("$SUPABASE_URL/auth/v1/token?grant_type=password") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
            
            if (response.status.isSuccess()) {
                Result.success("Login exitoso")
            } else {
                Result.failure(Exception("Error al iniciar sesión: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
