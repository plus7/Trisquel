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

class FilmRollViewModel(application: Application?) : AndroidViewModel(
    application!!
) {
    private val mRepository: TrisquelRepo

    init {
        mRepository = TrisquelRepo(application)
    }

    val viewRule = MutableLiveData<Pair<Int, Pair<Int, String>>>(Pair(0, Pair(0, "")))

    val allFilmRollAndRels: LiveData<List<FilmRollAndRels>> = viewRule.switchMap {
        val sortBy = it.first
        val filterByKind = it.second.first
        val filterByValue = it.second.second
        mRepository.getAllFilmRolls(sortBy, filterByKind, filterByValue)
    }

    fun handleAddResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val c = dao.getCamera(bundle.getInt("camera"))
        dao.close()
        val f = FilmRoll(0, bundle.getString("name")!!, c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
        mRepository.upsertFilmRoll(f.toEntity())
    }

    fun handleEditResult(intent: android.content.Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val c = dao.getCamera(bundle.getInt("camera"))
        dao.close()
        val f = FilmRoll(bundle.getInt("id"), bundle.getString("name")!!, bundle.getString("created")!!, Util.dateToStringUTC(Date()), c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
        mRepository.upsertFilmRoll(f.toEntity())
    }

    fun insert(entity: FilmRollEntity) = viewModelScope.launch {
        mRepository.upsertFilmRoll(entity)
    }

    // 意味ないけどViewModelの段階ではなんとなく分けておく
    fun update(entity: FilmRollEntity) = viewModelScope.launch {
        mRepository.upsertFilmRoll(entity)
    }

    fun refresh(id: Int) = viewModelScope.launch {
        val entity = mRepository.getFilmRoll(id)
        mRepository.upsertFilmRoll(entity.value!!)
    }

    fun delete(id : Int)  = viewModelScope.launch {
        mRepository.deleteFilmRoll(id)
    }
}