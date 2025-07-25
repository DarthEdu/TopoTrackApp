package com.epdev.topotrackapp.ui.polygon

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.epdev.topotrackapp.databinding.FragmentPolygonBinding
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon
import android.widget.Toast
import android.content.Context

class PolygonFragment : Fragment() {

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: MapView
    private lateinit var viewModel: PolygonViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[PolygonViewModel::class.java]

        map = binding.mapPolygon
        Configuration.getInstance().load(context, context?.getSharedPreferences("prefs", Context.MODE_PRIVATE))
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        viewModel.points.observe(viewLifecycleOwner) { points ->
            drawPolygon(points)
        }

        viewModel.area.observe(viewLifecycleOwner) { area ->
            binding.textPolygon.text = "Área calculada: ${"%.2f".format(area)} m²"
        }

        return binding.root
    }

    private fun drawPolygon(points: List<GeoPoint>) {
        map.overlays.clear()
        val polygon = Polygon()
        polygon.points = points
        polygon.fillColor = 0x5500ff00
        polygon.strokeColor = 0xFF000000.toInt()
        polygon.strokeWidth = 4.0f
        map.overlays.add(polygon)
        map.invalidate()

        if (points.isNotEmpty()) {
            map.controller.setZoom(18.0)
            map.controller.setCenter(points[0])
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
