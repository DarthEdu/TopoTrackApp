package com.epdev.topotrackapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _welcome = MutableLiveData<String>().apply {
        value = "¡Bienvenido a TopoTrackApp!"
    }
    val welcome: LiveData<String> = _welcome


    private val _description = MutableLiveData<String>().apply {
        value = "TopoTrackApp es una aplicación diseñada para ayudarte a calcular el área de cualquier terreno de manera sencilla en conjunto con tu equipo de trabajo. Explora el mapa, visualiza las ubicaciones de tus compañeros topógrafos en tiempo real y lleva un control detallado de los terrenos registrados."
    }
    val description: LiveData<String> = _description

    private val _tituloIn = MutableLiveData<String>().apply {
        value = "Equipo de desarrollo"
    }
    val tituloIn: LiveData<String> = _tituloIn

    private val _integrantes = MutableLiveData<String>().apply {
        value = "Martin Ayala \n" +"Mateo Bernal \n" + "Dennis Díaz \n" + "Eduardo Porras \n" + "Mateo Tacuri"
    }
    val integrantes: LiveData<String> = _integrantes

}
