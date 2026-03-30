package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LensViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TrisquelDao(application)
    
    private val _lenses = MutableLiveData<List<LensSpec>>(emptyList())
    val lenses: LiveData<List<LensSpec>> = _lenses

    private val _sortKey = MutableLiveData<Int>(0)

    init {
        load()
    }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        val list = dao.allVisibleLenses
        dao.close()
        withContext(Dispatchers.Main) {
            _lenses.value = sortList(list, _sortKey.value ?: 0)
        }
    }

    fun changeSortKey(key: Int) {
        _sortKey.value = key
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

    fun handleAddResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val l = androidx.core.os.BundleCompat.getParcelable(intent?.extras ?: return@launch, "lensspec", LensSpec::class.java)
        if (l != null) {
            dao.connection()
            dao.addLens(l)
            dao.close()
            load()
        }
    }

    fun handleEditResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val l = androidx.core.os.BundleCompat.getParcelable(intent?.extras ?: return@launch, "lensspec", LensSpec::class.java)
        if (l != null) {
            dao.connection()
            dao.updateLens(l)
            dao.close()
            load()
        }
    }

    fun insertLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        dao.addLens(lens)
        dao.close()
        load()
    }

    fun updateLens(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        dao.updateLens(lens)
        dao.close()
        load()
    }

    fun deleteLens(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        dao.deleteLens(id)
        dao.close()
        load()
    }
}
