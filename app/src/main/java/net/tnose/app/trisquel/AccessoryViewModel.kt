package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class AccessoryViewModel(application: Application?) : AndroidViewModel(
    application!!
) {
    private val mRepository: TrisquelRepo

    init {
        mRepository = TrisquelRepo(application)
    }

    val sortingRule = MutableLiveData<Int>(0)

    val allAccessories: LiveData<List<AccessoryEntity>> = sortingRule.switchMap {
        val x = mRepository.getAllAccessories(it)
        x
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
}