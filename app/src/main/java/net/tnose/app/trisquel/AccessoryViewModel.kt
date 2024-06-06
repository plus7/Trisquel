package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AccessoryViewModel(application: Application?) : AndroidViewModel(
    application!!
) {
    private val mRepository: TrisquelRepo
    //private val mAllAccessories: LiveData<List<AccessoryEntity>>

    init {
        mRepository = TrisquelRepo(application)
        //mAllAccessories = mRepository.getAllAccessories()
    }

    val sortingRule = MutableLiveData<Int>(0)

    val allAccessories: LiveData<List<AccessoryEntity>> = sortingRule.switchMap {
        val x = mRepository.getAllAccessories(it)
        x
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