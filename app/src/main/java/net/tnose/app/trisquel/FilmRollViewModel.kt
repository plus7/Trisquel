package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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