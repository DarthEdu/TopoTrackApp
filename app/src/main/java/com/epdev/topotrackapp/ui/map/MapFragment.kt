package com.epdev.topotrackapp.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.epdev.topotrackapp.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var marker: Marker
    private var contador : Int = 1
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



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle? 
    ): View? {
            _binding = FragmentMapBinding.inflate(inflater, container, false)

        val context = requireContext().applicationContext
        val prefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)

        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        map.controller.setZoom(15.0)
        val startPoint = GeoPoint(-0.1807, -78.4678)
        map.controller.setCenter(startPoint)

        marker = Marker(map).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Estoy aquí"
        }

        map.overlays.add(marker)

        mapViewModel.location.observe(viewLifecycleOwner) { location ->
            marker.position = location
            contador--
            if (contador == 0){
                map.controller.animateTo(location)
            }
            mapViewModel.saveLocationToSupabase(requireContext(), location.latitude, location.longitude)
            map.invalidate()
        }

        mapViewModel.requestLocationUpdates(requireContext())
        checkLocationPermissionAndStartUpdates()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapViewModel.stopLocationUpdates()
        _binding = null
    }
}
