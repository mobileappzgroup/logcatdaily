package dev.logcatdaily.samples.nav3results

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class Home(val picked: String? = null) : NavKey

@Serializable
data object Picker : NavKey
