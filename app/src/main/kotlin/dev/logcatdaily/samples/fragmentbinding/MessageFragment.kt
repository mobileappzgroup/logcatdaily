package dev.logcatdaily.samples.fragmentbinding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dev.logcatdaily.samples.R
import dev.logcatdaily.samples.databinding.FragmentMessageBinding

class MessageFragment : Fragment(R.layout.fragment_message) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMessageBinding.bind(view)
        binding.subject.text = requireArguments().getString("subject")
    }

    companion object {
        fun of(subject: String) = MessageFragment().apply {
            arguments = Bundle().apply { putString("subject", subject) }
        }
    }
}
