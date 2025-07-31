package com.epdev.topotrackapp.ui.map

import android.Manifest
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
import com.epdev.topotrackapp.databinding.FragmentMapBinding
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val userMarkers = mutableMapOf<String, Marker>()
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var mostrado = 0

    private lateinit var marker: Marker
    private val mapViewModel: MapViewModel by viewModels()
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            mapViewModel.requestLocationUpdates(requireContext())
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }
    private fun checkLocationPermissionAndStartUpdates() {
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

        map.overlays.removeAll(userMarkers.values)
        userMarkers.clear()
        for ((usuario, geoPoint) in usuarios) {
            if (userMarkers.containsKey(usuario)) {
                userMarkers[usuario]?.position = geoPoint
            } else {
                val marker = Marker(map).apply {
                    position = geoPoint
                    title = usuario
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                userMarkers[usuario] = marker
            }
        }
        map.invalidate()
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            // Validación segura antes de ejecutar
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
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        binding.map.onPause()
        handler.removeCallbacks(updateRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        // handler.removeCallbacks(updateRunnable) // Comentado según tu prueba
        mapViewModel.stopLocationUpdates()

        if (_binding != null) {
            val map = binding.map // Obtener la referencia antes de que _binding sea null
            map.onPause() // Es bueno llamar onPause aquí también si no se hizo en onPause del Fragment
            map.overlays.clear() // Limpiar overlays
            val parent = map.parent as? ViewGroup
            parent?.removeView(map)
            map.onDetach()
        }

        userMarkers.clear()
        _binding = null
        super.onDestroyView()
    }
}
