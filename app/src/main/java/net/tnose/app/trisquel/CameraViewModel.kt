package net.tnose.app.trisquel

import android.app.Application
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _cameras = MutableLiveData<List<CameraSpec>>(emptyList())
    val cameras: LiveData<List<CameraSpec>> = _cameras

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var _sortKey: Int
        get() = savedStateHandle["sort_key"] ?: 0
        set(value) { savedStateHandle["sort_key"] = value }

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _isLoading.value = true
        withContext(Dispatchers.IO) {
            val entities = repo.getAllCamerasRaw()
            val list = entities.map { CameraSpec.fromEntity(it) }
            withContext(Dispatchers.Main) {
                _cameras.value = sortList(list, _sortKey)
                _isLoading.value = false
            }
        }
    }

    fun changeSortKey(key: Int) {
        _sortKey = key
        _cameras.value = sortList(_cameras.value ?: emptyList(), key)
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

    fun handleAddResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val c = BundleCompat.getParcelable(bundle, "cameraspec", CameraSpec::class.java) ?: return@launch
        val newId = repo.upsertCamera(c.toEntity())
        if (c.type == 1) {
            val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)
            if (l != null) {
                l.body = newId.toInt()
                repo.upsertLens(l.toEntity())
            }
        }
        load()
    }

    fun handleEditResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val c = BundleCompat.getParcelable(bundle, "cameraspec", CameraSpec::class.java) ?: return@launch
        repo.upsertCamera(c.toEntity())
        if (c.type == 1) {
            val lensEntity = repo.getLensByFixedBody(c.id)
            val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)
            if (l != null) {
                l.id = lensEntity?.id ?: 0
                l.body = c.id
                repo.upsertLens(l.toEntity())
            }
        }
        load()
    }

    fun insertCamera(camera: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertCamera(camera.toEntity())
        load()
    }

    fun updateCamera(camera: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertCamera(camera.toEntity())
        load()
    }

    fun deleteCamera(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        val entity = repo.getCamera(id)
        if (entity?.type == 1) {
            val fixedLens = repo.getLensByFixedBody(id)
            if (fixedLens != null) repo.deleteLens(fixedLens.id)
        }
        repo.deleteCamera(id)
        load()
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
