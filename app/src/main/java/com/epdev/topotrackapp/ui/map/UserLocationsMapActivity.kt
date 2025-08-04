package com.epdev.topotrackapp.ui.map

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epdev.topotrackapp.databinding.ActivityUserLocationsMapBinding
import com.epdev.topotrackapp.model.Location
import com.epdev.topotrackapp.utils.SupabaseManager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class UserLocationsMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserLocationsMapBinding
    private val markers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserLocationsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar como header con botón de retroceso
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ubicación Usuario"
        binding.toolbar.setNavigationOnClickListener { finish() }

        val email = intent.getStringExtra(EXTRA_EMAIL)
        if (email == null) {
            Toast.makeText(this, "No se recibió el correo del usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        // Quito por defecto
        val startPoint = GeoPoint(-0.1807, -78.4678)
        map.controller.setCenter(startPoint)

        CoroutineScope(Dispatchers.IO).launch {
            val result = SupabaseManager.getUserLocations(email)
            runOnUiThread {
                result.onSuccess { locations ->
                    if (locations.isEmpty()) {
                        Toast.makeText(this@UserLocationsMapActivity, "No hay ubicaciones para este usuario", Toast.LENGTH_SHORT).show()
                    } else {
                        mostrarUbicacionesEnMapa(locations)
                    }
                }.onFailure {
                    Toast.makeText(this@UserLocationsMapActivity, "Error al obtener ubicaciones", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarUbicacionesEnMapa(locations: List<Location>) {
        Log.d("MAP_DEBUG", "Ubicaciones recibidas: ${locations.size} -> $locations")
        val map = binding.map
        map.overlays.clear()
        markers.clear()
        if (locations.isEmpty()) {
            Toast.makeText(this, "No hay ubicaciones para este usuario", Toast.LENGTH_SHORT).show()
            return
        }
        // Marcar todas las ubicaciones y destacar la última
        locations.forEachIndexed { index, loc ->
            Log.d("MAP_DEBUG", "Marcador: lat=${loc.latitud}, lon=${loc.longitud}, fecha=${loc.fecha}")
            val geoPoint = GeoPoint(loc.latitud, loc.longitud)
            val marker = Marker(map).apply {
                position = geoPoint
                title = loc.fecha ?: "Ubicación"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (index == locations.lastIndex) {
                    // Puedes personalizar el icono aquí si tienes uno, por ejemplo:
                    // setIcon(ContextCompat.getDrawable(this@UserLocationsMapActivity, R.drawable.ic_marker_last))
                    title = "Última ubicación\n${loc.fecha ?: ""}"
                }
            }
            map.overlays.add(marker)
            markers.add(marker)
        }
        // Centrar en la última ubicación
        val last = locations.last()
        map.controller.setCenter(GeoPoint(last.latitud, last.longitud))
        map.invalidate()
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"
        fun start(context: Context, email: String) {
            val intent = Intent(context, UserLocationsMapActivity::class.java)
            intent.putExtra(EXTRA_EMAIL, email)
            context.startActivity(intent)
        }
    }
}
