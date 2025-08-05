package com.epdev.topotrackapp.ui.polygon

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.epdev.topotrackapp.databinding.FragmentPolygonBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
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
        binding.mapPolygon.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 12.0
            maxZoomLevel = 21.0
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0
            )
        }
    }

    private fun setupUI() {
        binding.btnRefresh.setOnClickListener {
            viewModel.fetchPoligonos()
            binding.progressBar.visibility = View.VISIBLE
        }

        binding.btnSelectPolygon.setOnClickListener {
            showPolygonSelectionDialog()
        }

        binding.btnDeletePolygon.setOnClickListener {
            val poligono = viewModel.selectedPoligono.value
            if (poligono == null) {
                Toast.makeText(requireContext(), "No hay polígono seleccionado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar polígono")
                .setMessage("¿Estás seguro de que querés eliminar el terreno \"${poligono.terreno}\"?")
                .setPositiveButton("Eliminar") { _, _ ->
                    viewModel.deletePoligono(poligono.id)
                    Toast.makeText(requireContext(), "Polígono eliminado", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
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
            poligono?.let {
                if (isAdded && !isDetached) {
                    displayPolygon(it)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.poligonoEliminado.observe(viewLifecycleOwner) { eliminado ->
            if (eliminado) {
                binding.mapPolygon.overlays.clear()
                binding.mapPolygon.invalidate()
                binding.tvPolygonName.text = "Nombre del terreno"
                binding.tvPolygonId.text = ""
                binding.tvPolygonArea.text = ""
                binding.tvPolygonDate.text = ""
                viewModel.fetchPoligonos()
            }
        }
    }

    private fun displayPolygon(poligono: PolygonViewModel.Poligono) {
        binding.mapPolygon.overlays.clear()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val geoPoints = poligono.coordenadas.map { GeoPoint(it.lat, it.lon) }

            withContext(Dispatchers.Main) {
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
        }
    }

    private fun updatePolygonInfo(poligono: PolygonViewModel.Poligono) {
        binding.tvPolygonId.text = "ID: ${poligono.id.take(8)}..."
        binding.tvPolygonArea.text = "${"%.2f".format(poligono.area)} m²"
        binding.tvPolygonDate.text = "${poligono.fecha_creacion}"
        binding.tvPolygonName.text = "Terreno: ${poligono.terreno}"
    }

    private fun drawMarkers(points: List<GeoPoint>) {
        if (points.size > 50) { // Limitar marcadores si son demasiados
            points.filterIndexed { index, _ -> index % 5 == 0 }.forEach { point ->
                addMarker(point)
            }
            return
        }

        points.forEach { point ->
            addMarker(point)
        }
    }

    private fun addMarker(point: GeoPoint) {
        Marker(binding.mapPolygon).apply {
            position = point
            title = "Punto (${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)})"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            binding.mapPolygon.overlays.add(this)
        }
    }

    private fun drawPolygon(points: List<GeoPoint>) {
        val simplifiedPoints = if (points.size > 100) simplifyPoints(points) else points

        Polygon().apply {
            fillPaint.color = Color.argb(70, 76, 175, 80)
            outlinePaint.color = Color.rgb(46, 125, 50)
            outlinePaint.strokeWidth = 5f
            setPoints(simplifiedPoints + simplifiedPoints.first())
            binding.mapPolygon.overlays.add(this)
        }
    }

    private fun simplifyPoints(points: List<GeoPoint>): List<GeoPoint> {
        return if (points.size > 100) points.filterIndexed { index, _ -> index % 2 == 0 }
        else points
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

        val boundingBox = BoundingBox.fromGeoPoints(points)
        binding.mapPolygon.post {
            binding.mapPolygon.zoomToBoundingBox(boundingBox, true, 50)
        }
    }

    private fun showPolygonSelectionDialog() {
        viewModel.poligonos.value?.let { poligonos ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar polígono")
                .setItems(poligonos.map { p ->
                    "${p.terreno} - ${"%.2f".format(p.area)} m²"
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
        binding.mapPolygon.apply {
            overlays.clear()
            overlayManager.clear()
            onPause()
        }
        _binding = null
        super.onDestroyView()
    }
}
