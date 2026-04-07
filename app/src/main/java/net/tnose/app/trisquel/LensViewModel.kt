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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class LensEvent {
    data class ShowCannotDeleteAlert(val modelName: String) : LensEvent()
    data class ShowDeleteConfirm(val id: Int, val modelName: String) : LensEvent()
}

class LensViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)
    
    private val _events = MutableSharedFlow<LensEvent>()
    val events = _events.asSharedFlow()

    private val _lenses = MutableLiveData<List<LensSpec>>(emptyList())
    val lenses: LiveData<List<LensSpec>> = _lenses

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
            val entities = repo.getAllLensesRaw()
            val list = entities.filter { it.body == 0 }.map { LensSpec.fromEntity(it) }
            withContext(Dispatchers.Main) {
                _lenses.value = sortList(list, _sortKey)
                _isLoading.value = false
            }
        }
    }

    fun changeSortKey(key: Int) {
        _sortKey = key
        _lenses.value = sortList(_lenses.value ?: emptyList(), key)
    }

    private fun sortList(list: List<LensSpec>, key: Int): List<LensSpec> {
        return when (key) {
            0 -> list.sortedByDescending { it.created }
            1 -> list.sortedBy { it.manufacturer + " " + it.modelName }
            2 -> list.sortedBy { it.mount }
            3 -> list.sortedBy { it.focalLengthRange.first }
            else -> list.toList()
        }
    }

    fun handleAddResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val l = BundleCompat.getParcelable(intent?.extras ?: return@launch, "lensspec", LensSpec::class.java)
        if (l != null) {
            repo.upsertLens(l.toEntity())
            load()
        }
    }

    fun handleEditResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val l = BundleCompat.getParcelable(intent?.extras ?: return@launch, "lensspec", LensSpec::class.java)
        if (l != null) {
            repo.upsertLens(l.toEntity())
            load()
        }
    }

    fun insertLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertLens(lens.toEntity())
        load()
    }

    fun updateLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertLens(lens.toEntity())
        load()
    }

    fun deleteLens(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteLens(id)
        load()
    }

    fun requestDeleteLens(item: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        val used = repo.isLensUsed(item.id)
        if (used) {
            _events.emit(LensEvent.ShowCannotDeleteAlert(item.modelName))
        } else {
            _events.emit(LensEvent.ShowDeleteConfirm(item.id, item.modelName))
        }
    }
}
