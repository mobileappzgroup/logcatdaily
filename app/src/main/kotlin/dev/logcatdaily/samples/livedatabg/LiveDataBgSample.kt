package dev.logcatdaily.samples.livedatabg

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "logcatdaily"

class ItemsViewModel(private val variant: String) : ViewModel() {
    private val _items = MutableLiveData<List<String>>()
    val items: LiveData<List<String>> = _items

    private suspend fun load(): List<String> {
        delay(400)
        return listOf("Nishant", "Asha", "Ravi")
    }

    fun loadBroken() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = load()
            _items.value = result
        }
    }

    fun loadFixed() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = load()
            _items.postValue(result)
        }
    }

    fun loadFixedMain() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = load()
            withContext(Dispatchers.Main) {
                _items.value = result
            }
        }
    }

    fun onLoad() {
        Log.d(TAG, "load: tapped variant=$variant")
        when (variant) {
            "fixed" -> loadFixed()
            "fixed-main" -> loadFixedMain()
            else -> loadBroken()
        }
    }
}

@Composable
fun LiveDataBgSample(variant: String) {
    val vm: ItemsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = ItemsViewModel(variant) as T
    })
    val items by vm.items.observeAsState(emptyList())
    Log.d(TAG, "items shown: ${items.size} variant=$variant")
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Items", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            Text("Nothing loaded yet", fontSize = 26.sp, modifier = Modifier.padding(top = 32.dp))
        }
        items.forEach { Text(it, fontSize = 40.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = { vm.onLoad() },
            modifier = Modifier.padding(top = 40.dp).fillMaxWidth().height(88.dp),
        ) { Text("Load", fontSize = 28.sp) }
    }
}
