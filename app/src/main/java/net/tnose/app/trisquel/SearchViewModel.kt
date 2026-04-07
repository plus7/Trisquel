package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val searchTags = MutableStateFlow<List<String>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val photosByAndQuery: StateFlow<List<Pair<Pair<String, Int>, PhotoAndRels>>> = searchTags
        .onEach { _isLoading.value = true }
        .flatMapLatest { tags ->
            repo.getPhotosByAndQuery(tags)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun update(entity: PhotoEntity) = viewModelScope.launch {
        repo.upsertPhoto(entity)
    }

    fun delete(id: Int) = viewModelScope.launch {
        repo.deletePhoto(id)
    }

    fun tagPhoto(photoId: Int, filmRollId: Int, tags: ArrayList<String>) = viewModelScope.launch {
        repo.tagPhoto(photoId, filmRollId, tags)
    }
}
