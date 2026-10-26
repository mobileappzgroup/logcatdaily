package dev.logcatdaily.samples.fragmentbinding

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import dev.logcatdaily.samples.R

// adb shell am start -n dev.logcatdaily.samples/.fragmentbinding.FragmentBindingActivity --es variant broken
// variant "broken" (default) launches the load in lifecycleScope, "fixed" in viewLifecycleOwner.lifecycleScope (InboxFragment).
class FragmentBindingActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_binding_host)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.fragmentContainer, InboxFragment())
            }
        }
    }
}
