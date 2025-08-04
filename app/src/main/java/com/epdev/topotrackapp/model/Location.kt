package com.epdev.topotrackapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitud: Double,
    val longitud: Double,
    val usuario: String? = null,
    val fecha: String? = null
)