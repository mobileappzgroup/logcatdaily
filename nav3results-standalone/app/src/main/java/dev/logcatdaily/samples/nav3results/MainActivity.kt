package dev.logcatdaily.samples.nav3results

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

private const val TAG = "logcatdaily"

// Variant A: the recipe pattern. The bus delivers, the ViewModel keeps it in memory.
class BusHomeViewModel : ViewModel() {
    var picked by mutableStateOf<String?>(null)
}

// Variant B2: the fix with SavedStateHandle.
class PickedViewModel(private val handle: SavedStateHandle) : ViewModel() {
    val picked: String? get() = handle["picked"]
    fun pick(name: String) { handle["picked"] = name }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val variant = intent.getStringExtra("variant") ?: "bus"
        Log.d(TAG, "onCreate variant=$variant restored=${savedInstanceState != null} pid=${android.os.Process.myPid()}")

        setContent {
            val backStack = rememberNavBackStack(Home())
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Home> { key ->
                        val shown: String? = when (variant) {
                            "bus" -> {
                                val vm = viewModel<BusHomeViewModel>()
                                ResultEffect<String> { vm.picked = it }
                                vm.picked
                            }
                            "bussaved" -> {
                                var picked by rememberSaveable { mutableStateOf<String?>(null) }
                                ResultEffect<String> { picked = it }
                                picked
                            }
                            "vm" -> viewModel<PickedViewModel>(viewModelStoreOwner = this@MainActivity).picked
                            else -> key.picked
                        }
                        Log.d(TAG, "Home shows picked=$shown")
                        HomeScreen(shown, onOpenPicker = { backStack.add(Picker) })
                    }
                    entry<Picker> {
                        val bus = LocalResultEventBus.current
                        val pickedVm = viewModel<PickedViewModel>(viewModelStoreOwner = this@MainActivity)
                        PickerScreen { name ->
                            when (variant) {
                                "bus", "bussaved" -> bus.sendResult<String>(name)
                                "vm" -> pickedVm.pick(name)
                                else -> backStack[0] = Home(name)
                            }
                            Log.d(TAG, "Picker set $name via $variant")
                            if (variant == "key") backStack.removeAt(backStack.lastIndex)
                            else backStack.removeLastOrNull()
                        }
                    }
                },
            )
        }
    }
}
