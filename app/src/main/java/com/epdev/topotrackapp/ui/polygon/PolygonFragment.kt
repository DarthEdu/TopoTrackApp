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
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

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

        // Configuración del mapa
        map = binding.mapPolygon
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.0)

        // Mostrar animación de carga
        binding.progressBar.visibility = View.VISIBLE

        // Llamar a Supabase para obtener ubicaciones
        polygonViewModel.fetchUbicaciones()

        // Observador de puntos
        polygonViewModel.points.observe(viewLifecycleOwner) { puntos ->
            binding.progressBar.visibility = View.GONE
            drawPolygon(puntos)
        }

        // Observador del área
        polygonViewModel.area.observe(viewLifecycleOwner) { area ->
            binding.textPolygon.text = "Área: %.2f m²".format(area)
        }

        // Botón de actualización manual
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
                Snackbar.make(
                    binding.root,
                    "⛔ No hay ubicaciones disponibles aún.",
                    Snackbar.LENGTH_LONG
                ).setBackgroundTint(0xFFEEEEEE.toInt())
                    .setTextColor(0xFF000000.toInt())
                    .setAction("OK") {}
                    .show()
                map.invalidate()
            }

            1 -> {
                val marker = Marker(map).apply {
                    position = points[0]
                    title = "Esperando más topógrafos..."
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                showDecorativeSnackbar()
                map.controller.setCenter(points[0])
                map.invalidate()
            }

            2 -> {
                val polyline = Polyline().apply {
                    setPoints(points)
                    color = 0xFF888888.toInt()
                    width = 6f
                }
                map.overlays.add(polyline)
                showDecorativeSnackbar()
                map.controller.setCenter(points[0])
                map.invalidate()
            }

            else -> {
                val polygon = Polygon().apply {
                    this.points = points + points.first()
                    strokeWidth = 4f
                    strokeColor = 0xFF000000.toInt()
                    fillColor = 0x5500FF00.toInt()
                }
                map.overlays.add(polygon)
                map.controller.setCenter(points[0])
                map.invalidate()
            }
        }
    }

    private fun showDecorativeSnackbar() {
        Snackbar.make(
            binding.root,
            "🧭 Esperando más topógrafos activos para calcular el área...",
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(0xFFEEEEEE.toInt())
            .setTextColor(0xFF000000.toInt())
            .setAction("OK") {}
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


