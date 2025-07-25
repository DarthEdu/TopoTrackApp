package com.epdev.topotrackapp.ui.polygon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class PolygonViewModel : ViewModel() {

    // LiveData to hold the text for the Polygon fragment
    private val _text = MutableLiveData<String>().apply {
        value = "This is polygon Fragment"
    }
    val text: LiveData<String> = _text

    // Additional properties and methods can be added here as needed
}