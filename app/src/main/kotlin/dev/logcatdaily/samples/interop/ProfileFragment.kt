package dev.logcatdaily.samples.interop

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import dev.logcatdaily.samples.R
import dev.logcatdaily.samples.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val price = "$4.99"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProfileBinding.bind(view)
        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy
                    .DisposeOnViewTreeLifecycleDestroyed
            )
            setContent { PriceBadge(price) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
fun PriceBadge(price: String) {
    Text(text = price, modifier = Modifier.padding(top = 12.dp))
}
