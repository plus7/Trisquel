package net.tnose.app.trisquel

import android.app.Application
import android.content.Intent
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

sealed class AccessoryEvent {
    data class ShowCannotDeleteAlert(val name: String) : AccessoryEvent()
    data class ShowDeleteConfirm(val id: Int, val name: String) : AccessoryEvent()
}

class AccessoryViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(
    application
) {
    private val mRepository: TrisquelRepo = TrisquelRepo(application)

    private val _events = MutableSharedFlow<AccessoryEvent>()
    val events = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val sortingRule = savedStateHandle.getStateFlow("sorting_rule", 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allAccessories: StateFlow<List<AccessoryEntity>> = sortingRule
        .onEach { _isLoading.value = true }
        .flatMapLatest { sortRule ->
            mRepository.getAllAccessoriesFlow(sortRule)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSortingRule(rule: Int) {
        savedStateHandle["sorting_rule"] = rule
    }

    fun handleAddResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val a = Accessory(0, Util.dateToStringUTC(Date()), Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
        mRepository.upsertAccessory(a.toEntity())
    }

    fun handleEditResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val a = Accessory(bundle.getInt("id"), bundle.getString("created")!!, Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
        mRepository.upsertAccessory(a.toEntity())
    }

    fun insert(entity: AccessoryEntity) = viewModelScope.launch {
        mRepository.upsertAccessory(entity)
    }

    fun update(entity: AccessoryEntity) = viewModelScope.launch {
        mRepository.upsertAccessory(entity)
    }

    fun delete(id : Int)  = viewModelScope.launch {
        mRepository.deleteAccessory(id)
    }

    fun requestDeleteAccessory(accessory: Accessory) = viewModelScope.launch(Dispatchers.IO) {
        val used = mRepository.isAccessoryUsed(accessory.id)
        if (used) {
            _events.emit(AccessoryEvent.ShowCannotDeleteAlert(accessory.name))
        } else {
            _events.emit(AccessoryEvent.ShowDeleteConfirm(accessory.id, accessory.name))
        }
    }
}
