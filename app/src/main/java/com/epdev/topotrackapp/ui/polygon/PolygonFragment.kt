package com.epdev.topotrackapp.ui.polygon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.epdev.topotrackapp.databinding.FragmentPolygonBinding
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class PolygonFragment : Fragment() {

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private val polygonViewModel: PolygonViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)

        val context = requireContext().applicationContext
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

        map = binding.mapPolygon
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.0)

        binding.progressBar.visibility = View.VISIBLE

        polygonViewModel.fetchUbicaciones()

        polygonViewModel.points.observe(viewLifecycleOwner) { puntos ->
            binding.progressBar.visibility = View.GONE
            drawPolygon(puntos)
        }

        polygonViewModel.area.observe(viewLifecycleOwner) { area ->
            binding.textPolygon.text = "📏 Área: %.2f m²".format(area)
        }

        binding.btnActualizarArea.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            polygonViewModel.fetchUbicaciones()
        }

        return binding.root
    }

    private fun drawPolygon(points: List<GeoPoint>) {
        map.overlays.clear()

        when (points.size) {
            0 -> {
                showSnackbar("⚠️ No hay puntos disponibles aún. Se necesitan al menos 3.")
            }

            1 -> {
                val marker = Marker(map).apply {
                    position = points[0]
                    title = "🔹 Punto 1"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                map.controller.setCenter(points[0])
                showSnackbar("✳️ Se necesita al menos 3 puntos para formar un polígono.")
            }

            2 -> {
                val line = Polyline().apply {
                    setPoints(points)
                    color = 0xFF00BCD4.toInt() // Cyan
                    width = 6f
                }
                map.overlays.add(line)
                map.controller.setCenter(points[0])
                showSnackbar("🔷 Se necesita 1 punto más para formar un polígono.")
            }

            else -> {
                val polygon = Polygon().apply {
                    this.points = points + points.first() // Cerrar polígono
                    strokeWidth = 5f
                    strokeColor = 0xFF1B5E20.toInt() // Verde oscuro
                    fillColor = 0x5532CD32.toInt()   // Verde claro translúcido
                    title = "Área del terreno"
                }
                map.overlays.add(polygon)
                map.controller.setCenter(points[0])
            }
        }

        map.invalidate()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(0xFFFAFAFA.toInt())
            .setTextColor(0xFF212121.toInt())
            .setAction("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
