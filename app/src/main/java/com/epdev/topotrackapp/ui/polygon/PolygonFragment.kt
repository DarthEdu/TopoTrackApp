package com.epdev.topotrackapp.ui.polygon

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.epdev.topotrackapp.databinding.FragmentPolygonBinding
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class PolygonFragment : Fragment() {

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PolygonViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mapPolygon.setMultiTouchControls(true)

        viewModel.points.observe(viewLifecycleOwner) { points ->
            binding.mapPolygon.overlays.clear()

            if (points.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No hay puntos disponibles", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return@observe
            }

            drawMarkers(points)

            if (points.size >= 3) {
                drawPolygon(points)
                val area = viewModel.calcularAreaMetrosCuadrados(points)
                binding.textPolygon.text = "📏 Área: %.2f m²".format(area)
            } else {
                drawDecorativeShape(points)
                Toast.makeText(requireContext(), "Se necesitan al menos 3 puntos para formar un polígono", Toast.LENGTH_LONG).show()
                binding.textPolygon.text = "Área: No disponible"
            }

            binding.progressBar.visibility = View.GONE
            binding.mapPolygon.invalidate()
        }

        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchUbicaciones()
    }

    private fun drawMarkers(points: List<GeoPoint>) {
        for ((index, point) in points.withIndex()) {
            val marker = Marker(binding.mapPolygon).apply {
                position = point
                title = "📍 Punto ${index + 1}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            binding.mapPolygon.overlays.add(marker)
        }
    }

    private fun drawPolygon(points: List<GeoPoint>) {
        val polygon = Polygon().apply {
            fillPaint.color = Color.argb(80, 0, 255, 0)
            outlinePaint.color = Color.GREEN
            outlinePaint.strokeWidth = 5f
            setPoints(points + points.first())
        }
        binding.mapPolygon.overlays.add(polygon)
    }

    private fun drawDecorativeShape(points: List<GeoPoint>) {
        val polyline = Polyline().apply {
            setPoints(points)
            color = Color.YELLOW
            width = 6f
            isGeodesic = true
        }
        binding.mapPolygon.overlays.add(polyline)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
