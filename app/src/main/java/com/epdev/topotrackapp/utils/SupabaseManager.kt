package com.epdev.topotrackapp.utils

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

    val supabase: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://fhqgsnjqdbyqgcoynxhr.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMDE4MTAsImV4cCI6MjA2ODg3NzgxMH0.mmcH4ThHqElEKxbmI_bh2e7brVtMUDr73t97myNeyPM"
        ) {
            install(io.github.jan.supabase.gotrue.GoTrue)
            install(io.github.jan.supabase.postgrest.Postgrest)
        }
    }
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
        val password: String
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
    
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val response: HttpResponse = httpClient.post("$SUPABASE_URL/auth/v1/signup") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
            
            if (response.status.isSuccess()) {
                Result.success("Usuario registrado exitosamente")
            } else {
                Result.failure(Exception("Error al registrar: ${response.status}"))
            }
        } catch (e: Exception) {
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
