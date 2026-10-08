package dev.logcatdaily.samples.newsfeed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeedGraph.repository(application)

    // cachedIn keeps the loaded pages across rotation.
    val feed = repository.feed().cachedIn(viewModelScope)
}
