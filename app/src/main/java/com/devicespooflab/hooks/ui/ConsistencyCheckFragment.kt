package com.devicespooflab.hooks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.devicespooflab.hooks.R
import com.devicespooflab.hooks.databinding.FragmentConsistencyCheckBinding
import kotlinx.coroutines.launch

class ConsistencyCheckFragment : Fragment() {
    private var _binding: FragmentConsistencyCheckBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConsistencyCheckViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConsistencyCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.refreshButton.setOnClickListener { runChecks() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.errorText.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    binding.errorText.text = state.error ?: ""
                    binding.resultsContainer.removeAllViews()
                    state.items.forEach { item ->
                        val tv = android.widget.TextView(requireContext())
                        tv.setPadding(0, 16, 0, 16)
                        val icon = when (item.status) {
                            CheckStatus.MATCH -> "🟢"
                            CheckStatus.HIDDEN -> "🟡"
                            CheckStatus.LEAKED -> "🔴"
                        }
                        tv.text = "$icon ${item.key}\nExpected: ${item.expected}\nActual: ${item.actual}"
                        val color = when (item.status) {
                            CheckStatus.MATCH -> R.color.dsl_success
                            CheckStatus.HIDDEN -> android.R.color.holo_orange_dark
                            CheckStatus.LEAKED -> android.R.color.holo_red_dark
                        }
                        tv.setTextColor(ContextCompat.getColor(requireContext(), color))
                        binding.resultsContainer.addView(tv)
                    }
                }
            }
        }
        runChecks()
    }

    private fun runChecks() {
        viewModel.runChecks(resources.displayMetrics)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
