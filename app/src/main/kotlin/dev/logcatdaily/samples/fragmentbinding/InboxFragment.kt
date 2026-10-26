package dev.logcatdaily.samples.fragmentbinding

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dev.logcatdaily.samples.R
import dev.logcatdaily.samples.databinding.FragmentInboxBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

class InboxFragment : Fragment(R.layout.fragment_inbox) {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private val broken: Boolean
        get() = requireActivity().intent
            .getStringExtra("variant") != "fixed"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInboxBinding.bind(view)
        Log.d(TAG, "inbox view created")
        val scope =
            if (broken) lifecycleScope
            else viewLifecycleOwner.lifecycleScope
        scope.launch {
            try {
                delay(2000)
            } catch (e: CancellationException) {
                Log.d(TAG, "load cancelled with the view")
                throw e
            }
            Log.d(TAG, "load done, setting status")
            binding.status.text = "3 new messages"
        }
        binding.row1.setOnClickListener { open("Lunch on Friday?") }
        binding.row2.setOnClickListener { open("Your invoice is ready") }
        binding.row3.setOnClickListener { open("Standup moved to 10:30") }
    }

    private fun open(subject: String) {
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, MessageFragment.of(subject))
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "inbox view destroyed, binding = null")
    }
}
