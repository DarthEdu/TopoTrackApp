package com.epdev.topotrackapp.ui.polygon

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.*

class PolygonViewModel : ViewModel() {

    private val _points = MutableLiveData<List<GeoPoint>>()
    val points: LiveData<List<GeoPoint>> = _points

    private val _area = MutableLiveData<Double>()
    val area: LiveData<Double> = _area

    init {
        fetchPointsFromSupabase()
    }

    private fun fetchPointsFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Simulación de llamada a Supabase
                val fetchedPoints = listOf(
                    GeoPoint(-0.1807, -78.4678),
                    GeoPoint(-0.1812, -78.4685),
                    GeoPoint(-0.1802, -78.4690)
                )

                _points.postValue(fetchedPoints)
                _area.postValue(calculateArea(fetchedPoints))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        val R = 6371000.0 // radio de la Tierra en metros
        val radians = { deg: Double -> deg * Math.PI / 180 }

        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            val lat1 = radians(p1.latitude)
            val lon1 = radians(p1.longitude)
            val lat2 = radians(p2.latitude)
            val lon2 = radians(p2.longitude)

            sum += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
        }

        return abs(sum * R * R / 2.0) // área en m²
    }
}
