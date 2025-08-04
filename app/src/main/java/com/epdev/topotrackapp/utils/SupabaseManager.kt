
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
import com.epdev.topotrackapp.model.User
import com.epdev.topotrackapp.model.Location
import kotlinx.serialization.decodeFromString

object SupabaseManager {

    // Elimina un usuario de Auth (requiere Service Key, solo para uso administrativo)
    suspend fun deleteUserFromAuthByEmail(email: String): Result<Unit> {
        try {
            val serviceKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZocWdzbmpxZGJ5cWdjb3lueGhyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1MzMwMTgxMCwiZXhwIjoyMDY4ODc3ODEwfQ.Uvu9B-bSpytm7L848qpUjhhaRLaK640nIp8_4yUm5DQ" // ¡NO USAR EN PRODUCCIÓN!
            // Buscar usuario por correo en Auth
            val responseList: HttpResponse = httpClient.get("$SUPABASE_URL/auth/v1/admin/users") {
                header("apikey", serviceKey)
                header("Authorization", "Bearer $serviceKey")
                parameter("email", email)
            }
            val responseBodyList = responseList.bodyAsText()
            Log.d("SUPABASE", "Respuesta búsqueda usuario Auth: ${responseList.status} - $responseBodyList")
            if (!responseList.status.isSuccess()) {
                Log.e("SUPABASE", "Error buscando usuario en Auth: ${responseList.status} - $responseBodyList")
                return Result.failure(Exception("No se pudo buscar el usuario en Auth: ${responseList.status} - $responseBodyList"))
            }
            // El endpoint retorna un objeto con la clave "users" que es una lista
            val usersWrapper = Json { ignoreUnknownKeys = true }.decodeFromString<AuthUserList>(responseBodyList)
            val user = usersWrapper.users.firstOrNull()
            if (user == null) {
                Log.e("SUPABASE", "No se encontró usuario en Auth con ese correo: $email")
                return Result.failure(Exception("No se encontró usuario en Auth con ese correo"))
            }
            val userId = user.id
            Log.d("SUPABASE", "Intentando eliminar usuario de Auth con userId: $userId para email: $email")
            val response: HttpResponse = httpClient.delete("$SUPABASE_URL/auth/v1/admin/users/$userId") {
                header("apikey", serviceKey)
                header("Authorization", "Bearer $serviceKey")
            }
            val responseBody = response.bodyAsText()
            Log.d("SUPABASE", "Respuesta AUTH: ${response.status} - $responseBody")
            return if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Log.e("SUPABASE", "Error al eliminar usuario de Auth: ${response.status} - $responseBody")
                Result.failure(Exception("Error al eliminar usuario de Auth: ${response.status} - $responseBody"))
            }
        } catch (e: Exception) {
            Log.e("SUPABASE", "Excepción al eliminar usuario de Auth: ${e.message}", e)
            return Result.failure(e)
        }
    }

    @Serializable
    data class AuthUserList(
        val users: List<AuthUser>
    )

    @Serializable
    data class AuthUser(
        val id: String,
        val email: String? = null
    )
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

    private fun parseUsers(json: String): List<User> {
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLocations(json: String): List<Location> {
        return try {
            val result = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<Location>>(json)
            Log.d("MAP_DEBUG", "Parseo correcto: $result")
            result
        } catch (e: Exception) {
            Log.e("MAP_DEBUG", "Error parseando ubicaciones: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/usuarios") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
            }
            val responseBody = response.bodyAsText()
            Log.d("SUPABASE", "Respuesta usuarios: $responseBody")
            if (response.status.isSuccess()) {
                val users: List<User> = parseUsers(responseBody)
                Log.d("SUPABASE", "Usuarios parseados: ${users.size}")
                if (users.isEmpty()) {
                    Log.w("SUPABASE", "La lista de usuarios está vacía")
                }
                Result.success(users)
            } else {
                Log.e("SUPABASE", "Error al obtener usuarios: ${response.status} - $responseBody")
                Result.failure(Exception("Error al obtener usuarios: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SUPABASE", "Excepción al obtener usuarios: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteUserByEmail(email: String): Result<Unit> {
        return try {
            val response = httpClient.delete("$SUPABASE_URL/rest/v1/usuarios") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
                parameter("correo", "eq.$email")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserLocations(email: String): Result<List<Location>> {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/Ubicaciones") {
                header("apikey", SUPABASE_KEY)
                header("Authorization", "Bearer $SUPABASE_KEY")
                parameter("usuario", "eq.$email")
            }
            if (response.status.isSuccess()) {
                val locations = parseLocations(response.bodyAsText())
                Result.success(locations)
            } else {
                Result.failure(Exception("Error al obtener ubicaciones"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
