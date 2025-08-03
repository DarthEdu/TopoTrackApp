package com.epdev.topotrackapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.epdev.topotrackapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        val textView2: TextView = binding.textView2
        val textView3: TextView = binding.textView3
        val textView4: TextView = binding.textView4

        homeViewModel.welcome.observe(viewLifecycleOwner) {
            textView.text = it
        }
        homeViewModel.description.observe(viewLifecycleOwner) {
            textView2.text = it
        }
        homeViewModel.tituloIn.observe(viewLifecycleOwner) {
            textView3.text = it
        }
        homeViewModel.integrantes.observe(viewLifecycleOwner) {
            textView4.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}