package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Date

sealed class AccessoryEvent {
    data class ShowCannotDeleteAlert(val name: String) : AccessoryEvent()
    data class ShowDeleteConfirm(val id: Int, val name: String) : AccessoryEvent()
}

class AccessoryViewModel(application: Application) : AndroidViewModel(
    application
) {
    private val mRepository: TrisquelRepo

    private val _events = MutableSharedFlow<AccessoryEvent>()
    val events = _events.asSharedFlow()

    init {
        mRepository = TrisquelRepo(application)
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val sortingRule = MutableLiveData<Int>(0)

    val allAccessories: LiveData<List<AccessoryEntity>> = sortingRule.switchMap {
        _isLoading.value = true
        mRepository.getAllAccessories(it)
    }.map {
        _isLoading.value = false
        it
    }

    fun handleAddResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val a = Accessory(0, Util.dateToStringUTC(Date()), Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
        mRepository.upsertAccessory(a.toEntity())
    }

    fun handleEditResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val a = Accessory(bundle.getInt("id"), bundle.getString("created")!!, Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
        mRepository.upsertAccessory(a.toEntity())
    }

    fun insert(entity: AccessoryEntity) = viewModelScope.launch {
        mRepository.upsertAccessory(entity)
    }

    // 意味ないけどViewModelの段階ではなんとなく分けておく
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