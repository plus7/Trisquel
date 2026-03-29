package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TrisquelDao(application)
    
    private val _cameras = MutableLiveData<List<CameraSpec>>(emptyList())
    val cameras: LiveData<List<CameraSpec>> = _cameras

    private val _sortKey = MutableLiveData<Int>(0)

    init {
        load()
    }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        val list = dao.allCameras
        dao.close()
        withContext(Dispatchers.Main) {
            _cameras.value = sortList(list, _sortKey.value ?: 0)
        }
    }

    fun changeSortKey(key: Int) {
        _sortKey.value = key
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

    fun insertCamera(camera: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        dao.addCamera(camera)
        dao.close()
        load()
    }

    fun updateCamera(camera: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        dao.updateCamera(camera)
        dao.close()
        load()
    }

    fun deleteCamera(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        val c = dao.getCamera(id)
        if (c?.type == 1) {
            dao.deleteLens(dao.getFixedLensIdByBody(id))
        }
        dao.deleteCamera(id)
        dao.close()
        load()
    }
}
