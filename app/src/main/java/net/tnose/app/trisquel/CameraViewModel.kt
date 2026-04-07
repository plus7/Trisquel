package net.tnose.app.trisquel

import android.app.Application
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CameraEvent {
    data class ShowCannotDeleteAlert(val modelName: String) : CameraEvent()
    data class ShowDeleteConfirm(val id: Int, val modelName: String) : CameraEvent()
}

class CameraViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)
    
    private val _events = MutableSharedFlow<CameraEvent>()
    val events = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val sortKey = savedStateHandle.getStateFlow("sort_key", 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val cameras: StateFlow<List<CameraSpec>> = combine(
        repo.getAllCamerasFlow(),
        sortKey
    ) { entities, key ->
        val list = entities.map { CameraSpec.fromEntity(it) }
        sortList(list, key)
    }
    .onEach { _isLoading.value = false }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun load() {
        // Now handled automatically by the cameras StateFlow
        _isLoading.value = true
    }

    fun changeSortKey(key: Int) {
        savedStateHandle["sort_key"] = key
    }

    private fun sortList(list: List<CameraSpec>, key: Int): List<CameraSpec> {
        return when (key) {
            0 -> list.sortedByDescending { it.created }
            1 -> list.sortedBy { it.manufacturer + " " + it.modelName }
            2 -> list.sortedBy { it.mount }
            3 -> list.sortedBy { it.format }
            else -> list.toList()
        }
    }

    fun deleteCamera(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        val entity = repo.getCamera(id)
        if (entity?.type == 1) {
            val fixedLens = repo.getLensByFixedBody(id)
            if (fixedLens != null) repo.deleteLens(fixedLens.id)
        }
        repo.deleteCamera(id)
    }

    fun requestDeleteCamera(item: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        val used = repo.isCameraUsed(item.id)
        if (used) {
            _events.emit(CameraEvent.ShowCannotDeleteAlert(item.modelName))
        } else {
            _events.emit(CameraEvent.ShowDeleteConfirm(item.id, item.modelName))
        }
    }
}
