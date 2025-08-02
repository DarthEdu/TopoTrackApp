package com.epdev.topotrackapp.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.epdev.topotrackapp.Foregrounds.LocationForegroundService
import com.epdev.topotrackapp.databinding.FragmentMapBinding
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val userMarkers = mutableMapOf<String, Marker>()
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var mostrado = 0
    private var userPolygon: Polygon? = null // Variable para el polígono

    private lateinit var marker: Marker
    private val mapViewModel: MapViewModel by viewModels()
    private var requestSuccessFull = false
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            mapViewModel.requestLocationUpdates(requireContext())
            requestSuccessFull = true
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLocationPermissionAndStartUpdates() {
        if(requestSuccessFull){return}
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                mapViewModel.requestLocationUpdates(requireContext())
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    requireContext(),
                    "Se necesita permiso de ubicación para mostrar tu ubicación.",
                    Toast.LENGTH_LONG
                ).show()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun actualizarMarcadoresUsuarios(usuarios: List<Pair<String, GeoPoint>>) {
        val map = binding.map
        val currentLocation = marker.position // Tu ubicación actual

        // Limpiar overlays
        map.overlays.removeAll(userMarkers.values)
        userPolygon?.let { map.overlays.remove(it) }
        userMarkers.clear()

        // Agregar marcador de tu ubicación (si no está en la lista)
        val allUsers = usuarios.toMutableList().apply {
            if (none { it.first == mapViewModel.nombreUsuarioActual(requireContext()) }) {
                add(mapViewModel.nombreUsuarioActual(requireContext()) to currentLocation)
            }
        }

        // Dibujar todos los marcadores (incluyendo el tuyo)
        allUsers.forEach { (usuario, geoPoint) ->
            val marker = Marker(map).apply {
                position = geoPoint
                title = usuario
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
            userMarkers[usuario] = marker
        }

        // Dibujar polígono si hay 3 o más puntos (incluyendo tu ubicación)
        if (allUsers.size >= 3) {
            userPolygon = Polygon().apply {
                points = allUsers.map { it.second }
                fillColor = 0x2200FF00  // Verde semitransparente
                strokeColor = 0xFF00FF00.toInt() // Borde verde
                setStrokeWidth(3.0f)
            }
            map.overlays.add(userPolygon)
        }

        map.invalidate()
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || _binding == null) return

            try {
                mapViewModel.fetchOtherUsersLocations(requireContext()) { usuarios ->
                    actualizarMarcadoresUsuarios(usuarios)
                }
                handler.postDelayed(this, 10000)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        val startPoint = GeoPoint(-0.1807, -78.4678)
        map.controller.setCenter(startPoint)

        marker = Marker(map).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = mapViewModel.nombreUsuarioActual(requireContext())
        }

        map.overlays.add(marker)

        mapViewModel.location.observe(viewLifecycleOwner) { location ->
            marker.position = location
            if (mostrado == 0){
                map.controller.animateTo(location)
                mostrado++
            }
            mapViewModel.saveLocationToSupabase(requireContext(), location.latitude, location.longitude)
            map.invalidate()
        }
        mapViewModel.requestLocationUpdates(requireContext())
        checkLocationPermissionAndStartUpdates()
        binding.btnGuardarPoligono.setOnClickListener {
            userPolygon?.actualPoints?.let { puntos ->
                if (puntos.size >= 3) {
                    val area = calcularAreaPoligono(puntos)
                    mapViewModel.guardarPoligonoEnSupabase(puntos, area)
                    Toast.makeText(requireContext(), "Polígono guardado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Se necesitan al menos 3 puntos", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(requireContext(), "No hay polígono para guardar", Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    private fun calcularAreaPoligono(puntos: List<GeoPoint>): Double {
        val radius = 6371000.0 // Radio de la Tierra en metros
        var area = 0.0
        for (i in puntos.indices) {
            val p1 = puntos[i]
            val p2 = puntos[(i + 1) % puntos.size]
            area += Math.toRadians(p2.longitude - p1.longitude) *
                    (2 + Math.sin(Math.toRadians(p1.latitude)) + Math.sin(Math.toRadians(p2.latitude)))
        }
        return Math.abs(area * radius * radius / 2.0)
    }


    override fun onResume() {
        super.onResume()
        requireContext().stopService(Intent(requireContext(), LocationForegroundService::class.java))
        binding.map.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        binding.map.onPause()
        handler.removeCallbacks(updateRunnable)
        super.onPause()
        val serviceIntent = Intent(requireContext(), LocationForegroundService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    override fun onDestroyView() {
        userPolygon?.let { binding.map.overlays.remove(it) }
        userPolygon = null
        mapViewModel.stopLocationUpdates()
        if (_binding != null) {
            val map = binding.map
            map.onPause()
            map.overlays.clear()
            val parent = map.parent as? ViewGroup
            parent?.removeView(map)
            map.onDetach()
        }
        userMarkers.clear()
        _binding = null
        super.onDestroyView()
    }
}
