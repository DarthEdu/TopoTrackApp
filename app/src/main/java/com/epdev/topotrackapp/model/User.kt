
package com.epdev.topotrackapp.model
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    @SerialName("id") val id: String,
    @SerialName("nombre") val nombre: String? = null,
    @SerialName("rol") val rol: String? = null,
    @SerialName("creado_en") val creadoEn: String? = null,
    @SerialName("correo") val correo: String,
    @SerialName("telefono") val telefono: String? = null
)