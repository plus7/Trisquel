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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LensEvent {
    data class ShowCannotDeleteAlert(val modelName: String) : LensEvent()
    data class ShowDeleteConfirm(val id: Int, val modelName: String) : LensEvent()
}

class LensViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)
    
    private val _events = MutableSharedFlow<LensEvent>()
    val events = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val sortKey = savedStateHandle.getStateFlow("sort_key", 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val lenses: StateFlow<List<LensSpec>> = combine(
        repo.getAllLensesFlow(),
        sortKey
    ) { entities, key ->
        val list = entities.filter { it.body == 0 }.map { LensSpec.fromEntity(it) }
        sortList(list, key)
    }
    .onEach { _isLoading.value = false }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun load() {
        _isLoading.value = true
    }

    fun changeSortKey(key: Int) {
        savedStateHandle["sort_key"] = key
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
        }
    }

    fun handleEditResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val l = BundleCompat.getParcelable(intent?.extras ?: return@launch, "lensspec", LensSpec::class.java)
        if (l != null) {
            repo.upsertLens(l.toEntity())
        }
    }

    fun insertLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertLens(lens.toEntity())
    }

    fun updateLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        repo.upsertLens(lens.toEntity())
    }

    fun deleteLens(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteLens(id)
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
