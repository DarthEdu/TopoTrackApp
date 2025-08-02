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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class PolygonFragment : Fragment() {

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PolygonViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupUI()
        setupObservers()
        viewModel.fetchPoligonos()
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        binding.mapPolygon.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapPolygon.setMultiTouchControls(true)
    }

    private fun setupUI() {
        binding.btnRefresh.setOnClickListener {
            viewModel.fetchPoligonos()
            binding.progressBar.visibility = View.VISIBLE
        }

        binding.btnSelectPolygon.setOnClickListener {
            showPolygonSelectionDialog()
        }
    }

    private fun setupObservers() {
        viewModel.poligonos.observe(viewLifecycleOwner) { poligonos ->
            binding.progressBar.visibility = View.GONE

            if (poligonos.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "No se encontraron polígonos",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }
            viewModel.selectPoligono(poligonos.first())
        }

        viewModel.selectedPoligono.observe(viewLifecycleOwner) { poligono ->
            poligono?.let { displayPolygon(it) }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayPolygon(poligono: PolygonViewModel.Poligono) {
        binding.mapPolygon.overlays.clear()
        val geoPoints = poligono.coordenadas.map { GeoPoint(it.lat, it.lon) }

        updatePolygonInfo(poligono)
        drawMarkers(geoPoints)

        if (geoPoints.size >= 3) {
            drawPolygon(geoPoints)
            zoomToPolygon(geoPoints)
        } else {
            drawDecorativeShape(geoPoints)
            Toast.makeText(
                requireContext(),
                "Polígono incompleto (solo ${geoPoints.size} puntos)",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.mapPolygon.invalidate()
    }

    private fun updatePolygonInfo(poligono: PolygonViewModel.Poligono) {
        binding.tvPolygonId.text = "ID: ${poligono.id.take(8)}..."
        binding.tvPolygonArea.text = "Área: ${"%.2f".format(poligono.area)} m²"
        binding.tvPolygonDate.text = "Fecha: ${poligono.fecha_creacion}"
        binding.tvPolygonAuthor.text = "Autor: ${poligono.autor}"
    }

    private fun drawMarkers(points: List<GeoPoint>) {
        points.forEachIndexed { index, point ->
            Marker(binding.mapPolygon).apply {
                position = point
                title = "Punto ${index + 1} (${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)})"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                binding.mapPolygon.overlays.add(this)
            }
        }
    }

    private fun drawPolygon(points: List<GeoPoint>) {
        Polygon().apply {
            fillPaint.color = Color.argb(70, 76, 175, 80)
            outlinePaint.color = Color.rgb(46, 125, 50)
            outlinePaint.strokeWidth = 5f
            setPoints(points + points.first())
            binding.mapPolygon.overlays.add(this)
        }
    }

    private fun drawDecorativeShape(points: List<GeoPoint>) {
        Polyline().apply {
            setPoints(points)
            color = Color.YELLOW
            width = 6f
            isGeodesic = true
            binding.mapPolygon.overlays.add(this)
        }
    }

    private fun zoomToPolygon(points: List<GeoPoint>) {
        if (points.size < 2) return

        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
        binding.mapPolygon.zoomToBoundingBox(boundingBox, true, 50)
    }

    private fun showPolygonSelectionDialog() {
        viewModel.poligonos.value?.let { poligonos ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar polígono")
                .setItems(poligonos.mapIndexed { index, poligono ->
                    "Polígono ${index + 1} - ${"%.2f".format(poligono.area)} m²"
                }.toTypedArray()) { _, which ->
                    viewModel.selectPoligono(poligonos[which])
                }
                .show()
        } ?: run {
            Toast.makeText(
                requireContext(),
                "Cargando polígonos...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapPolygon.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapPolygon.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapPolygon.onPause()
        _binding = null
    }
}